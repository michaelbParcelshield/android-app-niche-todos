// ABOUTME: Unit tests for EncryptedAuthTokenStore.
// ABOUTME: Verifies corrupted JSON logs a warning and returns null.
package com.example.niche_todos

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class EncryptedAuthTokenStoreTest {

    @Test
    fun load_withCorruptedJson_logsWarningAndReturnsNull() {
        ShadowLog.setupLogging()
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val store = EncryptedAuthTokenStore(context)

        val preferences = EncryptedSharedPreferences.create(
            context,
            "auth_tokens",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        preferences.edit()
            .putString("auth_tokens_json", "{not-json")
            .apply()

        val result = store.load()

        assertNull(result)
        val warnings = ShadowLog.getLogsForTag("AuthTokenStore")
        assertTrue(warnings.any { it.type == Log.WARN })
    }
}
