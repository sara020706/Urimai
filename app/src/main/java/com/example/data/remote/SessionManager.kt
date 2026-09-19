package com.example.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "urimai_session")

private val TOKEN_KEY = stringPreferencesKey("auth_token")
private val USER_ID_KEY = longPreferencesKey("user_id")
private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")

class SessionManager(private val context: Context) {

    val tokenFlow = context.sessionDataStore.data.map { it[TOKEN_KEY] }

    suspend fun currentToken(): String? = tokenFlow.first()

    suspend fun saveSession(token: String, userId: Long, displayName: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[DISPLAY_NAME_KEY] = displayName
        }
    }

    suspend fun currentUserId(): Long? = context.sessionDataStore.data.map { it[USER_ID_KEY] }.first()

    suspend fun currentDisplayName(): String? = context.sessionDataStore.data.map { it[DISPLAY_NAME_KEY] }.first()

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }
}
