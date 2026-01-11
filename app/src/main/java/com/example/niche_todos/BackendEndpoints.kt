// ABOUTME: Holds backend endpoint URLs used by repositories.
// ABOUTME: Keeps configuration separate from UI and data logic.
package com.example.niche_todos

import java.net.URL

data class BackendEndpoints(
    val healthUrl: URL,
    val authUrl: URL
)
