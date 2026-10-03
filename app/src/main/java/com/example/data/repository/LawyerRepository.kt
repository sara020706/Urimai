package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.remote.ApiClient
import com.example.data.remote.LawyerProfileResponse
import com.example.data.remote.UpdateLawyerProfileRequest
import com.example.data.remote.VerificationDocumentResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Outcome of a call that can fail in a way the user needs explained.
 *
 * The server returns a machine-readable `code` alongside the human message; it
 * is surfaced here so the UI can react (for example routing a
 * LAWYER_NOT_VERIFIED response to the verification-status screen) without
 * string-matching the message.
 */
sealed class LawyerResult<out T> {
    data class Success<T>(val value: T) : LawyerResult<T>()
    data class Failure(val message: String, val code: String? = null) : LawyerResult<Nothing>()
}

class LawyerRepository(private val context: Context) {

    private val errorAdapter = Moshi.Builder().build().adapter(ApiError::class.java)

    @com.squareup.moshi.JsonClass(generateAdapter = true)
    data class ApiError(val error: String? = null, val code: String? = null)

    private fun parseError(raw: String?, fallback: String): LawyerResult.Failure {
        if (raw.isNullOrBlank()) return LawyerResult.Failure(fallback)
        return try {
            val parsed = errorAdapter.fromJson(raw)
            LawyerResult.Failure(parsed?.error ?: fallback, parsed?.code)
        } catch (_: Exception) {
            LawyerResult.Failure(fallback)
        }
    }

    suspend fun getMyProfile(): LawyerResult<LawyerProfileResponse?> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context).getLawyerProfile()
            when {
                response.isSuccessful -> LawyerResult.Success(response.body())
                // 404 is not an error: it simply means the lawyer has not filled
                // in their professional details yet.
                response.code() == 404 -> LawyerResult.Success(null)
                else -> parseError(response.errorBody()?.string(), "Could not load your profile.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "getMyProfile failed", e)
            LawyerResult.Failure("Could not reach the server.")
        }
    }

    suspend fun updateMyProfile(
        request: UpdateLawyerProfileRequest
    ): LawyerResult<LawyerProfileResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context).updateLawyerProfile(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                LawyerResult.Success(body)
            } else {
                parseError(response.errorBody()?.string(), "Could not save your profile.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "updateMyProfile failed", e)
            LawyerResult.Failure("Could not reach the server.")
        }
    }

    suspend fun listMyDocuments(): List<VerificationDocumentResponse> = withContext(Dispatchers.IO) {
        try {
            ApiClient.getService(context).listVerificationDocuments().body() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "listMyDocuments failed", e)
            emptyList()
        }
    }

    /**
     * Upload a verification document.
     *
     * Mirrors [DocumentRepository]: copy the content URI to a temp file, upload,
     * then delete the temp file in a finally so it is cleaned up even on failure.
     */
    suspend fun uploadDocument(
        docType: String,
        fileUri: String,
        fileName: String,
        mimeType: String?
    ): LawyerResult<VerificationDocumentResponse> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val resolved = Uri.parse(fileUri)
            val input = context.contentResolver.openInputStream(resolved)
                ?: return@withContext LawyerResult.Failure("Could not read the selected file.")

            tempFile = File.createTempFile("verification_", null, context.cacheDir)
            input.use { stream -> tempFile.outputStream().use { stream.copyTo(it) } }

            val media = mimeType?.toMediaTypeOrNull()
            val part = MultipartBody.Part.createFormData(
                "file", fileName, tempFile.asRequestBody(media)
            )
            val typePart = docType.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = ApiClient.getService(context).uploadVerificationDocument(typePart, part)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                LawyerResult.Success(body)
            } else {
                parseError(response.errorBody()?.string(), "Upload failed.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "uploadDocument failed", e)
            LawyerResult.Failure("Could not upload the document.")
        } finally {
            tempFile?.delete()
        }
    }

    suspend fun deleteDocument(id: String): LawyerResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context).deleteVerificationDocument(id)
            if (response.isSuccessful) {
                LawyerResult.Success(Unit)
            } else {
                parseError(response.errorBody()?.string(), "Could not remove the document.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteDocument failed", e)
            LawyerResult.Failure("Could not reach the server.")
        }
    }

    companion object {
        private const val TAG = "LawyerRepository"
    }
}
