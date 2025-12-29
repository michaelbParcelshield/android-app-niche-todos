// ABOUTME: Tests AlertDialog extension helpers in MainActivity.
// ABOUTME: Ensures UI helpers do not break existing dialog contracts.
package com.example.niche_todos

import android.content.Context
import android.content.DialogInterface
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class AlertDialogExtensionsTest {

    @Test
    fun configureSaveButtonState_internalUpdatesButtonAndRemovesWatcherOnDetach() {
        val baseContext = ApplicationProvider.getApplicationContext<android.app.Application>()
        val themedContext =
            ContextThemeWrapper(baseContext, android.R.style.Theme_DeviceDefault)
        val titleInput = TestEditText(themedContext)
        val saveButton = Button(themedContext)

        configureSaveButtonStateInternal(saveButton, titleInput)

        assertFalse(saveButton.isEnabled)

        titleInput.setText("A")
        assertTrue(saveButton.isEnabled)

        titleInput.setText("")
        assertFalse(saveButton.isEnabled)

        val attachListener = titleInput.lastAttachListener
        assertNotNull("Attach listener should be registered for cleanup", attachListener)

        titleInput.simulateDetach()

        assertTrue("Watcher should be removed on detach", titleInput.watcherRemoved)
    }

    @Test
    fun configureSaveButtonState_preservesExistingDismissListener() {
        val baseContext = ApplicationProvider.getApplicationContext<android.app.Application>()
        val themedContext =
            ContextThemeWrapper(baseContext, androidx.appcompat.R.style.Theme_AppCompat)
        val titleInput = TestEditText(themedContext)
        val dialog = Mockito.mock(AlertDialog::class.java)
        val saveButton = Button(themedContext)
        Mockito.`when`(dialog.getButton(AlertDialog.BUTTON_POSITIVE)).thenReturn(saveButton)

        exerciseConfigureSaveButtonState(titleInput = titleInput, dialog = dialog, saveButton = saveButton)
    }

    private fun exerciseConfigureSaveButtonState(
        titleInput: TestEditText,
        dialog: AlertDialog,
        saveButton: Button
    ) {
        dialog.configureSaveButtonState(titleInput)
        assertFalse(saveButton.isEnabled)

        titleInput.setText("hello world")
        assertTrue(saveButton.isEnabled)

        titleInput.setText("")
        assertFalse(saveButton.isEnabled)

        titleInput.simulateDetach()

        assertTrue("Watcher should be removed when dialog is dismissed", titleInput.watcherRemoved)
        verify(dialog, never()).setOnDismissListener(
            Mockito.any(DialogInterface.OnDismissListener::class.java)
        )
    }

    private class TestEditText(context: Context) : EditText(context) {
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

        override fun removeTextChangedListener(watcher: android.text.TextWatcher?) {
            watcherRemoved = true
            super.removeTextChangedListener(watcher)
        }

        fun simulateDetach() {
            lastAttachListener?.onViewDetachedFromWindow(this)
        }
    }
}
