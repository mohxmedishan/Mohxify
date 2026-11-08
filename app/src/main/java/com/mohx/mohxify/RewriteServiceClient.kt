package com.mohx.mohxify

import java.net.HttpURLConnection
import java.net.URL

/** Calls a user-configured HTTPS backend. Provider secrets belong on that backend, never in this APK. */
class RewriteServiceClient(private val endpoint: String) {
    fun rewrite(text: String, instruction: String): String {
        require(endpoint.startsWith("https://", ignoreCase = true)) {
            "Configure a secure HTTPS rewrite backend first."
        }
        val error = RewriteMode.validate(text, instruction)
        require(error == null) { error ?: "Invalid rewrite request." }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use {
                it.write(RewriteMode.requestBody(text, instruction).toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("Rewrite service error ($code). Check your backend and try again.")
            }
            return RewriteMode.parseResponse(body)
        } finally {
            connection.disconnect()
        }
    }
}
