package com.mohx.mohxify

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private lateinit var masterToggle: Button
    private var visibleRuleCount = 10
    private var displayedLoadoutId: String? = null
    private var renderedLoadoutId: String? = null
    private var renderedRules: List<Rule>? = null
    private var renderedRuleCount = -1

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
            setPadding(dp(18), dp(20), dp(18), dp(16))
            setBackgroundColor(0xFFF3F5F9.toInt())
        }
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        content.addView(TextView(this).apply {
            text = "Mohxify"
            textSize = 27f
            setTextColor(0xFF17191C.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Personal text replacements"
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

        masterToggle = button("Turn Mohxify on") {
            LoadoutStore.setEnabled(this, !LoadoutStore.enabled(this))
            refresh()
        }
        content.addView(masterToggle)

        serviceButton = button("Enable accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        serviceButton.text = "Enable accessibility service"
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
            text = "Replacements"
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
        showMoreButton = button("Show more") { }
        content.addView(showMoreButton)

        content.addView(button("Copy loadout rules") {
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
        setTextColor(0xFF243247.toInt())
        background = rounded(0xFFE7ECF4.toInt(), 14)
        minHeight = dp(48)
        setOnClickListener { action() }
    }

    private fun compactButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = if (label == "+") 23f else 21f
        isAllCaps = false
        minWidth = dp(42)
        setTextColor(0xFF263238.toInt())
        background = rounded(0xFFE7ECF4.toInt(), 14)
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
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

        masterToggle.text = if (enabled) "Turn Mohxify off" else "Turn Mohxify on"

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
        replacementCount.text = "$shownRules of $totalRules replacements"
        showMoreButton.visibility = if (totalRules > 10) View.VISIBLE else View.GONE
        showMoreButton.text = if (shownRules < totalRules) {
            "Show 10 more (${totalRules - shownRules} left)"
        } else "Show fewer"
        val rulesNeedRendering = renderedLoadoutId != active.id ||
            renderedRules != active.rules || renderedRuleCount != shownRules
        if (rulesNeedRendering) {
            list.removeAllViews()
            active.rules.take(shownRules)
                .groupBy { it.from.lowercase(java.util.Locale.ROOT) }
                .values.forEach { synonyms ->
                    val first = synonyms.first()
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), dp(5), dp(4), dp(5))
                        background = rounded(0xFFFFFFFF.toInt(), 14)
                        elevation = dp(1).toFloat()
                    }
                    row.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@MainActivity).apply {
                            text = first.from
                            textSize = 15f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(0xFF20252A.toInt())
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = synonyms.joinToString("  ·  ") { it.to }
                            textSize = 13f
                            setTextColor(0xFF687078.toInt())
                        })
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    row.addView(compactButton("✎") { showRuleDialog(first) })
                    row.addView(compactButton("×") {
                        AlertDialog.Builder(this)
                            .setMessage("Delete “${first.from}” and all its synonyms?")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                val current = LoadoutStore.active(this).rules
                                LoadoutStore.updateRules(this, current.filterNot {
                                    it.from.equals(first.from, true)
                                })
                                refresh()
                            }.show()
                    })
                    list.addView(row, LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(7) })
                }
            if (active.rules.isEmpty()) list.addView(TextView(this).apply {
                text = "No replacements yet. Add one or import a list."
                textSize = 14f
                setTextColor(0xFF687078.toInt())
                setPadding(dp(12), dp(18), dp(12), dp(18))
            })
            renderedLoadoutId = active.id
            renderedRules = active.rules.toList()
            renderedRuleCount = shownRules
        }
        showMoreButton.setOnClickListener {
            visibleRuleCount = if (visibleRuleCount < totalRules) {
                minOf(visibleRuleCount + 10, totalRules)
            } else 10
            refresh()
        }
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
        val existingTargets = existing?.let { selected ->
            LoadoutStore.active(this).rules.filter { it.from.equals(selected.from, true) }
                .joinToString("\n") { it.to }
        }.orEmpty()
        val to = EditText(this).apply {
            hint = "Replacement (one per line for synonyms)"
            minLines = 2
            gravity = Gravity.TOP
            setText(existingTargets)
        }
        layout.addView(from); layout.addView(to)
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add replacement" else "Edit replacement")
            .setView(layout).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val original = from.text.toString().trim()
                val replacements = to.text.toString().split(Regex("[,;\\n]+"))
                    .map { it.trim() }.filter { it.isNotEmpty() }.distinctBy {
                        it.lowercase(java.util.Locale.ROOT)
                    }
                val rules = LoadoutStore.active(this).rules
                when {
                    original.isEmpty() || replacements.isEmpty() -> toast("Enter an original and at least one replacement")
                    original.any { it.isWhitespace() } -> toast("Original must be a single word")
                    replacements.any { it.equals(original, true) } -> toast("A word cannot replace itself")
                    rules.any { it.from.equals(original, true) &&
                        (existing == null || !it.from.equals(existing.from, true)) } ->
                        toast("That original word already has rules")
                    else -> {
                        val retained = rules.filterNot { it.from.equals(original, true) }
                        LoadoutStore.updateRules(this, retained + replacements.map { Rule(original, it) })
                        renderedRules = null
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
                parsed.rules.groupBy { it.from.lowercase(java.util.Locale.ROOT) }
                    .values.forEach { incomingGroup ->
                        val source = incomingGroup.first().from
                        merged.removeAll { it.from.equals(source, true) }
                        merged.addAll(incomingGroup)
                    }
                LoadoutStore.updateRules(this, merged)
                renderedRules = null
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
