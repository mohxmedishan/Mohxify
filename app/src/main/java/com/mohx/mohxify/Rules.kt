package com.mohx.mohxify

import android.content.Context

/** Compatibility facade retained for existing app components. */
object Rules {
    fun defaults() = LoadoutStore.defaults().first().rules
    fun all(context: Context): List<Rule> = LoadoutStore.active(context).rules
    fun save(context: Context, rules: List<Rule>) = LoadoutStore.updateRules(context, rules)
    fun enabled(context: Context): Boolean = LoadoutStore.enabled(context)
    fun setEnabled(context: Context, enabled: Boolean) = LoadoutStore.setEnabled(context, enabled)
    fun replaceWord(word: String, rules: List<Rule>): String =
        ReplacementEngine.replaceWord(word, rules.map { it.from to it.to })
}
