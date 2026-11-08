package com.mohx.mohxify

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

class MohxifyAccessibilityService : AccessibilityService() {
    private var editing = false
    private var expectedReplacementText: String? = null
    private var actionBubble: View? = null

    private data class LastReplacement(
        val windowId: Int,
        val viewId: String?,
        val afterText: String,
        val tokenStart: Int,
        val original: String,
        val alternatives: List<String>,
        val selectedIndex: Int,
        val tokenSuffix: String
    )

    private var lastReplacement: LastReplacement? = null
    private data class SuppressedReentry(val windowId: Int, val viewId: String?, val beforeText: String)
    private var suppressedReentry: SuppressedReentry? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            updateActionBubble(event.packageName?.toString())
            return
        }
        if (editing || event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        if (!LoadoutStore.enabled(this)) {
            lastReplacement = null
            suppressedReentry = null
            return
        }
        val node = event.source ?: return
        try {
            if (!node.isEditable || !node.isEnabled || node.isPassword) return
            val current = node.text?.toString() ?: return
            if (expectedReplacementText != null) {
                val expected = expectedReplacementText
                expectedReplacementText = null
                if (current == expected) return
            }

            val previous = event.beforeText?.toString()
            val stamp = lastReplacement
            val isSingleBackspace = event.removedCount == 1 && event.addedCount == 0
            if (isSingleBackspace) {
                val deletedTrailingSpace = stamp != null &&
                    stamp.windowId == event.windowId &&
                    stamp.viewId == node.viewIdResourceName &&
                    previous == stamp.afterText &&
                    previous.isNotEmpty() &&
                    previous.last().isWhitespace() &&
                    event.fromIndex == previous.length - 1 &&
                    current == previous.dropLast(1)

                if (deletedTrailingSpace && stamp != null) {
                    val nextIndex = stamp.selectedIndex + 1
                    if (nextIndex < stamp.alternatives.size) {
                        val next = stamp.alternatives[nextIndex]
                        val updated = current.substring(0, stamp.tokenStart) + next + stamp.tokenSuffix + previous!!.last()
                        if (setTextAndCursor(node, updated, updated.length)) {
                            lastReplacement = stamp.copy(
                                afterText = updated,
                                alternatives = stamp.alternatives,
                                selectedIndex = nextIndex
                            )
                        } else lastReplacement = null
                    } else {
                        lastReplacement = null
                        suppressedReentry = SuppressedReentry(event.windowId, node.viewIdResourceName, current)
                    }
                } else {
                    lastReplacement = null
                    suppressedReentry = null
                }
                return
            }

            val suppression = suppressedReentry
            if (suppression != null) {
                suppressedReentry = null
                if (suppression.windowId == event.windowId &&
                    suppression.viewId == node.viewIdResourceName &&
                    previous == suppression.beforeText &&
                    current == suppression.beforeText + " ") return
            }

            if (!ReplacementEngine.isCompletedWordSeparator(previous, current)) {
                lastReplacement = null
                return
            }
            lastReplacement = null

            val separatorIndex = current.length - 1
            var tokenStart = separatorIndex
            while (tokenStart > 0 && !current[tokenStart - 1].isWhitespace()) tokenStart--
            if (tokenStart == separatorIndex) return

            val token = current.substring(tokenStart, separatorIndex)
            var coreStart = 0
            var coreEnd = token.length
            while (coreStart < coreEnd && !token[coreStart].isLetterOrDigit()) coreStart++
            while (coreEnd > coreStart && !token[coreEnd - 1].isLetterOrDigit()) coreEnd--
            if (coreStart == coreEnd) return

            val original = token.substring(coreStart, coreEnd)
            val alternatives = ReplacementEngine.alternatives(original, LoadoutStore.active(this).rules)
            val replacement = alternatives.firstOrNull() ?: return
            if (replacement == original) return

            val absoluteTokenStart = tokenStart + coreStart
            val newToken = token.substring(0, coreStart) + replacement + token.substring(coreEnd)
            val updated = current.substring(0, tokenStart) + newToken + current.substring(separatorIndex)
            val oldSelection = try { node.textSelectionEnd } catch (_: Exception) { current.length }
            val newCursor = (oldSelection + replacement.length - original.length).coerceIn(0, updated.length)

            if (setTextAndCursor(node, updated, newCursor)) {
                lastReplacement = LastReplacement(
                    windowId = event.windowId,
                    viewId = node.viewIdResourceName,
                    afterText = updated,
                    tokenStart = absoluteTokenStart,
                    original = original,
                    tokenSuffix = token.substring(coreEnd),
                    alternatives = alternatives,
                    selectedIndex = 0
                )
            }
        } catch (_: Exception) {
            lastReplacement = null
        } finally {
            node.recycle()
        }
    }

    private fun updateActionBubble(packageName: String?) {
        if (packageName == "com.whatsapp") {
            if (actionBubble != null) return
            val bubble = TextView(this).apply {
                text = "GIF"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
                contentDescription = "Open Mohxify WhatsApp GIF actions"
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(0xFF8B5CF6.toInt(), 0xFF5B3AA8.toInt())
                ).apply { shape = GradientDrawable.OVAL }
                elevation = dp(8).toFloat()
                setOnClickListener {
                    val launch = Intent(this@MohxifyAccessibilityService, MohxifyActionsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                }
            }
            val params = WindowManager.LayoutParams(
                dp(52), dp(52),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                x = dp(8)
                y = dp(52)
            }
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).addView(bubble, params)
                actionBubble = bubble
            } catch (_: Exception) { actionBubble = null }
        } else {
            removeActionBubble()
        }
    }

    private fun removeActionBubble() {
        val bubble = actionBubble ?: return
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(bubble) }
        catch (_: Exception) { }
        actionBubble = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun setTextAndCursor(node: AccessibilityNodeInfo, text: String, cursor: Int): Boolean {
        editing = true
        return try {
            val args = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
            val selectionArgs = android.os.Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
            expectedReplacementText = text
            true
        } finally { editing = false }
    }

    override fun onInterrupt() {
        lastReplacement = null
        suppressedReentry = null
        expectedReplacementText = null
        removeActionBubble()
    }

    override fun onDestroy() {
        removeActionBubble()
        super.onDestroy()
    }
}
