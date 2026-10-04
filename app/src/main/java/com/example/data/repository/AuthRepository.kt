package com.example.data.repository

import android.content.Context
import com.example.data.remote.ApiClient
import com.example.data.remote.ApiErrorBody
import com.example.data.remote.LogInRequest
import com.example.data.remote.SessionManager
import com.example.data.remote.GoogleSignInClient
import com.example.data.remote.GoogleSignInRequest
import com.example.data.remote.SignUpRequest
import com.squareup.moshi.Moshi
import retrofit2.Response

sealed class AuthResult {
    data class Success(val userId: Long, val displayName: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class AuthRepository(context: Context) {
    private val api = ApiClient.getService(context)
    private val sessionManager = SessionManager(context)
    private val errorAdapter = Moshi.Builder().build().adapter(ApiErrorBody::class.java)

    suspend fun signUp(
        username: String,
        password: String,
        displayName: String,
        role: String? = null
    ): AuthResult {
        val normalizedUsername = username.trim().lowercase()
        if (normalizedUsername.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Username and password are required.")
        }
        if (password.length < 4) {
            return AuthResult.Failure("Password must be at least 4 characters.")
        }

        return try {
            val response = api.signUp(
                SignUpRequest(normalizedUsername, password, displayName.ifBlank { username }, role)
            )
            handleAuthResponse(response)
        } catch (e: Exception) {
            AuthResult.Failure("Could not reach the server. Check your connection and try again.")
        }
    }

    suspend fun logIn(username: String, password: String): AuthResult {
        val normalizedUsername = username.trim().lowercase()
        if (normalizedUsername.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Username and password are required.")
        }

        return try {
            val response = api.logIn(LogInRequest(normalizedUsername, password))
            handleAuthResponse(response)
        } catch (e: Exception) {
            AuthResult.Failure("Could not reach the server. Check your connection and try again.")
        }
    }

    /**
     * Sign in with Google.
     *
     * Two steps: get an ID token from Credential Manager, then exchange it at
     * the backend, which verifies Google's signature before issuing our own
     * session. A cancelled sheet returns null so the caller can stay silent
     * rather than reporting an error the person caused deliberately.
     *
     * @param activityContext must be an Activity context.
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        role: String? = null
    ): AuthResult? {
        return when (val outcome = GoogleSignInClient.getIdToken(activityContext)) {
            is GoogleSignInClient.Outcome.Cancelled -> null
            is GoogleSignInClient.Outcome.NoAccount ->
                AuthResult.Failure(
                    "No Google account was found on this device. Add one in " +
                        "Settings, or sign in with a username and password."
                )
            is GoogleSignInClient.Outcome.Failure -> AuthResult.Failure(outcome.message)
            is GoogleSignInClient.Outcome.Success -> try {
                handleAuthResponse(api.googleSignIn(GoogleSignInRequest(outcome.idToken, role)))
            } catch (e: Exception) {
                AuthResult.Failure(
                    "Could not reach the server. Check your connection and try again."
                )
            }
        }
    }

    /**
     * Whether this server offers Google sign-in.
     *
     * Defaults to false on any failure: hiding a working button is a smaller
     * harm than showing one that always fails.
     */
    suspend fun googleSignInAvailable(): Boolean {
        if (!GoogleSignInClient.isConfigured()) return false
        return try {
            api.authMethods().body()?.google == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logOut() {
        sessionManager.clearSession()
    }

    suspend fun restoreSession(): AuthResult? {
        val token = sessionManager.currentToken() ?: return null
        val userId = sessionManager.currentUserId() ?: return null
        val displayName = sessionManager.currentDisplayName() ?: return null
        return AuthResult.Success(userId, displayName)
    }

    private suspend fun handleAuthResponse(response: Response<com.example.data.remote.AuthResponse>): AuthResult {
        val body = response.body()
        if (response.isSuccessful && body != null) {
            val userId = body.userId.toLong()
            sessionManager.saveSession(
                    body.token, userId, body.displayName,
                    body.role, body.lawyerVerificationStatus
                )
            return AuthResult.Success(userId, body.displayName)
        }
        val message = response.errorBody()?.string()?.let {
            try { errorAdapter.fromJson(it)?.error } catch (_: Exception) { null }
        } ?: "Something went wrong. Please try again."
        return AuthResult.Failure(message)
    }
}
