package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val username: String,
    val password: String,
    val displayName: String,
    // The server accepts USER or LAWYER and silently downgrades anything else.
    // Asserting LAWYER grants nothing until an admin verifies the account.
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class LogInRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class GoogleSignInRequest(
    // The ID token Google issued. The server verifies its signature against
    // Google's public keys; nothing here is trusted on the client's word.
    val idToken: String,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthMethodsResponse(
    val password: Boolean = true,
    val google: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val userId: String,
    val displayName: String,
    val token: String,
    // Added when roles landed; nullable so an older server still parses.
    val role: String? = null,
    val lawyerVerificationStatus: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorBody(
    val error: String
)

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    val name: String,
    val age: Int?,
    val gender: String,
    val state: String,
    val district: String,
    val occupation: String,
    val education: String,
    val annualIncome: Long?,
    val familySize: Int,
    val isStudent: Boolean?,
    val isEmployed: Boolean?,
    val isFarmer: Boolean?,
    val isBusinessOwner: Boolean?,
    val socialCategory: String?,
    val disabilityStatus: String?,
    val maritalStatus: String?,
    val ownedDocuments: List<String>
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val name: String,
    val age: Int?,
    val gender: String,
    val state: String,
    val district: String,
    val occupation: String,
    val education: String,
    val annualIncome: Long?,
    val familySize: Int,
    val isStudent: Boolean?,
    val isEmployed: Boolean?,
    val isFarmer: Boolean?,
    val isBusinessOwner: Boolean?,
    val socialCategory: String?,
    val disabilityStatus: String?,
    val maritalStatus: String?,
    val ownedDocuments: List<String>
)

@JsonClass(generateAdapter = true)
data class DocumentResponse(
    val id: String,
    val documentName: String,
    val fileName: String,
    val mimeType: String?,
    val uploadedAt: Long
)
