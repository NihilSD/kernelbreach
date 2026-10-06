package com.kernelbreach.core.content

import com.kernelbreach.core.model.Capstone
import com.kernelbreach.core.model.Curriculum
import com.kernelbreach.core.model.Lab
import com.kernelbreach.core.model.LabStep
import com.kernelbreach.core.model.LearningPath
import com.kernelbreach.core.model.Lesson
import com.kernelbreach.core.model.Level
import com.kernelbreach.core.model.Module
import com.kernelbreach.core.model.Question
import com.kernelbreach.core.model.Section
import com.kernelbreach.core.model.SimType
import com.kernelbreach.core.model.Snippet
import com.kernelbreach.core.model.Term

/**
 * Reads the bundled content from a [ContentSource], validates it against the
 * rules in the spec, and assembles the in-memory [Curriculum]. Also computes the
 * content hash used for idempotent re-import.
 *
 * Behaviour matches the spec: structural problems are reported as [Severity.ERROR]
 * and the offending module is assembled *without* content (so callers can fail
 * loudly in debug or skip-with-log in release); softer problems (plan/file drift,
 * empty sections) are [Severity.WARNING]; unauthored modules (PEN in v1) are
 * [Severity.INFO] "coming soon".
 */
class ContentAssembler(
    private val source: ContentSource,
    /** Module codes marked human-reviewed. Everything else defaults to false. */
    private val reviewedCodes: Set<String> = emptySet(),
) {
    private val issues = mutableListOf<ValidationIssue>()
    private val hashParts = linkedMapOf<String, String>()

    fun assemble(): ImportResult {
        issues.clear()
        hashParts.clear()

        val curriculumRaw = source.readCurriculum()
        hashParts["curriculum"] = curriculumRaw
        val pathDtos = ContentJson.decodeFromString<List<PathDto>>(curriculumRaw)

        val paths = pathDtos.map { pathDto -> buildPath(pathDto) }

        return ImportResult(
            curriculum = Curriculum(paths),
            issues = issues.toList(),
            contentHash = ContentHash.of(hashParts),
        )
    }

    private fun buildPath(pathDto: PathDto): LearningPath {
        val planDto = source.readPlan(pathDto.key)?.let { raw ->
            hashParts["plan/${pathDto.key}"] = raw
            runCatching { ContentJson.decodeFromString<PlanDto>(raw) }
                .onFailure { error(pathDto.key, "plan JSON failed to parse: ${it.message}") }
                .getOrNull()
        }
        if (planDto != null && planDto.key != pathDto.key) {
            warn(pathDto.key, "plan key '${planDto.key}' != curriculum key '${pathDto.key}'")
        }
        val planByCode = planDto?.modules?.associateBy { it.code }.orEmpty()

        val modules = pathDto.mods.mapIndexed { index, mod ->
            buildModule(pathDto.key, index, mod, planByCode[mod.code])
        }

        return LearningPath(
            key = pathDto.key,
            name = pathDto.name,
            tagline = pathDto.tagline,
            bestFor = pathDto.best,
            needs = pathDto.needs,
            align = pathDto.align,
            color = pathDto.color,
            modules = modules,
            capstone = planDto?.capstone?.let { Capstone(it.title, it.desc) },
            certNote = planDto?.certNote,
        )
    }

    private fun buildModule(
        pathKey: String,
        order: Int,
        mod: ModDto,
        plan: PlanModuleDto?,
    ): Module {
        val code = mod.code
        val level = runCatching { Level.fromWire(mod.lvl) }
            .onFailure { error(code, "invalid level '${mod.lvl}'") }
            .getOrDefault(Level.BEGINNER)

        val lessonRaw = source.readLesson(code)
        val baseModule = Module(
            code = code,
            pathKey = pathKey,
            order = order,
            title = mod.title,
            topics = mod.topics,
            level = level,
            stageTitle = mod.stage,
            canDo = plan?.canDo.orEmpty(),
            lessons = emptyList(),
            labs = emptyList(),
            checkpoint = emptyList(),
            checkpointFocus = plan?.checkpoint?.focus?.takeIf { it.isNotBlank() },
            reviewed = code in reviewedCodes,
        )

        if (lessonRaw == null) {
            info(code, "no authored content yet — will render as \"Coming soon\"")
            return baseModule
        }
        hashParts["lesson/$code"] = lessonRaw

        val file = runCatching { ContentJson.decodeFromString<LessonFileDto>(lessonRaw) }
            .onFailure { error(code, "lesson JSON failed to parse: ${it.message}") }
            .getOrNull() ?: return baseModule

        // --- structural validation; any ERROR leaves the module content-less ---
        var ok = true
        if (file.code != code) {
            error(code, "lesson file code '${file.code}' != module code '$code'")
            ok = false
        }
        if (file.lessons.size != mod.n) {
            error(code, "curriculum n=${mod.n} but file has ${file.lessons.size} lessons")
            ok = false
        }
        if (file.checkpoint.size != CHECKPOINT_SIZE) {
            error(code, "checkpoint must have $CHECKPOINT_SIZE questions, found ${file.checkpoint.size}")
            ok = false
        }
        file.checkpoint.forEachIndexed { i, q -> ok = validateQuestion(code, "checkpoint[$i]", q) && ok }
        file.lessons.forEachIndexed { i, l ->
            if (l.check.size != CHECK_PER_LESSON) {
                error(code, "lesson ${i + 1} '${l.title}' must have $CHECK_PER_LESSON check questions, found ${l.check.size}")
                ok = false
            }
            l.check.forEachIndexed { j, q -> ok = validateQuestion(code, "L${i + 1}.check[$j]", q) && ok }
            if (l.sections.isEmpty()) warn(code, "lesson ${i + 1} '${l.title}' has no sections")
            if (l.terms.isEmpty()) warn(code, "lesson ${i + 1} '${l.title}' has no glossary terms")
        }
        file.labs.forEach { lab ->
            if (SimType.fromWireOrNull(lab.sim) == null) {
                error(code, "lab '${lab.title}' has unknown sim '${lab.sim}'")
                ok = false
            }
        }

        // --- soft plan/file consistency checks (warnings only) ---
        if (plan == null) {
            warn(code, "no plan entry; module outcomes (can_do) are missing")
        } else {
            val planTitles = plan.lessons.map { it.title }
            val fileTitles = file.lessons.map { it.title }
            if (planTitles.isNotEmpty() && planTitles != fileTitles) {
                warn(code, "lesson titles differ between plan and content file")
            }
            if (plan.labs.isNotEmpty() && plan.labs.size != file.labs.size) {
                warn(code, "plan has ${plan.labs.size} labs but file has ${file.labs.size}")
            }
        }

        if (!ok) return baseModule

        val lessons = file.lessons.mapIndexed { i, dto -> dto.toDomain(code, i + 1) }
        val labs = file.labs.mapIndexed { i, dto -> dto.toDomain(code, i + 1) }
        val checkpoint = file.checkpoint.map { it.toDomain() }

        return baseModule.copy(lessons = lessons, labs = labs, checkpoint = checkpoint)
    }

    private fun validateQuestion(code: String, where: String, q: QuestionDto): Boolean {
        var ok = true
        if (q.options.size != Question.OPTION_COUNT) {
            error(code, "$where must have ${Question.OPTION_COUNT} options, found ${q.options.size}")
            ok = false
        }
        if (q.a !in 0 until maxOf(q.options.size, 1) || q.a !in 0..3) {
            error(code, "$where answer index ${q.a} out of range 0..3")
            ok = false
        }
        return ok
    }

    private fun error(scope: String, message: String) = issues.add(ValidationIssue(Severity.ERROR, scope, message))
    private fun warn(scope: String, message: String) = issues.add(ValidationIssue(Severity.WARNING, scope, message))
    private fun info(scope: String, message: String) = issues.add(ValidationIssue(Severity.INFO, scope, message))

    companion object {
        const val CHECK_PER_LESSON = 4
        const val CHECKPOINT_SIZE = 10
    }
}

// --- DTO -> domain mapping ---------------------------------------------------

private fun QuestionDto.toDomain() = Question(
    prompt = q,
    options = options,
    answerIndex = a,
    explanation = why,
)

private fun LessonDto.toDomain(moduleCode: String, oneBasedIndex: Int) = Lesson(
    id = "$moduleCode#L$oneBasedIndex",
    moduleCode = moduleCode,
    index = oneBasedIndex,
    title = title,
    why = why,
    sections = sections.map { Section(it.h, it.p) },
    snippet = snippet?.let { Snippet(it.label, it.text) },
    example = example,
    terms = terms.map { Term(it.term, it.def) },
    check = check.map { it.toDomain() },
    recap = recap,
)

private fun LabDto.toDomain(moduleCode: String, oneBasedIndex: Int) = Lab(
    id = "$moduleCode#LAB$oneBasedIndex",
    moduleCode = moduleCode,
    index = oneBasedIndex,
    title = title,
    sim = SimType.fromWire(sim),
    scenario = scenario,
    objective = objective,
    steps = steps.map { LabStep(instruction = it.instruction, output = it.see, expect = it.expect) },
    answer = answer,
    hint = hint,
    answerOptions = answerOptions,
)
