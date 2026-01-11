// ABOUTME: Tests for persisting debug endpoint selection preferences.
// ABOUTME: Ensures cloud selection defaults and saved values are honored.
package com.example.niche_todos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackendEndpointSelectorTest {

    @Test
    fun defaultSelection_usesCloud() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(
            BackendEndpointSelector.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).edit().clear().commit()

        val selector = BackendEndpointSelector(context)

        assertTrue(selector.useCloud())
    }

    @Test
    fun setUseCloud_persistsChoice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(
            BackendEndpointSelector.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).edit().clear().commit()

        val selector = BackendEndpointSelector(context)
        selector.setUseCloud(false)

        val reloaded = BackendEndpointSelector(context)

        assertFalse(reloaded.useCloud())
    }
}
