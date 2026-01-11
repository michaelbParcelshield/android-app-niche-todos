// ABOUTME: Persists auth tokens for reuse across app sessions.
// ABOUTME: Stores tokens as JSON in shared preferences.
package com.example.niche_todos

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface AuthTokenStore {
    fun save(tokens: AuthTokens)
    fun load(): AuthTokens?
    fun clear()
}

class EncryptedAuthTokenStore(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AuthTokenStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun save(tokens: AuthTokens) {
        preferences.edit()
            .putString(KEY_AUTH_TOKENS, json.encodeToString(tokens))
            .apply()
    }

    override fun load(): AuthTokens? {
        val payload = preferences.getString(KEY_AUTH_TOKENS, null) ?: return null
        return runCatching { json.decodeFromString(AuthTokens.serializer(), payload) }
            .onFailure { error ->
                Log.w(TAG, "Failed to decode auth tokens", error)
            }
            .getOrNull()
    }

    override fun clear() {
        preferences.edit()
            .remove(KEY_AUTH_TOKENS)
            .apply()
    }

    private companion object {
        private const val TAG = "AuthTokenStore"
        private const val PREFS_NAME = "auth_tokens"
        private const val KEY_AUTH_TOKENS = "auth_tokens_json"
    }
}
