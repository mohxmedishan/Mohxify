package com.mohx.mohxify

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class RemoteGif(
    val id: String,
    val title: String,
    val previewUrl: String,
    val gifUrl: String
)

object GifSearchClient {
    private const val PREFS = "mohxify_gif_provider"
    private const val KEY_API = "giphy_api_key"

    fun apiKey(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_API, "").orEmpty()

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_API, key.trim()).apply()
    }

    fun search(context: Context, query: String, callback: (Result<List<RemoteGif>>) -> Unit) {
        val key = apiKey(context)
        if (key.isBlank()) {
            callback(Result.failure(IllegalStateException("Add a GIPHY API key in Mohxify Keyboard setup first.")))
            return
        }
        Thread {
            try {
                val params = "api_key=${enc(key)}&q=${enc(query.ifBlank { "reaction" })}&limit=24&rating=g&lang=en"
                val connection = (URL("https://api.giphy.com/v1/gifs/search?$params").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 12000
                    requestMethod = "GET"
                }
                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (status !in 200..299) throw IllegalStateException("GIPHY returned HTTP $status. Check the API key and connection.")
                val data = JSONObject(body).optJSONArray("data") ?: org.json.JSONArray()
                val gifs = (0 until data.length()).mapNotNull { i ->
                    val item = data.optJSONObject(i) ?: return@mapNotNull null
                    val images = item.optJSONObject("images") ?: return@mapNotNull null
                    val preview = images.optJSONObject("fixed_width_small")?.optString("url").orEmpty()
                    val original = images.optJSONObject("original")?.optString("url").orEmpty()
                    if (preview.startsWith("https://") && original.startsWith("https://")) {
                        RemoteGif(item.optString("id"), item.optString("title", "GIF"), preview, original)
                    } else null
                }
                callback(Result.success(gifs))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }.start()
    }

    fun download(context: Context, remote: RemoteGif): Uri {
        val connection = (URL(remote.gifUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Couldn't download this GIF.")
            val folder = File(context.filesDir, "gifs").apply { mkdirs() }
            val name = "${UUID.randomUUID()}.gif"
            val destination = File(folder, name)
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 12L * 1024L * 1024L) throw IllegalStateException("GIF is too large (12 MB limit).")
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (destination.length() == 0L) throw IllegalStateException("Downloaded GIF was empty.")
            return Uri.parse("content://${context.packageName}.gifprovider/gifs/$name")
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name())
}
