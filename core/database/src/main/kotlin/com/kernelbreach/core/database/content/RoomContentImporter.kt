package com.kernelbreach.core.database.content

import android.util.Log
import androidx.room.withTransaction
import com.kernelbreach.core.content.ContentAssembler
import com.kernelbreach.core.content.ContentSource
import com.kernelbreach.core.content.ImportResult
import com.kernelbreach.core.content.Severity
import com.kernelbreach.core.database.KernelBreachDatabase
import com.kernelbreach.core.database.entity.ContentVersionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Outcome of a content import attempt. */
sealed interface ImportOutcome {
    /** The stored content hash already matched; nothing was written. */
    data object UpToDate : ImportOutcome

    /** Content was (re)written. [issues] are the validator findings. */
    data class Imported(val result: ImportResult) : ImportOutcome
}

/**
 * Imports the bundled content into Room on first launch and after app updates.
 *
 * Idempotent: content is assembled and hashed; if the hash matches the stored
 * [ContentVersionEntity] nothing happens. On change, content tables are rewritten
 * inside a single transaction keyed by stable ids, leaving every user-state table
 * untouched. Bad modules are already excluded by the assembler (content-less); in
 * debug builds we fail loudly, in release we log and carry on.
 */
class RoomContentImporter(
    private val db: KernelBreachDatabase,
    private val source: ContentSource,
    private val reviewedCodes: Set<String> = emptySet(),
    private val failOnError: Boolean = false,
    private val ioDispatcher: CoroutineDispatcher,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun importIfNeeded(): ImportOutcome = withContext(ioDispatcher) {
        val result = ContentAssembler(source, reviewedCodes).assemble()

        result.issues.forEach { issue ->
            when (issue.severity) {
                Severity.ERROR -> Log.e(TAG, issue.toString())
                Severity.WARNING -> Log.w(TAG, issue.toString())
                Severity.INFO -> Log.i(TAG, issue.toString())
            }
        }
        if (result.hasErrors && failOnError) {
            error("Content import failed with ${result.errors.size} error(s):\n" + result.errors.joinToString("\n"))
        }

        val current = db.contentVersionDao().current()
        if (current?.hash == result.contentHash) {
            return@withContext ImportOutcome.UpToDate
        }

        val entities = result.curriculum.toEntities()
        db.withTransaction {
            val dao = db.contentDao()
            // Clear content only; user-state tables are never touched here.
            dao.clearQuestions()
            dao.clearTerms()
            dao.clearLabs()
            dao.clearLessons()
            dao.clearModules()
            dao.clearPaths()

            dao.insertPaths(entities.paths)
            dao.insertModules(entities.modules)
            dao.insertLessons(entities.lessons)
            dao.insertLabs(entities.labs)
            dao.insertQuestions(entities.questions)
            dao.insertTerms(entities.terms)

            db.contentVersionDao().set(
                ContentVersionEntity(id = 1, hash = result.contentHash, importedAtEpochMillis = now()),
            )
        }
        ImportOutcome.Imported(result)
    }

    private companion object {
        const val TAG = "ContentImporter"
    }
}
