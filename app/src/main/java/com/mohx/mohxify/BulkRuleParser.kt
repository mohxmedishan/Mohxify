package com.mohx.mohxify

/** Pure Kotlin parser: no Android APIs, so it can be exercised by local JVM tests. */
object BulkRuleParser {
    data class InvalidEntry(val entry: String, val reason: String)
    data class Result(
        val rules: List<Rule>,
        val duplicateCount: Int,
        val invalidEntries: List<InvalidEntry>
    )

    fun parse(text: String): Result {
        val entries = text.lineSequence()
            .filterNot { it.trimStart().startsWith("#") }
            .joinToString("\n")
            .split(Regex("[;\\n]+"))
        val valid = mutableListOf<Rule>()
        val invalid = mutableListOf<InvalidEntry>()

        entries.forEach { raw ->
            val entry = raw.trim()
            if (entry.isEmpty()) return@forEach
            val colon = entry.indexOf(':')
            if (colon < 0) {
                invalid += InvalidEntry(entry, "Missing ':' separator")
                return@forEach
            }
            val original = unwrap(entry.substring(0, colon).trim())
            val replacement = unwrap(entry.substring(colon + 1).trim())
            when {
                original.isEmpty() -> invalid += InvalidEntry(entry, "Original word is empty")
                original.any { it.isWhitespace() } ->
                    invalid += InvalidEntry(entry, "Original must be a single word")
                replacement.isEmpty() -> invalid += InvalidEntry(entry, "Replacement is empty")
                else -> valid += Rule(original, replacement)
            }
        }

        val seen = mutableSetOf<String>()
        var duplicates = 0
        val uniqueReversed = mutableListOf<Rule>()
        valid.asReversed().forEach { rule ->
            if (seen.add(rule.from.lowercase())) uniqueReversed += rule else duplicates++
        }
        return Result(uniqueReversed.asReversed(), duplicates, invalid)
    }

    private fun unwrap(input: String): String {
        var value = input.trim()
        var changed = true
        while (changed && value.length >= 2) {
            changed = false
            if (value.first() == '[' && value.last() == ']') {
                value = value.substring(1, value.length - 1).trim()
                changed = true
            } else if ((value.first() == '"' && value.last() == '"') ||
                (value.first() == '\'' && value.last() == '\'') ||
                (value.first() == '`' && value.last() == '`')) {
                value = value.substring(1, value.length - 1).trim()
                changed = true
            }
        }
        return value
    }

    fun export(rules: List<Rule>): String =
        rules.joinToString("\n") { "${it.from}: ${it.to};" }
}
