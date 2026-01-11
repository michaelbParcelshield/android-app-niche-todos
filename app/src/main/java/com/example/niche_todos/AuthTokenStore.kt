// ABOUTME: Persists auth tokens for reuse across app sessions.
// ABOUTME: Stores tokens as JSON in shared preferences.
package com.example.niche_todos

import android.content.Context
import android.content.SharedPreferences
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
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val nowProvider: () -> Long = { System.currentTimeMillis() / 1000 },
    private val preferencesProvider: (Context) -> SharedPreferences = { providerContext ->
        EncryptedSharedPreferences.create(
            providerContext,
            PREFS_NAME,
            MasterKey.Builder(providerContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
) : AuthTokenStore {
    private val preferences = preferencesProvider(context.applicationContext)

    override fun save(tokens: AuthTokens) {
        val savedAtSeconds = nowProvider()
        preferences.edit()
            .putString(KEY_AUTH_TOKENS, json.encodeToString(tokens))
            .putLong(KEY_AUTH_TOKENS_SAVED_AT, savedAtSeconds)
            .apply()
    }

    override fun load(): AuthTokens? {
        val payload = preferences.getString(KEY_AUTH_TOKENS, null) ?: return null
        val tokens = runCatching { json.decodeFromString(AuthTokens.serializer(), payload) }
            .onFailure { error ->
                Log.w(TAG, "Failed to decode auth tokens", error)
            }
            .getOrNull()
            ?: return null

        val savedAtSeconds = preferences.getLong(KEY_AUTH_TOKENS_SAVED_AT, -1L)
        if (savedAtSeconds > 0 && hasExpired(savedAtSeconds, tokens.expiresInSeconds)) {
            clear()
            return null
        }

        return tokens
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
        private const val KEY_AUTH_TOKENS_SAVED_AT = "auth_tokens_saved_at_seconds"
    }

    private fun hasExpired(savedAtSeconds: Long, expiresInSeconds: Int): Boolean {
        if (expiresInSeconds <= 0) {
            return true
        }
        val elapsedSeconds = nowProvider() - savedAtSeconds
        return elapsedSeconds >= expiresInSeconds.toLong()
    }
}
