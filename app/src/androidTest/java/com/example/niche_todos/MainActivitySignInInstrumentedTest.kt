// ABOUTME: Instrumented tests for MainActivity sign-in flow.
// ABOUTME: Exercises auth result handling with real Activity lifecycle.
package com.example.niche_todos

import android.app.Activity
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySignInInstrumentedTest {
    private lateinit var authRepository: TestAuthRepository
    private lateinit var googleSignInFacade: TestGoogleSignInFacade

    @Before
    fun setUp() {
        authRepository = TestAuthRepository()
        googleSignInFacade = TestGoogleSignInFacade()
        MainActivityDependencies.repositoryFactory = { _, _ ->
            BackendRepositoryBundle(
                healthRepository = TestHealthRepository(),
                authRepository = authRepository,
                todoRepository = TestTodoRepository()
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
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity ->
            val statusText = activity.findViewById<TextView>(R.id.text_auth_status)
            assertEquals(
                activity.getString(R.string.auth_status_success, 200),
                statusText.text.toString()
            )
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

        scenario.onActivity { activity ->
            val statusText = activity.findViewById<TextView>(R.id.text_auth_status)
            assertEquals(
                activity.getString(R.string.auth_status_missing_id_token),
                statusText.text.toString()
            )
        }
    }

    @Test
    fun handleGoogleSignInResult_disablesButtonWhileAuthenticating() {
        val gate = CompletableDeferred<Unit>()
        authRepository.gate = gate
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.handleGoogleSignInResult(Activity.RESULT_OK, Intent())
            val signInButton = activity.findViewById<Button>(R.id.button_google_sign_in)
            assertFalse(signInButton.isEnabled)
        }

        gate.complete(Unit)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        scenario.onActivity { activity ->
            val signInButton = activity.findViewById<Button>(R.id.button_google_sign_in)
            assertTrue(signInButton.isEnabled)
        }
    }

    private class TestHealthRepository : HealthRepository {
        override suspend fun runHealthCheck(): HealthCheckResult =
            HealthCheckResult.Success(200)
    }

    private class TestAuthRepository : AuthRepository {
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun exchangeGoogleIdToken(idToken: String): AuthResult {
            gate?.await()
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
        override suspend fun fetchTodos(): TodoSyncResult =
            TodoSyncResult.Success(emptyList(), 200)

        override suspend fun createTodo(
            title: String,
            startDateTime: java.time.LocalDateTime?,
            endDateTime: java.time.LocalDateTime?,
            isCompleted: Boolean
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

        override suspend fun reorderTodos(orderedIds: List<String>): TodoSyncResult =
            TodoSyncResult.Success(emptyList(), 200)
    }

    private class TestGoogleSignInFacade : GoogleSignInFacade {
        var idToken: String? = "token-123"

        override fun createSignInIntent(): Intent = Intent("test")

        override fun extractIdToken(data: Intent?): String? = idToken
    }
}
