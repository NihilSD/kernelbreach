package com.kernelbreach.core.model

/** Lifecycle of a single lesson for one learner. */
enum class LessonStatus {
    /** Not yet reachable (a prior lesson is unfinished). */
    LOCKED,

    /** Reachable and not started. */
    AVAILABLE,

    /** Started but not completed. */
    IN_PROGRESS,

    /** Finished (all check questions attempted). */
    COMPLETED,
}

/**
 * The learner's self-reported confidence at the end of a lesson. Drives the
 * initial spaced-repetition interval for that lesson's review items.
 */
enum class Confidence(val wire: String, val initialIntervalDays: Int) {
    SHAKY("Shaky", 1),
    OKAY("Okay", 3),
    SOLID("Solid", 7),
    ;

    companion object {
        fun fromWire(value: String): Confidence =
            entries.firstOrNull { it.wire.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown confidence '$value'")
    }
}

/** How well a learner recalled an item during a refresh/review. */
enum class ReviewGrade {
    /** Could not recall — interval resets. */
    MISSED,

    /** Recalled with effort — interval grows slowly. */
    SLOW,

    /** Recalled easily — interval grows fast. */
    EASY,
}

/**
 * Concept strength shown on the You screen, derived from how many of a module's
 * review items are on long intervals without recent lapses.
 */
enum class ConceptStrength {
    NEW,
    OKAY,
    GOOD,
    SOLID,
}
