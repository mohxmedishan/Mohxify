package com.mohx.mohxify

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ClipDescription
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import org.json.JSONArray

/**
 * Mohxify's on-device IME. Typed characters are delivered to the active editor
 * through InputConnection; this service does not transmit or log input.
 */
class MohxifyKeyboardService : InputMethodService() {
    private var shifted = false
    private var capsLock = false
    private var symbols = false
    private var panel = Panel.KEYS
    private var keyboardRoot: LinearLayout? = null

    private enum class Panel { KEYS, EMOJI, CLIPBOARD, GIFS }

    override fun onCreateInputView(): View =
        buildKeyboard().also { keyboardRoot = it }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        panel = Panel.KEYS
        symbols = false
        val type = attribute?.inputType ?: 0
        val textClass = type and android.text.InputType.TYPE_MASK_CLASS ==
            android.text.InputType.TYPE_CLASS_TEXT
        val caps = type and android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0
        shifted = textClass && caps
        capsLock = false
        keyboardRoot?.let { refreshKeyboard(it) }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        keyboardRoot = null
    }

    private fun buildKeyboard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(4), dp(6), dp(4), dp(8))
        setBackgroundColor(Color.rgb(28, 27, 36))
        refreshKeyboard(this)
    }

    private fun refreshKeyboard(root: LinearLayout) {
        root.removeAllViews()
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        toolbar.addView(TextView(this).apply {
            text = "MOHXIFY"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.rgb(190, 166, 255))
        }, LinearLayout.LayoutParams(0, dp(34), 1f))
        toolbar.addView(toolButton("☺") { panel = if (panel == Panel.EMOJI) Panel.KEYS else Panel.EMOJI; refreshKeyboard(root) })
        toolbar.addView(toolButton("▤") { panel = if (panel == Panel.CLIPBOARD) Panel.KEYS else Panel.CLIPBOARD; refreshKeyboard(root) })
        toolbar.addView(toolButton("GIF") { panel = if (panel == Panel.GIFS) Panel.KEYS else Panel.GIFS; refreshKeyboard(root) })
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(34)))

        when (panel) {
            Panel.KEYS -> if (!symbols) {
                addRow(root, listOf("q","w","e","r","t","y","u","i","o","p"))
                addRow(root, listOf("a","s","d","f","g","h","j","k","l"))
                addRow(root, listOf("⇧","z","x","c","v","b","n","m","⌫"))
                addRow(root, listOf("?123",",","☺","space",".","↵"))
            } else {
                addRow(root, listOf("1","2","3","4","5","6","7","8","9","0"))
                addRow(root, listOf("@","#","$","%","&","-","+","(",")","/"))
                addRow(root, listOf("=", "*", "`", "'", ":", ";", "!", "?", "⌫"))
                addRow(root, listOf("ABC",",","space",".","↵"))
            }
            Panel.EMOJI -> {
                val emojis = listOf("😀","😂","🥹","😍","😭","😎","🥳","🤔","❤️","🔥","✨","🙏","👍","👎","👏","💀","🎉","💜","🌸","🐣","🐍","🍎","🍓","🌈","💯","😊","😴","🤍","🫶","🙃")
                emojis.chunked(6).forEach { group -> addRow(root, group) }
                addRow(root, listOf("ABC"))
            }
            Panel.CLIPBOARD -> renderClipboardPanel(root)
            Panel.GIFS -> renderGifPanel(root)
        }
    }

    private fun renderGifPanel(root: LinearLayout) {
        root.addView(TextView(this).apply {
            text = "Find a saved GIF by its trigger phrase"
            textSize = 13f
            setTextColor(Color.rgb(220, 214, 235))
            setPadding(dp(10), dp(8), dp(10), dp(6))
        })
        val search = EditText(this).apply {
            hint = "Try: good morning"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(160, 153, 178))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackground(keyBackground("GIF"))
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(44)).apply {
            setMargins(dp(6), dp(2), dp(6), dp(8))
        })
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(results, LinearLayout.LayoutParams(-1, 0, 1f))
        fun refreshResults(query: String) {
            results.removeAllViews()
            val actions = GifActionStore.all(this@MohxifyKeyboardService)
                .filter { query.isBlank() || it.trigger.contains(query.trim(), ignoreCase = true) }
                .take(8)
            if (actions.isEmpty()) {
                results.addView(TextView(this).apply {
                    text = if (GifActionStore.all(this@MohxifyKeyboardService).isEmpty())
                        "No saved GIFs yet. Open Mohxify Actions to add one."
                    else "No matching GIF triggers."
                    textSize = 12f
                    setTextColor(Color.rgb(190, 183, 208))
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                })
            }
            actions.forEach { action ->
                results.addView(toolButton("GIF  ·  ${action.trigger}") {
                    insertOrShareGif(action)
                }, LinearLayout.LayoutParams(-1, dp(42)).apply {
                    setMargins(dp(5), dp(2), dp(5), dp(3))
                })
            }
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = refreshResults(s?.toString().orEmpty())
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        refreshResults("")
        addRow(root, listOf("ABC"))
    }

    private fun insertOrShareGif(action: GifAction) {
        val uri = Uri.parse(action.uri)
        val connection = currentInputConnection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
            connection != null && currentInputEditorInfo != null) {
            try {
                val description = ClipDescription(action.trigger, arrayOf("image/gif"))
                val content = InputContentInfo(uri, description, null)
                val accepted = connection.commitContent(
                    content,
                    InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                    Bundle()
                )
                if (accepted) {
                    panel = Panel.KEYS
                    keyboardRoot?.let { refreshKeyboard(it) }
                    return
                }
            } catch (_: Exception) { }
        }
        // Editors that do not accept rich content can still receive the GIF via Android's share sheet.
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/gif"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, action.trigger)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(send, "Share GIF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Toast.makeText(this, "This app doesn't accept GIF insertion directly; choose the chat in the share sheet.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Couldn't open this GIF. Try re-importing it in Mohxify Actions.", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderClipboardPanel(root: LinearLayout) {
        if (isSensitiveEditor()) {
            root.addView(TextView(this).apply {
                text = "Clipboard history is disabled for password and sensitive fields."
                textSize = 13f
                setTextColor(Color.rgb(220, 214, 235))
                setPadding(dp(12), dp(12), dp(12), dp(12))
            })
            addRow(root, listOf("ABC"))
            return
        }
        root.addView(toolButton("Save current clipboard") { saveCurrentClipboard(); refreshKeyboard(root) },
            LinearLayout.LayoutParams(-1, dp(42)))
        val items = clipboardHistory()
        if (items.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "No saved clipboard items yet. Tap “Save current clipboard” to save the text currently on your clipboard."
                textSize = 13f
                setTextColor(Color.rgb(220, 214, 235))
                setPadding(dp(12), dp(10), dp(12), dp(10))
            })
        } else {
            items.take(5).forEach { value ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.addView(toolButton(value.take(26)) {
                    currentInputConnection?.commitText(value, 1)
                    panel = Panel.KEYS
                    refreshKeyboard(root)
                }, LinearLayout.LayoutParams(0, dp(42), 1f))
                row.addView(toolButton("×") {
                    saveClipboardHistory(clipboardHistory().filterNot { it == value })
                    refreshKeyboard(root)
                })
                root.addView(row, LinearLayout.LayoutParams(-1, dp(44)))
            }
            root.addView(toolButton("Clear history") { saveClipboardHistory(emptyList()); refreshKeyboard(root) },
                LinearLayout.LayoutParams(-1, dp(40)))
        }
        addRow(root, listOf("ABC"))
    }

    private fun saveCurrentClipboard() {
        if (isSensitiveEditor()) return
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = manager.primaryClip
        if (clip == null || clip.itemCount == 0) return
        val value = clip.getItemAt(0).coerceToText(this)?.toString()?.trim().orEmpty()
        if (value.isBlank()) return
        saveClipboardHistory((listOf(value) + clipboardHistory().filterNot { it == value }).take(10))
    }

    private fun isSensitiveEditor(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return false
        val inputClass = type and android.text.InputType.TYPE_MASK_CLASS
        val variation = type and android.text.InputType.TYPE_MASK_VARIATION
        return (inputClass == android.text.InputType.TYPE_CLASS_TEXT &&
            variation in listOf(
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )) ||
            (inputClass == android.text.InputType.TYPE_CLASS_NUMBER &&
                variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun clipboardHistory(): List<String> {
        val raw = getSharedPreferences("mohxify_keyboard_clipboard", Context.MODE_PRIVATE)
            .getString("history", "[]") ?: "[]"
        return try {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { json.optString(it).takeIf(String::isNotBlank) }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveClipboardHistory(values: List<String>) {
        val json = JSONArray()
        values.take(10).forEach(json::put)
        getSharedPreferences("mohxify_keyboard_clipboard", Context.MODE_PRIVATE)
            .edit().putString("history", json.toString()).apply()
    }

    private fun toolButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        setTextColor(Color.rgb(250, 247, 255))
        background = keyBackground(label)
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        stateListAnimator = null
        setOnClickListener { action() }
    }

    private fun addRow(root: LinearLayout, keys: List<String>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        keys.forEach { key ->
            val label = when (key) {
                "space" -> "English (US)"
                "⇧" -> if (shifted) "⬆" else "⇧"
                else -> key
            }
            val button = Button(this).apply {
                text = label
                textSize = when (key) { "space" -> 11f; "⇧", "⌫", "↵", "?123", "ABC" -> 13f; else -> 17f }
                isAllCaps = false
                setTextColor(Color.rgb(250, 247, 255))
                background = keyBackground(key)
                setPadding(dp(1), 0, dp(1), 0)
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                stateListAnimator = null
                setOnClickListener { handleKey(key) }
            }
            val weight = if (key == "space") 4.2f else if (key in listOf("⇧","⌫","?123","ABC","↵")) 1.35f else 1f
            row.addView(button, LinearLayout.LayoutParams(0, dp(46), weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
        root.addView(row, LinearLayout.LayoutParams(-1, dp(50)))
    }

    private fun keyBackground(key: String) = GradientDrawable().apply {
        cornerRadius = dp(6).toFloat()
        setColor(if (key in listOf("⇧","⌫","?123","ABC","↵","GIF","▤","☺")) Color.rgb(77, 72, 94) else Color.rgb(57, 55, 68))
    }

    private fun handleKey(key: String) {
        val connection = currentInputConnection ?: return
        when (key) {
            "ABC" -> { panel = Panel.KEYS; symbols = false; refreshKeyboard(keyboardRoot ?: return) }
            "⇧" -> {
                if (shifted && !capsLock) capsLock = true
                else if (capsLock) { capsLock = false; shifted = false }
                else shifted = true
                keyboardRoot?.let { refreshKeyboard(it) }
            }
            "?123" -> { symbols = true; refreshKeyboard(keyboardRoot ?: return) }
            "⌫" -> deleteBeforeCursor(connection)
            "space" -> {
                connection.commitText(" ", 1)
                if (!capsLock) shifted = false
                keyboardRoot?.let { refreshKeyboard(it) }
            }
            "↵" -> sendEnter(connection)
            "☺" -> { panel = Panel.EMOJI; keyboardRoot?.let { refreshKeyboard(it) } }
            else -> {
                val output = if (panel == Panel.EMOJI) key else if (!symbols && shifted) key.uppercase() else key
                connection.commitText(output, 1)
                if (panel == Panel.EMOJI) { panel = Panel.KEYS; keyboardRoot?.let { refreshKeyboard(it) } }
                else if (shifted && !capsLock) { shifted = false; keyboardRoot?.let { refreshKeyboard(it) } }
            }
        }
    }

    private fun deleteBeforeCursor(connection: InputConnection) {
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
            return
        }
        val before = connection.getTextBeforeCursor(2, 0) ?: return
        if (before.isEmpty()) return
        val last = Character.codePointBefore(before, before.length)
        connection.deleteSurroundingText(Character.charCount(last), 0)
    }

    private fun sendEnter(connection: InputConnection) {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            connection.commitText("\n", 1)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
