package com.mohx.mohxify

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputContentInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GifImportActivity : Activity() {
    private lateinit var trigger: EditText
    private lateinit var pasteField: GifContentEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(0xFF090A12.toInt())
        }
        root.addView(TextView(this).apply {
            text = "IMPORT FROM GBOARD"
            textSize = 12f
            setTextColor(0xFFBDA8FF.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Paste your GIF"
            textSize = 25f
            setTextColor(0xFFF5F1FF.toInt())
            setPadding(0, dp(8), 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "Tap the box, open Gboard's GIF tab, and select a GIF. Mohxify will capture the media attachment directly."
            textSize = 14f
            setTextColor(0xFFB8B1C8.toInt())
        })
        trigger = EditText(this).apply {
            hint = "Trigger phrase, e.g. I love you"
            setSingleLine(true)
            textSize = 15f
        }
        root.addView(trigger, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(20); bottomMargin = dp(12)
        })
        pasteField = GifContentEditText(this) { uri -> saveReceivedGif(uri) }.apply {
            hint = "Tap here, then choose a GIF in Gboard"
            minLines = 3
            textSize = 15f
            setTextColor(0xFFF5F1FF.toInt())
            setHintTextColor(0xFFAAA4BC.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFF242236.toInt())
        }
        root.addView(pasteField, LinearLayout.LayoutParams(-1, dp(130)).apply {
            topMargin = dp(8)
        })
        root.addView(TextView(this).apply {
            text = "Waiting for GIF attachment…"
            textSize = 12f
            setTextColor(0xFFAAA4BC.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        })
        root.addView(TextView(this).apply {
            text = "Done"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(14), dp(12), dp(14))
            setBackgroundColor(0xFF6D4BC3.toInt())
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(20) })
        setContentView(root)
    }

    private fun saveReceivedGif(source: Uri) {
        val phrase = trigger.text.toString().trim()
        if (phrase.isBlank()) {
            Toast.makeText(this, "Enter a trigger before importing the GIF", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val folder = File(filesDir, "gifs").apply { mkdirs() }
            val name = "${UUID.randomUUID()}.gif"
            val destination = File(folder, name)
            contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { "Gboard did not provide a readable GIF stream" }
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            require(destination.length() > 0) { "The received GIF was empty" }
            val savedUri = Uri.parse("content://$packageName.gifprovider/gifs/$name")
            GifActionStore.add(this, phrase, savedUri.toString())
            Toast.makeText(this, "GIF saved to trigger: $phrase", Toast.LENGTH_LONG).show()
            finish()
        } catch (error: Exception) {
            Toast.makeText(this, "Couldn't import this GIF: ${error.message ?: "unsupported media"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

private class GifContentEditText(
    context: Context,
    private val onGifReceived: (Uri) -> Unit
) : EditText(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val target = super.onCreateInputConnection(outAttrs) ?: return null
        outAttrs.contentMimeTypes = arrayOf("image/gif", "image/*")
        return object : InputConnectionWrapper(target, false) {
            override fun commitContent(
                inputContentInfo: InputContentInfo,
                flags: Int,
                opts: Bundle?
            ): Boolean {
                val description = inputContentInfo.description
                val isGif = (0 until description.mimeTypeCount).any {
                    description.getMimeType(it).equals("image/gif", ignoreCase = true)
                }
                if (!isGif) return super.commitContent(inputContentInfo, flags, opts)
                return try {
                    inputContentInfo.requestPermission()
                    onGifReceived(inputContentInfo.contentUri)
                    true
                } catch (_: Exception) {
                    false
                } finally {
                    try { inputContentInfo.releasePermission() } catch (_: Exception) { }
                }
            }
        }
    }
}
