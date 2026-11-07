package com.mohx.mohxify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refresh()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(0xFFF5F5F5.toInt())
        }
        val title = TextView(this).apply {
            text = "MOHXIFY"
            textSize = 28f
            setTextColor(0xFF151515.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title)
        root.addView(TextView(this).apply {
            text = "Your words. Your autocorrect."
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(18), 0, dp(12))
        }
        root.addView(status)

        val toggle = Button(this).apply {
            text = "Enable / disable Mohxify"
            setOnClickListener {
                Rules.setEnabled(this@MainActivity, !Rules.enabled(this@MainActivity))
                refresh()
            }
        }
        root.addView(toggle, matchWidth())

        val accessibility = Button(this).apply {
            text = "Enable accessibility service"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        root.addView(accessibility, matchWidth())

        root.addView(TextView(this).apply {
            text = "REPLACEMENT RULES"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(22), 0, dp(8))
            setTextColor(0xFF333333.toInt())
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val add = Button(this).apply {
            text = "＋ Add replacement"
            setOnClickListener { showAddDialog() }
        }
        root.addView(add, matchWidth())

        setContentView(root)
        refresh()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(8) }

    private fun refresh() {
        status.text = "Status: " + if (Rules.enabled(this)) "ON" else "OFF"
        list.removeAllViews()
        Rules.all(this).forEach { rule ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(7), dp(4), dp(7))
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val label = TextView(this).apply {
                text = "${rule.from}   →   ${rule.to}"
                textSize = 16f
                setTextColor(0xFF202020.toInt())
            }
            row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val edit = TextView(this).apply {
                text = "Edit"
                textSize = 14f
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setTextColor(0xFF2457A6.toInt())
                setOnClickListener { showRuleDialog(rule) }
            }
            row.addView(edit)
            val remove = TextView(this).apply {
                text = "✕"
                textSize = 18f
                setPadding(dp(12), dp(8), dp(8), dp(8))
                setTextColor(0xFFB3261E.toInt())
                setOnClickListener {
                    Rules.save(this@MainActivity, Rules.all(this@MainActivity).filterNot { it == rule })
                    refresh()
                }
            }
            row.addView(remove)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = dp(5)
            list.addView(row, params)
        }
    }

    private fun showAddDialog() = showRuleDialog(null)

    private fun showRuleDialog(existing: Rule?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val from = EditText(this).apply {
            hint = "Original word (e.g. because)"
            setSingleLine(true)
            setText(existing?.from ?: "")
        }
        val to = EditText(this).apply {
            hint = "Replacement (e.g. bcs)"
            setSingleLine(true)
            setText(existing?.to ?: "")
        }
        layout.addView(from)
        layout.addView(to)

        android.app.AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add replacement" else "Edit replacement")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val original = from.text.toString().trim()
                        val replacement = to.text.toString().trim()
                        when {
                            original.isEmpty() || replacement.isEmpty() ->
                                Toast.makeText(this, "Enter both words first", Toast.LENGTH_SHORT).show()
                            original.equals(replacement, ignoreCase = true) ->
                                Toast.makeText(this, "Those words are identical", Toast.LENGTH_SHORT).show()
                            Rules.all(this).any {
                                it.from.equals(original, ignoreCase = true) &&
                                    (existing == null || !it.from.equals(existing.from, ignoreCase = true))
                            } -> Toast.makeText(this, "That original word already has a rule", Toast.LENGTH_SHORT).show()
                            else -> {
                                val updated = Rules.all(this).filterNot {
                                    existing != null && it.from.equals(existing.from, ignoreCase = true)
                                } + Rule(original, replacement)
                                Rules.save(this, updated)
                                refresh()
                                dialog.dismiss()
                            }
                        }
                    }
                }
                dialog.show()
            }
    }
}
