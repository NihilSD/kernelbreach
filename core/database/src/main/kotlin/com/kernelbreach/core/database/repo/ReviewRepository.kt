package com.kernelbreach.core.database.repo

import com.kernelbreach.core.database.KernelBreachDatabase
import com.kernelbreach.core.database.entity.ConceptStateEntity
import com.kernelbreach.core.model.ConceptStrength
import com.kernelbreach.core.model.Confidence
import com.kernelbreach.core.model.Lesson
import com.kernelbreach.core.model.ReviewGrade
import com.kernelbreach.core.srs.DailyPlan
import com.kernelbreach.core.srs.ReviewItem
import com.kernelbreach.core.srs.ReviewKind
import com.kernelbreach.core.srs.SrsClock
import com.kernelbreach.core.srs.SrsEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * Bridges the pure [SrsEngine] with persistence. Lesson completion seeds review
 * items (one per glossary term plus one for the lesson itself); refreshes grade
 * them; the engine does all the date math.
 */
class ReviewRepository(
    private val db: KernelBreachDatabase,
    private val engine: SrsEngine,
    private val clock: SrsClock,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val dao get() = db.conceptStateDao()

    suspend fun onLessonComplete(lesson: Lesson, confidence: Confidence) = withContext(ioDispatcher) {
        val items = buildList {
            add(engine.schedule(lesson.id, lesson.moduleCode, ReviewKind.LESSON, confidence))
            lesson.terms.forEach { term ->
                val id = "${lesson.id}#term#${term.term.lowercase()}"
                add(engine.schedule(id, lesson.moduleCode, ReviewKind.TERM, confidence))
            }
        }
        dao.upsertAll(items.map { it.toEntity() })
    }

    suspend fun onCheckpointMiss(moduleCode: String, questionId: String) = withContext(ioDispatcher) {
        dao.upsert(engine.scheduleFromCheckpointMiss(questionId, moduleCode).toEntity())
    }

    suspend fun dueItems(): List<ReviewItem> = withContext(ioDispatcher) {
        dao.due(clock.today().toEpochDays().toLong()).map { it.toDomain() }
    }

    suspend fun grade(itemId: String, grade: ReviewGrade) = withContext(ioDispatcher) {
        val current = dao.byId(itemId)?.toDomain() ?: return@withContext
        dao.upsert(engine.onReview(current, grade).toEntity())
    }

    suspend fun dailyPlan(continueLessonId: String?, recentQuickCheckIds: List<String>): DailyPlan =
        withContext(ioDispatcher) {
            engine.buildDailyPlan(continueLessonId, dao.all().map { it.toDomain() }, recentQuickCheckIds)
        }

    suspend fun conceptStrength(moduleCode: String): ConceptStrength = withContext(ioDispatcher) {
        engine.conceptStrength(dao.forModule(moduleCode).map { it.toDomain() })
    }
}

private fun ReviewItem.toEntity() = ConceptStateEntity(
    id = id,
    moduleCode = moduleCode,
    kind = kind.name,
    intervalDays = intervalDays,
    dueDateEpochDay = dueDate.toEpochDays().toLong(),
    lapses = lapses,
    lastReviewedEpochDay = lastReviewed?.toEpochDays()?.toLong(),
)

private fun ConceptStateEntity.toDomain() = ReviewItem(
    id = id,
    moduleCode = moduleCode,
    kind = ReviewKind.valueOf(kind),
    intervalDays = intervalDays,
    dueDate = LocalDate.fromEpochDays(dueDateEpochDay.toInt()),
    lapses = lapses,
    lastReviewed = lastReviewedEpochDay?.let { LocalDate.fromEpochDays(it.toInt()) },
)
