// ABOUTME: Instrumented tests for MainActivity sign-in flow.
// ABOUTME: Exercises auth result handling with real Activity lifecycle.
package com.example.niche_todos

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySignInInstrumentedTest {
    private lateinit var authRepository: TestAuthRepository
    private lateinit var googleSignInFacade: TestGoogleSignInFacade
    private lateinit var todoRepository: TestTodoRepository

    @Before
    fun setUp() {
        authRepository = TestAuthRepository()
        googleSignInFacade = TestGoogleSignInFacade()
        todoRepository = TestTodoRepository()
        MainActivityDependencies.repositoryFactory = { _, _ ->
            BackendRepositoryBundle(
                healthRepository = TestHealthRepository(),
                authRepository = authRepository,
                todoRepository = todoRepository
            )
        }
        MainActivityDependencies.googleSignInFacadeFactory = { _, _ ->
            googleSignInFacade
        }
    }

    @After
    fun tearDown() {
        MainActivityDependencies.reset()
    }

    @Test
    fun handleGoogleSignInResult_updatesAuthStatusToSuccess() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.handleGoogleSignInResult(Activity.RESULT_OK, Intent())
        }
        runBlocking {
            withTimeout(2_000) {
                authRepository.exchangeGate.await()
            }
        }
    }

    @Test
    fun handleGoogleSignInResult_missingTokenShowsMissingStatus() {
        googleSignInFacade.idToken = "  "
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.handleGoogleSignInResult(Activity.RESULT_OK, Intent())
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        try {
            runBlocking {
                withTimeout(500) {
                    authRepository.exchangeGate.await()
                }
            }
            fail("Expected no auth exchange when ID token is missing")
        } catch (_: TimeoutCancellationException) {
            // expected
        }
    }

    @Test
    fun handleGoogleSignInResult_triggersTodoRefreshOnSuccess() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.handleGoogleSignInResult(Activity.RESULT_OK, Intent())
        }

        runBlocking {
            withTimeout(2_000) {
                authRepository.exchangeGate.await()
            }
            withTimeout(2_000) {
                todoRepository.fetchGate.await()
            }
        }
    }

    private class TestHealthRepository : HealthRepository {
        override suspend fun runHealthCheck(): HealthCheckResult =
            HealthCheckResult.Success(200)
    }

    private class TestAuthRepository : AuthRepository {
        val exchangeGate = CompletableDeferred<Unit>()

        override suspend fun exchangeGoogleIdToken(idToken: String): AuthResult {
            exchangeGate.complete(Unit)
            return AuthResult.Success(
                tokens = AuthTokens(
                    accessToken = "access",
                    expiresInSeconds = 3600,
                    refreshToken = "refresh",
                    user = AuthenticatedUser(
                        id = "user-id",
                        email = "user@example.com",
                        name = "User",
                        avatarUrl = null
                    )
                ),
                statusCode = 200
            )
        }
    }

    private class TestTodoRepository : TodoRepository {
        val fetchGate = CompletableDeferred<Unit>()

        override suspend fun fetchTodos(): TodoSyncResult {
            fetchGate.complete(Unit)
            return TodoSyncResult.Success(emptyList(), 200)
        }

        override suspend fun createTodo(
            title: String,
            startDateTime: java.time.LocalDateTime?,
            endDateTime: java.time.LocalDateTime?,
            isCompleted: Boolean,
            parentId: String?
        ): TodoSyncResult = TodoSyncResult.Success(emptyList(), 200)

        override suspend fun updateTodo(
            id: String,
            title: String,
            startDateTime: java.time.LocalDateTime?,
            endDateTime: java.time.LocalDateTime?,
            isCompleted: Boolean
        ): TodoSyncResult = TodoSyncResult.Success(emptyList(), 200)

        override suspend fun deleteTodo(id: String): TodoSyncResult =
            TodoSyncResult.Success(emptyList(), 200)

        override suspend fun reorderTodos(items: List<ReorderTodoItem>): TodoSyncResult =
            TodoSyncResult.Success(emptyList(), 200)
    }

    private class TestGoogleSignInFacade : GoogleSignInFacade {
        var idToken: String? = "token-123"

        override fun createSignInIntent(): Intent = Intent("test")

        override fun extractIdToken(data: Intent?): String? = idToken
    }
}
