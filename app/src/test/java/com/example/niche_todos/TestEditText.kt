// ABOUTME: Shared test EditText subclass for dialog helper tests.
// ABOUTME: Captures attach listeners and watcher removal to assert cleanup.
package com.example.niche_todos

import android.content.Context
import android.text.TextWatcher
import android.view.View
import android.widget.EditText

class TestEditText(context: Context) : EditText(context) {
    var lastAttachListener: View.OnAttachStateChangeListener? = null
        private set
    var watcherRemoved = false
        private set

    override fun addOnAttachStateChangeListener(listener: View.OnAttachStateChangeListener) {
        lastAttachListener = listener
        super.addOnAttachStateChangeListener(listener)
    }

    override fun removeOnAttachStateChangeListener(listener: View.OnAttachStateChangeListener) {
        if (listener == lastAttachListener) {
            lastAttachListener = null
        }
        super.removeOnAttachStateChangeListener(listener)
    }

    override fun removeTextChangedListener(watcher: TextWatcher?) {
        watcherRemoved = true
        super.removeTextChangedListener(watcher)
    }

    fun simulateDetach() {
        lastAttachListener?.onViewDetachedFromWindow(this)
    }
}
