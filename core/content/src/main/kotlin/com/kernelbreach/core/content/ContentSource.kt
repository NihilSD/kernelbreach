package com.kernelbreach.core.content

/**
 * Supplies the raw JSON for the content pipeline. Implementations read from
 * Android assets, the classpath, or the filesystem — the pipeline doesn't care.
 */
interface ContentSource {
    /** Raw JSON of `content/plans/curriculum.json`. */
    fun readCurriculum(): String

    /** Raw JSON of `content/plans/<pathKey>.json`, or null if there is no plan. */
    fun readPlan(pathKey: String): String?

    /** Raw JSON of `content/lessons/<moduleCode>.json`, or null if unauthored. */
    fun readLesson(moduleCode: String): String?
}

/** A trivial in-memory source, handy for tests and previews. */
class MapContentSource(
    private val curriculum: String,
    private val plans: Map<String, String> = emptyMap(),
    private val lessons: Map<String, String> = emptyMap(),
) : ContentSource {
    override fun readCurriculum(): String = curriculum
    override fun readPlan(pathKey: String): String? = plans[pathKey]
    override fun readLesson(moduleCode: String): String? = lessons[moduleCode]
}
