package com.mohx.mohxify

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class RewriteModeTest {
    @Test fun rejectsBlankTextAndInstruction() {
        assertNotNull(RewriteMode.validate("  ", "Polish this"))
        assertNotNull(RewriteMode.validate("Hello", " "))
        assertNull(RewriteMode.validate("Hello", "Polish this"))
    }

    @Test fun rejectsOversizedInput() {
        assertNotNull(RewriteMode.validate("x".repeat(RewriteMode.MAX_INPUT_CHARS + 1), "Polish"))
    }

    @Test fun requestContainsWholeTextAndInstruction() {
        val body = JSONObject(RewriteMode.requestBody("A full paragraph.", "Make it formal."))
        assertEquals("A full paragraph.", body.getString("text"))
        assertEquals("Make it formal.", body.getString("instruction"))
    }

    @Test fun parsesNonEmptyResult() {
        assertEquals("Rewritten.", RewriteMode.parseResponse("""{"text":" Rewritten. "}"""))
    }

    @Test(expected = Exception::class)
    fun rejectsEmptyResponse() {
        RewriteMode.parseResponse("""{"text":" "}""")
    }

    @Test fun quickExpandRulesRemainIndependent() {
        assertEquals("friend", ReplacementEngine.replaceWord("friend", listOf(Rule("hello", "hi"))))
    }
}
