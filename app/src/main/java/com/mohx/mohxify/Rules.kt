package com.mohx.mohxify

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class Rule(val from: String, val to: String)

object Rules {
    private const val PREFS = "mohxify"
    private const val KEY_RULES = "rules"
    private const val KEY_ENABLED = "enabled"

    fun defaults() = listOf(
        Rule("what", "wt"),
        Rule("just", "js"),
        Rule("about", "abt"),
        Rule("because", "bcs")
    )

    fun all(context: Context): List<Rule> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RULES, null) ?: return defaults()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val from = item.optString("from").trim()
                val to = item.optString("to").trim()
                if (from.isNotEmpty() && to.isNotEmpty()) Rule(from, to) else null
            }
        } catch (_: Exception) { defaults() }
    }

    fun save(context: Context, rules: List<Rule>) {
        val array = JSONArray()
        rules.forEach {
            array.put(JSONObject().put("from", it.from).put("to", it.to))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_RULES, array.toString()).apply()
    }

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun replaceWord(word: String, rules: List<Rule>): String =
        ReplacementEngine.replaceWord(word, rules.map { it.from to it.to })
}
