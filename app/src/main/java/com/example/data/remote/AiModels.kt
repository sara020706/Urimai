package com.example.data.remote

import com.squareup.moshi.JsonClass

/**
 * Wire types for the server-side AI proxy.
 *
 * The Gemini API key lives only in backend environment. It used to be injected
 * into BuildConfig by the Secrets Gradle plugin, which shipped it inside every
 * APK — readable by anyone who decompiled a build.
 */

@JsonClass(generateAdapter = true)
data class CriterionSummary(
    val title: String,
    val detail: String? = null
)

@JsonClass(generateAdapter = true)
data class ExplainSchemeRequest(
    val schemeName: String,
    val status: String,
    val department: String? = null,
    val benefitHighlight: String? = null,
    val passed: List<CriterionSummary> = emptyList(),
    val failed: List<CriterionSummary> = emptyList(),
    val missing: List<CriterionSummary> = emptyList(),
    val language: String = "en"
)

@JsonClass(generateAdapter = true)
data class ExplainSchemeResponse(val explanation: String)

@JsonClass(generateAdapter = true)
data class SchemeChatRequest(
    val question: String,
    val schemeName: String,
    val status: String? = null,
    val department: String? = null,
    val passed: List<CriterionSummary> = emptyList(),
    val failed: List<CriterionSummary> = emptyList(),
    val documents: List<String> = emptyList(),
    val language: String = "en"
)

@JsonClass(generateAdapter = true)
data class SchemeChatResponse(val answer: String)

/**
 * Candidate profile fields read from a certificate.
 *
 * Advisory only. The server never writes these to the profile; the citizen
 * reviews and corrects them, then the existing profile update applies them.
 */
@JsonClass(generateAdapter = true)
data class ExtractedProfileFields(
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val state: String? = null,
    val district: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val annualIncome: Long? = null,
    val familySize: Int? = null,
    val socialCategory: String? = null,
    val disabilityStatus: String? = null,
    val maritalStatus: String? = null
)

@JsonClass(generateAdapter = true)
data class ExtractProfileResponse(
    val fields: ExtractedProfileFields,
    val documentType: String?,
    val confidence: String,
    val advisory: String
)

@JsonClass(generateAdapter = true)
data class AiStatusResponse(
    val available: Boolean,
    val model: String?
)

/** How much to trust an extraction before showing it to the citizen. */
enum class ExtractionConfidence {
    HIGH, MEDIUM, LOW, UNKNOWN;

    companion object {
        fun from(value: String?): ExtractionConfidence =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

// --- General legal chat -----------------------------------------------------

/**
 * One turn in the legal chat.
 *
 * `role` is "user" or "assistant". The server clamps both the number of turns
 * and the size of each, so a long conversation degrades by forgetting its
 * oldest turns rather than by failing.
 */
@JsonClass(generateAdapter = true)
data class LegalChatTurn(
    val role: String,
    val text: String
)

@JsonClass(generateAdapter = true)
data class LegalChatRequest(
    val question: String,
    val history: List<LegalChatTurn> = emptyList(),
    val language: String = "en"
)

@JsonClass(generateAdapter = true)
data class LegalChatResponse(val answer: String)
