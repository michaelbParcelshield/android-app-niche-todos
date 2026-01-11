// ABOUTME: Stores the debug preference for choosing cloud or local backend endpoints.
// ABOUTME: Reads and writes a simple boolean flag from app shared preferences.
package com.example.niche_todos

import android.content.Context

class BackendEndpointSelector(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun useCloud(): Boolean {
        return preferences.getBoolean(KEY_USE_CLOUD, DEFAULT_USE_CLOUD)
    }

    fun setUseCloud(useCloud: Boolean) {
        preferences.edit().putBoolean(KEY_USE_CLOUD, useCloud).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "backend_endpoint_preferences"
        const val KEY_USE_CLOUD = "use_cloud_backend"
        const val DEFAULT_USE_CLOUD = true
    }
}
