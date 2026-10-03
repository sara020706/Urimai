package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.DirectoryLawyerResponse
import com.example.data.remote.InboxRequestResponse
import com.example.data.remote.LawyerQuestionDetailResponse
import com.example.data.remote.LawyerQuestionSummaryResponse
import com.example.data.remote.MyAnswerSummaryResponse
import com.example.data.remote.MyContactRequestResponse
import com.example.data.remote.QuestionDetailResponse
import com.example.data.remote.QuestionSummaryResponse
import com.example.data.repository.QnaRepository
import com.example.data.repository.QnaResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for anonymous Q&A and the lawyer directory.
 *
 * Kept separate from [UrimaiViewModel], which already owns the scheme and
 * profile flows. Folding these in would have pushed one class past the point
 * where any of it is followable.
 */
class QnaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QnaRepository(application)

    // --- Shared transient UI state ----------------------------------------

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** One-shot message for a snackbar. Cleared by the UI once shown. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    // --- Citizen: my questions --------------------------------------------

    private val _myQuestions = MutableStateFlow<List<QuestionSummaryResponse>>(emptyList())
    val myQuestions: StateFlow<List<QuestionSummaryResponse>> = _myQuestions.asStateFlow()

    private val _selectedQuestion = MutableStateFlow<QuestionDetailResponse?>(null)
    val selectedQuestion: StateFlow<QuestionDetailResponse?> = _selectedQuestion.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun loadMyQuestions() {
        viewModelScope.launch {
            _isLoading.value = true
            _myQuestions.value = repository.myQuestions()
            _isLoading.value = false
        }
    }

    /** Post a question. [onPosted] runs only on success, so the UI can navigate. */
    fun askQuestion(
        title: String,
        body: String,
        category: String?,
        state: String?,
        language: String,
        onPosted: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.askQuestion(title, body, category, state, language)) {
                is QnaResult.Success -> {
                    _myQuestions.value = listOf(result.value) + _myQuestions.value
                    _message.value = "Your question has been sent to verified lawyers."
                    onPosted()
                }
                is QnaResult.Failure -> _message.value = result.message
            }
            _isSubmitting.value = false
        }
    }

    fun openQuestion(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedQuestion.value = null
            when (val result = repository.questionDetail(id)) {
                is QnaResult.Success -> _selectedQuestion.value = result.value
                is QnaResult.Failure -> _message.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun closeQuestion(id: String) {
        viewModelScope.launch {
            when (val result = repository.closeQuestion(id)) {
                is QnaResult.Success -> {
                    _message.value = "Question closed."
                    loadMyQuestions()
                    openQuestion(id)
                }
                is QnaResult.Failure -> _message.value = result.message
            }
        }
    }

    fun report(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            when (repository.report(targetType, targetId, reason)) {
                is QnaResult.Success ->
                    _message.value = "Reported. Our moderators will review it."
                is QnaResult.Failure ->
                    _message.value = "Could not submit the report."
            }
        }
    }

    // --- Lawyer: question feed and answers ---------------------------------

    private val _lawyerFeed = MutableStateFlow<List<LawyerQuestionSummaryResponse>>(emptyList())
    val lawyerFeed: StateFlow<List<LawyerQuestionSummaryResponse>> = _lawyerFeed.asStateFlow()

    private val _lawyerQuestion = MutableStateFlow<LawyerQuestionDetailResponse?>(null)
    val lawyerQuestion: StateFlow<LawyerQuestionDetailResponse?> = _lawyerQuestion.asStateFlow()

    private val _myAnswers = MutableStateFlow<List<MyAnswerSummaryResponse>>(emptyList())
    val myAnswers: StateFlow<List<MyAnswerSummaryResponse>> = _myAnswers.asStateFlow()

    /** Set when the server refuses because verification is not approved. */
    private val _lawyerAccessDenied = MutableStateFlow(false)
    val lawyerAccessDenied: StateFlow<Boolean> = _lawyerAccessDenied.asStateFlow()

    fun loadLawyerFeed(category: String? = null, state: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _lawyerFeed.value = repository.lawyerFeed(category, state)
            _isLoading.value = false
        }
    }

    fun openLawyerQuestion(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _lawyerQuestion.value = null
            when (val result = repository.lawyerQuestionDetail(id)) {
                is QnaResult.Success -> _lawyerQuestion.value = result.value
                is QnaResult.Failure -> {
                    if (result.code == "LAWYER_NOT_VERIFIED") _lawyerAccessDenied.value = true
                    _message.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Submit or update an answer, depending on whether one already exists.
     *
     * The server enforces one answer per lawyer per question, so attempting a
     * second insert would be refused; this routes to the edit path instead.
     */
    fun submitAnswer(questionId: String, existingAnswerId: String?, body: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = if (existingAnswerId != null) {
                repository.updateAnswer(existingAnswerId, body)
            } else {
                repository.submitAnswer(questionId, body)
            }
            when (result) {
                is QnaResult.Success -> {
                    _message.value = if (existingAnswerId != null) {
                        "Your answer has been updated."
                    } else {
                        "Your answer has been sent."
                    }
                    openLawyerQuestion(questionId)
                    loadLawyerFeed()
                }
                is QnaResult.Failure -> {
                    if (result.code == "LAWYER_NOT_VERIFIED") _lawyerAccessDenied.value = true
                    _message.value = result.message
                }
            }
            _isSubmitting.value = false
        }
    }

    fun deleteAnswer(answerId: String, questionId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteAnswer(answerId)) {
                is QnaResult.Success -> {
                    _message.value = "Answer withdrawn."
                    openLawyerQuestion(questionId)
                }
                is QnaResult.Failure -> _message.value = result.message
            }
        }
    }

    fun loadMyAnswers() {
        viewModelScope.launch {
            _isLoading.value = true
            _myAnswers.value = repository.myAnswers()
            _isLoading.value = false
        }
    }

    // --- Directory ---------------------------------------------------------

    private val _lawyers = MutableStateFlow<List<DirectoryLawyerResponse>>(emptyList())
    val lawyers: StateFlow<List<DirectoryLawyerResponse>> = _lawyers.asStateFlow()

    private val _selectedLawyer = MutableStateFlow<DirectoryLawyerResponse?>(null)
    val selectedLawyer: StateFlow<DirectoryLawyerResponse?> = _selectedLawyer.asStateFlow()

    private val _myContactRequests = MutableStateFlow<List<MyContactRequestResponse>>(emptyList())
    val myContactRequests: StateFlow<List<MyContactRequestResponse>> =
        _myContactRequests.asStateFlow()

    private val _contactInbox = MutableStateFlow<List<InboxRequestResponse>>(emptyList())
    val contactInbox: StateFlow<List<InboxRequestResponse>> = _contactInbox.asStateFlow()

    // Directory filters, held here so they survive navigation.
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _specializationFilter = MutableStateFlow<String?>(null)
    val specializationFilter: StateFlow<String?> = _specializationFilter.asStateFlow()

    private val _languageFilter = MutableStateFlow<String?>(null)
    val languageFilter: StateFlow<String?> = _languageFilter.asStateFlow()

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setSpecializationFilter(value: String?) {
        _specializationFilter.value = value
        searchLawyers()
    }

    fun setLanguageFilter(value: String?) {
        _languageFilter.value = value
        searchLawyers()
    }

    fun searchLawyers() {
        viewModelScope.launch {
            _isLoading.value = true
            _lawyers.value = repository.findLawyers(
                query = _searchQuery.value.takeIf { it.isNotBlank() },
                specialization = _specializationFilter.value,
                language = _languageFilter.value
            )
            _isLoading.value = false
        }
    }

    fun openLawyer(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedLawyer.value = null
            when (val result = repository.lawyerProfile(id)) {
                is QnaResult.Success -> _selectedLawyer.value = result.value
                is QnaResult.Failure -> _message.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun requestContact(lawyerId: String, message: String?) {
        viewModelScope.launch {
            _isSubmitting.value = true
            when (val result = repository.requestContact(lawyerId, message)) {
                is QnaResult.Success -> {
                    _message.value = when (result.value) {
                        "ACCEPTED" -> "This lawyer has already shared their details with you."
                        else -> "Request sent. You will be notified if they accept."
                    }
                    loadMyContactRequests()
                }
                is QnaResult.Failure -> _message.value = result.message
            }
            _isSubmitting.value = false
        }
    }

    fun loadMyContactRequests() {
        viewModelScope.launch {
            _myContactRequests.value = repository.myContactRequests()
        }
    }

    fun loadContactInbox() {
        viewModelScope.launch {
            _isLoading.value = true
            _contactInbox.value = repository.contactInbox()
            _isLoading.value = false
        }
    }

    fun respondToContact(requestId: String, action: String) {
        viewModelScope.launch {
            when (val result = repository.respondToContact(requestId, action)) {
                is QnaResult.Success -> {
                    _message.value = when (action) {
                        "ACCEPTED" -> "Your contact details have been shared."
                        "DECLINED" -> "Request declined."
                        else -> "This person can no longer contact you."
                    }
                    loadContactInbox()
                }
                is QnaResult.Failure -> _message.value = result.message
            }
        }
    }
}
