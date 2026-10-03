package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.remote.ApiClient
import com.example.data.remote.ExplainSchemeRequest
import com.example.data.remote.ExtractProfileResponse
import com.example.data.remote.SchemeChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Client for the server-side AI proxy.
 *
 * Every method returns null rather than throwing when AI is unavailable, so
 * callers can fall back to a deterministic path. "Unavailable" covers a
 * backend with no key configured (503 AI_UNAVAILABLE), an upstream timeout, and
 * simply being offline — all three are ordinary states for this app, not errors
 * worth surfacing to the citizen.
 */
class AiRepository(private val context: Context) {

    suspend fun explainScheme(request: ExplainSchemeRequest): String? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context).explainScheme(request)
            val body = response.body()
            if (response.isSuccessful && body != null && body.explanation.isNotBlank()) {
                body.explanation
            } else {
                Log.i(TAG, "explainScheme unavailable: HTTP ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.i(TAG, "explainScheme failed: ${e.message}")
            null
        }
    }

    suspend fun schemeChat(request: SchemeChatRequest): String? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context).schemeChat(request)
            val body = response.body()
            if (response.isSuccessful && body != null && body.answer.isNotBlank()) {
                body.answer
            } else {
                Log.i(TAG, "schemeChat unavailable: HTTP ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.i(TAG, "schemeChat failed: ${e.message}")
            null
        }
    }

    /** Whether the backend has a key configured, so the UI can hide AI affordances. */
    suspend fun isAiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            ApiClient.getService(context).aiStatus().body()?.available ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Read profile fields from a certificate already uploaded to the server.
     *
     * Returns candidates for the citizen to confirm. Nothing is written to the
     * profile here: OCR is wrong often enough that trusting it silently would
     * put incorrect income or category data behind an eligibility verdict.
     */
    suspend fun extractFromUploadedDocument(documentId: String): ExtractionOutcome =
        withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.getService(context).extractProfileFromDocument(documentId)
                toOutcome(response.code(), response.isSuccessful, response.body())
            } catch (e: Exception) {
                Log.i(TAG, "extractFromUploadedDocument failed: ${e.message}")
                ExtractionOutcome.Unavailable
            }
        }

    /** Read profile fields from a file the citizen just picked, without storing it. */
    suspend fun extractFromFile(
        fileUri: String,
        fileName: String,
        mimeType: String?
    ): ExtractionOutcome = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val input = context.contentResolver.openInputStream(Uri.parse(fileUri))
                ?: return@withContext ExtractionOutcome.Unreadable

            tempFile = File.createTempFile("ocr_", null, context.cacheDir)
            input.use { stream -> tempFile.outputStream().use { stream.copyTo(it) } }

            val part = MultipartBody.Part.createFormData(
                "file", fileName, tempFile.asRequestBody(mimeType?.toMediaTypeOrNull())
            )
            val response = ApiClient.getService(context).extractProfileFromFile(part)
            toOutcome(response.code(), response.isSuccessful, response.body())
        } catch (e: Exception) {
            Log.i(TAG, "extractFromFile failed: ${e.message}")
            ExtractionOutcome.Unavailable
        } finally {
            // Runs even on failure, so a picked document never lingers in cache.
            tempFile?.delete()
        }
    }

    private fun toOutcome(
        code: Int,
        successful: Boolean,
        body: ExtractProfileResponse?
    ): ExtractionOutcome = when {
        successful && body != null -> ExtractionOutcome.Success(body)
        code == 415 -> ExtractionOutcome.UnsupportedType
        code == 413 -> ExtractionOutcome.TooLarge
        code == 429 -> ExtractionOutcome.RateLimited
        code == 502 -> ExtractionOutcome.Unreadable
        else -> ExtractionOutcome.Unavailable
    }

    companion object {
        private const val TAG = "AiRepository"
    }
}

/**
 * Result of a document extraction.
 *
 * Distinguished so the UI can say something useful: "try a clearer photo" is
 * actionable, "AI is unavailable" is not the citizen's problem to solve.
 */
sealed class ExtractionOutcome {
    data class Success(val response: ExtractProfileResponse) : ExtractionOutcome()

    /** The document was read but nothing usable came back — a clearer scan may help. */
    object Unreadable : ExtractionOutcome()

    object UnsupportedType : ExtractionOutcome()
    object TooLarge : ExtractionOutcome()
    object RateLimited : ExtractionOutcome()

    /** No key configured, offline, or an upstream failure. */
    object Unavailable : ExtractionOutcome()
}
