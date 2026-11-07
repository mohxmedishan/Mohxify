package com.mohx.mohxify

import java.util.Locale

/** Pure replacement logic, separated so it can be tested without Android APIs. */
object ReplacementEngine {
    fun replaceWord(word: String, rules: List<Pair<String, String>>): String {
        val rule = rules.firstOrNull { it.first.equals(word, ignoreCase = true) } ?: return word
        val isAllCaps = word.length > 1 && word == word.uppercase(Locale.ROOT)
        val isTitle = word.firstOrNull()?.isUpperCase() == true
        return when {
            isAllCaps -> rule.second.uppercase(Locale.ROOT)
            isTitle -> rule.second.replaceFirstChar { it.uppercase(Locale.ROOT) }
            else -> rule.second
        }
    }
}
