// ABOUTME: Focus helper for todo title dialogs
// ABOUTME: Registers focus behavior so title input is ready when dialog shows
package com.example.niche_todos

import android.content.DialogInterface
import android.content.Context
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager

class TitleInputFocusController(
    private val dialogOnShowRegistrar: DialogOnShowRegistrar,
    private val focusActions: TitleInputFocusActions,
    private val softInputVisibilityController: SoftInputVisibilityController
) {
    @MainThread
    fun selectTitle(selectAllExistingText: Boolean) {
        dialogOnShowRegistrar.setOnShowListener(DialogInterface.OnShowListener {
            softInputVisibilityController.ensureVisible()
            focusActions.requestFocus()
            focusActions.showKeyboard()
            if (selectAllExistingText) {
                focusActions.selectAll()
            } else {
                focusActions.moveCursorToEnd()
            }
        })
    }
}

fun interface DialogOnShowRegistrar {
    @MainThread
    fun setOnShowListener(listener: DialogInterface.OnShowListener)
}

interface TitleInputFocusActions {
    fun requestFocus()
    fun showKeyboard()
    fun selectAll()
    fun moveCursorToEnd()
}

fun interface SoftInputVisibilityController {
    fun ensureVisible()
}

class AlertDialogOnShowRegistrar(
    private val alertDialog: AlertDialog
) : DialogOnShowRegistrar {
    private val listeners = mutableListOf<DialogInterface.OnShowListener>()
    private var registered = false

    @MainThread
    override fun setOnShowListener(listener: DialogInterface.OnShowListener) {
        listeners.add(listener)
        if (!registered) {
            registered = true
            alertDialog.setOnShowListener(DialogInterface.OnShowListener { dialogInterface ->
                val currentListeners = listeners.toList()
                listeners.clear()
                registered = false
                currentListeners.forEach { it.onShow(dialogInterface) }
            })
        }
    }
}

class TextInputFocusActions(
    private val titleInput: TextInputEditText
) : TitleInputFocusActions {
    override fun requestFocus() {
        titleInput.requestFocus()
    }

    override fun showKeyboard() {
        val imm = titleInput.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun selectAll() {
        titleInput.selectAll()
    }

    override fun moveCursorToEnd() {
        val length = titleInput.text?.length ?: 0
        titleInput.setSelection(length)
    }
}

class AlertDialogSoftInputVisibilityController(
    private val alertDialog: AlertDialog
) : SoftInputVisibilityController {
    override fun ensureVisible() {
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}
