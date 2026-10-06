package com.kernelbreach.core.srs

import com.kernelbreach.core.model.Confidence
import com.kernelbreach.core.model.ConceptStrength
import com.kernelbreach.core.model.ReviewGrade
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The spaced-repetition engine.
 *
 * All timing is in whole days and fully deterministic given the [clock]:
 *
 *  - Initial interval comes from the learner's confidence: Shaky 1d, Okay 3d,
 *    Solid 7d.
 *  - On review: Missed resets to 1 day and counts a lapse; Slow multiplies by
 *    1.3 (min 3 days); Easy multiplies by 2.5 (min 7, max 120 days).
 *
 * The engine is stateless — it takes an item and returns a new item — so it is
 * trivial to test and to persist around.
 */
class SrsEngine(private val clock: SrsClock) {

    /** Create the first schedule for a new review item from a confidence rating. */
    fun schedule(
        id: String,
        moduleCode: String,
        kind: ReviewKind,
        confidence: Confidence,
    ): ReviewItem {
        val today = clock.today()
        val interval = confidence.initialIntervalDays
        return ReviewItem(
            id = id,
            moduleCode = moduleCode,
            kind = kind,
            intervalDays = interval,
            dueDate = today.plus(interval, DateTimeUnit.DAY),
            lapses = 0,
            lastReviewed = today,
        )
    }

    /** A checkpoint miss becomes a review item due as soon as possible (1 day). */
    fun scheduleFromCheckpointMiss(id: String, moduleCode: String): ReviewItem {
        val today = clock.today()
        return ReviewItem(
            id = id,
            moduleCode = moduleCode,
            kind = ReviewKind.QUESTION,
            intervalDays = MIN_INTERVAL_DAYS,
            dueDate = today.plus(MIN_INTERVAL_DAYS, DateTimeUnit.DAY),
            lapses = 0,
            lastReviewed = today,
        )
    }

    /** Apply a review outcome, returning the updated item. */
    fun onReview(item: ReviewItem, grade: ReviewGrade): ReviewItem {
        val today = clock.today()
        val newInterval: Int
        val newLapses: Int
        when (grade) {
            ReviewGrade.MISSED -> {
                newInterval = MIN_INTERVAL_DAYS
                newLapses = item.lapses + 1
            }

            ReviewGrade.SLOW -> {
                newInterval = maxOf(SLOW_MIN_DAYS, (item.intervalDays * SLOW_FACTOR).roundToInt())
                newLapses = item.lapses
            }

            ReviewGrade.EASY -> {
                val grown = (item.intervalDays * EASY_FACTOR).roundToInt()
                newInterval = grown.coerceIn(EASY_MIN_DAYS, MAX_INTERVAL_DAYS)
                newLapses = item.lapses
            }
        }
        return item.copy(
            intervalDays = min(newInterval, MAX_INTERVAL_DAYS),
            dueDate = today.plus(newInterval, DateTimeUnit.DAY),
            lapses = newLapses,
            lastReviewed = today,
        )
    }

    /** Items due today or earlier, soonest-due first (oldest debt first). */
    fun due(items: List<ReviewItem>): List<ReviewItem> {
        val today = clock.today()
        return items.filter { it.isDue(today) }.sortedBy { it.dueDate }
    }

    /**
     * Concept strength for a module, from the share of its review items that are
     * on a long interval with no lapses.
     */
    fun conceptStrength(moduleItems: List<ReviewItem>): ConceptStrength {
        if (moduleItems.isEmpty()) return ConceptStrength.NEW
        val strongShare = moduleItems.count { it.isStrong }.toDouble() / moduleItems.size
        return when {
            strongShare >= SOLID_SHARE -> ConceptStrength.SOLID
            strongShare >= GOOD_SHARE -> ConceptStrength.GOOD
            else -> ConceptStrength.OKAY
        }
    }

    /**
     * Build today's plan: one lesson to continue, up to five due reviews, and up
     * to five quick-check questions from recently finished modules, trimmed to a
     * ~20-minute budget. Deterministic given its inputs.
     */
    fun buildDailyPlan(
        continueLessonId: String?,
        allItems: List<ReviewItem>,
        recentQuickCheckIds: List<String>,
    ): DailyPlan {
        val dueReviews = due(allItems).take(MAX_DUE_REVIEWS)

        // Budget order: the lesson first, then due reviews, then quick-checks fill
        // whatever time is left up to the ~20-minute target.
        val baseMinutes =
            (if (continueLessonId != null) LESSON_MINUTES else 0) +
                dueReviews.size * REVIEW_MINUTES
        val remaining = (TARGET_MINUTES - baseMinutes).coerceAtLeast(0)
        val quickBudget = (remaining / QUICK_CHECK_MINUTES).coerceAtMost(MAX_QUICK_CHECKS)
        val quickChecks = recentQuickCheckIds.distinct().take(quickBudget)

        return DailyPlan(
            continueLessonId = continueLessonId,
            dueReviews = dueReviews,
            quickCheckIds = quickChecks,
            estimatedMinutes = baseMinutes + quickChecks.size * QUICK_CHECK_MINUTES,
        )
    }

    companion object {
        const val MIN_INTERVAL_DAYS = 1
        const val SLOW_MIN_DAYS = 3
        const val EASY_MIN_DAYS = 7
        const val MAX_INTERVAL_DAYS = 120
        const val SLOW_FACTOR = 1.3
        const val EASY_FACTOR = 2.5

        /** Interval at/above which an item counts toward concept strength. */
        const val STRONG_INTERVAL_DAYS = 7

        const val SOLID_SHARE = 0.8
        const val GOOD_SHARE = 0.5

        const val MAX_DUE_REVIEWS = 5
        const val MAX_QUICK_CHECKS = 5
        const val TARGET_MINUTES = 20
        const val LESSON_MINUTES = 12
        const val REVIEW_MINUTES = 1
        const val QUICK_CHECK_MINUTES = 1
    }
}

/** Today's study plan produced by [SrsEngine.buildDailyPlan]. */
data class DailyPlan(
    val continueLessonId: String?,
    val dueReviews: List<ReviewItem>,
    val quickCheckIds: List<String>,
    val estimatedMinutes: Int,
) {
    val isEmpty: Boolean
        get() = continueLessonId == null && dueReviews.isEmpty() && quickCheckIds.isEmpty()
}
