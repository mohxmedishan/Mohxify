package com.mohx.mohxify

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class RewriteModeActivity : Activity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private val prefs by lazy { getSharedPreferences("mohxify_rewrite", MODE_PRIVATE) }
    private lateinit var source: EditText
    private lateinit var customInstruction: EditText
    private lateinit var preset: Spinner
    private lateinit var result: EditText
    private lateinit var rewriteButton: Button
    private lateinit var backendStatus: TextView
    private var working = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(20))
            setBackgroundColor(0xFF090A12.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "REWRITE MODE"; textSize = 10f; letterSpacing = .16f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFB59BFF.toInt())
        })
        content.addView(TextView(this).apply {
            text = "Transform the whole thought."; textSize = 25f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFF8F5FF.toInt())
            setPadding(0, dp(5), 0, dp(5))
        })
        content.addView(TextView(this).apply {
            text = "Rewrite runs only when you tap the button. Your shortcuts remain local and independent."
            textSize = 13f; setTextColor(0xFFAAA4BC.toInt()); setPadding(0, 0, 0, dp(14))
        })
        source = editor("Paste or type the sentence or paragraph you want to transform.", 5)
        content.addView(source, params(160).apply { bottomMargin = dp(12) })
        preset = Spinner(this).apply {
            adapter = ArrayAdapter(this@RewriteModeActivity, android.R.layout.simple_spinner_dropdown_item,
                RewriteMode.presets.map { it.first })
            background = rounded(0xFF24243A.toInt())
        }
        content.addView(label("TRANSFORMATION"))
        content.addView(preset, params(50).apply { bottomMargin = dp(10) })
        customInstruction = editor("Custom instruction, e.g. rewrite this as a short message about the science fair.", 3)
        content.addView(customInstruction, params(100).apply { bottomMargin = dp(10) })
        preset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                customInstruction.visibility = if (position == RewriteMode.presets.lastIndex) View.VISIBLE else View.GONE
            }
        }
        backendStatus = TextView(this).apply {
            textSize = 12f; setTextColor(0xFFFFC078.toInt()); setPadding(0, 0, 0, dp(8))
        }
        content.addView(backendStatus)
        content.addView(button("Configure secure backend") { configureBackend() }.apply {
            background = rounded(0xFF29243B.toInt())
        }, params(46).apply { bottomMargin = dp(10) })
        rewriteButton = button("Rewrite text") { runRewrite() }
        content.addView(rewriteButton, params(52).apply { bottomMargin = dp(16) })
        content.addView(label("REVIEW RESULT"))
        result = editor("Your rewritten text will appear here. Review it before using it.", 5).apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }
        content.addView(result, params(160).apply { bottomMargin = dp(10) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("Copy result") { copyResult() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(button("Use as input") {
            if (result.text.isNotBlank()) source.setText(result.text)
        }.apply { background = rounded(0xFF29243B.toInt()) },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(8) })
        content.addView(actions)
        content.addView(TextView(this).apply {
            text = "Mohxify cannot force-insert text into every app. Copy the reviewed result, switch to your app, and paste it. Password fields and unsupported custom editors remain outside QuickExpand's access."
            textSize = 11f; setTextColor(0xFFAAA4BC.toInt()); setPadding(0, dp(14), 0, 0)
        })
        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(page)
        refreshBackendStatus()
        if (intent?.getStringExtra(Intent.EXTRA_PROCESS_TEXT) != null) {
            source.setText(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))
        }
    }

    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 9f; letterSpacing = .13f
        typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFB59BFF.toInt())
        setPadding(0, 0, 0, dp(7))
    }

    private fun editor(hintText: String, lines: Int) = EditText(this).apply {
        hint = hintText; minLines = lines; gravity = Gravity.TOP
        textSize = 14f; setTextColor(0xFFF8F5FF.toInt()); setHintTextColor(0xFFAAA4BC.toInt())
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = rounded(0xFF24243A.toInt())
        inputType = android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }

    private fun params(height: Int) = LinearLayout.LayoutParams(-1, dp(height))
    private fun rounded(color: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color); cornerRadius = dp(14).toFloat()
    }
    private fun button(title: String, action: () -> Unit) = Button(this).apply {
        text = title; isAllCaps = false; textSize = 14f; setTextColor(0xFFF7F3FF.toInt())
        background = rounded(0xFF7040D8.toInt()); setOnClickListener { action() }
    }

    private fun configureBackend() {
        val field = EditText(this).apply {
            hint = "https://your-domain.example/rewrite"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(prefs.getString("endpoint", ""))
        }
        AlertDialog.Builder(this).setTitle("Rewrite backend")
            .setMessage("Enter your HTTPS backend endpoint. It must accept POST JSON {text, instruction} and return JSON {text}. Keep model-provider API keys on the server, never in the APK.")
            .setView(field).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val value = field.text.toString().trim()
                if (value.isNotEmpty() && !value.startsWith("https://", true)) {
                    Toast.makeText(this, "Only HTTPS endpoints are allowed.", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putString("endpoint", value).apply()
                    refreshBackendStatus()
                }
            }.show()
    }

    private fun refreshBackendStatus() {
        val configured = !prefs.getString("endpoint", "").isNullOrBlank()
        backendStatus.text = if (configured) "Backend configured · requests are sent only when you tap Rewrite."
            else "AI is not configured yet. Set up a secure backend to enable transformations."
        backendStatus.setTextColor(if (configured) 0xFFC5F36B.toInt() else 0xFFFFC078.toInt())
    }

    private fun runRewrite() {
        if (working) return
        val text = source.text.toString()
        val selected = RewriteMode.presets.getOrNull(preset.selectedItemPosition) ?: RewriteMode.presets.first()
        val instruction = if (selected.first == "Custom instruction") customInstruction.text.toString().trim()
            else selected.second
        val validation = RewriteMode.validate(text, instruction)
        if (validation != null) {
            Toast.makeText(this, validation, Toast.LENGTH_SHORT).show()
            return
        }
        val endpoint = prefs.getString("endpoint", "").orEmpty()
        if (endpoint.isBlank()) {
            AlertDialog.Builder(this).setTitle("Backend not configured")
                .setMessage("Rewrite Mode is ready, but no AI provider is connected. Configure your secure HTTPS backend first. QuickExpand continues working without it.")
                .setPositiveButton("Configure") { _, _ -> configureBackend() }
                .setNegativeButton("Cancel", null).show()
            return
        }
        working = true
        rewriteButton.isEnabled = false
        rewriteButton.text = "Rewriting…"
        result.setText("")
        Thread {
            try {
                val rewritten = RewriteServiceClient(endpoint).rewrite(text, instruction)
                runOnUiThread {
                    result.setText(rewritten)
                    working = false
                    rewriteButton.isEnabled = true
                    rewriteButton.text = "Rewrite text"
                }
            } catch (error: Exception) {
                runOnUiThread {
                    working = false
                    rewriteButton.isEnabled = true
                    rewriteButton.text = "Rewrite text"
                    Toast.makeText(this, error.message ?: "Rewrite failed. Check your connection.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun copyResult() {
        val value = result.text.toString().trim()
        if (value.isBlank()) {
            Toast.makeText(this, "There is no result to copy.", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Mohxify rewritten text", value))
        Toast.makeText(this, "Rewritten text copied. Paste it where you need it.", Toast.LENGTH_SHORT).show()
    }
}
