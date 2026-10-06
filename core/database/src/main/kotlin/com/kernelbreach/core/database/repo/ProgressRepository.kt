package com.kernelbreach.core.database.repo

import com.kernelbreach.core.database.KernelBreachDatabase
import com.kernelbreach.core.database.entity.CheckpointAttemptEntity
import com.kernelbreach.core.database.entity.LabProgressEntity
import com.kernelbreach.core.database.entity.LessonProgressEntity
import com.kernelbreach.core.model.LessonStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Reads and writes user progress (lessons, labs, checkpoints). */
class ProgressRepository(
    private val db: KernelBreachDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val dao get() = db.progressDao()

    fun observeLessonProgress(): Flow<Map<String, LessonProgressEntity>> =
        dao.observeLessonProgress().map { list -> list.associateBy { it.lessonId } }

    fun observePassedCheckpoints(): Flow<Set<String>> =
        dao.observePassedCheckpoints().map { it.toSet() }

    suspend fun passedCheckpoints(): Set<String> = withContext(ioDispatcher) {
        dao.passedCheckpoints().toSet()
    }

    suspend fun markLessonOpened(lessonId: String) = withContext(ioDispatcher) {
        val existing = dao.lessonProgress(lessonId)
        val status = if (existing?.status == LessonStatus.COMPLETED.name) {
            LessonStatus.COMPLETED
        } else {
            LessonStatus.IN_PROGRESS
        }
        dao.upsertLessonProgress(
            LessonProgressEntity(
                lessonId = lessonId,
                status = status.name,
                bestScore = existing?.bestScore ?: 0,
                lastOpenedEpochMillis = now(),
            ),
        )
    }

    suspend fun completeLesson(lessonId: String, score: Int) = withContext(ioDispatcher) {
        val existing = dao.lessonProgress(lessonId)
        dao.upsertLessonProgress(
            LessonProgressEntity(
                lessonId = lessonId,
                status = LessonStatus.COMPLETED.name,
                bestScore = maxOf(existing?.bestScore ?: 0, score),
                lastOpenedEpochMillis = now(),
            ),
        )
    }

    suspend fun completedLessonIds(lessonIds: List<String>): Set<String> = withContext(ioDispatcher) {
        dao.lessonProgressFor(lessonIds)
            .filter { it.status == LessonStatus.COMPLETED.name }
            .map { it.lessonId }
            .toSet()
    }

    suspend fun labProgress(labId: String): LabProgressEntity? = withContext(ioDispatcher) {
        dao.labProgress(labId)
    }

    suspend fun saveLabProgress(labId: String, stepIndex: Int, completed: Boolean) =
        withContext(ioDispatcher) {
            dao.upsertLabProgress(LabProgressEntity(labId, stepIndex, completed))
        }

    suspend fun recordCheckpoint(
        moduleCode: String,
        score: Int,
        total: Int,
        passed: Boolean,
        answers: List<Int>,
    ) = withContext(ioDispatcher) {
        dao.insertCheckpointAttempt(
            CheckpointAttemptEntity(
                moduleCode = moduleCode,
                score = score,
                total = total,
                passed = passed,
                dateEpochMillis = now(),
                answers = answers,
            ),
        )
    }

    suspend fun attemptsForModule(moduleCode: String): List<CheckpointAttemptEntity> =
        withContext(ioDispatcher) { dao.attemptsForModule(moduleCode) }
}
