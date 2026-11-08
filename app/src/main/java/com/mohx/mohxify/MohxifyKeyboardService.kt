package com.mohx.mohxify

import android.content.ClipDescription
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Mohxify's typing keyboard with a compact row of saved GIF trigger actions. */
class MohxifyKeyboardService : InputMethodService() {
    private var caps = false
    private var numbers = false
    private var rootView: LinearLayout? = null

    override fun onCreateInputView(): View = buildKeyboard().also { rootView = it }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        caps = attribute?.let {
            it.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0
        } ?: false
        rootView?.let { render(it) }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        rootView?.let { render(it) }
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
        })
        toolbar.addView(key("GIFs", accent = true) {
            try {
                startActivity(android.content.Intent(this, MohxifyActionsActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                Toast.makeText(this, "Open Mohxify to manage GIF actions.", Toast.LENGTH_SHORT).show()
            }
        }, LinearLayout.LayoutParams(dp(55), dp(34)))
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(36)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val gifActions = if (isSensitiveField()) emptyList() else GifActionStore.all(this)
        if (gifActions.isEmpty()) {
            actions.addView(TextView(this).apply {
                text = if (isSensitiveField()) "Commands disabled in password fields" else "Saved GIF triggers appear here"
                textSize = 10f
                setTextColor(Color.rgb(155, 148, 176))
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(5), 0, 0, 0)
            })
        } else {
            gifActions.forEach { action ->
                actions.addView(key(action.trigger, accent = false) { insertGif(action.uri) },
                    LinearLayout.LayoutParams(-2, dp(34)).apply {
                        rightMargin = dp(5)
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
            addKeyRow(root, listOf("=","*","\"","'",":",";","!","?")) { insertKey(it) }
            addBottomRow(root, symbolMode = true)
        } else {
            addKeyRow(root, listOf("q","w","e","r","t","y","u","i","o","p")) { insertKey(if (caps) it.uppercase() else it) }
            addKeyRow(root, listOf("a","s","d","f","g","h","j","k","l")) { insertKey(if (caps) it.uppercase() else it) }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.addView(key("⇧", accent = caps) { caps = !caps; render(root) },
                LinearLayout.LayoutParams(0, dp(43), 1.25f).apply { rightMargin = dp(3) })
            listOf("z","x","c","v","b","n","m").forEach { letter ->
                row.addView(key(if (caps) letter.uppercase() else letter) { insertKey(if (caps) letter.uppercase() else letter) },
                    LinearLayout.LayoutParams(0, dp(43), 1f).apply { rightMargin = dp(3) })
            }
            row.addView(key("⌫", accent = true) { deleteBeforeCursor() },
                LinearLayout.LayoutParams(0, dp(43), 1.35f))
            root.addView(row, LinearLayout.LayoutParams(-1, dp(47)).apply { bottomMargin = dp(3) })
            addBottomRow(root, symbolMode = false)
        }
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
        }, LinearLayout.LayoutParams(dp(58), dp(43)).apply { rightMargin = dp(3) })
        row.addView(key(",", accent = false) { insertKey(",") },
            LinearLayout.LayoutParams(dp(38), dp(43)).apply { rightMargin = dp(3) })
        row.addView(key("English (US)", accent = false) { insertKey(" ") },
            LinearLayout.LayoutParams(0, dp(43), 1f).apply { rightMargin = dp(3) })
        row.addView(key(".", accent = false) { insertKey(".") },
            LinearLayout.LayoutParams(dp(38), dp(43)).apply { rightMargin = dp(3) })
        row.addView(key("↵", accent = true) { performEnter() },
            LinearLayout.LayoutParams(dp(55), dp(43)))
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

    private fun performEnter() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            connection.commitText("\n", 1)
        }
    }

    private fun insertGif(rawUri: String) {
        if (isSensitiveField()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(this, "Direct GIF insertion needs Android 7 or newer.", Toast.LENGTH_SHORT).show()
            return
        }
        val connection = currentInputConnection
        val uri = Uri.parse(rawUri)
        if (connection == null || uri.scheme != "content") {
            Toast.makeText(this, "This GIF can't be inserted directly. Re-import it in Mohxify.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val description = ClipDescription("Mohxify GIF", arrayOf("image/gif"))
            val content = InputContentInfo(uri, description, null)
            val accepted = connection.commitContent(
                content,
                InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                Bundle()
            )
            if (!accepted) {
                Toast.makeText(this, "This app doesn't support direct GIF insertion. Try a chat app that accepts GIF keyboard content.", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Couldn't insert this GIF. Re-import it in Mohxify and try again.", Toast.LENGTH_LONG).show()
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

    private fun key(label: String, accent: Boolean = false, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = if (label.length > 7) 10f else 16f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = rounded(if (accent) Color.rgb(91, 58, 168) else Color.rgb(39, 37, 51), 7)
        setPadding(dp(3), dp(4), dp(3), dp(4))
        isSingleLine = true
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
