package com.kernelbreach.core.model

/** Whether a path is reachable, and if not, whether the learner may force it. */
enum class PathAccess {
    /** Freely available. */
    OPEN,

    /** Locked, but the learner can "open anyway" after a warning. */
    LOCKED_CAN_OVERRIDE,

    /** Locked; a prerequisite must be met first. */
    LOCKED,
}

/**
 * Pure, data-driven unlock policy. Nothing here names ROOK/CORE literally; the
 * gate for each path is derived from curriculum order:
 *
 *  - The first path (foundation) is always [PathAccess.OPEN].
 *  - The second path (core) unlocks after the foundation's final module
 *    checkpoint is passed, or a placement check is passed.
 *  - Every later path (specialization) unlocks after core's final module
 *    checkpoint is passed, but the learner may open it anyway.
 *
 * Within a module: lessons are sequential, labs unlock after the lesson they
 * follow (see [LabPlacement]), and the checkpoint unlocks when every lesson is
 * complete.
 */
class UnlockRules(private val curriculum: Curriculum) {

    private val foundation: LearningPath? = curriculum.paths.getOrNull(0)
    private val core: LearningPath? = curriculum.paths.getOrNull(1)

    /** The module checkpoint that gates [pathKey], or null if the path is always open. */
    fun gateCheckpoint(pathKey: String): String? = when (pathKey) {
        foundation?.key -> null
        core?.key -> foundation?.modules?.lastOrNull()?.code
        else -> core?.modules?.lastOrNull()?.code
    }

    fun pathAccess(
        pathKey: String,
        passedCheckpoints: Set<String>,
        placementPassed: Boolean = false,
    ): PathAccess {
        return when (pathKey) {
            foundation?.key -> PathAccess.OPEN
            core?.key -> {
                val gate = gateCheckpoint(pathKey)
                if (gate == null || gate in passedCheckpoints || placementPassed) {
                    PathAccess.OPEN
                } else {
                    PathAccess.LOCKED
                }
            }

            else -> {
                val gate = gateCheckpoint(pathKey)
                if (gate == null || gate in passedCheckpoints) {
                    PathAccess.OPEN
                } else {
                    PathAccess.LOCKED_CAN_OVERRIDE
                }
            }
        }
    }

    companion object {
        /**
         * Status of a lesson given which lessons in its module are complete.
         * [completedIndices] are 0-based lesson indices. Lesson 0 is always
         * available; lesson i is available once lesson i-1 is complete.
         */
        fun lessonStatus(
            index: Int,
            completedIndices: Set<Int>,
            inProgressIndices: Set<Int> = emptySet(),
        ): LessonStatus = when {
            index in completedIndices -> LessonStatus.COMPLETED
            index == 0 || (index - 1) in completedIndices ->
                if (index in inProgressIndices) LessonStatus.IN_PROGRESS else LessonStatus.AVAILABLE

            else -> LessonStatus.LOCKED
        }

        /** A lab unlocks once the lesson it follows is complete. */
        fun isLabUnlocked(afterLessonNumber: Int, completedLessonNumbers: Set<Int>): Boolean =
            afterLessonNumber in completedLessonNumbers

        /** The module checkpoint unlocks when every lesson is complete. */
        fun isCheckpointUnlocked(lessonCount: Int, completedLessonCount: Int): Boolean =
            lessonCount > 0 && completedLessonCount >= lessonCount
    }
}
