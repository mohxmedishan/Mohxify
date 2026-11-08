package com.mohx.mohxify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkRuleParserTest {
    @Test fun parsesSemicolonAndNewlineSeparatedRules() {
        val result = BulkRuleParser.parse("what: thou;\nhello: hark")
        assertEquals(listOf(Rule("what", "thou"), Rule("hello", "hark")), result.rules)
        assertTrue(result.invalidEntries.isEmpty())
    }

    @Test fun ignoresCommentLines() {
        val result = BulkRuleParser.parse("# sample rules\nhello: hi")
        assertEquals(listOf(Rule("hello", "hi")), result.rules)
    }

    @Test fun preservesSynonymsAndCountsExactDuplicates() {
        val result = BulkRuleParser.parse("hello: hi; hello: hey; HELLO: HI")
        assertEquals(listOf(Rule("hello", "hi"), Rule("hello", "hey")), result.rules)
        assertEquals(1, result.duplicateCount)
    }

    @Test fun reportsMalformedEntriesWithoutDroppingValidOnes() {
        val result = BulkRuleParser.parse("valid: yes; missing separator; : empty; two words: no; blank: ")
        assertEquals(listOf(Rule("valid", "yes")), result.rules)
        assertEquals(4, result.invalidEntries.size)
    }

    @Test fun unwrapsBracketedAndQuotedValues() {
        val result = BulkRuleParser.parse("[hello]: `good day`; \"bye\": 'farewell'")
        assertEquals(listOf(Rule("hello", "good day"), Rule("bye", "farewell")), result.rules)
    }

    @Test fun exportedRulesCanBeParsedAgain() {
        val original = listOf(Rule("hello", "hi"), Rule("hello", "hey"), Rule("you", "thou"))
        assertEquals(original, BulkRuleParser.parse(BulkRuleParser.export(original)).rules)
    }
}
