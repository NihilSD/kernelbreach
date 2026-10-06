package com.kernelbreach.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serialization DTOs that mirror the bundled JSON exactly. They are deliberately
 * separate from the domain model: parsing is lenient about unknown fields (so
 * later additions like `simple`/`analogy` reading depths don't break old
 * builds), while the domain stays clean. Wire names that are Kotlin keywords or
 * terse keys (`a`, `n`, `do`, `can_do`) are mapped with @SerialName.
 */

// ---------------------------------------------------------------------------
// curriculum.json — a list of paths with their module metadata.
// ---------------------------------------------------------------------------

@Serializable
data class PathDto(
    val key: String,
    val name: String,
    val tagline: String = "",
    val best: String = "",
    val needs: String = "",
    val align: String = "",
    val color: String = "#101B33",
    val mods: List<ModDto> = emptyList(),
)

@Serializable
data class ModDto(
    val code: String,
    val title: String,
    val topics: String = "",
    val n: Int,
    val lvl: String,
    val stage: String = "",
)

// ---------------------------------------------------------------------------
// content/plans/<KEY>.json — per-module outcomes, lesson/lab plans, capstone.
// ---------------------------------------------------------------------------

@Serializable
data class PlanDto(
    val key: String,
    val capstone: CapstoneDto? = null,
    @SerialName("cert_note") val certNote: String? = null,
    val modules: List<PlanModuleDto> = emptyList(),
)

@Serializable
data class CapstoneDto(
    val title: String,
    val desc: String,
)

@Serializable
data class PlanModuleDto(
    val code: String,
    @SerialName("can_do") val canDo: List<String> = emptyList(),
    val lessons: List<PlanLessonDto> = emptyList(),
    val labs: List<PlanLabDto> = emptyList(),
    val checkpoint: PlanCheckpointDto? = null,
)

@Serializable
data class PlanLessonDto(
    val title: String,
    val goal: String = "",
)

@Serializable
data class PlanLabDto(
    val title: String,
    val sim: String,
    val task: String = "",
)

@Serializable
data class PlanCheckpointDto(
    val focus: String = "",
)

// ---------------------------------------------------------------------------
// content/lessons/<CODE>.json — the authored lesson content for one module.
// ---------------------------------------------------------------------------

@Serializable
data class LessonFileDto(
    val code: String,
    val intro: String = "",
    val lessons: List<LessonDto> = emptyList(),
    val labs: List<LabDto> = emptyList(),
    val checkpoint: List<QuestionDto> = emptyList(),
)

@Serializable
data class LessonDto(
    val title: String,
    val why: String = "",
    val sections: List<SectionDto> = emptyList(),
    val snippet: SnippetDto? = null,
    val example: String? = null,
    val terms: List<TermDto> = emptyList(),
    val check: List<QuestionDto> = emptyList(),
    val recap: List<String> = emptyList(),
    // Reserved for later reading depths; parsed if present, ignored otherwise.
    val simple: List<SectionDto>? = null,
    val analogy: List<SectionDto>? = null,
)

@Serializable
data class SectionDto(
    val h: String = "",
    val p: List<String> = emptyList(),
)

@Serializable
data class SnippetDto(
    val label: String = "",
    val text: String = "",
)

@Serializable
data class TermDto(
    val term: String,
    val def: String,
)

@Serializable
data class QuestionDto(
    val q: String,
    val options: List<String> = emptyList(),
    val a: Int,
    val why: String = "",
)

@Serializable
data class LabDto(
    val title: String,
    val sim: String,
    val scenario: String = "",
    val objective: String = "",
    val steps: List<StepDto> = emptyList(),
    val answer: String = "",
    val hint: String = "",
    // Reserved for a later version with structured answers.
    @SerialName("answer_options") val answerOptions: List<String>? = null,
)

@Serializable
data class StepDto(
    @SerialName("do") val instruction: String = "",
    val see: String = "",
    // Reserved for later real-command validation.
    val expect: String? = null,
)
