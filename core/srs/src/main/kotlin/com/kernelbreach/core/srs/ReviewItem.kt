package com.kernelbreach.core.srs

import kotlinx.datetime.LocalDate

/** What a review item represents. */
enum class ReviewKind {
    /** A glossary term from a lesson. */
    TERM,

    /** A lesson's check question (or a checkpoint miss). */
    QUESTION,

    /** A whole lesson's key idea. */
    LESSON,
}

/**
 * The spaced-repetition state for one reviewable item. Scheduling works in whole
 * days: [dueDate] is the calendar day the item next becomes due, and
 * [intervalDays] is the current spacing. This maps cleanly onto persistence
 * (store [dueDate] as an epoch-day Long).
 */
data class ReviewItem(
    val id: String,
    val moduleCode: String,
    val kind: ReviewKind,
    val intervalDays: Int,
    val dueDate: LocalDate,
    val lapses: Int = 0,
    val lastReviewed: LocalDate? = null,
) {
    /** Due if its due date is today or earlier. */
    fun isDue(today: LocalDate): Boolean = dueDate <= today

    /** On a long interval with no lapses — the signal for a "strong" concept. */
    val isStrong: Boolean get() = intervalDays >= SrsEngine.STRONG_INTERVAL_DAYS && lapses == 0
}
