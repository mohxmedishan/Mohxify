package com.mohx.mohxify

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.view.Gravity
import android.view.View
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
    private enum class CommandFilter { ALL, TEXT, GIFS }
    private var commandFilter = CommandFilter.ALL

    override fun onCreateInputView(): View = buildCommandPad().also { rootView = it }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        commandFilter = CommandFilter.ALL
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
            text = "Choose a text replacement or run a saved GIF action. This is a command pad, not a typing keyboard."
            textSize = 12f
            setTextColor(Color.rgb(180, 174, 198))
        })

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(8))
        }
        fun addFilter(label: String, filter: CommandFilter) {
            tabs.addView(commandButton(label) {
                commandFilter = filter
                render(root)
            }.apply {
                background = rounded(
                    if (commandFilter == filter) Color.rgb(91, 58, 168) else Color.rgb(36, 34, 54),
                    11
                )
            }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                if (filter != CommandFilter.ALL) leftMargin = dp(6)
            })
        }
        addFilter("All", CommandFilter.ALL)
        addFilter("Text rules", CommandFilter.TEXT)
        addFilter("GIF actions", CommandFilter.GIFS)
        root.addView(tabs)

        root.addView(TextView(this).apply {
            text = when (commandFilter) { CommandFilter.ALL -> "ALL COMMANDS"; CommandFilter.TEXT -> "TEXT COMMANDS"; CommandFilter.GIFS -> "GIF COMMANDS" }
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(190, 166, 255))
            setPadding(0, dp(3), 0, dp(7))
        })
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.tag = "command_list"
        root.addView(ScrollView(this).apply {
            isFillViewport = false
            addView(list, android.widget.FrameLayout.LayoutParams(-1, -2))
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
        if (isSensitiveField()) {
            list.addView(TextView(this).apply {
                text = "Mohxify commands are disabled in password fields."
                textSize = 13f
                setTextColor(Color.rgb(180, 174, 198))
                setPadding(dp(2), dp(8), dp(2), dp(12))
            })
            return
        }
        val activeLoadout = LoadoutStore.active(this)
        val allGifActions = GifActionStore.all(this)
        val textRules = if (commandFilter == CommandFilter.GIFS) emptyList() else activeLoadout.rules
        val gifActions = if (commandFilter == CommandFilter.TEXT) emptyList() else allGifActions

        if (textRules.isEmpty() && gifActions.isEmpty()) {
            list.addView(TextView(this).apply {
                text = if (activeLoadout.rules.isEmpty() && allGifActions.isEmpty()) {
                    "No commands saved yet. Add text rules in your active loadout or GIF actions in the Mohxify GIF screen."
                } else {
                    "No commands in this section yet."
                }
                textSize = 13f
                setTextColor(Color.rgb(180, 174, 198))
                setPadding(dp(2), dp(8), dp(2), dp(12))
            })
            return
        }

        if (textRules.isNotEmpty()) {
            addSectionHeader(list, "TEXT COMMANDS", "${activeLoadout.name} · ${textRules.size}")
            textRules.forEach { rule ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = rounded(Color.rgb(23, 21, 34), 15)
                }
                card.addView(TextView(this).apply {
                    text = rule.from
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                })
                card.addView(TextView(this).apply {
                    text = "Replaces with  →  ${rule.to}"
                    textSize = 12f
                    setTextColor(Color.rgb(190, 166, 255))
                    setPadding(0, dp(3), 0, dp(8))
                })
                card.addView(commandButton("Insert replacement  →") {
                    val connection = currentInputConnection
                    if (connection == null) {
                        Toast.makeText(this@MohxifyKeyboardService, "Open a text field first.", Toast.LENGTH_SHORT).show()
                    } else {
                        connection.commitText(rule.to, 1)
                    }
                }, LinearLayout.LayoutParams(-1, dp(40)))
                list.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(8)
                })
            }
        }

        if (gifActions.isNotEmpty()) {
            addSectionHeader(list, "GIF COMMANDS", "${gifActions.size} saved")
            gifActions.forEach { action ->
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
                    text = "Saved GIF action"
                    textSize = 11f
                    setTextColor(Color.rgb(170, 164, 188))
                    setPadding(0, dp(3), 0, dp(8))
                })
                card.addView(commandButton("Run GIF command  →") { runCommand(action) },
                    LinearLayout.LayoutParams(-1, dp(40)))
                list.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(8)
                })
            }
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

    private fun addSectionHeader(list: LinearLayout, title: String, detail: String) {
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MohxifyKeyboardService).apply {
                text = title
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(190, 166, 255))
            }, LinearLayout.LayoutParams(0, dp(28), 1f))
            addView(TextView(this@MohxifyKeyboardService).apply {
                text = detail
                textSize = 10f
                setTextColor(Color.rgb(160, 153, 178))
            })
        }, LinearLayout.LayoutParams(-1, dp(30)))
    }

    private fun runCommand(action: GifAction) {
        val uri = Uri.parse(action.uri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/gif"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (packageManager.getLaunchIntentForPackage("com.whatsapp") != null) {
                // This command is designed around the existing WhatsApp GIF workflow.
                intent.setPackage("com.whatsapp")
                startActivity(intent)
            } else {
                startActivity(Intent.createChooser(intent, "Run Mohxify GIF command")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (_: Exception) {
            try {
                intent.setPackage(null)
                startActivity(Intent.createChooser(intent, "Choose an app for this GIF")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                Toast.makeText(this, "Couldn't run this command. Re-save the GIF in Mohxify and try again.", Toast.LENGTH_LONG).show()
            }
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
