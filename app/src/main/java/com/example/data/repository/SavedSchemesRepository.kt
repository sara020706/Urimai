package com.example.data.repository

import android.content.Context
import com.example.data.remote.ApiClient

class SavedSchemesRepository(context: Context) {
    private val api = ApiClient.getService(context)

    suspend fun getSavedSchemeIds(): Set<String> {
        val response = api.getSavedSchemes()
        return response.body()?.toSet() ?: emptySet()
    }

    suspend fun saveScheme(schemeId: String) {
        api.saveScheme(schemeId)
    }

    suspend fun unsaveScheme(schemeId: String) {
        api.unsaveScheme(schemeId)
    }
}
