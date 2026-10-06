package com.kernelbreach.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LabPlacementTest {

    @Test
    fun `lab placement spreads labs evenly across lessons`() {
        // 4 lessons, 2 labs: ceil((i+1)*4/2) -> lab0 after 2, lab1 after 4.
        assertEquals(2, LabPlacement.lessonNumberAfter(0, lessonCount = 4, labCount = 2))
        assertEquals(4, LabPlacement.lessonNumberAfter(1, lessonCount = 4, labCount = 2))
    }

    @Test
    fun `lab placement with three labs over four lessons`() {
        // ceil((i+1)*4/3): lab0 -> ceil(4/3)=2, lab1 -> ceil(8/3)=3, lab2 -> ceil(12/3)=4
        assertEquals(2, LabPlacement.lessonNumberAfter(0, 4, 3))
        assertEquals(3, LabPlacement.lessonNumberAfter(1, 4, 3))
        assertEquals(4, LabPlacement.lessonNumberAfter(2, 4, 3))
    }

    @Test
    fun `placement never exceeds lesson count`() {
        for (n in 1..9) {
            for (labs in 1..6) {
                for (i in 0 until labs) {
                    val after = LabPlacement.lessonNumberAfter(i, n, labs)
                    assertTrue(after in 1..n, "lab $i of $labs over $n lessons -> $after")
                }
            }
        }
    }

    @Test
    fun `sequence interleaves every lesson and lab once`() {
        val lessons = (1..4).map { fakeLesson(it) }
        val labs = (1..2).map { fakeLab(it) }
        val seq = LabPlacement.sequence(lessons, labs)
        assertEquals(6, seq.size)
        assertEquals(4, seq.filterIsInstance<ModuleItem.LessonItem>().size)
        assertEquals(2, seq.filterIsInstance<ModuleItem.LabItem>().size)
        // Labs should land after lessons 2 and 4.
        val labPositions = seq.filterIsInstance<ModuleItem.LabItem>().map { it.afterLessonNumber }
        assertEquals(listOf(2, 4), labPositions)
    }

    private fun fakeLesson(n: Int) = Lesson(
        id = "X#L$n",
        moduleCode = "X",
        index = n,
        title = "L$n",
        why = "",
        sections = emptyList(),
        snippet = null,
        example = null,
        terms = emptyList(),
        check = emptyList(),
        recap = emptyList(),
    )

    private fun fakeLab(n: Int) = Lab(
        id = "X#LAB$n",
        moduleCode = "X",
        index = n,
        title = "Lab$n",
        sim = SimType.TERMINAL,
        scenario = "",
        objective = "",
        steps = emptyList(),
        answer = "",
        hint = "",
    )
}
