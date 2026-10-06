package com.kernelbreach.core.srs

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * The engine's only source of "now", so every schedule is deterministic and
 * testable. Production wires this to the system clock; tests supply a fixed date.
 */
fun interface SrsClock {
    fun today(): LocalDate

    companion object {
        /** A clock backed by the real system clock in the given time zone. */
        fun system(timeZone: TimeZone = TimeZone.currentSystemDefault()): SrsClock =
            SrsClock { Clock.System.todayIn(timeZone) }

        /** A clock fixed at [date] — for tests and previews. */
        fun fixed(date: LocalDate): SrsClock = SrsClock { date }
    }
}
