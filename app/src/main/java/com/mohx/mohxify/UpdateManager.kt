package com.mohx.mohxify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object UpdateManager {
    data class Release(
        val version: String,
        val title: String,
        val notes: String,
        val downloadUrl: String,
        val sha256: String
    )

    fun fetchLatest(context: Context): Release? {
        val connection = (URL("https://api.github.com/repos/mohxmedishan/Mohxify/releases/latest")
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Mohxify-Android")
        }

        val release = try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("GitHub returned HTTP ${connection.responseCode}")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }

        val versionMatch = Regex("""(\\d+\\.\\d+\\.\\d+)""")
            .find(release.optString("tag_name"))
            ?: throw IllegalStateException("The latest release has no readable version number")
        val latestVersion = versionMatch.value
        val installedVersion = context.packageManager
            .getPackageInfo(context.packageName, 0).versionName.orEmpty()

        if (compareVersions(latestVersion, installedVersion) <= 0) return null

        val assets = release.optJSONArray("assets")
            ?: throw IllegalStateException("The latest release has no APK assets")
        var apkAsset: JSONObject? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (name.startsWith("mohxify-", ignoreCase = true) &&
                name.endsWith(".apk", ignoreCase = true)) {
                apkAsset = asset
                break
            }
        }
        val asset = apkAsset ?: throw IllegalStateException("No Mohxify APK was attached to the latest release")
        val digest = asset.optString("digest")
        if (!digest.startsWith("sha256:", ignoreCase = true)) {
            throw IllegalStateException("The release APK is missing its SHA-256 checksum")
        }

        return Release(
            version = latestVersion,
            title = release.optString("name", "Mohxify $latestVersion"),
            notes = release.optString("body").take(1800),
            downloadUrl = asset.optString("browser_download_url"),
            sha256 = digest.substringAfter(":").lowercase()
        )
    }

    fun downloadAndInstall(
        context: Context,
        release: Release,
        onProgress: (Int) -> Unit
    ) {
        val apkFile = File(context.cacheDir, "mohxify-update-${release.version}.apk")
        val connection = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mohxify-Android")
            setRequestProperty("Accept", "application/octet-stream")
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("APK download failed (HTTP ${connection.responseCode})")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                apkFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        if (!apkFile.isFile || apkFile.length() == 0L) {
            apkFile.delete()
            throw IllegalStateException("The downloaded APK is empty")
        }

        val actualDigest = MessageDigest.getInstance("SHA-256")
            .digest(apkFile.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        if (!actualDigest.equals(release.sha256, ignoreCase = true)) {
            apkFile.delete()
            throw SecurityException("The APK checksum did not match the GitHub release")
        }

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apkFile.length())
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                apkFile.inputStream().use { input ->
                    session.openWrite("mohxify-update.apk", 0, apkFile.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val callback = Intent(context, UpdateInstallReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    7316,
                    callback,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                session.commit(pendingIntent.intentSender)
            }
        } catch (error: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            apkFile.delete()
            throw error
        }
    }

    private fun compareVersions(first: String, second: String): Int {
        val left = first.split(".").map { it.toIntOrNull() ?: 0 }
        val right = second.split(".").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(left.size, right.size)) {
            val comparison = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
