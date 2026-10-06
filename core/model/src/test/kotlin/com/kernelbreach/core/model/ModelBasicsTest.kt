package com.kernelbreach.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModelBasicsTest {

    @Test
    fun `level parses B I A and rejects junk`() {
        assertEquals(Level.BEGINNER, Level.fromWire("B"))
        assertEquals(Level.INTERMEDIATE, Level.fromWire("i"))
        assertEquals(Level.ADVANCED, Level.fromWire(" A "))
        assertThrows(IllegalArgumentException::class.java) { Level.fromWire("X") }
    }

    @Test
    fun `sim type round-trips all eleven renderers`() {
        val wires = listOf(
            "terminal", "log-viewer", "packet-viewer", "siem", "web-proxy",
            "code-editor", "config-editor", "file-explorer", "diagram-builder",
            "scenario", "chat-sim",
        )
        assertEquals(wires.size, SimType.entries.size)
        wires.forEach { assertEquals(it, SimType.fromWire(it).wire) }
        assertNull(SimType.fromWireOrNull("nope"))
    }

    @Test
    fun `confidence intervals follow the spec`() {
        assertEquals(1, Confidence.SHAKY.initialIntervalDays)
        assertEquals(3, Confidence.OKAY.initialIntervalDays)
        assertEquals(7, Confidence.SOLID.initialIntervalDays)
        assertEquals(Confidence.SOLID, Confidence.fromWire("Solid"))
    }

    @Test
    fun `question enforces four options and a valid answer index`() {
        assertThrows(IllegalArgumentException::class.java) {
            Question("q", listOf("a", "b", "c"), 0, "w")
        }
        assertThrows(IllegalArgumentException::class.java) {
            Question("q", listOf("a", "b", "c", "d"), 4, "w")
        }
        val q = Question("q", listOf("a", "b", "c", "d"), 2, "w")
        assertEquals("c", q.answer)
    }

    @Test
    fun `curriculum splits trunk and branches by order`() {
        fun path(key: String) = LearningPath(key, key, "", "", "", "", "#000000", emptyList(), null, null)
        val c = Curriculum(listOf(path("ROOK"), path("CORE"), path("SOC"), path("AI")))
        assertEquals(listOf("ROOK", "CORE"), c.trunkPaths.map { it.key })
        assertEquals(listOf("SOC", "AI"), c.branchPaths.map { it.key })
        assertTrue(c.path("ROOK") != null)
        assertNull(c.path("NOPE"))
    }
}
