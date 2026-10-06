package com.kernelbreach.core.database.content

import com.kernelbreach.core.database.entity.LabEntity
import com.kernelbreach.core.database.entity.LessonEntity
import com.kernelbreach.core.database.entity.ModuleEntity
import com.kernelbreach.core.database.entity.PathEntity
import com.kernelbreach.core.database.entity.QuizQuestionEntity
import com.kernelbreach.core.database.entity.SectionRecord
import com.kernelbreach.core.database.entity.StepRecord
import com.kernelbreach.core.database.entity.TermEntity
import com.kernelbreach.core.database.entity.TermRecord
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

/** All content rows for one import, ready to insert. */
data class ContentEntities(
    val paths: List<PathEntity>,
    val modules: List<ModuleEntity>,
    val lessons: List<LessonEntity>,
    val labs: List<LabEntity>,
    val questions: List<QuizQuestionEntity>,
    val terms: List<TermEntity>,
)

// --- domain -> entities (for import) ----------------------------------------

fun Curriculum.toEntities(): ContentEntities {
    val paths = mutableListOf<PathEntity>()
    val modules = mutableListOf<ModuleEntity>()
    val lessons = mutableListOf<LessonEntity>()
    val labs = mutableListOf<LabEntity>()
    val questions = mutableListOf<QuizQuestionEntity>()
    val terms = linkedMapOf<String, TermEntity>() // deduped by lowercase term

    this.paths.forEachIndexed { pathOrder, path ->
        paths += PathEntity(
            key = path.key,
            name = path.name,
            tagline = path.tagline,
            bestFor = path.bestFor,
            needs = path.needs,
            align = path.align,
            color = path.color,
            order = pathOrder,
            capstoneTitle = path.capstone?.title,
            capstoneDesc = path.capstone?.description,
            certNote = path.certNote,
        )
        path.modules.forEach { module ->
            modules += module.toEntity()
            module.lessons.forEach { lesson ->
                lessons += lesson.toEntity()
                lesson.check.forEachIndexed { i, q ->
                    questions += q.toEntity(
                        id = "${lesson.id}#Q$i",
                        moduleCode = module.code,
                        ownerId = lesson.id,
                        kind = "CHECK",
                        order = i,
                    )
                }
                lesson.terms.forEach { t ->
                    val key = t.term.trim().lowercase()
                    if (key.isNotEmpty() && key !in terms) {
                        terms[key] = TermEntity(
                            termLower = key,
                            term = t.term.trim(),
                            definition = t.definition,
                            moduleCode = module.code,
                        )
                    }
                }
            }
            module.labs.forEach { lab -> labs += lab.toEntity() }
            module.checkpoint.forEachIndexed { i, q ->
                questions += q.toEntity(
                    id = "${module.code}#CP$i",
                    moduleCode = module.code,
                    ownerId = module.code,
                    kind = "CHECKPOINT",
                    order = i,
                )
            }
        }
    }
    return ContentEntities(paths, modules, lessons, labs, questions, terms.values.toList())
}

private fun Module.toEntity() = ModuleEntity(
    code = code,
    pathKey = pathKey,
    order = order,
    title = title,
    topics = topics,
    level = level.wire,
    stageTitle = stageTitle,
    checkpointFocus = checkpointFocus,
    canDo = canDo,
    reviewed = reviewed,
    hasContent = hasContent,
)

private fun Lesson.toEntity() = LessonEntity(
    id = id,
    moduleCode = moduleCode,
    orderIndex = index,
    title = title,
    why = why,
    sections = sections.map { SectionRecord(it.heading, it.paragraphs) },
    snippetLabel = snippet?.label,
    snippetText = snippet?.text,
    example = example,
    terms = terms.map { TermRecord(it.term, it.definition) },
    recap = recap,
)

private fun Lab.toEntity() = LabEntity(
    id = id,
    moduleCode = moduleCode,
    orderIndex = index,
    title = title,
    sim = sim.wire,
    scenario = scenario,
    objective = objective,
    steps = steps.map { StepRecord(it.instruction, it.output, it.expect) },
    answer = answer,
    hint = hint,
)

private fun Question.toEntity(id: String, moduleCode: String, ownerId: String, kind: String, order: Int) =
    QuizQuestionEntity(
        id = id,
        moduleCode = moduleCode,
        ownerId = ownerId,
        kind = kind,
        orderIndex = order,
        prompt = prompt,
        options = options,
        answerIndex = answerIndex,
        why = explanation,
    )

// --- entities -> domain (for reads) -----------------------------------------

fun buildCurriculum(
    paths: List<PathEntity>,
    modules: List<ModuleEntity>,
    lessonsByModule: Map<String, List<LessonEntity>>,
    labsByModule: Map<String, List<LabEntity>>,
    questionsByOwner: Map<String, List<QuizQuestionEntity>>,
): Curriculum {
    val modulesByPath = modules.groupBy { it.pathKey }
    val learningPaths = paths.sortedBy { it.order }.map { p ->
        LearningPath(
            key = p.key,
            name = p.name,
            tagline = p.tagline,
            bestFor = p.bestFor,
            needs = p.needs,
            align = p.align,
            color = p.color,
            modules = modulesByPath[p.key].orEmpty().sortedBy { it.order }.map { m ->
                m.toDomain(
                    lessons = lessonsByModule[m.code].orEmpty(),
                    labs = labsByModule[m.code].orEmpty(),
                    questionsByOwner = questionsByOwner,
                )
            },
            capstone = p.capstoneTitle?.let { Capstone(it, p.capstoneDesc.orEmpty()) },
            certNote = p.certNote,
        )
    }
    return Curriculum(learningPaths)
}

private fun ModuleEntity.toDomain(
    lessons: List<LessonEntity>,
    labs: List<LabEntity>,
    questionsByOwner: Map<String, List<QuizQuestionEntity>>,
): Module = Module(
    code = code,
    pathKey = pathKey,
    order = order,
    title = title,
    topics = topics,
    level = Level.fromWire(level),
    stageTitle = stageTitle,
    canDo = canDo,
    lessons = lessons.sortedBy { it.orderIndex }.map { it.toDomain(questionsByOwner[it.id].orEmpty()) },
    labs = labs.sortedBy { it.orderIndex }.map { it.toDomain() },
    checkpoint = questionsByOwner[code].orEmpty().sortedBy { it.orderIndex }.map { it.toDomain() },
    checkpointFocus = checkpointFocus,
    reviewed = reviewed,
)

fun LessonEntity.toDomain(questions: List<QuizQuestionEntity>): Lesson = Lesson(
    id = id,
    moduleCode = moduleCode,
    index = orderIndex,
    title = title,
    why = why,
    sections = sections.map { Section(it.h, it.p) },
    snippet = if (snippetLabel != null || snippetText != null) {
        Snippet(snippetLabel.orEmpty(), snippetText.orEmpty())
    } else {
        null
    },
    example = example,
    terms = terms.map { Term(it.term, it.def) },
    check = questions.sortedBy { it.orderIndex }.map { it.toDomain() },
    recap = recap,
)

fun LabEntity.toDomain(): Lab = Lab(
    id = id,
    moduleCode = moduleCode,
    index = orderIndex,
    title = title,
    sim = SimType.fromWire(sim),
    scenario = scenario,
    objective = objective,
    steps = steps.map { LabStep(it.instruction, it.output, it.expect) },
    answer = answer,
    hint = hint,
)

fun QuizQuestionEntity.toDomain(): Question = Question(
    prompt = prompt,
    options = options,
    answerIndex = answerIndex,
    explanation = why,
)

fun TermEntity.toDomain(): Term = Term(term, definition)
