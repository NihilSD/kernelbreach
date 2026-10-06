package com.kernelbreach.core.model

/**
 * Where labs sit relative to lessons inside a module.
 *
 * Labs are listed after lessons in the content files; the app distributes them
 * evenly through the module so a lab follows roughly every `n / labs` lessons.
 *
 * Rule (from the spec, kept in this one place so it is easy to change):
 *   lab `i` (0-based) appears after lesson `ceil((i + 1) * n / labs)`
 * where `n` is the lesson count and `labs` is the lab count. The result is a
 * 1-based lesson number, clamped to `1..n`.
 */
object LabPlacement {

    /** The 1-based lesson number a lab unlocks after. */
    fun lessonNumberAfter(labIndex: Int, lessonCount: Int, labCount: Int): Int {
        require(labIndex in 0 until labCount) { "labIndex $labIndex out of range 0 until $labCount" }
        require(lessonCount >= 1) { "lessonCount must be >= 1" }
        val raw = ceilDiv((labIndex + 1) * lessonCount, labCount)
        return raw.coerceIn(1, lessonCount)
    }

    /**
     * An ordered interleaving of lesson and lab ids for one module, used to drive
     * the module page and the "what unlocks after what" rules.
     */
    fun sequence(lessons: List<Lesson>, labs: List<Lab>): List<ModuleItem> {
        if (lessons.isEmpty()) return emptyList()
        // Group labs by the lesson number they follow.
        val labsAfterLesson: Map<Int, List<Lab>> = labs
            .mapIndexed { index, lab -> lessonNumberAfter(index, lessons.size, labs.size) to lab }
            .groupBy({ it.first }, { it.second })
        val out = ArrayList<ModuleItem>(lessons.size + labs.size)
        lessons.forEachIndexed { zeroBased, lesson ->
            val lessonNumber = zeroBased + 1
            out += ModuleItem.LessonItem(lesson)
            labsAfterLesson[lessonNumber]?.forEach { out += ModuleItem.LabItem(it, afterLessonNumber = lessonNumber) }
        }
        return out
    }

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
}

/** An item in a module's ordered Learn/Try/Lab flow. */
sealed interface ModuleItem {
    data class LessonItem(val lesson: Lesson) : ModuleItem
    data class LabItem(val lab: Lab, val afterLessonNumber: Int) : ModuleItem
}
