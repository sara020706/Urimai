package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.UploadedDocumentEntity
import kotlinx.coroutines.flow.Flow

class DocumentRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.uploadedDocumentDao()

    fun observeForUser(userId: Long): Flow<List<UploadedDocumentEntity>> =
        dao.observeForUser(userId)

    suspend fun saveUpload(
        userId: Long,
        documentName: String,
        fileUri: String,
        fileName: String,
        mimeType: String?
    ) {
        dao.insert(
            UploadedDocumentEntity(
                userId = userId,
                documentName = documentName,
                fileUri = fileUri,
                fileName = fileName,
                mimeType = mimeType,
                uploadedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeUpload(document: UploadedDocumentEntity) {
        dao.delete(document)
    }
}
