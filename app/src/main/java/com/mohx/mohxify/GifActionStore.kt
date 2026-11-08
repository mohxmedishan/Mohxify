package com.mohx.mohxify

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class GifAction(val id: String, val trigger: String, val uri: String)

object GifActionStore {
    private const val PREFS = "mohxify_gif_actions"
    private const val KEY_ACTIONS = "actions"

    @Synchronized
    fun all(context: Context): List<GifAction> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ACTIONS, null)
            ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                val trigger = item.optString("trigger").trim()
                val uri = item.optString("uri")
                if (id.isBlank() || trigger.isBlank() || uri.isBlank()) null
                else GifAction(id, trigger, uri)
            }
        } catch (_: Exception) { emptyList() }
    }

    @Synchronized
    fun add(context: Context, trigger: String, uri: String) {
        val clean = trigger.trim()
        require(clean.isNotBlank() && clean.length <= 100)
        val updated = all(context).filterNot { it.trigger.equals(clean, true) } +
            GifAction(UUID.randomUUID().toString(), clean, uri)
        save(context, updated)
    }

    @Synchronized
    fun remove(context: Context, id: String) {
        save(context, all(context).filterNot { it.id == id })
    }

    private fun save(context: Context, actions: List<GifAction>) {
        val array = JSONArray()
        actions.forEach { action ->
            array.put(JSONObject().put("id", action.id).put("trigger", action.trigger).put("uri", action.uri))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTIONS, array.toString()).apply()
    }
}
