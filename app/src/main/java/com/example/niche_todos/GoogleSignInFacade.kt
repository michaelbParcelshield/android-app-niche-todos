// ABOUTME: Wrapper around Google Sign-In APIs for easier testing.
// ABOUTME: Produces sign-in intents and extracts ID tokens from results.
package com.example.niche_todos

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

interface GoogleSignInFacade {
    fun createSignInIntent(): Intent
    fun extractIdToken(data: Intent?): String?
}

class GoogleSignInFacadeImpl(
    private val context: Context,
    private val serverClientId: String
) : GoogleSignInFacade {
    private val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestIdToken(serverClientId)
        .build()
    private val client = GoogleSignIn.getClient(context, options)

    override fun createSignInIntent(): Intent = client.signInIntent

    override fun extractIdToken(data: Intent?): String? {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            account.idToken
        } catch (error: ApiException) {
            null
        }
    }
}
