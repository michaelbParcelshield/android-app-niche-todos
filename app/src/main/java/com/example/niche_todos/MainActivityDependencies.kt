// ABOUTME: Dependency providers used by MainActivity for backend operations.
// ABOUTME: Enables test overrides without altering Activity construction.
package com.example.niche_todos

import android.content.Context

object MainActivityDependencies {
    private val defaultRepositoryFactory: (Context, BackendEndpoints) -> BackendRepositories =
        { context, endpoints ->
            BackendRepositories(
                healthRepository = BackendHealthRepository(
                    client = HealthCheckClient(),
                    endpoints = endpoints
                ),
                authRepository = BackendAuthRepository(
                    client = BackendAuthClient(),
                    endpoints = endpoints,
                    tokenStore = SharedPreferencesAuthTokenStore(context.applicationContext)
                )
            )
        }

    private val defaultGoogleSignInFacadeFactory: (Context, String) -> GoogleSignInFacade =
        { context, serverClientId ->
            GoogleSignInFacadeImpl(context, serverClientId)
        }

    var repositoryFactory: (Context, BackendEndpoints) -> BackendRepositories =
        defaultRepositoryFactory
    var googleSignInFacadeFactory: (Context, String) -> GoogleSignInFacade =
        defaultGoogleSignInFacadeFactory

    fun reset() {
        repositoryFactory = defaultRepositoryFactory
        googleSignInFacadeFactory = defaultGoogleSignInFacadeFactory
    }
}
