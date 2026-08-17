package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserAccountEntity?

    @Insert
    suspend fun insert(account: UserAccountEntity): Long
}
