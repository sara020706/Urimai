package com.example.data.remote

import com.squareup.moshi.JsonClass

/**
 * Wire types for lawyer registration, verification and administration.
 *
 * Verification status is carried as a String rather than an enum: the server is
 * the authority on which statuses exist, and an unrecognised value must not
 * crash the client. [LawyerVerificationStatus] interprets it.
 */
@JsonClass(generateAdapter = true)
data class LawyerProfileResponse(
    val userId: String,
    val fullName: String,
    val barCouncilRegNumber: String,
    val barCouncilState: String,
    val enrollmentYear: Int?,
    val yearsExperience: Int,
    val specializations: List<String>,
    val languages: List<String>,
    val practiceState: String?,
    val practiceDistrict: String?,
    val bio: String?,
    val verificationStatus: String,
    val verificationNotes: String?,
    val acceptingQuestions: Boolean,
    val contactEmail: String?,
    val contactPhone: String?,
    val reviewedAt: Long?
)

@JsonClass(generateAdapter = true)
data class UpdateLawyerProfileRequest(
    val fullName: String,
    val barCouncilRegNumber: String,
    val barCouncilState: String,
    val enrollmentYear: Int? = null,
    val yearsExperience: Int = 0,
    val specializations: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val practiceState: String? = null,
    val practiceDistrict: String? = null,
    val bio: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null
)

@JsonClass(generateAdapter = true)
data class VerificationDocumentResponse(
    val id: String,
    val docType: String,
    val fileName: String,
    val mimeType: String?,
    val byteSize: Int?,
    val uploadedAt: Long
)

@JsonClass(generateAdapter = true)
data class AdminUserResponse(
    val id: String,
    val username: String,
    val displayName: String,
    val role: String,
    val accountStatus: String,
    val statusReason: String?,
    val createdAt: Long,
    val lawyerVerificationStatus: String?
)

@JsonClass(generateAdapter = true)
data class AdminLawyerResponse(
    val userId: String,
    val username: String,
    val displayName: String,
    val accountStatus: String,
    val fullName: String,
    val barCouncilRegNumber: String,
    val barCouncilState: String,
    val enrollmentYear: Int?,
    val yearsExperience: Int,
    val specializations: List<String>,
    val languages: List<String>,
    val practiceState: String?,
    val practiceDistrict: String?,
    val bio: String?,
    val verificationStatus: String,
    val verificationNotes: String?,
    val documentCount: Int,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class VerificationDecisionRequest(
    val status: String,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class AccountStatusRequest(
    val status: String,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class AuditLogEntryResponse(
    val id: String,
    val adminUsername: String,
    val action: String,
    val targetType: String,
    val targetId: String?,
    val reason: String?,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val body: String?,
    val targetType: String?,
    val targetId: String?,
    val readAt: Long?,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class UnreadCountResponse(val count: Int)

@JsonClass(generateAdapter = true)
data class MarkReadRequest(val ids: List<String>? = null)

// --- Moderation ------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ContentReportResponse(
    val id: String,
    val reporterUsername: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val details: String?,
    val status: String,
    val resolutionNote: String?,
    val createdAt: Long
)

/**
 * The reported item itself, so a moderator judges rather than guesses.
 *
 * The shape varies by target type, so the payload stays loosely typed here and
 * the screen reads the handful of fields it needs.
 */
@JsonClass(generateAdapter = true)
data class ReportedContentResponse(
    val targetType: String,
    val content: Map<String, String?>?
)

@JsonClass(generateAdapter = true)
data class ResolveReportRequest(
    val action: String,
    val note: String? = null
)

// --- Enums -----------------------------------------------------------------

/**
 * Account role. UNKNOWN covers a role this build does not recognise, which is
 * treated as having no special capability rather than as an error.
 */
enum class UserRole {
    USER, LAWYER, ADMIN, UNKNOWN;

    companion object {
        fun from(value: String?): UserRole =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class LawyerVerificationStatus {
    PENDING, VERIFIED, REJECTED, SUSPENDED, MORE_INFO_REQUESTED, UNKNOWN;

    /**
     * Whether lawyer features should be offered in the UI.
     *
     * This is a presentation hint only. The server enforces the real rule on
     * every request; the client must never treat this as the authorization
     * decision.
     */
    val unlocksLawyerFeatures: Boolean get() = this == VERIFIED

    companion object {
        fun from(value: String?): LawyerVerificationStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
