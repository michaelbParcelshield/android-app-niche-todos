// ABOUTME: ViewModel coordinating backend health and auth interactions.
// ABOUTME: Exposes status updates for UI and handles coroutine lifecycle.
package com.example.niche_todos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class HealthStatus {
    data object Idle : HealthStatus()
    data object InProgress : HealthStatus()
    data class Success(val statusCode: Int) : HealthStatus()
    data class Failure(val statusCode: Int?, val message: String?) : HealthStatus()
}

sealed class AuthStatus {
    data object SignedOut : AuthStatus()
    data object SigningIn : AuthStatus()
    data object Authenticating : AuthStatus()
    data class Success(val statusCode: Int, val tokens: AuthTokens) : AuthStatus()
    data class Failure(val statusCode: Int?, val message: String?) : AuthStatus()
    data object MissingIdToken : AuthStatus()
}

class BackendStatusViewModel(
    private val healthRepository: HealthRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _healthStatus = MutableLiveData<HealthStatus>(HealthStatus.Idle)
    val healthStatus: LiveData<HealthStatus> = _healthStatus

    private val _authStatus = MutableLiveData<AuthStatus>(AuthStatus.SignedOut)
    val authStatus: LiveData<AuthStatus> = _authStatus

    fun runHealthCheck() {
        _healthStatus.value = HealthStatus.InProgress
        viewModelScope.launch {
            val result = healthRepository.runHealthCheck()
            _healthStatus.value = when (result) {
                is HealthCheckResult.Success -> HealthStatus.Success(result.statusCode)
                is HealthCheckResult.Failure -> HealthStatus.Failure(
                    result.statusCode,
                    result.message
                )
            }
        }
    }

    fun startSignIn() {
        _authStatus.value = AuthStatus.SigningIn
    }

    fun reportMissingIdToken() {
        _authStatus.value = AuthStatus.MissingIdToken
    }

    fun reportSignInFailure() {
        _authStatus.value = AuthStatus.Failure(null, "Sign-in failed")
    }

    fun authenticateWithGoogle(idToken: String) {
        _authStatus.value = AuthStatus.Authenticating
        viewModelScope.launch {
            val result = authRepository.exchangeGoogleIdToken(idToken)
            _authStatus.value = when (result) {
                is AuthResult.Success -> AuthStatus.Success(result.statusCode, result.tokens)
                is AuthResult.Failure -> AuthStatus.Failure(result.statusCode, result.message)
            }
        }
    }
}
