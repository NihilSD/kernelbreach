package com.kernelbreach.core.content

import java.io.File

/**
 * Reads the real bundled content from a directory on disk (the repo's `content/`
 * folder). The directory is supplied by the `kb.contentDir` system property, set
 * by the module's Gradle `test` task.
 */
class FileContentSource(private val contentDir: File) : ContentSource {

    init {
        require(contentDir.isDirectory) { "content dir not found: $contentDir" }
    }

    override fun readCurriculum(): String =
        File(contentDir, "plans/curriculum.json").readText()

    override fun readPlan(pathKey: String): String? =
        File(contentDir, "plans/$pathKey.json").takeIf { it.isFile }?.readText()

    override fun readLesson(moduleCode: String): String? =
        File(contentDir, "lessons/$moduleCode.json").takeIf { it.isFile }?.readText()

    companion object {
        fun fromSystemProperty(): FileContentSource {
            val dir = System.getProperty("kb.contentDir")
                ?: error("kb.contentDir system property not set")
            return FileContentSource(File(dir))
        }
    }
}
