// ABOUTME: Unit tests for MainActivityDependencies.
// ABOUTME: Verifies dependency factories reset to defaults.
package com.example.niche_todos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class MainActivityDependenciesTest {

    @Test
    fun reset_restoresDefaultFactories() {
        MainActivityDependencies.repositoryFactory = { _, _ ->
            BackendRepositoryBundle(
                healthRepository = FakeHealthRepository(HealthCheckResult.Success(200)),
                authRepository = FakeAuthRepository(),
                todoRepository = FakeTodoRepository()
            )
        }
        MainActivityDependencies.googleSignInFacadeFactory = { _, _ ->
            object : GoogleSignInFacade {
                override fun createSignInIntent() = android.content.Intent("fake")
                override fun extractIdToken(data: android.content.Intent?) = "token"
            }
        }

        MainActivityDependencies.reset()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val endpoints = BackendEndpoints(
            healthUrl = URL("https://example.com/healthz"),
            authUrl = URL("https://example.com/auth/google"),
            todosUrl = URL("https://example.com/todos")
        )
        val repositories = MainActivityDependencies.repositoryFactory(context, endpoints)
        val googleFacade = MainActivityDependencies.googleSignInFacadeFactory(context, "client-id")

        assertTrue(repositories.healthRepository is BackendHealthRepository)
        assertTrue(repositories.authRepository is BackendAuthRepository)
        assertTrue(repositories.todoRepository is BackendTodoRepository)
        assertTrue(googleFacade is GoogleSignInFacadeImpl)
    }
}
