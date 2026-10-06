package com.kernelbreach.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnlockRulesTest {

    private fun path(key: String, moduleCodes: List<String>) = LearningPath(
        key = key,
        name = key,
        tagline = "",
        bestFor = "",
        needs = "",
        align = "",
        color = "#000000",
        modules = moduleCodes.mapIndexed { i, code ->
            Module(
                code = code,
                pathKey = key,
                order = i,
                title = code,
                topics = "",
                level = Level.BEGINNER,
                stageTitle = "S",
                canDo = emptyList(),
                lessons = emptyList(),
                labs = emptyList(),
                checkpoint = emptyList(),
                checkpointFocus = null,
                reviewed = false,
            )
        },
        capstone = null,
        certNote = null,
    )

    private val curriculum = Curriculum(
        listOf(
            path("ROOK", listOf("ROOK-01", "ROOK-11")),
            path("CORE", listOf("CORE-01", "CORE-15")),
            path("SOC", listOf("SOC-01")),
            path("AI", listOf("AI-01")),
        ),
    )
    private val rules = UnlockRules(curriculum)

    @Test
    fun `foundation path is always open`() {
        assertEquals(PathAccess.OPEN, rules.pathAccess("ROOK", emptySet()))
    }

    @Test
    fun `core locks until foundation final checkpoint or placement`() {
        assertEquals(PathAccess.LOCKED, rules.pathAccess("CORE", emptySet()))
        assertEquals(PathAccess.OPEN, rules.pathAccess("CORE", setOf("ROOK-11")))
        assertEquals(PathAccess.OPEN, rules.pathAccess("CORE", emptySet(), placementPassed = true))
    }

    @Test
    fun `specializations lock but can be overridden`() {
        assertEquals(PathAccess.LOCKED_CAN_OVERRIDE, rules.pathAccess("SOC", emptySet()))
        assertEquals(PathAccess.LOCKED_CAN_OVERRIDE, rules.pathAccess("AI", setOf("ROOK-11")))
        assertEquals(PathAccess.OPEN, rules.pathAccess("SOC", setOf("CORE-15")))
    }

    @Test
    fun `gate checkpoints are derived from curriculum order`() {
        assertEquals(null, rules.gateCheckpoint("ROOK"))
        assertEquals("ROOK-11", rules.gateCheckpoint("CORE"))
        assertEquals("CORE-15", rules.gateCheckpoint("SOC"))
    }

    @Test
    fun `lessons are sequential`() {
        assertEquals(LessonStatus.AVAILABLE, UnlockRules.lessonStatus(0, emptySet()))
        assertEquals(LessonStatus.LOCKED, UnlockRules.lessonStatus(1, emptySet()))
        assertEquals(LessonStatus.AVAILABLE, UnlockRules.lessonStatus(1, setOf(0)))
        assertEquals(LessonStatus.COMPLETED, UnlockRules.lessonStatus(0, setOf(0)))
        assertEquals(LessonStatus.IN_PROGRESS, UnlockRules.lessonStatus(1, setOf(0), inProgressIndices = setOf(1)))
    }

    @Test
    fun `checkpoint unlocks only when all lessons complete`() {
        assertEquals(false, UnlockRules.isCheckpointUnlocked(lessonCount = 4, completedLessonCount = 3))
        assertEquals(true, UnlockRules.isCheckpointUnlocked(lessonCount = 4, completedLessonCount = 4))
        assertEquals(false, UnlockRules.isCheckpointUnlocked(lessonCount = 0, completedLessonCount = 0))
    }
}
