package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Scheme
import com.example.data.remote.ApiClient
import com.example.data.remote.SchemeDto
import com.example.data.remote.toDomain
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Supplies the scheme catalog, preferring the backend and degrading safely.
 *
 * Resolution order:
 *   1. the backend (GET /schemes)
 *   2. the last successful response, cached on disk
 *   3. [SchemeRepository], the catalog compiled into the APK
 *
 * Step 3 is what makes the migration to a server-side catalog safe to ship: a
 * backend outage, or a bad deploy, cannot leave the user with an empty app. The
 * bundled catalog stays in the APK for at least one release after the API goes
 * live, and is also the oracle the differential test compares against.
 */
class SchemeCatalogRepository(private val context: Context) {

    private val cacheFile: File
        get() = File(context.cacheDir, CACHE_FILE_NAME)

    private val adapter by lazy {
        Moshi.Builder().build().adapter<List<SchemeDto>>(
            Types.newParameterizedType(List::class.java, SchemeDto::class.java)
        )
    }

    /**
     * Load the catalog, falling back as described above.
     *
     * Never throws and never returns an empty list: the bundled catalog is
     * always available as a last resort.
     */
    suspend fun loadSchemes(): CatalogResult = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context).getSchemes()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val schemes = body.mapNotNull { it.toDomain() }
                if (schemes.isNotEmpty()) {
                    writeCache(body)
                    return@withContext CatalogResult(schemes, CatalogSource.NETWORK)
                }
                Log.w(TAG, "Catalog response parsed to zero usable schemes; falling back.")
            } else {
                Log.w(TAG, "Catalog request failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Catalog request error: ${e.message}")
        }

        readCache()?.let { cached ->
            if (cached.isNotEmpty()) {
                return@withContext CatalogResult(cached, CatalogSource.CACHE)
            }
        }

        CatalogResult(SchemeRepository.allSchemes, CatalogSource.BUNDLED)
    }

    private fun writeCache(dtos: List<SchemeDto>) {
        try {
            cacheFile.writeText(adapter.toJson(dtos))
        } catch (e: Exception) {
            // A failed cache write is not worth surfacing: the next launch just
            // fetches from the network again.
            Log.w(TAG, "Could not write catalog cache: ${e.message}")
        }
    }

    private fun readCache(): List<Scheme>? {
        return try {
            if (!cacheFile.exists()) return null
            adapter.fromJson(cacheFile.readText())?.mapNotNull { it.toDomain() }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read catalog cache: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SchemeCatalog"
        private const val CACHE_FILE_NAME = "scheme_catalog.json"
    }
}

enum class CatalogSource { NETWORK, CACHE, BUNDLED }

data class CatalogResult(
    val schemes: List<Scheme>,
    val source: CatalogSource
)
