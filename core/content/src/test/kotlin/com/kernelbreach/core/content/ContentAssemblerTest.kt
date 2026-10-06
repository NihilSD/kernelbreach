package com.kernelbreach.core.content

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Focused tests for the assembler/validator behaviour using small synthetic
 * content, complementing the full-bundle acceptance test.
 */
class ContentAssemblerTest {

    private val curriculum = """
        [
          {"key":"ROOK","name":"Rookie","color":"#0D9488","mods":[
            {"code":"ROOK-01","title":"One","topics":"t","n":1,"lvl":"B","stage":"S1"}
          ]},
          {"key":"PEN","name":"Pen","color":"#E5532D","mods":[
            {"code":"PEN-01","title":"Pentest intro","topics":"t","n":3,"lvl":"I","stage":"S1"}
          ]}
        ]
    """.trimIndent()

    private val rookPlan = """
        {"key":"ROOK","cert_note":"note","capstone":{"title":"Cap","desc":"d"},
         "modules":[{"code":"ROOK-01","can_do":["a","b","c"],
           "lessons":[{"title":"What security is","goal":"g"}],
           "labs":[{"title":"Lab A","sim":"terminal","task":"t"}],
           "checkpoint":{"focus":"f"}}]}
    """.trimIndent()

    private fun lessonFile(
        checkpointCount: Int = 10,
        checkCount: Int = 4,
        lessonTitle: String = "What security is",
        answerIndex: Int = 0,
        sim: String = "terminal",
    ): String {
        fun q(i: Int) = """{"q":"Q$i","options":["a","b","c","d"],"a":$answerIndex,"why":"because"}"""
        val checks = (0 until checkCount).joinToString(",") { q(it) }
        val cp = (0 until checkpointCount).joinToString(",") { q(it) }
        return """
            {"code":"ROOK-01","intro":"intro",
             "lessons":[{"title":"$lessonTitle","why":"w",
               "sections":[{"h":"H","p":["para"]}],
               "snippet":{"label":"L","text":"cmd"},"example":"ex",
               "terms":[{"term":"IP address","def":"a number"}],
               "check":[$checks],"recap":["r1","r2"]}],
             "labs":[{"title":"Lab A","sim":"$sim","scenario":"s","objective":"o",
               "steps":[{"do":"Run: ls","see":"out"}],"answer":"ans","hint":"h"}],
             "checkpoint":[$cp]}
        """.trimIndent()
    }

    private fun source(lesson: String?, plan: String? = rookPlan) = MapContentSource(
        curriculum = curriculum,
        plans = buildMap { if (plan != null) put("ROOK", plan) },
        lessons = buildMap { if (lesson != null) put("ROOK-01", lesson) },
    )

    @Test
    fun `valid synthetic content assembles with no errors`() {
        val result = ContentAssembler(source(lessonFile())).assemble()
        assertFalse(result.hasErrors, result.errors.toString())
        val module = result.curriculum.module("ROOK-01")!!
        assertTrue(module.hasContent)
        assertEquals(1, module.lessons.size)
        assertEquals("ROOK-01#L1", module.lessons.first().id)
        assertEquals("ROOK-01#LAB1", module.labs.first().id)
        assertEquals(listOf("a", "b", "c"), module.canDo)
        assertEquals("Cap", result.curriculum.path("ROOK")!!.capstone!!.title)
    }

    @Test
    fun `wrong checkpoint count is an error and leaves the module content-less`() {
        val result = ContentAssembler(source(lessonFile(checkpointCount = 9))).assemble()
        assertTrue(result.hasErrors)
        assertTrue(result.failedModuleCodes.contains("ROOK-01"))
        assertFalse(result.curriculum.module("ROOK-01")!!.hasContent)
    }

    @Test
    fun `answer index out of range is an error`() {
        val result = ContentAssembler(source(lessonFile(answerIndex = 5))).assemble()
        assertTrue(result.hasErrors)
    }

    @Test
    fun `unknown sim type is an error`() {
        val result = ContentAssembler(source(lessonFile(sim = "hologram"))).assemble()
        assertTrue(result.errors.any { "sim" in it.message })
    }

    @Test
    fun `lesson title drift from plan is a warning, not an error`() {
        val result = ContentAssembler(source(lessonFile(lessonTitle = "Different title"))).assemble()
        assertFalse(result.hasErrors)
        assertTrue(result.warnings.any { "titles differ" in it.message })
    }

    @Test
    fun `module without a lesson file is coming-soon (info only)`() {
        val result = ContentAssembler(source(lesson = null)).assemble()
        assertFalse(result.hasErrors)
        assertFalse(result.curriculum.module("ROOK-01")!!.hasContent)
        assertTrue(result.issues.any { it.severity == Severity.INFO && it.scope == "ROOK-01" })
        // PEN module has no plan and no lesson file -> coming soon.
        assertFalse(result.curriculum.module("PEN-01")!!.hasContent)
    }

    @Test
    fun `content hash changes when content changes`() {
        val a = ContentAssembler(source(lessonFile())).assemble().contentHash
        val b = ContentAssembler(source(lessonFile(lessonTitle = "What security is"))).assemble().contentHash
        val c = ContentAssembler(source(lessonFile(lessonTitle = "Changed"))).assemble().contentHash
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `reviewed codes flow through to the module`() {
        val result = ContentAssembler(source(lessonFile()), reviewedCodes = setOf("ROOK-01")).assemble()
        assertTrue(result.curriculum.module("ROOK-01")!!.reviewed)
    }
}
