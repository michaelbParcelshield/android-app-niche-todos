// ABOUTME: Generates ViewModel keys scoped to backend endpoint mode.
// ABOUTME: Allows new ViewModels when switching between local and cloud.
package com.example.niche_todos

object BackendEndpointViewModelKeys {
    fun todoKey(mode: BackendEndpointMode): String = "TodoViewModel:${mode.name}"
    fun statusKey(mode: BackendEndpointMode): String = "BackendStatusViewModel:${mode.name}"
}
