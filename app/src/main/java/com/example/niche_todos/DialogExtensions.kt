// ABOUTME: AlertDialog extension helpers for Todo dialogs.
// ABOUTME: Provides UI wiring so activities can stay focused on behavior.
package com.example.niche_todos

import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged

fun AlertDialog.configureSaveButtonState(titleInput: EditText) {
    val saveButton = getButton(AlertDialog.BUTTON_POSITIVE)
    configureSaveButtonStateInternal(saveButton, titleInput)
}

internal fun configureSaveButtonStateInternal(
    saveButton: Button,
    titleInput: EditText
) {
    fun updateState() {
        saveButton.isEnabled = TodoTitleValidator.isValid(titleInput.text)
    }
    updateState()
    val watcher = titleInput.doOnTextChanged { _, _, _, _ ->
        updateState()
    }
    val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            // Remove callbacks once the view detaches to avoid leaking dialog references.
            titleInput.removeTextChangedListener(watcher)
            titleInput.removeOnAttachStateChangeListener(this)
        }
    }
    titleInput.addOnAttachStateChangeListener(attachStateListener)
}
