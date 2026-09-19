package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val username: String,
    val password: String,
    val displayName: String
)

@JsonClass(generateAdapter = true)
data class LogInRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val userId: String,
    val displayName: String,
    val token: String
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
