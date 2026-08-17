package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.UserAccountEntity
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed class AuthResult {
    data class Success(val userId: Long, val displayName: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class AuthRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.userAccountDao()

    suspend fun signUp(username: String, password: String, displayName: String): AuthResult {
        val normalizedUsername = username.trim().lowercase()
        if (normalizedUsername.isBlank() || password.isBlank()) {
            return AuthResult.Failure("Username and password are required.")
        }
        if (password.length < 4) {
            return AuthResult.Failure("Password must be at least 4 characters.")
        }
        if (dao.findByUsername(normalizedUsername) != null) {
            return AuthResult.Failure("An account with this username already exists.")
        }

        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val id = dao.insert(
            UserAccountEntity(
                username = normalizedUsername,
                passwordHash = hash,
                salt = salt,
                displayName = displayName.ifBlank { username },
                createdAt = System.currentTimeMillis()
            )
        )
        return AuthResult.Success(id, displayName.ifBlank { username })
    }

    suspend fun logIn(username: String, password: String): AuthResult {
        val normalizedUsername = username.trim().lowercase()
        val account = dao.findByUsername(normalizedUsername)
            ?: return AuthResult.Failure("No account found for this username.")

        val hash = hashPassword(password, account.salt)
        return if (hash == account.passwordHash) {
            AuthResult.Success(account.id, account.displayName)
        } else {
            AuthResult.Failure("Incorrect password.")
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, 10_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}
