package com.kernelbreach.core.content

import com.kernelbreach.core.model.Question
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The CLI-style acceptance test the spec calls for: load every bundled content
 * file and check all the import rules. If this passes, the whole curriculum
 * imports cleanly on-device.
 */
class BundledContentTest {

    private val result: ImportResult by lazy {
        ContentAssembler(FileContentSource.fromSystemProperty()).assemble()
    }

    @Test
    fun `all bundled content imports with zero errors`() {
        val errors = result.errors
        if (errors.isNotEmpty()) {
            System.err.println("Content errors (${errors.size}):")
            errors.forEach { System.err.println("  $it") }
        }
        assertTrue(errors.isEmpty(), "Expected no ERROR-level issues, found ${errors.size}")
    }

    @Test
    fun `curriculum has six paths and ninety-six modules`() {
        assertEquals(6, result.curriculum.paths.size)
        assertEquals(96, result.curriculum.allModules.size)
        assertEquals(
            listOf("ROOK", "CORE", "SOC", "PEN", "ENG", "AI"),
            result.curriculum.paths.map { it.key },
        )
    }

    @Test
    fun `seventy-six modules have authored content`() {
        val withContent = result.curriculum.allModules.filter { it.hasContent }
        assertEquals(76, withContent.size)
    }

    @Test
    fun `aggregate lesson and lab counts match the bundle`() {
        val totalLessons = result.curriculum.allModules.sumOf { it.lessons.size }
        val totalLabs = result.curriculum.allModules.sumOf { it.labs.size }
        assertEquals(433, totalLessons)
        assertEquals(316, totalLabs)
    }

    @Test
    fun `PEN path is entirely coming-soon`() {
        val pen = result.curriculum.path("PEN")!!
        assertEquals(20, pen.modules.size)
        assertTrue(pen.modules.none { it.hasContent }, "PEN should have no authored content in v1")
        assertFalse(pen.hasAnyContent)
    }

    @Test
    fun `every content module has a ten-question checkpoint and four-option questions`() {
        result.curriculum.allModules.filter { it.hasContent }.forEach { m ->
            assertEquals(10, m.checkpoint.size, "${m.code} checkpoint size")
            m.lessons.forEach { l ->
                assertEquals(4, l.check.size, "${l.id} check size")
                l.check.forEach { q ->
                    assertEquals(Question.OPTION_COUNT, q.options.size, "${l.id} option count")
                    assertTrue(q.answerIndex in 0..3, "${l.id} answer index")
                }
            }
            m.checkpoint.forEach { q ->
                assertTrue(q.answerIndex in q.options.indices)
            }
        }
    }

    @Test
    fun `stable content ids are assigned to lessons and labs`() {
        val rook01 = result.curriculum.module("ROOK-01")!!
        assertEquals("ROOK-01#L1", rook01.lessons.first().id)
        assertEquals("ROOK-01#LAB1", rook01.labs.first().id)
    }

    @Test
    fun `content hash is deterministic across imports`() {
        val a = ContentAssembler(FileContentSource.fromSystemProperty()).assemble().contentHash
        val b = ContentAssembler(FileContentSource.fromSystemProperty()).assemble().contentHash
        assertEquals(a, b)
        assertEquals(64, a.length) // SHA-256 hex
    }

    @Test
    fun `paths expose capstones and derived stages`() {
        val rook = result.curriculum.path("ROOK")!!
        assertTrue(rook.capstone != null, "ROOK should have a capstone")
        assertTrue(rook.stages.isNotEmpty(), "ROOK should expose derived stages")
        // Stages partition modules without loss.
        assertEquals(rook.modules.size, rook.stages.sumOf { it.modules.size })
    }
}
