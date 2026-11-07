package com.mohx.mohxify

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Loadout(val id: String, val name: String, val rules: List<Rule>)

object LoadoutStore {
    private const val PREFS = "mohxify"
    private const val KEY_LOADOUTS = "loadouts_json"
    private const val KEY_ACTIVE = "active_loadout_id"
    private const val KEY_ENABLED = "enabled"
    private const val LEGACY_KEY_RULES = "rules"

    // A new installation starts with one empty loadout. No example rules are inserted.
    fun defaults(): List<Loadout> = listOf(Loadout("loadout-1", "Loadout 1", emptyList()))

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun initialize(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_LOADOUTS)) {
            val parsed = decode(p.getString(KEY_LOADOUTS, null))
            if (parsed.isNotEmpty()) {
                // Remove only the untouched sample loadouts from the previous app version.
                // Any user-edited or user-created data is preserved.
                val oldDefaults = listOf(
                    Loadout("shortcuts", "Shortcuts", listOf(
                        Rule("what", "wt"), Rule("just", "js"),
                        Rule("about", "abt"), Rule("because", "bcs")
                    )),
                    Loadout("shakespeare", "Shakespeare", listOf(
                        Rule("you", "thou"), Rule("your", "thy"), Rule("are", "art"),
                        Rule("yes", "aye"), Rule("no", "nay"), Rule("hello", "hark"),
                        Rule("friend", "companion"), Rule("why", "wherefore"),
                        Rule("where", "whither"), Rule("before", "ere"),
                        Rule("goodbye", "farewell"), Rule("love", "adore"),
                        Rule("my", "mine"), Rule("is", "doth"), Rule("the", "ye")
                    ))
                )
                if (parsed == oldDefaults) {
                    val clean = defaults()
                    p.edit().putString(KEY_LOADOUTS, encode(clean))
                        .putString(KEY_ACTIVE, clean.first().id).apply()
                    return
                }
                if (parsed.none { it.id == p.getString(KEY_ACTIVE, null) }) {
                    p.edit().putString(KEY_ACTIVE, parsed.first().id).apply()
                }
                return
            }
        }

        // Preserve any pre-loadout rules from older Mohxify versions.
        val legacy = p.getString(LEGACY_KEY_RULES, null)
        val migratedRules = decodeRules(legacy)
        val initial = listOf(Loadout(
            "loadout-1", "Loadout 1", migratedRules
        ))
        p.edit()
            .putString(KEY_LOADOUTS, encode(initial))
            .putString(KEY_ACTIVE, initial.first().id)
            .remove(LEGACY_KEY_RULES)
            .apply()
    }

    fun all(context: Context): List<Loadout> {
        initialize(context)
        return decode(prefs(context).getString(KEY_LOADOUTS, null))
    }

    fun active(context: Context): Loadout {
        val items = all(context)
        val id = prefs(context).getString(KEY_ACTIVE, items.first().id)
        return items.firstOrNull { it.id == id } ?: items.first()
    }

    fun setActive(context: Context, id: String) {
        val items = all(context)
        if (items.any { it.id == id }) prefs(context).edit().putString(KEY_ACTIVE, id).apply()
    }

    fun save(context: Context, loadouts: List<Loadout>, activeId: String = active(context).id) {
        require(loadouts.isNotEmpty()) { "At least one loadout is required" }
        val safeActive = if (loadouts.any { it.id == activeId }) activeId else loadouts.first().id
        prefs(context).edit()
            .putString(KEY_LOADOUTS, encode(loadouts))
            .putString(KEY_ACTIVE, safeActive)
            .apply()
    }

    fun updateRules(context: Context, rules: List<Rule>) {
        val current = active(context)
        save(context, all(context).map {
            if (it.id == current.id) it.copy(rules = rules) else it
        }, current.id)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun exportJson(context: Context): String {
        val root = JSONObject()
        root.put("format", "mohxify-loadouts-v1")
        root.put("activeLoadoutId", active(context).id)
        root.put("loadouts", JSONArray(encode(all(context))))
        return root.toString(2)
    }

    fun importJson(context: Context, raw: String): Boolean {
        return try {
            val root = JSONObject(raw)
            val array = root.optJSONArray("loadouts") ?: return false
            val imported = decode(array.toString())
            if (imported.isEmpty()) return false
            val activeId = root.optString("activeLoadoutId").takeIf { id ->
                imported.any { it.id == id }
            } ?: imported.first().id
            save(context, imported, activeId)
            true
        } catch (_: Exception) { false }
    }

    private fun encode(items: List<Loadout>): String {
        val array = JSONArray()
        items.forEach { loadout ->
            val item = JSONObject().put("id", loadout.id).put("name", loadout.name)
            val rules = JSONArray()
            loadout.rules.forEach { rules.put(JSONObject().put("from", it.from).put("to", it.to)) }
            item.put("rules", rules)
            array.put(item)
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<Loadout> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                val name = item.optString("name").trim().ifBlank { "Loadout" }
                val rules = decodeRules(item.optJSONArray("rules")?.toString())
                Loadout(id, name, rules)
            }.distinctBy { it.id }
        } catch (_: Exception) { emptyList() }
    }

    private fun decodeRules(raw: String?): List<Rule> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val from = item.optString("from").trim()
                val to = item.optString("to").trim()
                if (from.isNotEmpty() && to.isNotEmpty()) Rule(from, to) else null
            }
        } catch (_: Exception) { emptyList() }
    }

    fun newId(): String = UUID.randomUUID().toString()
}
