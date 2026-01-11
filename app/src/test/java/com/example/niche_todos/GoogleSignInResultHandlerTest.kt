// ABOUTME: Unit tests for GoogleSignInResultHandler.
// ABOUTME: Verifies mapping from result codes to auth outcomes.
package com.example.niche_todos

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleSignInResultHandlerTest {

    @Test
    fun resolve_nonOkResult_returnsCancelled() {
        val handler = GoogleSignInResultHandler()

        val outcome = handler.resolve(Activity.RESULT_CANCELED, "token")

        assertEquals(GoogleSignInOutcome.Cancelled, outcome)
    }

    @Test
    fun resolve_missingToken_returnsMissingIdToken() {
        val handler = GoogleSignInResultHandler()

        val outcome = handler.resolve(Activity.RESULT_OK, "   ")

        assertEquals(GoogleSignInOutcome.MissingIdToken, outcome)
    }

    @Test
    fun resolve_validToken_returnsSuccess() {
        val handler = GoogleSignInResultHandler()

        val outcome = handler.resolve(Activity.RESULT_OK, "token-123")

        assertEquals(GoogleSignInOutcome.Success("token-123"), outcome)
    }
}
