package com.kernelbreach.core.model

/**
 * Pure-Kotlin domain model for Kernel Breach.
 *
 * These types are free of Android and serialization annotations on purpose: the
 * content layer parses JSON into its own DTOs and maps them onto these models,
 * the database layer maps Room entities onto these models, and the UI consumes
 * only these. That keeps the domain portable (KMP-ready) and stable while the
 * wire format and persistence evolve.
 *
 * The curriculum is intentionally data-driven: nothing here hard-codes the set
 * of paths. New paths are added as JSON content, not code.
 */

/** Difficulty level of a module. Wire values are the single letters B / I / A. */
enum class Level(val wire: String, val displayName: String) {
    BEGINNER("B", "Beginner"),
    INTERMEDIATE("I", "Intermediate"),
    ADVANCED("A", "Advanced"),
    ;

    companion object {
        fun fromWire(value: String): Level =
            entries.firstOrNull { it.wire.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown level '$value' (expected B, I or A)")
    }
}

/**
 * The simulator a lab renders in. The set is closed in v1; adding a new one is a
 * code change (a new renderer), so unknown values are rejected at import time
 * rather than silently shown as a generic panel.
 */
enum class SimType(val wire: String) {
    TERMINAL("terminal"),
    LOG_VIEWER("log-viewer"),
    PACKET_VIEWER("packet-viewer"),
    SIEM("siem"),
    WEB_PROXY("web-proxy"),
    CODE_EDITOR("code-editor"),
    CONFIG_EDITOR("config-editor"),
    FILE_EXPLORER("file-explorer"),
    DIAGRAM_BUILDER("diagram-builder"),
    SCENARIO("scenario"),
    CHAT_SIM("chat-sim"),
    ;

    companion object {
        private val byWire = entries.associateBy { it.wire }

        fun fromWireOrNull(value: String): SimType? = byWire[value.trim()]

        fun fromWire(value: String): SimType =
            fromWireOrNull(value)
                ?: throw IllegalArgumentException("Unknown simulator type '$value'")
    }
}

/** A single paragraph-group inside a lesson: a heading and its body paragraphs. */
data class Section(
    val heading: String,
    val paragraphs: List<String>,
)

/** A monospace snippet shown in a lesson (terminology table, command, config, …). */
data class Snippet(
    val label: String,
    val text: String,
)

/** A glossary term and its definition. Deduped by lowercase term in the library. */
data class Term(
    val term: String,
    val definition: String,
)

/**
 * A multiple-choice question. [answerIndex] is a 0-based index into [options]
 * (always four). [explanation] is shown after the learner answers.
 */
data class Question(
    val prompt: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
) {
    init {
        require(options.size == OPTION_COUNT) {
            "A question must have exactly $OPTION_COUNT options, found ${options.size}"
        }
        require(answerIndex in options.indices) {
            "answerIndex $answerIndex out of range for ${options.size} options"
        }
    }

    val answer: String get() = options[answerIndex]

    companion object {
        const val OPTION_COUNT = 4
    }
}

/**
 * One lesson (~15 min). [id] is the stable content id `CODE#L<n>` (1-based) used
 * to match user progress across content re-imports.
 */
data class Lesson(
    val id: String,
    val moduleCode: String,
    val index: Int,
    val title: String,
    val why: String,
    val sections: List<Section>,
    val snippet: Snippet?,
    val example: String?,
    val terms: List<Term>,
    val check: List<Question>,
    val recap: List<String>,
)

/** A single guided-script step in a lab: what to do, and the output it reveals. */
data class LabStep(
    val instruction: String,
    val output: String,
    /** Reserved for a later version that validates learner input. Null in v1. */
    val expect: String? = null,
)

/**
 * A simulated lab. [id] is the stable content id `CODE#LAB<n>` (1-based).
 * Labs are not scored in v1; completion is what counts toward progress.
 */
data class Lab(
    val id: String,
    val moduleCode: String,
    val index: Int,
    val title: String,
    val sim: SimType,
    val scenario: String,
    val objective: String,
    val steps: List<LabStep>,
    val answer: String,
    val hint: String,
    /** Reserved for a later version with structured answer choices. */
    val answerOptions: List<String>? = null,
)

/**
 * A module (e.g. ROOK-03). Content modules carry lessons, labs and a 10-question
 * checkpoint. Modules without authored content yet (the PEN path in v1) carry
 * only their curriculum metadata and render as "Coming soon".
 */
data class Module(
    val code: String,
    val pathKey: String,
    val order: Int,
    val title: String,
    val topics: String,
    val level: Level,
    val stageTitle: String,
    val canDo: List<String>,
    val lessons: List<Lesson>,
    val labs: List<Lab>,
    val checkpoint: List<Question>,
    val checkpointFocus: String?,
    /** AI-drafted content is not human-reviewed; the dev screen surfaces this. */
    val reviewed: Boolean,
) {
    /** True once lessons/checkpoint have been authored for this module. */
    val hasContent: Boolean get() = lessons.isNotEmpty()
}

/** The path-level capstone project. */
data class Capstone(
    val title: String,
    val description: String,
)

/**
 * A learning path (ROOK, CORE, …). [modules] are in curriculum order; [stages]
 * is a derived grouping of those modules by their [Module.stageTitle].
 */
data class LearningPath(
    val key: String,
    val name: String,
    val tagline: String,
    val bestFor: String,
    val needs: String,
    val align: String,
    /** Hex colour from the curriculum, e.g. "#0D9488". */
    val color: String,
    val modules: List<Module>,
    val capstone: Capstone?,
    val certNote: String?,
) {
    val stages: List<Stage>
        get() = modules
            .groupBy { it.stageTitle }
            .entries
            .mapIndexed { index, (title, mods) -> Stage(title, index, mods) }

    val hasAnyContent: Boolean get() = modules.any { it.hasContent }
}

/** A derived grouping of consecutive modules that share a stage title. */
data class Stage(
    val title: String,
    val order: Int,
    val modules: List<Module>,
)
