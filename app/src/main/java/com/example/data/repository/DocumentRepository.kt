package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.UploadedDocumentEntity
import com.example.data.remote.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
