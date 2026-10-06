package com.kernelbreach.core.database.repo

import com.kernelbreach.core.content.ImportResult
import com.kernelbreach.core.database.KernelBreachDatabase
import com.kernelbreach.core.database.content.ImportOutcome
import com.kernelbreach.core.database.content.RoomContentImporter
import com.kernelbreach.core.database.content.buildCurriculum
import com.kernelbreach.core.database.content.toDomain
import com.kernelbreach.core.model.Curriculum
import com.kernelbreach.core.model.Lab
import com.kernelbreach.core.model.Lesson
import com.kernelbreach.core.model.Module
import com.kernelbreach.core.model.Term
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads the assembled curriculum out of Room and exposes it as domain models.
 * Also drives the one-time import. The curriculum is cached in memory (it is
 * small and immutable until the next import).
 */
class CurriculumRepository(
    private val db: KernelBreachDatabase,
    private val importer: RoomContentImporter,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    @Volatile private var cached: Curriculum? = null

    /** Import if needed, then invalidate the cache so reads see fresh content. */
    suspend fun ensureImported(): ImportResult? {
        val outcome = importer.importIfNeeded()
        if (outcome is ImportOutcome.Imported) {
            mutex.withLock { cached = null }
            return outcome.result
        }
        return null
    }

    suspend fun curriculum(): Curriculum = mutex.withLock {
        cached ?: loadFromDb().also { cached = it }
    }

    private suspend fun loadFromDb(): Curriculum = withContext(ioDispatcher) {
        val dao = db.contentDao()
        val paths = dao.allPaths()
        val modules = dao.allModules()
        val lessonsByModule = dao.allLessons().groupBy { it.moduleCode }
        val labsByModule = dao.allLabs().groupBy { it.moduleCode }
        val questionsByOwner = dao.allQuestions().groupBy { it.ownerId }
        buildCurriculum(paths, modules, lessonsByModule, labsByModule, questionsByOwner)
    }

    suspend fun module(code: String): Module? = curriculum().module(code)

    suspend fun lesson(id: String): Lesson? =
        curriculum().allModules.firstNotNullOfOrNull { m -> m.lessons.firstOrNull { it.id == id } }

    suspend fun lab(id: String): Lab? =
        curriculum().allModules.firstNotNullOfOrNull { m -> m.labs.firstOrNull { it.id == id } }

    suspend fun allTerms(): List<Term> = withContext(ioDispatcher) {
        db.contentDao().allTerms().map { it.toDomain() }
    }

    suspend fun searchTerms(query: String): List<Term> = withContext(ioDispatcher) {
        val sanitized = query.trim().replace("\"", "")
        if (sanitized.isEmpty()) return@withContext emptyList()
        // Prefix match on each token.
        val match = sanitized.split(Regex("\\s+")).joinToString(" ") { "$it*" }
        db.contentDao().searchTerms(match).map { it.toDomain() }
    }

    suspend fun termDefinition(term: String): Term? = withContext(ioDispatcher) {
        db.contentDao().term(term.trim().lowercase())?.toDomain()
    }
}
