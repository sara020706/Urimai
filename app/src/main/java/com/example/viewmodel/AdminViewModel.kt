package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.AdminLawyerResponse
import com.example.data.remote.AdminUserResponse
import com.example.data.remote.AuditLogEntryResponse
import com.example.data.remote.ContentReportResponse
import com.example.data.remote.LawyerProfileResponse
import com.example.data.remote.NotificationResponse
import com.example.data.remote.ReportedContentResponse
import com.example.data.remote.UpdateLawyerProfileRequest
import com.example.data.remote.VerificationDocumentResponse
import com.example.data.repository.AdminRepository
import com.example.data.repository.LawyerRepository
import com.example.data.repository.LawyerResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Administration, lawyer self-service, and notifications.
 *
 * Notifications live here rather than in [QnaViewModel] because every role sees
 * them, including admins who never touch the Q&A screens.
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)
    private val lawyerRepository = LawyerRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    // --- Notifications -----------------------------------------------------

    private val _notifications = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationResponse>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /**
     * Poll the unread count while the app is in the foreground.
     *
     * Deliberately polling rather than push: FCM would need a Firebase project,
     * a service-account key, token rotation plumbing and an Android 13+
     * permission prompt, and its failure modes (stale tokens, silent
     * non-delivery) are hard to debug. One indexed count query on resume plus a
     * slow timer covers every case this app has, and the partial index on
     * unread rows keeps it cheap no matter how large the table grows.
     */
    fun startNotificationPolling() {
        viewModelScope.launch {
            while (isActive) {
                _unreadCount.value = repository.unreadCount()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch { _unreadCount.value = repository.unreadCount() }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _notifications.value = repository.notifications()
            _isLoading.value = false
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            if (repository.markRead(null)) {
                _unreadCount.value = 0
                loadNotifications()
            }
        }
    }

    // --- Lawyer self-service ----------------------------------------------

    private val _myLawyerProfile = MutableStateFlow<LawyerProfileResponse?>(null)
    val myLawyerProfile: StateFlow<LawyerProfileResponse?> = _myLawyerProfile.asStateFlow()

    private val _myVerificationDocs =
        MutableStateFlow<List<VerificationDocumentResponse>>(emptyList())
    val myVerificationDocs: StateFlow<List<VerificationDocumentResponse>> =
        _myVerificationDocs.asStateFlow()

    fun loadMyLawyerProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = lawyerRepository.getMyProfile()) {
                is LawyerResult.Success -> _myLawyerProfile.value = result.value
                is LawyerResult.Failure -> _message.value = result.message
            }
            _myVerificationDocs.value = lawyerRepository.listMyDocuments()
            _isLoading.value = false
        }
    }

    fun saveLawyerProfile(request: UpdateLawyerProfileRequest, onSaved: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = lawyerRepository.updateMyProfile(request)) {
                is LawyerResult.Success -> {
                    _myLawyerProfile.value = result.value
                    _message.value = "Details saved. An administrator will review them."
                    onSaved()
                }
                is LawyerResult.Failure -> _message.value = result.message
            }
            _isSubmitting.value = false
        }
    }

    fun uploadVerificationDocument(
        docType: String,
        fileUri: String,
        fileName: String,
        mimeType: String?
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result =
                lawyerRepository.uploadDocument(docType, fileUri, fileName, mimeType)) {
                is LawyerResult.Success -> {
                    _myVerificationDocs.value = listOf(result.value) + _myVerificationDocs.value
                    _message.value = "Document uploaded."
                }
                is LawyerResult.Failure -> _message.value = result.message
            }
            _isSubmitting.value = false
        }
    }

    fun deleteVerificationDocument(id: String) {
        viewModelScope.launch {
            when (val result = lawyerRepository.deleteDocument(id)) {
                is LawyerResult.Success -> {
                    _myVerificationDocs.value = _myVerificationDocs.value.filterNot { it.id == id }
                    _message.value = "Document removed."
                }
                is LawyerResult.Failure -> _message.value = result.message
            }
        }
    }

    // --- Admin: users ------------------------------------------------------

    private val _users = MutableStateFlow<List<AdminUserResponse>>(emptyList())
    val users: StateFlow<List<AdminUserResponse>> = _users.asStateFlow()

    private val _userQuery = MutableStateFlow("")
    val userQuery: StateFlow<String> = _userQuery.asStateFlow()

    fun setUserQuery(value: String) { _userQuery.value = value }

    fun loadUsers(role: String? = null, status: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _users.value = repository.listUsers(
                role, status, _userQuery.value.takeIf { it.isNotBlank() }
            )
            _isLoading.value = false
        }
    }

    fun setAccountStatus(userId: String, status: String, reason: String?) {
        viewModelScope.launch {
            if (repository.setAccountStatus(userId, status, reason)) {
                _message.value = when (status) {
                    "BLOCKED" -> "Account blocked. Their session ended immediately."
                    "SUSPENDED" -> "Account suspended. Their session ended immediately."
                    else -> "Account reactivated."
                }
                loadUsers()
            } else {
                _message.value = "Could not change the account status."
            }
        }
    }

    // --- Admin: lawyer verification ---------------------------------------

    private val _pendingLawyers = MutableStateFlow<List<AdminLawyerResponse>>(emptyList())
    val pendingLawyers: StateFlow<List<AdminLawyerResponse>> = _pendingLawyers.asStateFlow()

    private val _selectedLawyerDocs =
        MutableStateFlow<List<VerificationDocumentResponse>>(emptyList())
    val selectedLawyerDocs: StateFlow<List<VerificationDocumentResponse>> =
        _selectedLawyerDocs.asStateFlow()

    private val _selectedLawyer = MutableStateFlow<AdminLawyerResponse?>(null)
    val selectedLawyer: StateFlow<AdminLawyerResponse?> = _selectedLawyer.asStateFlow()

    fun loadLawyerQueue(status: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _pendingLawyers.value = repository.listLawyers(status)
            _isLoading.value = false
        }
    }

    fun openLawyerApplication(lawyer: AdminLawyerResponse) {
        _selectedLawyer.value = lawyer
        viewModelScope.launch {
            _selectedLawyerDocs.value = repository.listLawyerDocuments(lawyer.userId)
        }
    }

    fun decideVerification(lawyerId: String, status: String, notes: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            if (repository.decideVerification(lawyerId, status, notes)) {
                _message.value = when (status) {
                    "VERIFIED" -> "Approved. They can now answer citizen questions."
                    "REJECTED" -> "Application rejected."
                    "MORE_INFO_REQUESTED" -> "More information requested."
                    "SUSPENDED" -> "Verification suspended."
                    else -> "Updated."
                }
                loadLawyerQueue()
                onDone()
            } else {
                _message.value = "Could not record the decision."
            }
            _isSubmitting.value = false
        }
    }

    // --- Admin: moderation -------------------------------------------------

    private val _reports = MutableStateFlow<List<ContentReportResponse>>(emptyList())
    val reports: StateFlow<List<ContentReportResponse>> = _reports.asStateFlow()

    private val _reportContent = MutableStateFlow<ReportedContentResponse?>(null)
    val reportContent: StateFlow<ReportedContentResponse?> = _reportContent.asStateFlow()

    fun loadReports(status: String? = "OPEN") {
        viewModelScope.launch {
            _isLoading.value = true
            _reports.value = repository.listReports(status)
            _isLoading.value = false
        }
    }

    fun openReport(reportId: String) {
        viewModelScope.launch {
            _reportContent.value = null
            _reportContent.value = repository.reportContent(reportId)
        }
    }

    fun resolveReport(reportId: String, action: String, note: String?) {
        viewModelScope.launch {
            if (repository.resolveReport(reportId, action, note)) {
                _message.value = when (action) {
                    "HIDE" -> "Content hidden."
                    "UNHIDE" -> "Content restored."
                    else -> "Report dismissed."
                }
                loadReports()
            } else {
                _message.value = "Could not resolve the report."
            }
        }
    }

    // --- Admin: audit ------------------------------------------------------

    private val _auditLog = MutableStateFlow<List<AuditLogEntryResponse>>(emptyList())
    val auditLog: StateFlow<List<AuditLogEntryResponse>> = _auditLog.asStateFlow()

    fun loadAuditLog() {
        viewModelScope.launch {
            _isLoading.value = true
            _auditLog.value = repository.auditLog()
            _isLoading.value = false
        }
    }

    companion object {
        /** Slow on purpose: this is a convenience, not a realtime channel. */
        private const val POLL_INTERVAL_MS = 60_000L
    }
}
