// ABOUTME: Unit tests for AlertDialogOnShowRegistrar
// ABOUTME: Ensures multiple onShow listeners can register and fire in order
package com.example.niche_todos

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class AlertDialogOnShowRegistrarTest {

    @Test
    fun setOnShowListener_invokesAllRegisteredListeners() {
        val alertDialog = mock(AlertDialog::class.java)
        val first = mock(DialogInterface.OnShowListener::class.java)
        val second = mock(DialogInterface.OnShowListener::class.java)
        val registrar = AlertDialogOnShowRegistrar(alertDialog)

        registrar.setOnShowListener(first)
        registrar.setOnShowListener(second)

        val listenerCaptor = ArgumentCaptor.forClass(DialogInterface.OnShowListener::class.java)
        verify(alertDialog, times(1)).setOnShowListener(listenerCaptor.capture())

        val dialogInterface = mock(DialogInterface::class.java)
        listenerCaptor.value.onShow(dialogInterface)

        verify(first).onShow(dialogInterface)
        verify(second).onShow(dialogInterface)
    }

    @Test
    fun setOnShowListener_reRegisteringAfterShowClearsPreviousListeners() {
        val alertDialog = mock(AlertDialog::class.java)
        val first = mock(DialogInterface.OnShowListener::class.java)
        val second = mock(DialogInterface.OnShowListener::class.java)
        val registrar = AlertDialogOnShowRegistrar(alertDialog)

        registrar.setOnShowListener(first)

        val listenerCaptor = ArgumentCaptor.forClass(DialogInterface.OnShowListener::class.java)
        verify(alertDialog, times(1)).setOnShowListener(listenerCaptor.capture())

        val dialogInterface = mock(DialogInterface::class.java)
        listenerCaptor.value.onShow(dialogInterface)

        verify(first, times(1)).onShow(dialogInterface)

        registrar.setOnShowListener(second)

        verify(alertDialog, times(2)).setOnShowListener(listenerCaptor.capture())

        val secondOnShowListener = listenerCaptor.allValues.last()
        secondOnShowListener.onShow(dialogInterface)

        verify(first, times(1)).onShow(dialogInterface)
        verify(second, times(1)).onShow(dialogInterface)
    }
}
