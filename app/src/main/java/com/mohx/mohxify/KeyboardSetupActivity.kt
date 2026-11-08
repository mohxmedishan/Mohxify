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
            text = "Enable Mohxify Keyboard in Android settings, then choose it from the keyboard switcher inside a text field. Use it to insert replacements from your active loadout or run saved GIF trigger actions."
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
            text = "Mohxify Keyboard is a command pad for your saved trigger actions, not a general-purpose typing keyboard. Add and manage GIF triggers in the Mohxify GIF screen, then run them here."
            textSize = 12f
            setTextColor(Color.rgb(170, 164, 188))
            setPadding(0, dp(20), 0, 0)
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
