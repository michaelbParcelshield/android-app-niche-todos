// ABOUTME: Unit tests for BackendHealthRepository.
// ABOUTME: Covers success and failure status handling for health checks.
package com.example.niche_todos

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class BackendHealthRepositoryTest {

    @Test
    fun runHealthCheck_non200Status_returnsFailureWithStatus() = runTest {
        val repository = BackendHealthRepository(
            client = FakeHealthClient(503),
            endpoints = BackendEndpoints(
                healthUrl = URL("https://example.com/healthz"),
                authUrl = URL("https://example.com/auth/google")
            )
        )

        val result = repository.runHealthCheck()

        assertEquals(
            HealthCheckResult.Failure(503, "Unexpected status: 503"),
            result
        )
    }
}
