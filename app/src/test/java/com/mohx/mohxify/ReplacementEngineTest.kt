package com.mohx.mohxify

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplacementEngineTest {
    @Test fun matchingIsCaseInsensitive() {
        assertEquals("thou", ReplacementEngine.replaceWord("you", listOf(Rule("You", "thou"))))
    }

    @Test fun titleCaseIsPreserved() {
        assertEquals("Thou", ReplacementEngine.replaceWord("You", listOf(Rule("you", "thou"))))
    }

    @Test fun allCapsIsPreserved() {
        assertEquals("THOU ART", ReplacementEngine.replaceWord("YOU", listOf(Rule("you", "thou art"))))
    }

    @Test fun alternativesKeepConfiguredOrderAndRemoveDuplicateTargets() {
        val rules = listOf(
            Rule("hello", "hi"),
            Rule("HELLO", "hey"),
            Rule("hello", "HI")
        )
        assertEquals(listOf("hi", "hey"), ReplacementEngine.alternatives("hello", rules))
    }

    @Test fun unmatchedWordsRemainUnchanged() {
        assertEquals("friend", ReplacementEngine.replaceWord("friend", listOf(Rule("hello", "hi"))))
    }

    @Test fun pairOverloadRemainsAvailable() {
        assertEquals("hi", ReplacementEngine.replaceWord("hello", listOf("hello" to "hi")))
    }
}
