package com.kernelbreach.core.srs

import com.kernelbreach.core.model.ConceptStrength
import com.kernelbreach.core.model.Confidence
import com.kernelbreach.core.model.ReviewGrade
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SrsEngineTest {

    private val today = LocalDate(2026, 1, 1)
    private val engine = SrsEngine(SrsClock.fixed(today))

    private fun item(interval: Int, lapses: Int = 0) = ReviewItem(
        id = "t",
        moduleCode = "ROOK-01",
        kind = ReviewKind.TERM,
        intervalDays = interval,
        dueDate = today,
        lapses = lapses,
    )

    @Test
    fun `initial interval comes from confidence`() {
        assertEquals(1, engine.schedule("t", "ROOK-01", ReviewKind.TERM, Confidence.SHAKY).intervalDays)
        assertEquals(3, engine.schedule("t", "ROOK-01", ReviewKind.TERM, Confidence.OKAY).intervalDays)
        assertEquals(7, engine.schedule("t", "ROOK-01", ReviewKind.TERM, Confidence.SOLID).intervalDays)
        val solid = engine.schedule("t", "ROOK-01", ReviewKind.TERM, Confidence.SOLID)
        assertEquals(today.plus(7, DateTimeUnit.DAY), solid.dueDate)
    }

    @Test
    fun `missed resets interval and increments lapses`() {
        val updated = engine.onReview(item(interval = 30, lapses = 1), ReviewGrade.MISSED)
        assertEquals(1, updated.intervalDays)
        assertEquals(2, updated.lapses)
        assertEquals(today.plus(1, DateTimeUnit.DAY), updated.dueDate)
    }

    @Test
    fun `slow multiplies by 1_3 with a floor of 3 days`() {
        assertEquals(9, engine.onReview(item(7), ReviewGrade.SLOW).intervalDays) // round(9.1)=9
        assertEquals(3, engine.onReview(item(1), ReviewGrade.SLOW).intervalDays) // round(1.3)=1 -> min 3
        assertEquals(0, engine.onReview(item(7), ReviewGrade.SLOW).lapses)
    }

    @Test
    fun `easy multiplies by 2_5 within 7 to 120 days`() {
        assertEquals(18, engine.onReview(item(7), ReviewGrade.EASY).intervalDays) // round(17.5)=18
        assertEquals(7, engine.onReview(item(1), ReviewGrade.EASY).intervalDays) // round(2.5)=3 -> min 7
        assertEquals(120, engine.onReview(item(60), ReviewGrade.EASY).intervalDays) // 150 -> max 120
        assertEquals(120, engine.onReview(item(120), ReviewGrade.EASY).intervalDays)
    }

    @Test
    fun `due filters and sorts by soonest due`() {
        val a = item(1).copy(id = "a", dueDate = today.minusDays(2))
        val b = item(1).copy(id = "b", dueDate = today)
        val c = item(1).copy(id = "c", dueDate = today.plus(5, DateTimeUnit.DAY))
        val due = engine.due(listOf(c, b, a))
        assertEquals(listOf("a", "b"), due.map { it.id })
    }

    @Test
    fun `concept strength bands`() {
        assertEquals(ConceptStrength.NEW, engine.conceptStrength(emptyList()))
        // 4/4 strong -> SOLID
        assertEquals(ConceptStrength.SOLID, engine.conceptStrength(List(4) { item(interval = 7) }))
        // 2/4 strong -> GOOD
        assertEquals(
            ConceptStrength.GOOD,
            engine.conceptStrength(listOf(item(7), item(7), item(3), item(1))),
        )
        // 1/4 strong -> OKAY
        assertEquals(
            ConceptStrength.OKAY,
            engine.conceptStrength(listOf(item(7), item(3), item(1), item(1))),
        )
        // long interval but lapsed -> not strong
        assertEquals(
            ConceptStrength.OKAY,
            engine.conceptStrength(listOf(item(interval = 30, lapses = 2))),
        )
    }

    @Test
    fun `daily plan caps counts and respects the time budget`() {
        val dueItems = (1..8).map { item(1).copy(id = "r$it", dueDate = today.minusDays(it.toLong())) }
        val quick = (1..8).map { "q$it" }
        val plan = engine.buildDailyPlan(
            continueLessonId = "ROOK-01#L1",
            allItems = dueItems,
            recentQuickCheckIds = quick,
        )
        assertEquals("ROOK-01#L1", plan.continueLessonId)
        assertEquals(5, plan.dueReviews.size) // capped at MAX_DUE_REVIEWS
        // base = lesson(12) + 5 reviews(5) = 17; remaining 3 -> 3 quick-checks
        assertEquals(3, plan.quickCheckIds.size)
        assertTrue(plan.estimatedMinutes <= SrsEngine.TARGET_MINUTES)
        assertEquals(20, plan.estimatedMinutes)
    }

    @Test
    fun `daily plan without a lesson leaves more room for quick-checks`() {
        val plan = engine.buildDailyPlan(
            continueLessonId = null,
            allItems = emptyList(),
            recentQuickCheckIds = (1..8).map { "q$it" },
        )
        assertTrue(plan.continueLessonId == null)
        assertEquals(5, plan.quickCheckIds.size) // capped at MAX_QUICK_CHECKS, budget allows
    }

    @Test
    fun `empty plan is reported as empty`() {
        val plan = engine.buildDailyPlan(null, emptyList(), emptyList())
        assertTrue(plan.isEmpty)
    }

    @Test
    fun `checkpoint miss is due within a day`() {
        val mi = engine.scheduleFromCheckpointMiss("ROOK-01#CP3", "ROOK-01")
        assertEquals(1, mi.intervalDays)
        assertEquals(today.plus(1, DateTimeUnit.DAY), mi.dueDate)
    }

    private fun LocalDate.minusDays(n: Long): LocalDate = plus(-n.toInt(), DateTimeUnit.DAY)
}
