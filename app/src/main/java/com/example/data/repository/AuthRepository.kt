package com.example.data.repository

import android.content.Context
import com.example.data.remote.ApiClient
import com.example.data.remote.ApiErrorBody
import com.example.data.remote.LogInRequest
import com.example.data.remote.SessionManager
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

    suspend fun signUp(username: String, password: String, displayName: String): AuthResult {
        val normalizedUsername = username.trim().lowercase()
        if (normalizedUsername.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Username and password are required.")
        }
        if (password.length < 4) {
            return AuthResult.Failure("Password must be at least 4 characters.")
        }

        return try {
            val response = api.signUp(SignUpRequest(normalizedUsername, password, displayName.ifBlank { username }))
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
            sessionManager.saveSession(body.token, userId, body.displayName)
            return AuthResult.Success(userId, body.displayName)
        }
        val message = response.errorBody()?.string()?.let {
            try { errorAdapter.fromJson(it)?.error } catch (_: Exception) { null }
        } ?: "Something went wrong. Please try again."
        return AuthResult.Failure(message)
    }
}
