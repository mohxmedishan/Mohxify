package com.mohx.mohxify

import android.content.ClipDescription
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Mohxify's responsive typing keyboard with comma-prefixed GIF shortcuts. */
class MohxifyKeyboardService : InputMethodService() {
    // 0 = off, 1 = capitalize the next letter, 2 = caps lock.
    private var shiftState = 0
    private var numbers = false
    private var rootView: LinearLayout? = null
    private var handlingTrigger = false
    private val deleteHandler = Handler(Looper.getMainLooper())
    private var repeatingDelete = false

    private val repeatDelete = object : Runnable {
        override fun run() {
            if (!repeatingDelete) return
            repeat(3) { deleteBeforeCursor() }
            deleteHandler.postDelayed(this, 42L)
        }
    }

    override fun onCreateInputView(): View = buildKeyboard().also { rootView = it }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        shiftState = if (attribute?.let {
                it.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0
            } == true) 1 else 0
        numbers = false
        rootView?.let { render(it) }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        rootView?.let { render(it) }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (handlingTrigger || isSensitiveField() || newSelStart < 0 || newSelStart != newSelEnd) return
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(180, 0)?.toString() ?: return
        val match = Regex("(^|\\s)(,[A-Za-z0-9_-]+)\\s$").find(before) ?: return
        val typedShortcut = match.groupValues[2]
        val shortcutWithSeparator = typedShortcut + " "
        val normalized = typedShortcut.drop(1).replace("_", "").replace("-", "")
        val action = GifActionStore.all(this).firstOrNull {
            it.trigger.trim().replace(Regex("\\s+"), "").replace("_", "").replace("-", "")
                .equals(normalized, ignoreCase = true)
        } ?: return

        handlingTrigger = true
        try {
            if (connection.deleteSurroundingText(shortcutWithSeparator.length, 0)) {
                if (!insertGif(action.uri)) connection.commitText(shortcutWithSeparator, 1)
            }
        } finally {
            handlingTrigger = false
        }
    }

    private fun buildKeyboard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(5), dp(5), dp(5), dp(5))
        setBackgroundColor(Color.rgb(9, 10, 18))
        render(this)
    }

    private fun render(root: LinearLayout) {
        root.removeAllViews()
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        toolbar.addView(TextView(this).apply {
            text = "MOHXIFY"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(190, 166, 255))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), 0, dp(7), 0)
        }, LinearLayout.LayoutParams(0, -1, 1f))
        toolbar.addView(key("GIFs", accent = true) {
            try {
                startActivity(android.content.Intent(this, MohxifyActionsActivity::class.java).apply {
                    // Keep the GIF page in its own task so Back returns to the app we were typing in.
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                })
            } catch (_: Exception) {
                Toast.makeText(this, "Couldn't open GIF actions.", Toast.LENGTH_SHORT).show()
            }
        }, LinearLayout.LayoutParams(dp(58), dp(34)))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(36)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val gifActions = if (isSensitiveField()) emptyList() else GifActionStore.all(this)
        if (gifActions.isEmpty()) {
            actions.addView(TextView(this).apply {
                text = if (isSensitiveField()) "GIF shortcuts disabled in password fields" else "Save GIF actions to create shortcuts"
                textSize = 10f
                setTextColor(Color.rgb(155, 148, 176))
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(5), 0, 0, 0)
            })
        } else {
            gifActions.forEach { action ->
                val shortcut = shortcutFor(action.trigger)
                val group = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(Color.rgb(39, 37, 51), 8)
                }
                group.addView(key(",$shortcut") { insertKey(",$shortcut") },
                    LinearLayout.LayoutParams(-2, dp(32)))
                group.addView(key("GIF", accent = true) { insertGif(action.uri) },
                    LinearLayout.LayoutParams(dp(38), dp(32)).apply { leftMargin = dp(3) })
                actions.addView(group, LinearLayout.LayoutParams(-2, dp(34)).apply {
                    rightMargin = dp(6)
                })
            }
        }
        root.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(actions, android.widget.FrameLayout.LayoutParams(-2, -1))
        }, LinearLayout.LayoutParams(-1, dp(39)).apply {
            topMargin = dp(3)
            bottomMargin = dp(3)
        })

        if (numbers) {
            addKeyRow(root, listOf("1","2","3","4","5","6","7","8","9","0")) { insertKey(it) }
            addKeyRow(root, listOf("@","#","$","%","&","-","+","(",")","/")) { insertKey(it) }
            addKeyRow(root, listOf("=","*","\\"","'",":",";","!","?")) { insertKey(it) }
            addBottomRow(root, symbolMode = true)
        } else {
            addKeyRow(root, listOf("q","w","e","r","t","y","u","i","o","p")) { insertLetter(it) }
            addKeyRow(root, listOf("a","s","d","f","g","h","j","k","l")) { insertLetter(it) }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.addView(key(when (shiftState) { 0 -> "⇧"; 1 -> "⇧"; else -> "⇪" }, accent = shiftState != 0) {
                shiftState = when (shiftState) { 0 -> 1; 1 -> 2; else -> 0 }
                render(root)
            }, LinearLayout.LayoutParams(0, dp(43), 1.3f).apply { rightMargin = dp(3) })
            listOf("z","x","c","v","b","n","m").forEach { letter ->
                row.addView(key(displayLetter(letter)) { insertLetter(letter) },
                    LinearLayout.LayoutParams(0, dp(43), 1f).apply { rightMargin = dp(3) })
            }
            row.addView(deleteKey(), LinearLayout.LayoutParams(0, dp(43), 1.4f))
            root.addView(row, LinearLayout.LayoutParams(-1, dp(47)).apply { bottomMargin = dp(3) })
            addBottomRow(root, symbolMode = false)
        }
    }

    private fun displayLetter(letter: String): String = if (shiftState != 0) letter.uppercase() else letter

    private fun insertLetter(letter: String) {
        insertKey(displayLetter(letter))
        if (shiftState == 1) {
            shiftState = 0
            rootView?.let { render(it) }
        }
    }

    private fun shortcutFor(trigger: String): String {
        val normalized = trigger.trim().removePrefix(",").replace(Regex("\\s+"), "")
            .replace(Regex("[^A-Za-z0-9_-]"), "")
        return normalized.take(14).ifBlank { "gif" }
    }

    private fun addKeyRow(root: LinearLayout, labels: List<String>, onTap: (String) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        labels.forEachIndexed { index, label ->
            row.addView(key(label) { onTap(label) },
                LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                    if (index < labels.lastIndex) rightMargin = dp(3)
                })
        }
        root.addView(row, LinearLayout.LayoutParams(-1, dp(47)).apply { bottomMargin = dp(3) })
    }

    private fun addBottomRow(root: LinearLayout, symbolMode: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(key(if (symbolMode) "ABC" else "?123", accent = true) {
            numbers = !numbers
            render(root)
        }, LinearLayout.LayoutParams(dp(54), dp(43)).apply { rightMargin = dp(3) })
        row.addView(key(",", accent = false) { insertKey(",") },
            LinearLayout.LayoutParams(dp(33), dp(43)).apply { rightMargin = dp(3) })
        row.addView(key("English (US)", accent = false) { insertKey(" ") },
            LinearLayout.LayoutParams(0, dp(43), 1f).apply { rightMargin = dp(3) })
        row.addView(key(".", accent = false) { insertKey(".") },
            LinearLayout.LayoutParams(dp(33), dp(43)).apply { rightMargin = dp(3) })
        row.addView(deleteKey(), LinearLayout.LayoutParams(dp(43), dp(43)).apply { rightMargin = dp(3) })
        row.addView(key("↵", accent = true, longAction = { insertKey("\n") }) { performEnter() },
            LinearLayout.LayoutParams(dp(49), dp(43)))
        root.addView(row, LinearLayout.LayoutParams(-1, dp(47)))
    }

    private fun insertKey(value: String) {
        if (isSensitiveField() && value.length > 1) return
        currentInputConnection?.commitText(value, 1)
    }

    private fun deleteBeforeCursor() {
        val connection = currentInputConnection ?: return
        if (!connection.deleteSurroundingText(1, 0)) {
            connection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL))
            connection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
        }
    }

    private fun deleteKey() = key("⌫", accent = true) {
        deleteBeforeCursor()
    }.apply {
        setOnClickListener(null)
        setOnLongClickListener { true }
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(.92f).scaleY(.92f).alpha(.8f).setDuration(55).start()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    deleteBeforeCursor()
                    repeatingDelete = true
                    deleteHandler.removeCallbacks(repeatDelete)
                    deleteHandler.postDelayed(repeatDelete, 260L)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    repeatingDelete = false
                    deleteHandler.removeCallbacks(repeatDelete)
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(85).start()
                    true
                }
                else -> true
            }
        }
    }

    private fun performEnter() {
        val connection = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val isTextField = info?.inputType?.and(InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
        val isMultiline = info?.inputType?.and(InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (!isMultiline && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
            // Apps may hide the IME after Send/Done. Keep the keyboard available for the next message.
            requestShowSelf(0)
        } else if (isTextField && !isMultiline &&
            (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED)) {
            connection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
        } else {
            connection.commitText("\n", 1)
        }
    }

    private fun insertGif(rawUri: String): Boolean {
        if (isSensitiveField()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val connection = currentInputConnection
        val uri = Uri.parse(rawUri)
        if (connection == null || uri.scheme != "content") {
            Toast.makeText(this, "This GIF isn't available for direct insertion.", Toast.LENGTH_LONG).show()
            return false
        }
        return try {
            val description = ClipDescription("Mohxify GIF", arrayOf("image/gif"))
            val content = InputContentInfo(uri, description, null)
            val accepted = connection.commitContent(
                content,
                InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                Bundle()
            )
            if (!accepted) {
                Toast.makeText(this, "This chat app doesn't support direct GIF insertion. Try the GIFs page to share it.", Toast.LENGTH_LONG).show()
            }
            accepted
        } catch (_: Exception) {
            Toast.makeText(this, "Couldn't insert this GIF in the current app.", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun isSensitiveField(): Boolean {
        val inputType = currentInputEditorInfo?.inputType ?: return false
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun key(
        label: String,
        accent: Boolean = false,
        longAction: (() -> Unit)? = null,
        action: () -> Unit
    ) = TextView(this).apply {
        text = label
        textSize = if (label.length > 7) 10f else 16f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = rounded(if (accent) Color.rgb(91, 58, 168) else Color.rgb(39, 37, 51), 7)
        setPadding(dp(3), dp(4), dp(3), dp(4))
        isSingleLine = true
        isClickable = true
        isFocusable = true
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(.94f).scaleY(.94f).alpha(.82f).setDuration(55).start()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(95).start()
            }
            false
        }
        setOnClickListener { action() }
        if (longAction != null) setOnLongClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            longAction()
            true
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        repeatingDelete = false
        deleteHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
