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
