package com.mohx.mohxify

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var activeLabel: TextView
    private lateinit var list: LinearLayout
    private lateinit var loadoutChoices: RadioGroup

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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(18))
            setBackgroundColor(0xFFF5F5F5.toInt())
        }
        root.addView(TextView(this).apply {
            text = "MOHXIFY"; textSize = 27f
            setTextColor(0xFF151515.toInt()); typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "Your words. Your autocorrect."; textSize = 14f
            setTextColor(0xFF666666.toInt())
        })
        status = TextView(this).apply { textSize = 15f; setPadding(0, dp(14), 0, dp(4)) }
        root.addView(status)
        activeLabel = TextView(this).apply {
            textSize = 16f; typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF222222.toInt()); setPadding(0, dp(6), 0, dp(4))
        }
        root.addView(activeLabel)
        loadoutChoices = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        root.addView(loadoutChoices, matchWidth())
        root.addView(button("Enable / disable Mohxify") {
            LoadoutStore.setEnabled(this, !LoadoutStore.enabled(this)); refresh()
        })
        root.addView(button("Enable accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("Create loadout") { nameDialog("Create loadout") { name ->
            if (nameExists(name)) toast("A loadout with that name already exists")
            else {
                val created = Loadout(LoadoutStore.newId(), name, emptyList())
                LoadoutStore.save(this, LoadoutStore.all(this) + created, created.id); refresh()
            }
        } })
        root.addView(button("Rename active loadout") {
            val current = LoadoutStore.active(this)
            nameDialog("Rename loadout", current.name) { name ->
                if (nameExists(name, current.id)) toast("That loadout name is already used")
                else {
                    LoadoutStore.save(this, LoadoutStore.all(this).map {
                        if (it.id == current.id) it.copy(name = name) else it
                    }, current.id); refresh()
                }
            }
        })
        root.addView(button("Duplicate active loadout") {
            val current = LoadoutStore.active(this)
            nameDialog("Duplicate loadout", current.name + " copy") { name ->
                if (nameExists(name)) toast("That loadout name is already used")
                else {
                    val copyLoadout = Loadout(LoadoutStore.newId(), name, current.rules.toList())
                    LoadoutStore.save(this, LoadoutStore.all(this) + copyLoadout, copyLoadout.id); refresh()
                }
            }
        })
        root.addView(button("Delete active loadout") {
            val items = LoadoutStore.all(this)
            if (items.size <= 1) toast("You must keep at least one loadout")
            else AlertDialog.Builder(this).setTitle("Delete loadout?")
                .setMessage("Delete " + LoadoutStore.active(this).name + " and its rules?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    val remaining = items.filterNot { it.id == LoadoutStore.active(this).id }
                    LoadoutStore.save(this, remaining, remaining.first().id); refresh()
                }.show()
        })
        root.addView(TextView(this).apply {
            text = "RULES IN THIS LOADOUT"; textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(18), 0, dp(8)); setTextColor(0xFF333333.toInt())
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this); scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(button("＋ Add replacement") { showRuleDialog(null) })
        root.addView(button("Import rules") { showImportDialog() })
        root.addView(button("Copy as text") {
            copy(BulkRuleParser.export(LoadoutStore.active(this).rules)); toast("Current loadout copied as text")
        })
        root.addView(button("Copy ChatGPT prompt") {
            copy("Create a list of single-word text replacement rules. Output ONLY rules in this exact format, one per line: original: replacement; Use one original word with no spaces, a colon, then its replacement. Do not add explanations, headings, bullets, or code fences.")
            toast("Prompt copied")
        })
        root.addView(button("Export all loadouts (copy backup)") {
            copy(LoadoutStore.exportJson(this)); toast("Backup JSON copied to clipboard")
        })
        root.addView(button("Import all loadouts (paste backup)") { importBackup() })
        root.addView(button("Check for updates") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mohxmedishan/Mohxify/releases/latest")))
        })
        setContentView(root)
        refresh()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(6) }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; setOnClickListener { action() }
    }

    private fun refresh() {
        if (!::status.isInitialized) return
        val items = LoadoutStore.all(this)
        val active = LoadoutStore.active(this)
        status.text = "Status: " + if (LoadoutStore.enabled(this)) "ON" else "OFF"
        activeLabel.text = "Active loadout: " + active.name
        loadoutChoices.removeAllViews()
        items.forEach { item ->
            val choice = RadioButton(this).apply {
                text = item.name
                isChecked = item.id == active.id
                setOnClickListener {
                    LoadoutStore.setActive(this@MainActivity, item.id)
                    refresh()
                }
            }
            loadoutChoices.addView(choice)
        }
        list.removeAllViews()
        active.rules.forEach { rule ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(6), dp(4), dp(6)); setBackgroundColor(0xFFFFFFFF.toInt())
            }
            row.addView(TextView(this).apply {
                text = rule.from + "   →   " + rule.to; textSize = 16f; setTextColor(0xFF202020.toInt())
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this).apply {
                text = "Edit"; textSize = 14f; setPadding(dp(9), dp(8), dp(9), dp(8))
                setTextColor(0xFF2457A6.toInt()); setOnClickListener { showRuleDialog(rule) }
            })
            row.addView(TextView(this).apply {
                text = "✕"; textSize = 18f; setPadding(dp(10), dp(8), dp(8), dp(8))
                setTextColor(0xFFB3261E.toInt())
                setOnClickListener {
                    LoadoutStore.updateRules(this@MainActivity, LoadoutStore.active(this@MainActivity).rules.filterNot { it == rule })
                    refresh()
                }
            })
            list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(5)
            })
        }
        if (active.rules.isEmpty()) list.addView(TextView(this).apply {
            text = "No rules yet. Add one or import a list."; setPadding(dp(8), dp(12), dp(8), dp(12))
        })
    }

    private fun nameExists(name: String, exceptId: String? = null) =
        LoadoutStore.all(this).any { it.id != exceptId && it.name.equals(name.trim(), true) }

    private fun nameDialog(title: String, initial: String = "", onSave: (String) -> Unit) {
        val field = EditText(this).apply { setSingleLine(true); setText(initial); hint = "Loadout name" }
        AlertDialog.Builder(this).setTitle(title).setView(field).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = field.text.toString().trim()
                if (name.isBlank()) toast("Enter a name") else onSave(name)
            }.show()
    }

    private fun showRuleDialog(existing: Rule?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0)
        }
        val from = EditText(this).apply { hint = "Original word"; setSingleLine(true); setText(existing?.from ?: "") }
        val to = EditText(this).apply { hint = "Replacement"; setSingleLine(true); setText(existing?.to ?: "") }
        layout.addView(from); layout.addView(to)
        val dialog = AlertDialog.Builder(this).setTitle(if (existing == null) "Add replacement" else "Edit replacement")
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
                    rules.any { it.from.equals(original, true) && (existing == null || !it.from.equals(existing.from, true)) } ->
                        toast("That original word already has a rule")
                    else -> {
                        LoadoutStore.updateRules(this, rules.filterNot {
                            existing != null && it.from.equals(existing.from, true)
                        } + Rule(original, replacement))
                        refresh(); dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showImportDialog() {
        val input = EditText(this).apply {
            hint = "what: wt;\njust: js;\nabout: abt;"; minLines = 5; gravity = Gravity.TOP
        }
        val scroll = ScrollView(this); scroll.addView(input)
        val dialog = AlertDialog.Builder(this).setTitle("Import rules").setView(scroll)
            .setNegativeButton("Cancel", null).setPositiveButton("Preview", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val parsed = BulkRuleParser.parse(input.text.toString())
                showImportPreview(parsed); dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showImportPreview(parsed: BulkRuleParser.Result) {
        val details = buildString {
            append("Valid rules: " + parsed.rules.size + "\n")
            append("Duplicate entries ignored: " + parsed.duplicateCount + "\n")
            append("Invalid entries: " + parsed.invalidEntries.size)
            parsed.invalidEntries.forEach { append("\n\n• " + it.entry + "\n  " + it.reason) }
        }
        AlertDialog.Builder(this).setTitle("Import preview").setMessage(details)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Replace all rules") { _, _ ->
                LoadoutStore.updateRules(this, parsed.rules); refresh(); toast("Rules replaced")
            }
            .setPositiveButton("Merge / overwrite") { _, _ ->
                val merged = LoadoutStore.active(this).rules.toMutableList()
                parsed.rules.forEach { incoming ->
                    val index = merged.indexOfFirst { it.from.equals(incoming.from, true) }
                    if (index >= 0) merged[index] = incoming else merged.add(incoming)
                }
                LoadoutStore.updateRules(this, merged); refresh(); toast("Rules merged")
            }.show()
    }

    private fun importBackup() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val raw = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (raw.isNullOrBlank()) { toast("Clipboard is empty"); return }
        AlertDialog.Builder(this).setTitle("Restore backup?")
            .setMessage("This replaces all current loadouts with the backup in your clipboard.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Import") { _, _ ->
                if (LoadoutStore.importJson(this, raw)) { refresh(); toast("Backup restored") }
                else toast("That backup could not be read")
            }.show()
    }

    private fun copy(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Mohxify", text))
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
