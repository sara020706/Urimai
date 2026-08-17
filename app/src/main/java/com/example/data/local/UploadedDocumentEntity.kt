package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaded_documents")
data class UploadedDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val documentName: String,
    val fileUri: String,
    val fileName: String,
    val mimeType: String?,
    val uploadedAt: Long
)
