package com.mohx.mohxify

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class KeyboardSetupActivity : Activity() {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(Color.rgb(9, 10, 18))
        }
        root.addView(TextView(this).apply {
            text = "MOHXIFY KEYBOARD"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(190, 166, 255))
        })
        root.addView(TextView(this).apply {
            text = "Set up your keyboard"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "Enable Mohxify Keyboard in Android settings, then choose it from the keyboard switcher whenever you type. Your typing stays on this device."
            textSize = 14f
            setTextColor(Color.rgb(196, 190, 213))
            setLineSpacing(dp(3).toFloat(), 1f)
        })
        root.addView(actionButton("Open keyboard settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(20) })
        root.addView(actionButton("Choose Mohxify Keyboard") {
            try {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            } catch (_: Exception) {
                Toast.makeText(this, "Open Android keyboard settings to select Mohxify.", Toast.LENGTH_LONG).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
        root.addView(TextView(this).apply {
            text = "Live GIF search (GIPHY)"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(20), 0, dp(6))
        })
        root.addView(TextView(this).apply {
            text = "To enable live search, create a GIPHY API key from the GIPHY developer dashboard and save it here. The key stays in this app's local preferences."
            textSize = 12f
            setTextColor(Color.rgb(170, 164, 188))
        })
        val apiKeyField = EditText(this).apply {
            hint = "GIPHY API key"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setText(GifSearchClient.apiKey(this@KeyboardSetupActivity))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(170, 164, 188))
        }
        root.addView(apiKeyField, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        root.addView(actionButton("Save GIF API key") {
            GifSearchClient.saveApiKey(this, apiKeyField.text.toString())
            Toast.makeText(this, if (apiKeyField.text.isNullOrBlank()) "Live GIF search disabled." else "GIPHY key saved on this device.", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(6) })
        root.addView(TextView(this).apply {
            text = "Core typing, emoji selection, a saved clipboard panel, and GIF-trigger search are included. GIF insertion depends on whether the app you're typing in accepts GIF content."
            textSize = 12f
            setTextColor(Color.rgb(170, 164, 188))
            setPadding(0, dp(18), 0, 0)
        })
        root.addView(Button(this).apply {
            text = "Done"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(20) })
        setContentView(android.widget.ScrollView(this).apply {
            isFillViewport = true
            addView(root, ViewGroup.LayoutParams(-1, -2))
        })
    }

    private fun actionButton(textValue: String, action: () -> Unit) = Button(this).apply {
        text = textValue
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.rgb(109, 75, 195))
        }
        setOnClickListener { action() }
    }
}
