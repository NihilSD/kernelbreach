package com.kernelbreach.core.model

/**
 * The whole curriculum: every path in display order. This is the in-memory
 * aggregate the UI reads after the content layer has assembled it.
 *
 * Paths are ordered as in the curriculum data. By convention the first two paths
 * form the trunk (foundation then core) and the rest are branches
 * (specializations). Nothing hard-codes the path keys, so a new path is a data
 * change only.
 */
data class Curriculum(
    val paths: List<LearningPath>,
) {
    private val pathsByKey = paths.associateBy { it.key }
    private val modulesByCode = paths.flatMap { it.modules }.associateBy { it.code }

    fun path(key: String): LearningPath? = pathsByKey[key]

    fun module(code: String): Module? = modulesByCode[code]

    val allModules: List<Module> get() = paths.flatMap { it.modules }

    /** Paths that form the main line everyone follows (foundation, then core). */
    val trunkPaths: List<LearningPath> get() = paths.take(TRUNK_SIZE)

    /** Specialization paths that branch off the trunk. */
    val branchPaths: List<LearningPath> get() = paths.drop(TRUNK_SIZE)

    companion object {
        const val TRUNK_SIZE = 2
    }
}
