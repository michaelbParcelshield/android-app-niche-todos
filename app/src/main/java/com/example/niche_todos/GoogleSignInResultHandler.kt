// ABOUTME: Translates Google sign-in activity results into auth actions.
// ABOUTME: Keeps sign-in decision logic testable outside the Activity.
package com.example.niche_todos

sealed class GoogleSignInOutcome {
    data object Cancelled : GoogleSignInOutcome()
    data object MissingIdToken : GoogleSignInOutcome()
    data class Success(val idToken: String) : GoogleSignInOutcome()
}

class GoogleSignInResultHandler {
    fun resolve(resultCode: Int, idToken: String?): GoogleSignInOutcome {
        if (resultCode != android.app.Activity.RESULT_OK) {
            return GoogleSignInOutcome.Cancelled
        }
        val token = idToken?.trim().orEmpty()
        if (token.isEmpty()) {
            return GoogleSignInOutcome.MissingIdToken
        }
        return GoogleSignInOutcome.Success(token)
    }
}
