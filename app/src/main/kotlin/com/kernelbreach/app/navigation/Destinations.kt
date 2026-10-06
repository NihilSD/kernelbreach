package com.kernelbreach.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigation routes. String-based for v1 (simple + stable). */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MAP = "map"
    const val REFRESH = "refresh"
    const val LIBRARY = "library"
    const val ME = "me"
    const val DEV_UNREVIEWED = "dev/unreviewed"

    const val MODULE = "module/{code}"
    fun module(code: String) = "module/$code"

    const val LESSON = "lesson/{lessonId}"
    fun lesson(lessonId: String) = "lesson/${encodeId(lessonId)}"

    const val LAB = "lab/{labId}"
    fun lab(labId: String) = "lab/${encodeId(labId)}"

    const val CHECKPOINT = "checkpoint/{code}"
    fun checkpoint(code: String) = "checkpoint/$code"
}

// Stable content ids contain '#', which is unsafe in a route path; swap for a
// token on the way in and back on the way out.
internal fun encodeId(id: String): String = id.replace("#", "~")
internal fun decodeId(id: String): String = id.replace("~", "#")

/** The four bottom-bar tabs (Learn, Refresh, Library, You). */
enum class TopTab(val route: String, val label: String, val icon: ImageVector) {
    LEARN(Routes.HOME, "Learn", Icons.AutoMirrored.Filled.MenuBook),
    REFRESH(Routes.REFRESH, "Refresh", Icons.Filled.Autorenew),
    LIBRARY(Routes.LIBRARY, "Library", Icons.Filled.Search),
    YOU(Routes.ME, "You", Icons.Filled.Person),
}
