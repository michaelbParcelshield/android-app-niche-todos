// ABOUTME: Unit tests for BackendStatusViewModel.
// ABOUTME: Verifies health and auth status updates through coroutine flows.
package com.example.niche_todos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackendStatusViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun runHealthCheck_updatesStatusToSuccess() = runTest {
        val viewModel = BackendStatusViewModel(
            healthRepository = FakeHealthRepository(HealthCheckResult.Success(200)),
            authRepository = FakeAuthRepository()
        )

        viewModel.runHealthCheck()
        advanceUntilIdle()

        assertEquals(HealthStatus.Success(200), viewModel.healthStatus.value)
    }

    @Test
    fun authenticateWithGoogle_updatesStatusToFailure() = runTest {
        val viewModel = BackendStatusViewModel(
            healthRepository = FakeHealthRepository(HealthCheckResult.Success(200)),
            authRepository = FakeAuthRepository(
                result = AuthResult.Failure(401, "Unauthorized")
            )
        )

        viewModel.authenticateWithGoogle("bad-token")
        advanceUntilIdle()

        assertEquals(AuthStatus.Failure(401, "Unauthorized"), viewModel.authStatus.value)
    }

    @Test
    fun runHealthCheck_updatesStatusToFailure() = runTest {
        val viewModel = BackendStatusViewModel(
            healthRepository = FakeHealthRepository(
                HealthCheckResult.Failure(503, "Service unavailable")
            ),
            authRepository = FakeAuthRepository()
        )

        viewModel.runHealthCheck()
        advanceUntilIdle()

        assertEquals(
            HealthStatus.Failure(503, "Service unavailable"),
            viewModel.healthStatus.value
        )
    }
}
