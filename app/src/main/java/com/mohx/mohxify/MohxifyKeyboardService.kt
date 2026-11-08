package com.mohx.mohxify

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * First-party Mohxify IME foundation. All text is sent directly to the active
 * editor through InputConnection; this service does not transmit or log input.
 */
class MohxifyKeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var keyboardRoot: LinearLayout? = null

    override fun onCreateInputView(): View {
        return buildKeyboard().also { keyboardRoot = it }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        shifted = attribute?.let {
            it.inputType and android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0
        } ?: false
        symbols = false
        keyboardRoot?.let { refreshKeyboard(it) }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        keyboardRoot = null
    }

    private fun buildKeyboard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(4), dp(6), dp(4), dp(8))
        setBackgroundColor(Color.rgb(28, 27, 36))
        refreshKeyboard(this)
    }

    private fun refreshKeyboard(root: LinearLayout) {
        root.removeAllViews()
        val toolbar = TextView(this).apply {
            text = "MOHXIFY"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.rgb(190, 166, 255))
            setPadding(dp(8), 0, dp(8), dp(5))
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(25)))

        if (!symbols) {
            addRow(root, listOf("q","w","e","r","t","y","u","i","o","p"))
            addRow(root, listOf("a","s","d","f","g","h","j","k","l"))
            addRow(root, listOf("⇧","z","x","c","v","b","n","m","⌫"))
            addRow(root, listOf("?123",",","☺","space",".","↵"))
        } else {
            addRow(root, listOf("1","2","3","4","5","6","7","8","9","0"))
            addRow(root, listOf("@","#","$","%","&","-","+","(",")","/"))
            addRow(root, listOf("=、","*","\"","' ",":",";","!","?","⌫"))
            addRow(root, listOf("ABC",",","space",".","↵"))
        }
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
        setColor(if (key in listOf("⇧","⌫","?123","ABC","↵")) Color.rgb(77, 72, 94) else Color.rgb(57, 55, 68))
    }

    private fun handleKey(key: String) {
        val connection = currentInputConnection ?: return
        when (key) {
            "⇧" -> { shifted = !shifted; keyboardRoot?.let { refreshKeyboard(it) } }
            "?123" -> { symbols = true; keyboardRoot?.let { refreshKeyboard(it) } }
            "ABC" -> { symbols = false; keyboardRoot?.let { refreshKeyboard(it) } }
            "⌫" -> deleteBeforeCursor(connection)
            "space" -> { connection.commitText(" ", 1); shifted = false; keyboardRoot?.let { refreshKeyboard(it) } }
            "↵" -> sendEnter(connection)
            "☺" -> android.widget.Toast.makeText(this, "Emoji panel is coming in the next keyboard stage.", android.widget.Toast.LENGTH_SHORT).show()
            else -> {
                val output = if (!symbols && shifted) key.uppercase() else key
                connection.commitText(output, 1)
                if (shifted) { shifted = false; keyboardRoot?.let { refreshKeyboard(it) } }
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
        val count = Character.charCount(last)
        connection.deleteSurroundingText(count, 0)
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
