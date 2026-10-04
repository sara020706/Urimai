package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.remote.AccountStatusRequest
import com.example.data.remote.AdminLawyerResponse
import com.example.data.remote.AdminUserResponse
import com.example.data.remote.ApiClient
import com.example.data.remote.AuditLogEntryResponse
import com.example.data.remote.ContentReportResponse
import com.example.data.remote.NotificationResponse
import com.example.data.remote.MarkReadRequest
import com.example.data.remote.ReportedContentResponse
import com.example.data.remote.ResolveReportRequest
import com.example.data.remote.VerificationDecisionRequest
import com.example.data.remote.VerificationDocumentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Administration and notifications.
 *
 * Every call here is refused server-side unless the caller is an ADMIN whose
 * account is ACTIVE, re-checked from the database on each request. This class
 * does no authorization of its own — it could not be trusted to.
 */
class AdminRepository(private val context: Context) {

    private suspend fun <T> listOrEmpty(block: suspend () -> Response<List<T>>): List<T> =
        withContext(Dispatchers.IO) {
            try {
                block().body() ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "list call failed: ${e.message}")
                emptyList()
            }
        }

    private suspend fun ok(block: suspend () -> Response<*>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                block().isSuccessful
            } catch (e: Exception) {
                Log.w(TAG, "call failed: ${e.message}")
                false
            }
        }

    // --- Users -------------------------------------------------------------

    suspend fun listUsers(
        role: String? = null,
        status: String? = null,
        query: String? = null
    ): List<AdminUserResponse> =
        listOrEmpty { ApiClient.getService(context).adminListUsers(role, status, query) }

    suspend fun setAccountStatus(userId: String, status: String, reason: String?): Boolean =
        ok {
            ApiClient.getService(context)
                .adminSetAccountStatus(userId, AccountStatusRequest(status, reason))
        }

    // --- Lawyer verification ----------------------------------------------

    suspend fun listLawyers(status: String? = null): List<AdminLawyerResponse> =
        listOrEmpty { ApiClient.getService(context).adminListLawyers(status) }

    suspend fun listLawyerDocuments(lawyerId: String): List<VerificationDocumentResponse> =
        listOrEmpty { ApiClient.getService(context).adminListLawyerDocuments(lawyerId) }

    /**
     * Download a verification document to a private cache file.
     *
     * The bytes come over an authenticated stream, so they cannot be handed to
     * an image loader as a URL; they are written to the app's own cache and the
     * file is returned. Previously this endpoint had no client method at all,
     * which meant admins approved or rejected applications without ever seeing
     * the credentials.
     *
     * The server audits every read of a verification document.
     */
    suspend fun downloadLawyerDocument(
        lawyerId: String,
        docId: String,
        fileName: String
    ): java.io.File? = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context)
                .adminDownloadLawyerDocument(lawyerId, docId)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                Log.w(TAG, "document download failed: HTTP ${response.code()}")
                return@withContext null
            }

            // Keep the extension: the viewer picks a renderer from it, and
            // FileProvider hands it to an external app for anything we cannot
            // render ourselves.
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(80)
            val dir = java.io.File(context.cacheDir, "verification_docs").apply { mkdirs() }
            val target = java.io.File(dir, "${docId}_$safeName")

            body.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target
        } catch (e: Exception) {
            Log.w(TAG, "document download error: ${e.message}")
            null
        }
    }

    suspend fun decideVerification(
        lawyerId: String,
        status: String,
        notes: String?
    ): Boolean = ok {
        ApiClient.getService(context)
            .adminDecideVerification(lawyerId, VerificationDecisionRequest(status, notes))
    }

    // --- Moderation --------------------------------------------------------

    suspend fun listReports(status: String? = "OPEN"): List<ContentReportResponse> =
        listOrEmpty { ApiClient.getService(context).adminListReports(status) }

    suspend fun reportContent(reportId: String): ReportedContentResponse? =
        withContext(Dispatchers.IO) {
            try {
                ApiClient.getService(context).adminReportContent(reportId).body()
            } catch (e: Exception) {
                Log.w(TAG, "reportContent failed: ${e.message}")
                null
            }
        }

    suspend fun resolveReport(reportId: String, action: String, note: String?): Boolean =
        ok {
            ApiClient.getService(context)
                .adminResolveReport(reportId, ResolveReportRequest(action, note))
        }

    suspend fun auditLog(targetType: String? = null): List<AuditLogEntryResponse> =
        listOrEmpty { ApiClient.getService(context).adminAuditLog(targetType) }

    // --- Notifications (available to every signed-in user) -----------------

    suspend fun notifications(unreadOnly: Boolean = false): List<NotificationResponse> =
        listOrEmpty { ApiClient.getService(context).listNotifications(unreadOnly) }

    /**
     * Unread count for the badge.
     *
     * Returns 0 rather than failing: a badge that cannot be fetched should
     * simply not appear, not surface an error over the rest of the UI.
     */
    suspend fun unreadCount(): Int = withContext(Dispatchers.IO) {
        try {
            ApiClient.getService(context).unreadNotificationCount().body()?.count ?: 0
        } catch (_: Exception) {
            0
        }
    }

    suspend fun markRead(ids: List<String>? = null): Boolean =
        ok { ApiClient.getService(context).markNotificationsRead(MarkReadRequest(ids)) }

    companion object {
        private const val TAG = "AdminRepository"
    }
}
