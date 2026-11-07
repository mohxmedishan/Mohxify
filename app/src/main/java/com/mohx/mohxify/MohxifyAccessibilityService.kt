package com.mohx.mohxify

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MohxifyAccessibilityService : AccessibilityService() {
    private var editing = false
    private var lastHandledText: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (editing || event == null || event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        if (!Rules.enabled(this)) return

        val node = event.source ?: return
        try {
            if (!node.isEditable || !node.isEnabled) return
            val current = node.text?.toString() ?: return
            if (current.isEmpty() || current == lastHandledText) return

            // Only act after a space or other whitespace has completed a word.
            if (!current.last().isWhitespace()) return

            val end = current.length - 1
            var start = end
            while (start > 0 && !current[start - 1].isWhitespace()) start--
            if (start == end) return

            val originalWord = current.substring(start, end)
            if (originalWord.isEmpty()) return

            val replacement = Rules.replaceWord(originalWord, Rules.all(this))
            if (replacement == originalWord) return

            val updated = current.substring(0, start) + replacement + current.substring(end)
            val selection = try {
                node.textSelectionEnd
            } catch (_: Exception) { updated.length }

            editing = true
            val args = android.os.Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
            val changed = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (changed) {
                val cursor = (selection + replacement.length - originalWord.length).coerceIn(0, updated.length)
                val selectionArgs = android.os.Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
                lastHandledText = updated
            }
        } catch (_: Exception) {
            // Some apps expose editable fields but reject ACTION_SET_TEXT.
        } finally {
            editing = false
            node.recycle()
        }
    }

    override fun onInterrupt() {}
}
