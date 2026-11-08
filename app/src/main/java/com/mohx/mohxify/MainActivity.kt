package com.mohx.mohxify

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import org.json.JSONObject
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var serviceButton: Button
    private lateinit var keyboardSetupButton: View
    private lateinit var loadoutSpinner: Spinner
    private lateinit var list: LinearLayout
    private lateinit var renameButton: Button
    private lateinit var deleteButton: Button
    private lateinit var replacementCount: TextView
    private lateinit var showMoreButton: Button
    private lateinit var masterToggle: Button
    private lateinit var updateStatus: TextView
    private lateinit var updateButton: Button
    private var updateCheckRunning = false
    private var downloadingUpdate = false
    private var awaitingInstallPermission = false
    private var pendingRelease: UpdateManager.Release? = null
    private var visibleRuleCount = 10
    private var displayedLoadoutId: String? = null
    private var renderedLoadoutId: String? = null
    private var renderedRules: List<Rule>? = null
    private var renderedRuleCount = -1
    private var importTextField: EditText? = null
    private lateinit var searchField: EditText
    private var searchQuery = ""
    private var renderedSearchQuery: String? = null
    companion object {
        private const val REQUEST_OPEN_RULES = 4101
        private const val REQUEST_SAVE_LOADOUT = 4102
        private const val REQUEST_SAVE_BACKUP = 4103
        private const val REQUEST_OPEN_BACKUP = 4104
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoadoutStore.initialize(this)
        render()
        checkForUpdates(silent = true)
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refresh()
        if (awaitingInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
            awaitingInstallPermission = false
            pendingRelease?.let { startUpdateDownload(it) }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun currentVersion(): String = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"

    private fun isDebugBuild(): Boolean =
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun render() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
            setBackgroundColor(0xFF090A12.toInt())
        }
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(18))
        }
        brand.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_mohxify_launcher)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Mohxify logo"
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        brand.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0)
            addView(TextView(this@MainActivity).apply {
                text = "Mohxify"; textSize = 25f
                typeface = Typeface.create("casual", Typeface.BOLD)
                setTextColor(0xFFF8F5FF.toInt())
            })
            addView(TextView(this@MainActivity).apply {
                text = "YOUR WORDS. YOUR RULES."; textSize = 9f; letterSpacing = .16f
                setTextColor(0xFFB5A8D8.toInt())
            })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        brand.addView(TextView(this).apply {
            text = "v${currentVersion()}"; textSize = 11f; setTextColor(0xFFCDBBFF.toInt())
            setPadding(dp(10), dp(6), dp(10), dp(6)); background = rounded(0xFF211A35.toInt(), 30)
        })
        content.addView(brand)

        val hero = panel(0xFF17132A.toInt(), 26)
        hero.addView(TextView(this).apply {
            text = "YOUR PERSONAL TEXT TOOLKIT"; textSize = 10f; letterSpacing = .15f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFBBA4FF.toInt())
        })
        hero.addView(TextView(this).apply {
            text = "Make every word\nsound like you."; textSize = 29f
            setLineSpacing(dp(2).toFloat(), 1f); typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt()); setPadding(0, dp(9), 0, dp(7))
        })
        hero.addView(TextView(this).apply {
            text = "Personal text rules and a dedicated command pad."
            textSize = 13f; setTextColor(0xFFC6BED7.toInt())
        })
        status = TextView(this).apply {
            textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFE6D9FF.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8)); background = rounded(0xFF30214E.toInt(), 30)
        }
        serviceStatus = TextView(this).apply {
            textSize = 9f; letterSpacing = .04f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFBBA4FF.toInt()); gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8)); background = rounded(0xFF211D31.toInt(), 30)
        }
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(15), 0, dp(10))
            addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(serviceStatus)
        }
        hero.addView(statusRow)
        masterToggle = button("Turn Mohxify on") {
            LoadoutStore.setEnabled(this, !LoadoutStore.enabled(this)); refresh()
        }
        hero.addView(masterToggle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
        serviceButton = button("Enable accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }.apply { background = rounded(0xFF2A2440.toInt(), 15); setTextColor(0xFFE7DDFF.toInt()) }
        hero.addView(serviceButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(9) })
        hero.addView(button("How to use Mohxify  →") { showHowToUse() }.apply {
            background = rounded(0xFF29243B.toInt(), 14)
            setTextColor(0xFFE8DDFF.toInt())
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply {
            topMargin = dp(8)
        })
        keyboardSetupButton = button("Set up Mohxify Keyboard  →") {
            startActivity(Intent(this@MainActivity, KeyboardSetupActivity::class.java))
        }.apply {
            background = rounded(0xFF6D4BC3.toInt(), 14)
            setTextColor(0xFFFFFFFF.toInt())
        }
        hero.addView(keyboardSetupButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(8) })
        hero.addView(TextView(this).apply {
            text = "Works in supported editable fields. Password fields are skipped; some apps use custom editors."
            textSize = 11f; setTextColor(0xFFAAA0C0.toInt()); setPadding(dp(2), dp(12), dp(2), 0)
        })
        content.addView(hero)

        val gifCard = panel()
        addSectionHeading(gifCard, "GIF ACTIONS", "Save keyboard GIFs to reusable triggers")
        gifCard.addView(TextView(this).apply {
            text = "Import GIFs, assign trigger phrases, and manage saved actions for the Mohxify command pad."
            textSize = 12f; setTextColor(0xFFAAA4BC.toInt())
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        })
        gifCard.addView(button("Open GIF Actions  →") {
            startActivity(Intent(this@MainActivity, MohxifyActionsActivity::class.java))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        content.addView(gifCard)

        val loadoutCard = panel()
        addSectionHeading(loadoutCard, "YOUR WORKSPACES", "Switch your vocabulary")
        loadoutSpinner = Spinner(this).apply {
            minimumHeight = dp(50); background = rounded(0xFF24243A.toInt(), 13)
            setPadding(dp(8), 0, dp(8), 0)
        }
        loadoutCard.addView(loadoutSpinner, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        val workspaceActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        renameButton = compactButton("Edit") { renameActiveLoadout() }.apply {
            contentDescription = "Rename selected workspace"
        }
        deleteButton = compactButton("Delete") { deleteActiveLoadout() }.apply {
            contentDescription = "Delete selected workspace"
        }
        workspaceActions.addView(renameButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(4) })
        workspaceActions.addView(deleteButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        workspaceActions.addView(compactButton("Copy") { duplicateActiveLoadout() },
            LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
        workspaceActions.addView(compactButton("+ New") { createLoadout() },
            LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(4) })
        loadoutCard.addView(workspaceActions)
        content.addView(loadoutCard)

        val rulesCard = panel()
        addSectionHeading(rulesCard, "YOUR DICTIONARY", "Every shortcut, organized")
        replacementCount = TextView(this).apply {
            textSize = 12f; setTextColor(0xFFAAA5BC.toInt()); setPadding(0, 0, 0, dp(10))
        }
        searchField = EditText(this).apply {
            hint = "Search words and replacements"; setSingleLine(true); textSize = 14f
            setTextColor(0xFFF8F5FF.toInt()); setHintTextColor(0xFFAAA4BC.toInt())
            inputType = InputType.TYPE_CLASS_TEXT; background = rounded(0xFF24243A.toInt(), 14)
            setPadding(dp(14), 0, dp(14), 0)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim().orEmpty(); visibleRuleCount = 10
                    renderedSearchQuery = null; refresh()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        rulesCard.addView(searchField, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)).apply { bottomMargin = dp(10) })
        rulesCard.addView(replacementCount)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rulesCard.addView(list)
        showMoreButton = button("Show more") { }.apply {
            background = rounded(0xFF24243A.toInt(), 14); setTextColor(0xFFE8DDFF.toInt())
        }
        rulesCard.addView(showMoreButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(8) })
        val ruleActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, 0) }
        ruleActions.addView(button("+  Add replacement") { showRuleDialog(null) },
            LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(5) })
        ruleActions.addView(button("Import rules") { showImportDialog() },
            LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(5) })
        rulesCard.addView(ruleActions)
        rulesCard.addView(button("Try a shortcut  ↗") { showRuleTester() }.apply {
            background = rounded(0xFF29243B.toInt(), 14)
            setTextColor(0xFFE8DDFF.toInt())
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply {
            topMargin = dp(7)
        })
        content.addView(rulesCard)

        val dataCard = panel()
        addSectionHeading(dataCard, "PORTABILITY", "Your data stays yours")
        dataCard.addView(button("Copy loadout rules") {
            copy(BulkRuleParser.export(LoadoutStore.active(this).rules)); toast("Loadout rules copied")
        })
        dataCard.addView(button("Download loadout (.txt)") { downloadActiveLoadout() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) })
        dataCard.addView(button("Back up all loadouts (.json)") { downloadBackup() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) })
        dataCard.addView(button("Restore from backup") { restoreBackup() }.apply {
            background = rounded(0xFF24243A.toInt(), 14); setTextColor(0xFFE8DDFF.toInt())
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) })
        content.addView(dataCard)

        val updateCard = panel()
        addSectionHeading(updateCard, "APP UPDATES", "Keep Mohxify current")
        updateStatus = TextView(this).apply {
            text = if (isDebugBuild()) "Debug build · signed releases need a one-time reinstall" else "Installed version ${currentVersion()} · Checking for updates when online"
            textSize = 12f
            setTextColor(0xFFAAA4BC.toInt())
            setPadding(0, 0, 0, dp(10))
        }
        updateCard.addView(updateStatus)
        updateButton = button(if (isDebugBuild()) "Switch to signed release" else "Check for updates") {
            if (isDebugBuild()) showDebugMigrationDialog()
            else {
                val release = pendingRelease
                if (release == null) checkForUpdates(silent = false) else showUpdateDialog(release)
            }
        }.apply {
            background = rounded(0xFF29243B.toInt(), 14)
            setTextColor(0xFFE8DDFF.toInt())
        }
        updateCard.addView(updateButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
        content.addView(updateCard)

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(8))
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_mohxify_launcher)
                contentDescription = "Mohxify logo"
            }, LinearLayout.LayoutParams(dp(22), dp(22)))
            addView(TextView(this@MainActivity).apply {
                text = "MOHXIFY · MADE FOR YOUR LANGUAGE"
                textSize = 9f
                letterSpacing = .12f
                setTextColor(0xFFAAA4BC.toInt())
                setPadding(dp(7), 0, 0, 0)
            })
        }
        content.addView(footer)
        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(page)
        refresh()
    }

    private fun checkForUpdates(silent: Boolean) {
        if (!::updateButton.isInitialized || updateCheckRunning) return
        if (isDebugBuild()) {
            updateStatus.text = "Debug build · back up your loadouts and switch to the signed release once"
            updateButton.text = "Switch to signed release"
            return
        }
        updateCheckRunning = true
        updateButton.isEnabled = false
        updateStatus.text = "Checking GitHub Releases…"
        Thread {
            try {
                val release = UpdateManager.fetchLatest(this)
                runOnUiThread {
                    updateCheckRunning = false
                    updateButton.isEnabled = true
                    pendingRelease = release
                    if (release == null) {
                        updateStatus.text = "You're up to date · v${currentVersion()}"
                        updateButton.text = "Check again"
                    } else {
                        updateStatus.text = "Version ${release.version} is available"
                        updateButton.text = "Review update"
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    updateCheckRunning = false
                    updateButton.isEnabled = true
                    updateStatus.text = "Couldn't check right now. Tap to retry."
                    updateButton.text = "Try again"
                    if (!silent) toast(error.message ?: "Could not check for updates")
                }
            }
        }.start()
    }

    private fun showDebugMigrationDialog() {
        AlertDialog.Builder(this)
            .setTitle("One-time switch to signed releases")
            .setMessage("This installation is a debug build. Android will not let a differently signed release replace it. Back up all loadouts, uninstall this build, install the latest signed APK from GitHub Releases, then restore your backup. Future signed releases can use the in-app updater.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Open releases") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mohxmedishan/Mohxify/releases/latest")))
            }
            .show()
    }

    private fun showUpdateDialog(release: UpdateManager.Release) {
        val details = buildString {
            append("Version ${release.version} is available.\n\n")
            if (release.notes.isNotBlank()) append(release.notes)
            else append("Install the latest Mohxify release.")
            append("\n\nThe APK will be downloaded and verified before Android opens its installer.")
        }
        AlertDialog.Builder(this)
            .setTitle(release.title)
            .setMessage(details)
            .setNegativeButton("Later", null)
            .setPositiveButton("Download & install") { _, _ -> startUpdateDownload(release) }
            .show()
    }

    private fun startUpdateDownload(release: UpdateManager.Release) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            awaitingInstallPermission = true
            AlertDialog.Builder(this)
                .setTitle("Allow Mohxify to install updates")
                .setMessage("Android requires permission before Mohxify can hand a downloaded update to the system installer. You can still cancel the installation there.")
                .setNegativeButton("Not now") { _, _ -> awaitingInstallPermission = false }
                .setPositiveButton("Open settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                }
                .show()
            return
        }
        if (downloadingUpdate) return
        downloadingUpdate = true
        val progress = ProgressDialog(this).apply {
            setTitle("Downloading Mohxify")
            setMessage("Verifying the release before installation…")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            setProgress(0)
            setCancelable(false)
            show()
        }
        Thread {
            try {
                UpdateManager.downloadAndInstall(this, release) { value ->
                    runOnUiThread { if (progress.isShowing) progress.progress = value }
                }
                runOnUiThread {
                    downloadingUpdate = false
                    progress.dismiss()
                    updateStatus.text = "Update downloaded · waiting for Android installer"
                    updateButton.text = "Check for updates"
                    pendingRelease = null
                    toast("Confirm the Mohxify update in Android's installer")
                }
            } catch (error: Exception) {
                runOnUiThread {
                    downloadingUpdate = false
                    progress.dismiss()
                    updateStatus.text = "Update couldn't be installed. You can retry."
                    updateButton.text = "Try again"
                    toast(error.message ?: "Update download failed")
                }
            }
        }.start()
    }

    private fun panel(color: Int = 0xFF151625.toInt(), radius: Int = 22) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(13), dp(13), dp(13))
            background = GradientDrawable().apply {
                setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), 0xFF302B43.toInt())
            }
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(13) }
        }

    private fun addSectionHeading(parent: LinearLayout, eyebrow: String, heading: String) {
        parent.addView(TextView(this).apply {
            text = eyebrow; textSize = 9f; letterSpacing = .14f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFFB59BFF.toInt())
        })
        parent.addView(TextView(this).apply {
            text = heading; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFF6F2FF.toInt()); setPadding(0, dp(4), 0, dp(12))
        })
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(0xFFF7F3FF.toInt())
        background = rounded(0xFF7040D8.toInt(), 14)
        minHeight = dp(48)
        setOnClickListener { action() }
    }

    private fun compactButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = if (label.length > 3) 10f else if (label == "+") 23f else 21f
        isAllCaps = false
        minWidth = dp(0)
        setTextColor(0xFFE8DDFF.toInt())
        background = rounded(0xFF29263B.toInt(), 14)
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
        status.text = "REPLACEMENT " + if (enabled) "ON" else "OFF"

        masterToggle.text = if (enabled) "Turn Mohxify off" else "Turn Mohxify on"

        val accessibilityEnabled = isMohxifyAccessibilityEnabled()
        serviceStatus.text = if (accessibilityEnabled) "SERVICE READY" else "SETUP NEEDED"
        serviceStatus.setTextColor(if (accessibilityEnabled) 0xFFC5F36B.toInt() else 0xFFFFC078.toInt())
        serviceButton.visibility = if (accessibilityEnabled) View.GONE else View.VISIBLE
        if (::keyboardSetupButton.isInitialized) {
            keyboardSetupButton.visibility = if (isMohxifyKeyboardEnabled()) View.GONE else View.VISIBLE
        }

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items.map { it.name }) {
            private fun style(view: View): View {
                (view as? TextView)?.apply {
                    textSize = 14f; typeface = Typeface.DEFAULT_BOLD
                    setTextColor(0xFFF4F0FF.toInt()); setPadding(dp(12), dp(8), dp(8), dp(8))
                }
                return view
            }
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getView(position, convertView, parent))
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                style(super.getDropDownView(position, convertView, parent)).apply {
                    setBackgroundColor(0xFF24243A.toInt())
                }
        }
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
        val groupedRules = active.rules.groupBy { it.from.lowercase(java.util.Locale.ROOT) }.values.toList()
        val filteredGroups = if (searchQuery.isBlank()) groupedRules else groupedRules.filter { group ->
            group.any { it.from.contains(searchQuery, true) || it.to.contains(searchQuery, true) }
        }
        val shownGroups = filteredGroups.take(visibleRuleCount)
        val shownRules = shownGroups.sumOf { it.size }
        replacementCount.text = if (searchQuery.isBlank()) {
            "$shownRules of ${active.rules.size} replacements"
        } else {
            "$shownRules of ${filteredGroups.sumOf { it.size }} matching replacements · ${active.rules.size} total"
        }
        showMoreButton.visibility = if (filteredGroups.size > 10) View.VISIBLE else View.GONE
        showMoreButton.text = if (shownGroups.size < filteredGroups.size) {
            "Show 10 more (${filteredGroups.size - shownGroups.size} groups left)"
        } else "Show fewer"
        val rulesNeedRendering = renderedLoadoutId != active.id ||
            renderedRules != active.rules || renderedRuleCount != shownGroups.size ||
            renderedSearchQuery != searchQuery
        if (rulesNeedRendering) {
            list.removeAllViews()
            shownGroups.forEach { synonyms ->
                    val first = synonyms.first()
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), dp(9), dp(4), dp(9))
                        background = GradientDrawable().apply {
                            setColor(0xFF202033.toInt())
                            cornerRadius = dp(14).toFloat()
                            setStroke(dp(1), 0xFF343047.toInt())
                        }
                    }
                    row.addView(LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@MainActivity).apply {
                            text = first.from
                            textSize = 15f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(0xFFF7F3FF.toInt())
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = synonyms.joinToString("  ·  ") { it.to }
                            textSize = 13f
                            setTextColor(0xFFAAA4BC.toInt())
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
            if (shownGroups.isEmpty()) list.addView(TextView(this).apply {
                text = if (searchQuery.isBlank()) "No replacements yet. Add one or import a list."
                else "No matches. Try another word or replacement."
                textSize = 14f
                setTextColor(0xFFAAA4BC.toInt())
                setPadding(dp(12), dp(18), dp(12), dp(18))
            })
            renderedLoadoutId = active.id
            renderedRules = active.rules.toList()
            renderedRuleCount = shownGroups.size
            renderedSearchQuery = searchQuery
        }
        showMoreButton.setOnClickListener {
            visibleRuleCount = if (visibleRuleCount < filteredGroups.size) {
                minOf(visibleRuleCount + 10, filteredGroups.size)
            } else 10
            refresh()
        }
    }

    private fun showRuleTester() {
        val input = EditText(this).apply {
            hint = "Type a trigger word, e.g. what"
            setSingleLine(true)
            textSize = 15f
            setTextColor(0xFFF7F3FF.toInt())
            setHintTextColor(0xFFAAA4BC.toInt())
            inputType = InputType.TYPE_CLASS_TEXT
            background = rounded(0xFF24243A.toInt(), 14)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val result = TextView(this).apply {
            text = "Your replacement preview will appear here."
            textSize = 14f
            setTextColor(0xFFC7B4FF.toInt())
            setPadding(dp(2), dp(14), dp(2), dp(4))
        }
        val alternatives = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFAAA4BC.toInt())
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
            addView(input)
            addView(result)
            addView(alternatives)
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val word = s?.toString()?.trim().orEmpty()
                val matches = if (word.isBlank() || word.any { it.isWhitespace() }) emptyList()
                    else ReplacementEngine.alternatives(word, LoadoutStore.active(this@MainActivity).rules)
                result.text = when {
                    word.isBlank() -> "Your replacement preview will appear here."
                    word.any { it.isWhitespace() } -> "Enter one trigger word at a time."
                    matches.isEmpty() -> "No matching rule in this workspace."
                    else -> "$word  →  ${matches.first()}"
                }
                alternatives.text = if (matches.size > 1) "Other alternatives: ${matches.drop(1).joinToString("  ·  ")}" else ""
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        AlertDialog.Builder(this)
            .setTitle("Try a shortcut")
            .setMessage("Test the active workspace without typing in another app.")
            .setView(content)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun showHowToUse() {
        val guide = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(12))
        }
        fun section(title: String, body: String) {
            guide.addView(TextView(this).apply {
                text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFFF5F0FF.toInt()); setPadding(0, dp(12), 0, dp(4))
            })
            guide.addView(TextView(this).apply {
                text = body; textSize = 13f; setTextColor(0xFFC2BCD2.toInt())
                setLineSpacing(dp(3).toFloat(), 1f)
            })
        }
        section("1. Turn it on", "Open Android Accessibility settings, find Mohxify under installed or downloaded apps, and allow the service. Return to Mohxify and switch the main toggle on. Both steps are needed.")
        section("2. Create a shortcut", "Tap + Add replacement. Enter the word you will type as the trigger, then enter what it should become. Example: what → wt, because → bcs. Save the rule.")
        section("3. Use it while typing", "Open a supported text field in any app with your usual keyboard. Type the trigger as a complete word, then press Space. Example: type “what ” and Mohxify changes it to “wt ”. Keep typing normally.")
        section("4. Cycle alternatives", "Give one trigger more than one replacement, such as hello → hey and hello → hi. After the first replacement, press Backspace once to move to the next alternative. This only works immediately after the replacement and its generated space.")
        section("5. Keep different vocabularies", "Create separate workspaces for school, casual chat, gaming or anything else. Select a workspace to activate its dictionary. Edit, copy or delete workspaces from the buttons below the selector.")
        section("6. Import and back up", "Use Import rules to paste or load a text file. The format is one rule per line: what: wt. Review the preview before merging or replacing. Use Back up all loadouts to save a JSON backup before reinstalling or changing devices.")
        section("Good to know", "Mohxify works only in supported editable text fields. Password fields are skipped, and some apps use custom editors that Android does not expose. Your dictionaries are stored locally on this device. The Accessibility service is what lets Mohxify detect completed words.")
        val scroll = ScrollView(this).apply {
            addView(guide)
            isFillViewport = true
        }
        AlertDialog.Builder(this)
            .setTitle("How to use Mohxify")
            .setView(scroll)
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun isMohxifyKeyboardEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_INPUT_METHODS
        ) ?: return false
        return enabled.split(':').any {
            it.contains(packageName) && it.contains("MohxifyKeyboardService")
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

    private fun duplicateActiveLoadout() {
        val items = LoadoutStore.all(this)
        val source = LoadoutStore.active(this)
        val baseName = "${source.name} copy"
        var name = baseName
        var suffix = 2
        while (nameExists(name)) {
            name = "$baseName $suffix"
            suffix++
        }
        val duplicate = Loadout(LoadoutStore.newId(), name, source.rules.toList())
        LoadoutStore.save(this, items + duplicate, duplicate.id)
        renderedRules = null
        refresh()
        toast("Loadout duplicated")
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
        importTextField = input
        val importLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), 0)
            addView(input)
            addView(button("Open .txt file") { openRulesFile() })
        }
        val scroll = ScrollView(this).apply { addView(importLayout) }
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

    private fun openRulesFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/*"))
        }
        startActivityForResult(intent, REQUEST_OPEN_RULES)
    }

    private fun downloadActiveLoadout() {
        val loadout = LoadoutStore.active(this)
        val safeName = loadout.name.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "Loadout" }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, safeName + ".txt")
        }
        startActivityForResult(intent, REQUEST_SAVE_LOADOUT)
    }

    private fun downloadBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "mohxify-backup.json")
        }
        startActivityForResult(intent, REQUEST_SAVE_BACKUP)
    }

    private fun restoreBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json"))
        }
        startActivityForResult(intent, REQUEST_OPEN_BACKUP)
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_OPEN_RULES -> try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Unable to open selected file")
                importTextField?.setText(text)
                toast("Text file loaded. Tap Preview to review its rules.")
            } catch (_: Exception) {
                toast("Could not read selected file")
            }
            REQUEST_SAVE_LOADOUT -> try {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(BulkRuleParser.export(LoadoutStore.active(this).rules))
                } ?: throw IllegalStateException("Unable to create selected file")
                toast("Loadout downloaded")
            } catch (_: Exception) {
                toast("Could not save loadout file")
            }
            REQUEST_SAVE_BACKUP -> try {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(LoadoutStore.exportJson(this))
                } ?: throw IllegalStateException("Unable to create backup")
                toast("All loadouts backed up")
            } catch (_: Exception) {
                toast("Could not save backup")
            }
            REQUEST_OPEN_BACKUP -> try {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Unable to read backup")
                val count = JSONObject(raw).optJSONArray("loadouts")?.length() ?: 0
                if (count < 1) {
                    toast("Backup contains no loadouts")
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Restore backup?")
                        .setMessage("This backup contains $count loadout(s). Restoring it replaces all current loadouts. Make a backup first if you want to keep them.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Restore") { _, _ ->
                            if (LoadoutStore.importJson(this, raw)) {
                                searchQuery = ""
                                searchField.setText("")
                                renderedRules = null
                                refresh()
                                toast("Backup restored")
                            } else toast("Backup is invalid or unsupported")
                        }.show()
                }
            } catch (_: Exception) {
                toast("Could not read a valid Mohxify backup")
            }
        }
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
