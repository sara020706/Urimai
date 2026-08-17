package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadedDocumentDao {
    @Query("SELECT * FROM uploaded_documents WHERE userId = :userId ORDER BY uploadedAt DESC")
    fun observeForUser(userId: Long): Flow<List<UploadedDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: UploadedDocumentEntity): Long

    @Delete
    suspend fun delete(document: UploadedDocumentEntity)
}
