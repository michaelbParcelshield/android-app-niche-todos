// ABOUTME: Unit tests for EncryptedAuthTokenStore.
// ABOUTME: Verifies corrupted JSON logs a warning and returns null.
package com.example.niche_todos

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class EncryptedAuthTokenStoreTest {

    @Test
    fun load_withCorruptedJson_logsWarningAndReturnsNull() {
        ShadowLog.setupLogging()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val store = EncryptedAuthTokenStore(
            context,
            preferencesProvider = { providerContext ->
                providerContext.getSharedPreferences("auth_tokens", android.content.Context.MODE_PRIVATE)
            }
        )
        val preferences = context.getSharedPreferences(
            "auth_tokens",
            android.content.Context.MODE_PRIVATE
        )
        preferences.edit()
            .putString("auth_tokens_json", "{not-json")
            .apply()

        val result = store.load()

        assertNull(result)
        val warnings = ShadowLog.getLogsForTag("AuthTokenStore")
        assertTrue(warnings.any { it.type == Log.WARN })
    }

    @Test
    fun load_withExpiredTokens_returnsNull() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        var nowSeconds = 1_000L
        val store = EncryptedAuthTokenStore(
            context,
            nowProvider = { nowSeconds },
            preferencesProvider = { providerContext ->
                providerContext.getSharedPreferences("auth_tokens", android.content.Context.MODE_PRIVATE)
            }
        )
        val tokens = AuthTokens(
            accessToken = "access",
            expiresInSeconds = 10,
            refreshToken = "refresh",
            user = AuthenticatedUser(
                id = "user-id",
                email = "user@example.com",
                name = "User",
                avatarUrl = null
            )
        )

        store.save(tokens)

        nowSeconds = 1_011L
        val loaded = store.load()

        assertNull(loaded)
    }
}
