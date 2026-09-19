package com.example.data.model

data class UploadedDocumentEntity(
    val id: String,
    val documentName: String,
    val fileName: String,
    val mimeType: String?,
    val uploadedAt: Long
)
