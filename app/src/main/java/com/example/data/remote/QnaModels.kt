package com.example.data.remote

import com.squareup.moshi.JsonClass

/**
 * Wire types for anonymous legal Q&A and the lawyer directory.
 *
 * Note what the lawyer-facing types do NOT carry: no asker identity and no
 * answer count. That is the server's guarantee, mirrored here so the absence is
 * visible in the client's type system rather than merely assumed.
 */

// --- Citizen side ----------------------------------------------------------

@JsonClass(generateAdapter = true)
data class AskQuestionRequest(
    val title: String,
    val body: String,
    val category: String? = null,
    val state: String? = null,
    val language: String = "en"
)

@JsonClass(generateAdapter = true)
data class QuestionSummaryResponse(
    val id: String,
    val title: String,
    val category: String?,
    val state: String?,
    val language: String,
    val status: String,
    val answerCount: Int,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class QuestionDetailBody(
    val id: String,
    val title: String,
    val body: String,
    val category: String?,
    val state: String?,
    val language: String,
    val status: String,
    val answerCount: Int,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class AnswerResponse(
    val id: String,
    val body: String,
    val lawyerUserId: String,
    val lawyerName: String,
    val lawyerYearsExperience: Int,
    val lawyerSpecializations: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class QuestionDetailResponse(
    val question: QuestionDetailBody,
    val answers: List<AnswerResponse>
)

// --- Lawyer side -----------------------------------------------------------

/**
 * A question as a lawyer sees it in the feed.
 *
 * [iHaveAnswered] is derived from the caller's own row. There is deliberately
 * no count of other lawyers' answers: a live count is a timing side channel and
 * encourages racing, and it does not help anyone write a better answer.
 */
@JsonClass(generateAdapter = true)
data class LawyerQuestionSummaryResponse(
    val id: String,
    val title: String,
    val category: String?,
    val state: String?,
    val language: String,
    val status: String,
    val iHaveAnswered: Boolean,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class LawyerQuestionBody(
    val id: String,
    val title: String,
    val body: String,
    val category: String?,
    val state: String?,
    val language: String,
    val status: String,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class MyAnswerBody(
    val id: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class LawyerQuestionDetailResponse(
    val question: LawyerQuestionBody,
    val myAnswer: MyAnswerBody?
)

@JsonClass(generateAdapter = true)
data class SubmitAnswerRequest(val body: String)

@JsonClass(generateAdapter = true)
data class MyAnswerSummaryResponse(
    val id: String,
    val body: String,
    val questionId: String,
    val questionTitle: String,
    val questionCategory: String?,
    val questionStatus: String,
    val createdAt: Long,
    val updatedAt: Long
)

// --- Directory -------------------------------------------------------------

/**
 * A directory entry.
 *
 * Contact details are absent by design: they are released only through an
 * accepted contact request. Adding them here would make them browsable.
 */
@JsonClass(generateAdapter = true)
data class DirectoryLawyerResponse(
    val userId: String,
    val fullName: String,
    val barCouncilState: String,
    val enrollmentYear: Int?,
    val yearsExperience: Int,
    val specializations: List<String>,
    val languages: List<String>,
    val practiceState: String?,
    val practiceDistrict: String?,
    val bio: String?,
    val acceptingQuestions: Boolean,
    val verified: Boolean
)

@JsonClass(generateAdapter = true)
data class ContactRequestRequest(val message: String? = null)

@JsonClass(generateAdapter = true)
data class ContactRequestCreatedResponse(
    val id: String,
    val status: String
)

@JsonClass(generateAdapter = true)
data class ContactRequestLawyer(
    val userId: String,
    val fullName: String,
    val practiceState: String?,
    val practiceDistrict: String?,
    val yearsExperience: Int,
    val specializations: List<String>
)

/**
 * A contact request as its sender sees it.
 *
 * [contactEmail] and [contactPhone] are null unless the lawyer accepted. The
 * server nulls them in SQL, so a client that forgot to check `status` still
 * cannot display details it was not granted.
 */
@JsonClass(generateAdapter = true)
data class MyContactRequestResponse(
    val id: String,
    val status: String,
    val message: String?,
    val createdAt: Long,
    val respondedAt: Long?,
    val lawyer: ContactRequestLawyer,
    val contactEmail: String?,
    val contactPhone: String?
)

@JsonClass(generateAdapter = true)
data class InboxRequestResponse(
    val id: String,
    val status: String,
    val message: String?,
    val citizenName: String,
    val citizenDistrict: String?,
    val citizenState: String?,
    val createdAt: Long,
    val respondedAt: Long?
)

@JsonClass(generateAdapter = true)
data class RespondToContactRequest(val action: String)

@JsonClass(generateAdapter = true)
data class AvailabilityRequest(val acceptingQuestions: Boolean)

@JsonClass(generateAdapter = true)
data class AvailabilityResponse(val acceptingQuestions: Boolean)

@JsonClass(generateAdapter = true)
data class ReportRequest(
    val targetType: String,
    val targetId: String,
    val reason: String,
    val details: String? = null
)

/** Status of a contact request, with UNKNOWN for a value this build predates. */
enum class ContactRequestStatus {
    PENDING, ACCEPTED, DECLINED, BLOCKED, UNKNOWN;

    companion object {
        fun from(value: String?): ContactRequestStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

/** Lifecycle of a question. */
enum class QuestionStatus {
    OPEN, ANSWERED, CLOSED, REMOVED, UNKNOWN;

    val acceptsAnswers: Boolean get() = this == OPEN || this == ANSWERED

    companion object {
        fun from(value: String?): QuestionStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
