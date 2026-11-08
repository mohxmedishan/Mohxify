package com.mohx.mohxify

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MohxifyActionsActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private var pendingTrigger: String? = null
    companion object { private const val REQUEST_GIF = 7301 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun label(value: String, size: Float = 14f, color: Int = 0xFFF5F1FF.toInt()) =
        TextView(this).apply { text = value; textSize = size; setTextColor(color) }
    private fun button(value: String, action: () -> Unit) = TextView(this).apply {
        text = value; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER; setTextColor(0xFFF7F2FF.toInt())
        background = rounded(0xFF6D4BC3.toInt(), 13)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setOnClickListener { action() }
    }
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(0xFF171522.toInt(), 18)
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(20))
            setBackgroundColor(0xFF090A12.toInt())
        }
        root.addView(label("MOHXIFY  /  ACTIONS", 11f, 0xFFBDA8FF.toInt()))
        root.addView(label("WhatsApp GIF actions", 25f).apply {
            typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(7), 0, dp(4))
        })
        root.addView(label("Type ,trigger followed by a space in a chat to insert its GIF. Trigger shortcuts remove spaces automatically.", 13f, 0xFFB8B1C8.toInt()))
        search = EditText(this).apply {
            hint = "Search triggers, e.g. I love you"; setSingleLine(true); textSize = 14f
            setTextColor(0xFFF5F1FF.toInt()); setHintTextColor(0xFFAAA4BC.toInt())
            background = rounded(0xFF242236.toInt(), 13); setPadding(dp(14), 0, dp(14), 0)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderActions()
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        root.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
            topMargin = dp(16); bottomMargin = dp(10)
        })
        root.addView(button("Import GIF from Gboard") {
            startActivity(Intent(this, GifImportActivity::class.java))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(8) })
        root.addView(button("+  Add GIF action") { showAddDialog() }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
        root.addView(label("YOUR SAVED ACTIONS", 10f, 0xFFBDA8FF.toInt()).apply {
            typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(22), 0, dp(10))
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(button("Done") { finish() }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(12) })
        setContentView(root)
        renderActions()
    }

    private fun renderActions() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val query = if (::search.isInitialized) search.text.toString().trim() else ""
        val all = GifActionStore.all(this)
        val actions = all.filter { query.isBlank() || it.trigger.contains(query, ignoreCase = true) }
        if (actions.isEmpty()) list.addView(label(
            if (all.isEmpty()) "No GIF actions yet. Add a trigger and choose a GIF." else "No matching triggers.",
            13f, 0xFFAAA4BC.toInt()
        ).apply { setPadding(0, dp(8), 0, dp(12)) })
        actions.forEach { action ->
            val item = card()
            item.addView(label(action.trigger, 16f).apply { typeface = Typeface.DEFAULT_BOLD })
            item.addView(label("Saved GIF · " + action.uri.substringAfterLast('/').take(28), 11f, 0xFFAAA4BC.toInt()).apply {
                setPadding(0, dp(4), 0, dp(10))
            })
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(button("Share to WhatsApp") { shareToWhatsApp(action) },
                LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(6) })
            row.addView(button("Delete") {
                GifActionStore.remove(this@MohxifyActionsActivity, action.id); renderActions()
            }.apply { background = rounded(0xFF343044.toInt(), 13) },
                LinearLayout.LayoutParams(dp(88), dp(44)))
            item.addView(row)
            list.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(9)
            })
        }
    }

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "Trigger phrase, e.g. I love you"; setSingleLine(true); setTextColor(0xFF171522.toInt())
        }
        AlertDialog.Builder(this).setTitle("Create GIF action")
            .setMessage("The keyboard shortcut will be ,${trigger.replace(Regex("\\s+"), "")}. Type it in a chat and press space to insert the saved GIF.")
            .setView(input).setNegativeButton("Cancel", null)
            .setPositiveButton("Choose GIF") { _, _ ->
                val trigger = input.text.toString().trim()
                if (trigger.isBlank()) {
                    Toast.makeText(this, "Enter a trigger first", Toast.LENGTH_SHORT).show()
                } else {
                    pendingTrigger = trigger
                    val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "image/gif"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    }
                    try { startActivityForResult(picker, REQUEST_GIF) }
                    catch (_: Exception) { Toast.makeText(this, "No file picker is available", Toast.LENGTH_LONG).show() }
                }
            }.show()
    }

    @Deprecated("Legacy activity result is used for compatibility with the current minSdk.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_GIF || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val grant = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (grant != 0) contentResolver.takePersistableUriPermission(uri, grant)
        } catch (_: Exception) { }
        val trigger = pendingTrigger ?: return
        GifActionStore.add(this, trigger, uri.toString())
        pendingTrigger = null
        Toast.makeText(this, "GIF action saved", Toast.LENGTH_SHORT).show()
        renderActions()
    }

    private fun shareToWhatsApp(action: GifAction) {
        if (packageManager.getLaunchIntentForPackage("com.whatsapp") == null) {
            Toast.makeText(this, "WhatsApp isn't installed", Toast.LENGTH_LONG).show(); return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/gif"; putExtra(Intent.EXTRA_STREAM, Uri.parse(action.uri))
            setPackage("com.whatsapp"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivity(intent) }
        catch (_: Exception) {
            Toast.makeText(this, "WhatsApp couldn't accept this GIF. Try another GIF file.", Toast.LENGTH_LONG).show()
        }
    }
}
