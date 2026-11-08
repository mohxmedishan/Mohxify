package com.mohx.mohxify

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Mohxify command pad, not a general-purpose typing keyboard.
 * Commands use the user's existing saved GIF actions and their trigger phrases.
 */
class MohxifyKeyboardService : InputMethodService() {
    private var rootView: LinearLayout? = null
    private var query = ""

    override fun onCreateInputView(): View = buildCommandPad().also { rootView = it }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        query = ""
        rootView?.let { render(it) }
    }

    private fun buildCommandPad() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setBackgroundColor(Color.rgb(9, 10, 18))
        render(this)
    }

    private fun render(root: LinearLayout) {
        root.removeAllViews()
        root.addView(TextView(this).apply {
            text = "MOHXIFY  /  COMMANDS"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(190, 166, 255))
            letterSpacing = 0.08f
        })
        root.addView(TextView(this).apply {
            text = "Run a trigger"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(5), 0, dp(3))
        })
        root.addView(TextView(this).apply {
            text = "Choose one of your saved Mohxify actions. This is a command pad, not a typing keyboard."
            textSize = 12f
            setTextColor(Color.rgb(180, 174, 198))
        })

        val search = EditText(this).apply {
            hint = "Search trigger phrases…"
            setSingleLine(true)
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(160, 153, 178))
            setPadding(dp(12), dp(4), dp(12), dp(4))
            background = rounded(Color.rgb(36, 34, 54), 13)
            setText(query)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s?.toString().orEmpty()
                    renderActions(actionsContainer(root))
                }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply {
            topMargin = dp(12)
            bottomMargin = dp(10)
        })

        root.addView(TextView(this).apply {
            text = "SAVED COMMANDS"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(190, 166, 255))
            setPadding(0, dp(3), 0, dp(7))
        })
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.tag = "command_list"
        root.addView(ScrollView(this).apply {
            isFillViewport = false
            addView(list, ScrollView.LayoutParams(-1, -2))
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        renderActions(list)

        root.addView(commandButton("Open Mohxify GIF screen") {
            try {
                startActivity(Intent(this, MohxifyActionsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                Toast.makeText(this, "Open Mohxify from your apps screen.", Toast.LENGTH_SHORT).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(8) })
        root.addView(commandButton("Done · close keyboard") { requestHideSelf(0) },
            LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(6) })
    }

    private fun actionsContainer(root: LinearLayout): LinearLayout {
        val scroll = (0 until root.childCount)
            .map { root.getChildAt(it) }
            .filterIsInstance<ScrollView>()
            .firstOrNull()
        return (scroll?.getChildAt(0) as? LinearLayout)
            ?: LinearLayout(this).also { it.orientation = LinearLayout.VERTICAL }
    }

    private fun renderActions(list: LinearLayout) {
        list.removeAllViews()
        val all = GifActionStore.all(this)
        val filtered = all.filter { query.isBlank() || it.trigger.contains(query.trim(), ignoreCase = true) }
        if (filtered.isEmpty()) {
            list.addView(TextView(this).apply {
                text = if (all.isEmpty()) {
                    "No saved commands yet. Open the Mohxify GIF screen to save a GIF with a trigger phrase."
                } else {
                    "No trigger matches that search."
                }
                textSize = 13f
                setTextColor(Color.rgb(180, 174, 198))
                setPadding(dp(2), dp(8), dp(2), dp(12))
            })
            return
        }
        filtered.forEach { action ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = rounded(Color.rgb(23, 21, 34), 15)
            }
            card.addView(TextView(this).apply {
                text = action.trigger
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            })
            card.addView(TextView(this).apply {
                text = "GIF command"
                textSize = 11f
                setTextColor(Color.rgb(170, 164, 188))
                setPadding(0, dp(3), 0, dp(8))
            })
            card.addView(commandButton("Run command  →") { runCommand(action) },
                LinearLayout.LayoutParams(-1, dp(40)))
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            })
        }
    }

    private fun runCommand(action: GifAction) {
        val uri = Uri.parse(action.uri)
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/gif"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, action.trigger)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val whatsapp = packageManager.getLaunchIntentForPackage("com.whatsapp") != null
            if (whatsapp) intent.setPackage("com.whatsapp")
            startActivity(Intent.createChooser(intent, "Run Mohxify GIF command")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(this, "Couldn't run this command. Re-save the GIF in Mohxify and try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun commandButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(91, 58, 168), 12)
        setPadding(dp(10), dp(7), dp(10), dp(7))
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
