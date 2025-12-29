// ABOUTME: AlertDialog extension helpers for Todo dialogs.
// ABOUTME: Provides UI wiring so activities can stay focused on behavior.
package com.example.niche_todos

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

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
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateState()
        }
        override fun afterTextChanged(s: Editable?) {}
    }
    titleInput.addTextChangedListener(watcher)
    val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            titleInput.removeTextChangedListener(watcher)
            titleInput.removeOnAttachStateChangeListener(this)
        }
    }
    titleInput.addOnAttachStateChangeListener(attachStateListener)
}
