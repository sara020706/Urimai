package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.remote.AnswerResponse
import com.example.data.remote.ApiClient
import com.example.data.remote.AskQuestionRequest
import com.example.data.remote.ContactRequestRequest
import com.example.data.remote.DirectoryLawyerResponse
import com.example.data.remote.InboxRequestResponse
import com.example.data.remote.LawyerQuestionDetailResponse
import com.example.data.remote.LawyerQuestionSummaryResponse
import com.example.data.remote.MyAnswerBody
import com.example.data.remote.MyAnswerSummaryResponse
import com.example.data.remote.MyContactRequestResponse
import com.example.data.remote.QuestionDetailResponse
import com.example.data.remote.QuestionSummaryResponse
import com.example.data.remote.ReportRequest
import com.example.data.remote.RespondToContactRequest
import com.example.data.remote.SubmitAnswerRequest
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Outcome of a Q&A or directory call.
 *
 * The server returns a machine-readable `code` alongside the message; it is
 * surfaced so the UI can branch on it (ALREADY_ANSWERED, LAWYER_NOT_VERIFIED,
 * NOT_ACCEPTING, RATE_LIMITED) instead of matching on prose.
 */
sealed class QnaResult<out T> {
    data class Success<T>(val value: T) : QnaResult<T>()
    data class Failure(val message: String, val code: String? = null) : QnaResult<Nothing>()
}

class QnaRepository(private val context: Context) {

    @JsonClass(generateAdapter = true)
    data class ApiError(val error: String? = null, val code: String? = null)

    private val errorAdapter = Moshi.Builder().build().adapter(ApiError::class.java)

    private fun failure(response: Response<*>, fallback: String): QnaResult.Failure {
        val raw = try { response.errorBody()?.string() } catch (_: Exception) { null }
        if (raw.isNullOrBlank()) return QnaResult.Failure(fallback)
        return try {
            val parsed = errorAdapter.fromJson(raw)
            QnaResult.Failure(parsed?.error ?: fallback, parsed?.code)
        } catch (_: Exception) {
            QnaResult.Failure(fallback)
        }
    }

    /** Run a call, mapping transport failures to a Failure rather than throwing. */
    private suspend fun <T> call(
        fallback: String,
        block: suspend () -> Response<T>
    ): QnaResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                QnaResult.Success(body)
            } else {
                failure(response, fallback)
            }
        } catch (e: Exception) {
            Log.w(TAG, "$fallback: ${e.message}")
            QnaResult.Failure("Could not reach the server.")
        }
    }

    /** For endpoints that return 204 with no body. */
    private suspend fun callUnit(
        fallback: String,
        block: suspend () -> Response<Unit>
    ): QnaResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (response.isSuccessful) QnaResult.Success(Unit) else failure(response, fallback)
        } catch (e: Exception) {
            Log.w(TAG, "$fallback: ${e.message}")
            QnaResult.Failure("Could not reach the server.")
        }
    }

    private suspend fun <T> listOrEmpty(block: suspend () -> Response<List<T>>): List<T> =
        withContext(Dispatchers.IO) {
            try {
                block().body() ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "list call failed: ${e.message}")
                emptyList()
            }
        }

    // --- Citizen -----------------------------------------------------------

    suspend fun askQuestion(
        title: String,
        body: String,
        category: String?,
        state: String?,
        language: String
    ): QnaResult<QuestionSummaryResponse> = call("Could not post your question.") {
        ApiClient.getService(context)
            .askQuestion(AskQuestionRequest(title, body, category, state, language))
    }

    suspend fun myQuestions(): List<QuestionSummaryResponse> =
        listOrEmpty { ApiClient.getService(context).myQuestions() }

    suspend fun questionDetail(id: String): QnaResult<QuestionDetailResponse> =
        call("Could not load the question.") {
            ApiClient.getService(context).questionDetail(id)
        }

    suspend fun closeQuestion(id: String): QnaResult<Unit> =
        callUnit("Could not close the question.") {
            ApiClient.getService(context).closeQuestion(id)
        }

    suspend fun report(
        targetType: String,
        targetId: String,
        reason: String,
        details: String? = null
    ): QnaResult<Map<String, Any?>> = call("Could not submit the report.") {
        ApiClient.getService(context).report(ReportRequest(targetType, targetId, reason, details))
    }

    // --- Lawyer ------------------------------------------------------------

    suspend fun lawyerFeed(
        category: String? = null,
        state: String? = null,
        language: String? = null
    ): List<LawyerQuestionSummaryResponse> =
        listOrEmpty { ApiClient.getService(context).lawyerQuestionFeed(category, state, language) }

    suspend fun lawyerQuestionDetail(id: String): QnaResult<LawyerQuestionDetailResponse> =
        call("Could not load the question.") {
            ApiClient.getService(context).lawyerQuestionDetail(id)
        }

    suspend fun submitAnswer(questionId: String, body: String): QnaResult<MyAnswerBody> =
        call("Could not submit your answer.") {
            ApiClient.getService(context).submitAnswer(questionId, SubmitAnswerRequest(body))
        }

    suspend fun updateAnswer(answerId: String, body: String): QnaResult<MyAnswerBody> =
        call("Could not update your answer.") {
            ApiClient.getService(context).updateAnswer(answerId, SubmitAnswerRequest(body))
        }

    suspend fun deleteAnswer(answerId: String): QnaResult<Unit> =
        callUnit("Could not delete your answer.") {
            ApiClient.getService(context).deleteAnswer(answerId)
        }

    suspend fun myAnswers(): List<MyAnswerSummaryResponse> =
        listOrEmpty { ApiClient.getService(context).myAnswers() }

    // --- Directory ---------------------------------------------------------

    suspend fun findLawyers(
        query: String? = null,
        state: String? = null,
        district: String? = null,
        specialization: String? = null,
        language: String? = null,
        minExperience: Int? = null,
        availableOnly: Boolean? = null
    ): List<DirectoryLawyerResponse> = listOrEmpty {
        ApiClient.getService(context).findLawyers(
            query, state, district, specialization, language, minExperience, availableOnly
        )
    }

    suspend fun lawyerProfile(id: String): QnaResult<DirectoryLawyerResponse> =
        call("Could not load this lawyer's profile.") {
            ApiClient.getService(context).lawyerProfile(id)
        }

    suspend fun requestContact(lawyerId: String, message: String?): QnaResult<String> =
        withContext(Dispatchers.IO) {
            when (val result = call<com.example.data.remote.ContactRequestCreatedResponse>(
                "Could not send your request."
            ) {
                ApiClient.getService(context).requestContact(lawyerId, ContactRequestRequest(message))
            }) {
                is QnaResult.Success -> QnaResult.Success(result.value.status)
                is QnaResult.Failure -> result
            }
        }

    suspend fun myContactRequests(): List<MyContactRequestResponse> =
        listOrEmpty { ApiClient.getService(context).myContactRequests() }

    suspend fun contactInbox(): List<InboxRequestResponse> =
        listOrEmpty { ApiClient.getService(context).contactInbox() }

    /** Master switch for whether citizens may send this lawyer contact requests. */
    suspend fun setAvailability(accepting: Boolean): QnaResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.getService(context).setAvailability(
                    com.example.data.remote.AvailabilityRequest(accepting)
                )
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    QnaResult.Success(body.acceptingQuestions)
                } else {
                    failure(response, "Could not change your availability.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "setAvailability failed: ${e.message}")
                QnaResult.Failure("Could not reach the server.")
            }
        }

    suspend fun respondToContact(requestId: String, action: String): QnaResult<Unit> =
        callUnit("Could not respond to the request.") {
            ApiClient.getService(context).respondToContact(
                requestId, RespondToContactRequest(action)
            )
        }

    companion object {
        private const val TAG = "QnaRepository"
    }
}

/** Convenience for rendering: has this answer been edited since posting? */
val AnswerResponse.wasEdited: Boolean
    get() = updatedAt > createdAt + 1000L
