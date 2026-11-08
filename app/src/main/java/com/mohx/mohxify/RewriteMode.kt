package com.mohx.mohxify

import org.json.JSONObject

/**
 * Shared Rewrite Mode contract. QuickExpand never calls this code.
 * The backend accepts { "text": "...", "instruction": "..." } and returns { "text": "..." }.
 */
object RewriteMode {
    const val MAX_INPUT_CHARS = 20_000

    val presets = listOf(
        "Polish" to "Improve grammar, clarity, and flow while preserving the original meaning.",
        "Professional" to "Rewrite in a formal, workplace-appropriate tone while preserving the original meaning.",
        "Casual" to "Rewrite naturally and conversationally while preserving the original meaning.",
        "Shakespearean" to "Rewrite in theatrical, old-fashioned English while preserving the original meaning.",
        "Shorten" to "Make the text more concise without losing important meaning.",
        "Elaborate" to "Expand the point with useful detail while keeping the original intent.",
        "Custom instruction" to ""
    )

    fun validate(text: String, instruction: String): String? = when {
        text.isBlank() -> "Enter some text to rewrite."
        text.length > MAX_INPUT_CHARS -> "Text is too long. The limit is $MAX_INPUT_CHARS characters."
        instruction.isBlank() -> "Choose a transformation or enter a custom instruction."
        else -> null
    }

    fun requestBody(text: String, instruction: String): String =
        JSONObject().put("text", text).put("instruction", instruction).toString()

    fun parseResponse(raw: String): String {
        val result = JSONObject(raw).optString("text").trim()
        require(result.isNotEmpty()) { "The rewrite service returned an empty result." }
        return result
    }
}
