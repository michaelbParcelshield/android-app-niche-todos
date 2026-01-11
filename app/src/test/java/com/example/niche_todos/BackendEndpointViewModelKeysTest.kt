// ABOUTME: Tests ViewModel key generation for backend endpoint modes.
// ABOUTME: Ensures different modes yield distinct ViewModel instances.
package com.example.niche_todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertNotSame
import org.junit.Test

class BackendEndpointViewModelKeysTest {

    @Test
    fun modeSpecificKeys_createDistinctViewModels() {
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TestViewModel() as T
            }
        }
        val provider = ViewModelProvider(store, factory)

        val localViewModel = provider.get(
            BackendEndpointViewModelKeys.todoKey(BackendEndpointMode.Local),
            TestViewModel::class.java
        )
        val cloudViewModel = provider.get(
            BackendEndpointViewModelKeys.todoKey(BackendEndpointMode.Cloud),
            TestViewModel::class.java
        )

        assertNotSame(localViewModel, cloudViewModel)
    }

    private class TestViewModel : ViewModel()
}
