package com.mohx.mohxify

import java.util.Locale

/** Pure replacement logic, separated so it can be tested without Android APIs. */
object ReplacementEngine {
    /** Returns every synonym in the order configured in the loadout. */
    fun alternatives(word: String, rules: List<Rule>): List<String> {
        val matching = rules.filter { it.from.equals(word, ignoreCase = true) }
            .map { it.to }
            .distinctBy { it.lowercase(Locale.ROOT) }
        return matching.map { applyWordCase(word, it) }
    }

    @kotlin.jvm.JvmName("replaceWordFromPairs")
    fun replaceWord(word: String, rules: List<Pair<String, String>>): String {
        val replacement = rules.firstOrNull { it.first.equals(word, ignoreCase = true) }?.second
            ?: return word
        return applyWordCase(word, replacement)
    }

    fun replaceWord(word: String, rules: List<Rule>): String =
        alternatives(word, rules).firstOrNull() ?: word

    /** Replaces the alphanumeric core while retaining punctuation around it. */
    fun replaceTokenPreservingAffixes(token: String, replacement: String): String {
        var start = 0
        var end = token.length
        while (start < end && !token[start].isLetterOrDigit()) start++
        while (end > start && !token[end - 1].isLetterOrDigit()) end--
        if (start == end) return token
        return token.substring(0, start) + replacement + token.substring(end)
    }

    private fun applyWordCase(word: String, replacement: String): String {
        val isAllCaps = word.length > 1 && word == word.uppercase(Locale.ROOT)
        val isTitle = word.firstOrNull()?.isUpperCase() == true
        return when {
            isAllCaps -> replacement.uppercase(Locale.ROOT)
            isTitle -> replacement.replaceFirstChar { it.uppercase(Locale.ROOT) }
            else -> replacement
        }
    }
}
