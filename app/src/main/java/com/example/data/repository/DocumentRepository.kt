package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.UploadedDocumentEntity
import com.example.data.remote.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(private val context: Context) {
    private val api = ApiClient.getService(context)

    suspend fun listDocuments(): List<UploadedDocumentEntity> {
        val response = api.listDocuments()
        val body = response.body() ?: return emptyList()
        return body.map {
            UploadedDocumentEntity(
                id = it.id,
                documentName = it.documentName,
                fileName = it.fileName,
                mimeType = it.mimeType,
                uploadedAt = it.uploadedAt
            )
        }
    }

    /**
     * Fetch a document the citizen already uploaded, so they can confirm they
     * picked the right file before it is used for eligibility or OCR.
     *
     * Written into a dedicated cache subdirectory, which is the only path the
     * FileProvider exposes -- the same scoping the admin viewer uses.
     */
    suspend fun downloadOwnDocument(
        documentId: String,
        fileName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadDocument(documentId)
            val body = response.body()
            if (!response.isSuccessful || body == null) return@withContext null

            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(80)
            val dir = File(context.cacheDir, "verification_docs").apply { mkdirs() }
            val target = File(dir, "${documentId}_$safeName")
            body.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadDocument(documentName: String, fileUri: String, fileName: String, mimeType: String?): UploadedDocumentEntity? {
        val uri = Uri.parse(fileUri)
        val tempFile = File.createTempFile("upload", null, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }

        val mediaType = (mimeType ?: "application/octet-stream").toMediaTypeOrNull()
        val filePart = MultipartBody.Part.createFormData("file", fileName, tempFile.asRequestBody(mediaType))
        val namePart = documentName.toRequestBody("text/plain".toMediaTypeOrNull())

        return try {
            val response = api.uploadDocument(namePart, filePart)
            val body = response.body() ?: return null
            UploadedDocumentEntity(
                id = body.id,
                documentName = body.documentName,
                fileName = body.fileName,
                mimeType = body.mimeType,
                uploadedAt = body.uploadedAt
            )
        } finally {
            tempFile.delete()
        }
    }

    suspend fun removeUpload(document: UploadedDocumentEntity) {
        api.deleteDocument(document.id)
    }
}
