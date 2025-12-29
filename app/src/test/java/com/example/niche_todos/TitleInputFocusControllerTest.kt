// ABOUTME: Unit tests for TitleInputFocusController helper
// ABOUTME: Verifies dialog focus logic triggers correct actions on show
package com.example.niche_todos

import android.content.DialogInterface
import org.junit.Assert.assertEquals
import org.junit.Test

class TitleInputFocusControllerTest {

    @Test
    fun selectTitle_selectsAllWhenRequested() {
        val registrar = FakeDialogOnShowRegistrar()
        val focusActions = FakeTitleInputFocusActions()
        val softInput = FakeSoftInputVisibilityController()
        val controller = TitleInputFocusController(registrar, focusActions, softInput)

        controller.selectTitle(selectAllExistingText = true)
        registrar.fireShow()

        assertEquals(listOf("requestFocus", "showKeyboard", "selectAll"), focusActions.callLog)
        assertEquals(true, softInput.ensureVisibleCalled)
    }

    @Test
    fun selectTitle_movesCursorToEndWhenNotSelectingAll() {
        val registrar = FakeDialogOnShowRegistrar()
        val focusActions = FakeTitleInputFocusActions()
        val softInput = FakeSoftInputVisibilityController()
        val controller = TitleInputFocusController(registrar, focusActions, softInput)

        controller.selectTitle(selectAllExistingText = false)
        registrar.fireShow()

        assertEquals(
            listOf("requestFocus", "showKeyboard", "moveCursorToEnd"),
            focusActions.callLog
        )
        assertEquals(true, softInput.ensureVisibleCalled)
    }
}

private class FakeDialogOnShowRegistrar : DialogOnShowRegistrar {
    private var listener: DialogInterface.OnShowListener? = null

    override fun setOnShowListener(listener: DialogInterface.OnShowListener) {
        this.listener = listener
    }

    fun fireShow() {
        listener?.onShow(null)
    }
}

private class FakeTitleInputFocusActions : TitleInputFocusActions {
    val callLog = mutableListOf<String>()

    override fun requestFocus() {
        callLog.add("requestFocus")
    }

    override fun showKeyboard() {
        callLog.add("showKeyboard")
    }

    override fun selectAll() {
        callLog.add("selectAll")
    }

    override fun moveCursorToEnd() {
        callLog.add("moveCursorToEnd")
    }
}

private class FakeSoftInputVisibilityController : SoftInputVisibilityController {
    var ensureVisibleCalled = false

    override fun ensureVisible() {
        ensureVisibleCalled = true
    }
}
