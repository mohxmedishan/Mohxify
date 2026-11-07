package com.mohx.mohxify

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MohxifyAccessibilityService : AccessibilityService() {
    private var editing = false
    private var expectedReplacementText: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (editing || event == null || event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        if (!LoadoutStore.enabled(this)) return
        val node = event.source ?: return
        try {
            if (!node.isEditable || !node.isEnabled || node.isPassword) return
            val current = node.text?.toString() ?: return
            if (current.isEmpty()) return
            if (expectedReplacementText != null) {
                val expected = expectedReplacementText
                expectedReplacementText = null
                if (current == expected) return
            }
            if (!current.last().isWhitespace()) return
            val end = current.length - 1
            var tokenStart = end
            while (tokenStart > 0 && !current[tokenStart - 1].isWhitespace()) tokenStart--
            if (tokenStart == end) return
            val token = current.substring(tokenStart, end)
            var coreStart = 0
            var coreEnd = token.length
            while (coreStart < coreEnd && !token[coreStart].isLetterOrDigit()) coreStart++
            while (coreEnd > coreStart && !token[coreEnd - 1].isLetterOrDigit()) coreEnd--
            if (coreStart == coreEnd) return
            val original = token.substring(coreStart, coreEnd)
            val replacement = ReplacementEngine.replaceWord(
                original, LoadoutStore.active(this).rules.map { it.from to it.to }
            )
            if (replacement == original) return
            val newToken = token.substring(0, coreStart) + replacement + token.substring(coreEnd)
            val updated = current.substring(0, tokenStart) + newToken + current.substring(end)
            val oldSelection = try { node.textSelectionEnd } catch (_: Exception) { current.length }
            val newCursor = (oldSelection + replacement.length - original.length).coerceIn(0, updated.length)
            editing = true
            val args = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
            }
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                val selectionArgs = android.os.Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursor)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursor)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
                expectedReplacementText = updated
            }
        } catch (_: Exception) {
            // Some apps reject ACTION_SET_TEXT or selection updates.
        } finally {
            editing = false
            node.recycle()
        }
    }

    override fun onInterrupt() {}
}
