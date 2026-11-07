package com.mohx.mohxify

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var serviceButton: Button
    private lateinit var loadoutSpinner: Spinner
    private lateinit var list: LinearLayout
    private lateinit var renameButton: Button
    private lateinit var deleteButton: Button
    private lateinit var replacementCount: TextView
    private lateinit var showMoreButton: Button
    private var visibleRuleCount = 10
    private var displayedLoadoutId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoadoutStore.initialize(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refresh()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun render() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(18))
            setBackgroundColor(0xFFF4F5F7.toInt())
        }
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        content.addView(TextView(this).apply {
            text = "MOHXIFY"
            textSize = 29f
            setTextColor(0xFF17191C.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Your words. Your autocorrect."
            textSize = 15f
            setTextColor(0xFF687078.toInt())
            setPadding(0, 0, 0, dp(12))
        })

        status = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF343A40.toInt())
            setPadding(0, dp(5), 0, dp(8))
        }
        content.addView(status)

        content.addView(button("Turn Mohxify OFF") {
            LoadoutStore.setEnabled(this, !LoadoutStore.enabled(this))
            refresh()
        }.also { toggle ->
            toggle.tag = "master_toggle"
            toggle.text = "ENABLE / DISABLE MOHXIFY"
        })

        serviceButton = button("Enable accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        serviceButton.text = "ENABLE ACCESSIBILITY SERVICE"
        content.addView(serviceButton)

        val selectorRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), 0, dp(8))
        }
        loadoutSpinner = Spinner(this).apply {
            minimumHeight = dp(48)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        selectorRow.addView(loadoutSpinner, LinearLayout.LayoutParams(0, dp(50), 1f))
        renameButton = compactButton("✎") { renameActiveLoadout() }
        renameButton.contentDescription = "Rename selected loadout"
        selectorRow.addView(renameButton, iconParams())
        deleteButton = compactButton("×") { deleteActiveLoadout() }
        deleteButton.contentDescription = "Delete selected loadout"
        selectorRow.addView(deleteButton, iconParams())
        selectorRow.addView(compactButton("+") { createLoadout() }, iconParams())
        content.addView(selectorRow)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actionRow.addView(button("＋ ADD REPLACEMENT") { showRuleDialog(null) },
            LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(6) })
        actionRow.addView(button("ADD BY RULES") { showImportDialog() },
            LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(6) })
        content.addView(actionRow)

        content.addView(TextView(this).apply {
            text = "REPLACEMENTS"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF454B52.toInt())
            setPadding(0, dp(18), 0, dp(8))
        })
        replacementCount = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF687078.toInt())
            setPadding(dp(2), 0, 0, dp(6))
        }
        content.addView(replacementCount)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        showMoreButton = button("SHOW MORE") { }
        content.addView(showMoreButton)

        content.addView(button("COPY LOADOUT RULES") {
            copy(BulkRuleParser.export(LoadoutStore.active(this).rules))
            toast("Loadout rules copied")
        }.apply {
            setPadding(dp(8), dp(4), dp(8), dp(4))
        })

        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(page)
        refresh()
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(0xFF20262C.toInt())
        setBackgroundColor(0xFFE2E6E9.toInt())
        setOnClickListener { action() }
    }

    private fun compactButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = if (label == "+") 23f else 21f
        isAllCaps = false
        minWidth = dp(42)
        setTextColor(0xFF263238.toInt())
        setBackgroundColor(0xFFE2E6E9.toInt())
        setOnClickListener { action() }
    }

    private fun iconParams() = LinearLayout.LayoutParams(dp(46), dp(50)).apply {
        leftMargin = dp(5)
    }

    private fun refresh() {
        if (!::status.isInitialized) return
        val items = LoadoutStore.all(this)
        val active = LoadoutStore.active(this)
        val enabled = LoadoutStore.enabled(this)
        status.text = "Status: " + if (enabled) "ON" else "OFF"

        val toggle = findToggleButton()
        toggle?.text = if (enabled) "TURN MOHXIFY OFF" else "TURN MOHXIFY ON"

        val accessibilityEnabled = isMohxifyAccessibilityEnabled()
        serviceButton.visibility = if (accessibilityEnabled) View.GONE else View.VISIBLE

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        loadoutSpinner.adapter = adapter
        val selected = items.indexOfFirst { it.id == active.id }.coerceAtLeast(0)
        loadoutSpinner.setSelection(selected, false)
        loadoutSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosen = items.getOrNull(position) ?: return
                if (chosen.id != LoadoutStore.active(this@MainActivity).id) {
                    LoadoutStore.setActive(this@MainActivity, chosen.id)
                    visibleRuleCount = 10
                    displayedLoadoutId = chosen.id
                    refresh()
                }
            }
        }
        deleteButton.isEnabled = items.size > 1
        deleteButton.alpha = if (items.size > 1) 1f else 0.45f

        if (displayedLoadoutId != active.id) {
            displayedLoadoutId = active.id
            visibleRuleCount = 10
        }
        val totalRules = active.rules.size
        val shownRules = minOf(visibleRuleCount, totalRules)
        replacementCount.text = "Showing " + shownRules + " of " + totalRules + " replacements"
        showMoreButton.visibility = if (totalRules > 10) View.VISIBLE else View.GONE
        showMoreButton.text = if (shownRules < totalRules) {
            "SHOW MORE (" + (totalRules - shownRules) + " remaining)"
        } else "SHOW LESS"
        list.removeAllViews()
        active.rules.take(shownRules).forEach { rule ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(4), dp(5), dp(4))
                setBackgroundColor(0xFFFFFFFF.toInt())
                elevation = dp(1).toFloat()
            }
            row.addView(TextView(this).apply {
                text = rule.from + "   →   " + rule.to
                textSize = 16f
                setTextColor(0xFF20252A.toInt())
                setPadding(0, dp(8), 0, dp(8))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(compactButton("✎") { showRuleDialog(rule) })
            row.addView(compactButton("×") {
                AlertDialog.Builder(this)
                    .setMessage("Delete the rule “${rule.from} → ${rule.to}”?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        val current = LoadoutStore.active(this).rules
                        LoadoutStore.updateRules(this, current.filterNot { it == rule })
                        refresh()
                    }.show()
            })
            list.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(7) })
        }
        showMoreButton.setOnClickListener {
            visibleRuleCount = if (visibleRuleCount < totalRules) {
                minOf(visibleRuleCount + 10, totalRules)
            } else 10
            refresh()
        }
        if (active.rules.isEmpty()) list.addView(TextView(this).apply {
            text = "No replacements yet. Add one or import a list."
            textSize = 14f
            setTextColor(0xFF687078.toInt())
            setPadding(dp(12), dp(18), dp(12), dp(18))
        })
    }

    private fun findToggleButton(): Button? {
        fun search(view: View): Button? {
            if (view is Button && view.tag == "master_toggle") return view
            if (view is ViewGroup) for (i in 0 until view.childCount) {
                val found = search(view.getChildAt(i))
                if (found != null) return found
            }
            return null
        }
        return search(window.decorView)
    }

    private fun isMohxifyAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = "$packageName/$packageName.MohxifyAccessibilityService"
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun createLoadout() {
        val items = LoadoutStore.all(this)
        val nextNumber = (items.mapNotNull { Regex("^Loadout ([0-9]+)$", RegexOption.IGNORE_CASE)
            .matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 1) + 1
        val name = "Loadout $nextNumber"
        val created = Loadout(LoadoutStore.newId(), name, emptyList())
        LoadoutStore.save(this, items + created, created.id)
        refresh()
    }

    private fun renameActiveLoadout() {
        val current = LoadoutStore.active(this)
        nameDialog("Rename loadout", current.name) { name ->
            if (nameExists(name, current.id)) toast("That loadout name is already used")
            else {
                LoadoutStore.save(this, LoadoutStore.all(this).map {
                    if (it.id == current.id) it.copy(name = name) else it
                }, current.id)
                refresh()
            }
        }
    }

    private fun deleteActiveLoadout() {
        val items = LoadoutStore.all(this)
        if (items.size <= 1) return
        val current = LoadoutStore.active(this)
        AlertDialog.Builder(this).setTitle("Delete loadout?")
            .setMessage("Delete ${current.name} and all its rules?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                val remaining = items.filterNot { it.id == current.id }
                LoadoutStore.save(this, remaining, remaining.first().id)
                refresh()
            }.show()
    }

    private fun nameExists(name: String, exceptId: String? = null) =
        LoadoutStore.all(this).any { it.id != exceptId && it.name.equals(name.trim(), true) }

    private fun nameDialog(title: String, initial: String = "", onSave: (String) -> Unit) {
        val field = EditText(this).apply {
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(initial)
            hint = "Loadout name"
        }
        AlertDialog.Builder(this).setTitle(title).setView(field).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = field.text.toString().trim()
                if (name.isBlank()) toast("Enter a name") else onSave(name)
            }.show()
    }

    private fun showRuleDialog(existing: Rule?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val from = EditText(this).apply {
            hint = "Original word"; setSingleLine(true); setText(existing?.from ?: "")
        }
        val to = EditText(this).apply {
            hint = "Replacement"; setSingleLine(true); setText(existing?.to ?: "")
        }
        layout.addView(from); layout.addView(to)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add replacement" else "Edit replacement")
            .setView(layout).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val original = from.text.toString().trim()
                val replacement = to.text.toString().trim()
                val rules = LoadoutStore.active(this).rules
                when {
                    original.isEmpty() || replacement.isEmpty() -> toast("Enter both words first")
                    original.any { it.isWhitespace() } -> toast("Original must be a single word")
                    original.equals(replacement, true) -> toast("Those words are identical")
                    rules.any { it.from.equals(original, true) &&
                        (existing == null || !it.from.equals(existing.from, true)) } ->
                        toast("That original word already has a rule")
                    else -> {
                        LoadoutStore.updateRules(this, rules.filterNot {
                            existing != null && it.from.equals(existing.from, true)
                        } + Rule(original, replacement))
                        refresh()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showImportDialog() {
        val input = EditText(this).apply {
            hint = "what: wt;\njust: js;\nabout: abt;"
            minLines = 5
            gravity = Gravity.TOP
        }
        val scroll = ScrollView(this).apply { addView(input) }
        val dialog = AlertDialog.Builder(this).setTitle("Add by rules")
            .setView(scroll).setNegativeButton("Cancel", null).setPositiveButton("Preview", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val parsed = BulkRuleParser.parse(input.text.toString())
                showImportPreview(parsed)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showImportPreview(parsed: BulkRuleParser.Result) {
        val details = buildString {
            append("Valid rules: ${parsed.rules.size}\n")
            append("Duplicate entries ignored: ${parsed.duplicateCount}\n")
            append("Invalid entries: ${parsed.invalidEntries.size}")
            parsed.invalidEntries.forEach { append("\n\n• ${it.entry}\n  ${it.reason}") }
        }
        AlertDialog.Builder(this).setTitle("Rules preview").setMessage(details)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Replace loadout rules") { _, _ ->
                LoadoutStore.updateRules(this, parsed.rules)
                refresh()
                toast("Loadout rules replaced")
            }
            .setPositiveButton("Add / update rules") { _, _ ->
                val merged = LoadoutStore.active(this).rules.toMutableList()
                parsed.rules.forEach { incoming ->
                    val index = merged.indexOfFirst { it.from.equals(incoming.from, true) }
                    if (index >= 0) merged[index] = incoming else merged.add(incoming)
                }
                LoadoutStore.updateRules(this, merged)
                refresh()
                toast("Rules added")
            }.show()
    }

    private fun copy(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Mohxify loadout rules", text))
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
