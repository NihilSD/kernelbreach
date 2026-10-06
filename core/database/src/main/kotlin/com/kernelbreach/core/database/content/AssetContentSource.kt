package com.kernelbreach.core.database.content

import android.content.Context
import com.kernelbreach.core.content.ContentSource
import java.io.FileNotFoundException

/**
 * Reads the bundled JSON from the app's `assets/content/...` directory. The
 * content files are synced into assets from the repo-root `content/` folder by a
 * Gradle task in the :app module (single source of truth, no committed copy).
 */
class AssetContentSource(
    private val context: Context,
    private val root: String = "content",
) : ContentSource {

    override fun readCurriculum(): String = read("$root/plans/curriculum.json")!!

    override fun readPlan(pathKey: String): String? = readOrNull("$root/plans/$pathKey.json")

    override fun readLesson(moduleCode: String): String? = readOrNull("$root/lessons/$moduleCode.json")

    private fun read(path: String): String? =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun readOrNull(path: String): String? = try {
        read(path)
    } catch (e: FileNotFoundException) {
        null
    }
}
