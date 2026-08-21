package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.UrimaiAiService
import com.example.data.local.UploadedDocumentEntity
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.DocumentRepository
import com.example.data.repository.SchemeRepository
import com.example.engine.EligibilityEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "urimai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val userId: Long? = null,
    val displayName: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class UrimaiViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val documentRepository = DocumentRepository(application)

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    val uploadedDocuments: StateFlow<List<UploadedDocumentEntity>> = _authState
        .map { it.userId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList()) else documentRepository.observeForUser(userId)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun logIn(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.logIn(username, password)) {
                is AuthResult.Success -> {
                    _authState.value = AuthUiState(
                        isLoggedIn = true,
                        userId = result.userId,
                        displayName = result.displayName
                    )
                    _userProfile.value = UserProfile(name = result.displayName)
                }
                is AuthResult.Failure -> _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun signUp(username: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signUp(username, password, displayName)) {
                is AuthResult.Success -> {
                    _authState.value = AuthUiState(
                        isLoggedIn = true,
                        userId = result.userId,
                        displayName = result.displayName
                    )
                    _userProfile.value = UserProfile(name = result.displayName)
                }
                is AuthResult.Failure -> _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun clearAuthError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }

    fun logOut() {
        _authState.value = AuthUiState()
    }

    fun uploadDocument(documentName: String, fileUri: String, fileName: String, mimeType: String?) {
        val userId = _authState.value.userId ?: return
        viewModelScope.launch {
            documentRepository.saveUpload(userId, documentName, fileUri, fileName, mimeType)
        }
        toggleDocumentOwnedIfMissing(documentName)
    }

    fun removeUploadedDocument(document: UploadedDocumentEntity) {
        viewModelScope.launch {
            documentRepository.removeUpload(document)
        }
    }

    private fun toggleDocumentOwnedIfMissing(documentName: String) {
        val current = _userProfile.value.ownedDocuments
        if (current.none { it.equals(documentName, ignoreCase = true) }) {
            _userProfile.value = _userProfile.value.copy(ownedDocuments = current + documentName)
        }
    }

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _savedSchemeIds = MutableStateFlow<Set<String>>(emptySet())
    val savedSchemeIds: StateFlow<Set<String>> = _savedSchemeIds.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SchemeCategory.ALL)
    val selectedCategory: StateFlow<SchemeCategory> = _selectedCategory.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<EligibilityStatus?>(null)
    val selectedStatusFilter: StateFlow<EligibilityStatus?> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSchemeId = MutableStateFlow<String?>(null)
    val selectedSchemeId: StateFlow<String?> = _selectedSchemeId.asStateFlow()

    // Analyzing animation state
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analyzingStepIndex = MutableStateFlow(0)
    val analyzingStepIndex: StateFlow<Int> = _analyzingStepIndex.asStateFlow()

    // AI Explanations & Q&A
    private val _aiExplanationMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val aiExplanationMap: StateFlow<Map<String, String>> = _aiExplanationMap.asStateFlow()

    private val _isGeneratingExplanation = MutableStateFlow(false)
    val isGeneratingExplanation: StateFlow<Boolean> = _isGeneratingExplanation.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _showHowItWorks = MutableStateFlow(false)
    val showHowItWorks: StateFlow<Boolean> = _showHowItWorks.asStateFlow()

    // Computed: Scheme Evaluation Results recalculated automatically whenever profile changes!
    val allEvaluationResults: StateFlow<List<SchemeMatchResult>> = _userProfile.map { profile ->
        EligibilityEngine.evaluateAll(profile, SchemeRepository.allSchemes)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EligibilityEngine.evaluateAll(UserProfile(), SchemeRepository.allSchemes))

    // Filtered matches for the dashboard
    val filteredMatches: StateFlow<List<SchemeMatchResult>> = combine(
        allEvaluationResults,
        _selectedCategory,
        _selectedStatusFilter,
        _searchQuery
    ) { results, category, statusFilter, query ->
        results.filter { item ->
            val matchCategory = category == SchemeCategory.ALL || item.scheme.category == category
            val matchStatus = statusFilter == null || item.status == statusFilter
            val matchQuery = query.isBlank() ||
                    item.scheme.name.contains(query, ignoreCase = true) ||
                    item.scheme.shortName.contains(query, ignoreCase = true) ||
                    item.scheme.tamilName.contains(query, ignoreCase = true) ||
                    item.scheme.hindiName.contains(query, ignoreCase = true) ||
                    item.scheme.department.contains(query, ignoreCase = true)
            matchCategory && matchStatus && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> = allEvaluationResults.map {
        EligibilityEngine.computeDashboardSummary(it)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EligibilityEngine.computeDashboardSummary(emptyList()))

    val selectedSchemeResult: StateFlow<SchemeMatchResult?> = combine(
        allEvaluationResults,
        _selectedSchemeId
    ) { results, id ->
        results.firstOrNull { it.scheme.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setLanguage(lang: AppLanguage) {
        _selectedLanguage.value = lang
    }

    fun setCategory(category: SchemeCategory) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: EligibilityStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectScheme(schemeId: String) {
        _selectedSchemeId.value = schemeId
        loadSchemeExplanation(schemeId)
        initChatForScheme(schemeId)
    }

    fun toggleSaveScheme(schemeId: String) {
        val current = _savedSchemeIds.value.toMutableSet()
        if (current.contains(schemeId)) {
            current.remove(schemeId)
        } else {
            current.add(schemeId)
        }
        _savedSchemeIds.value = current
    }

    fun updateProfile(newProfile: UserProfile) {
        _userProfile.value = newProfile
    }


    fun startAnalysisAnimation(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analyzingStepIndex.value = 0
            val stepsCount = 5
            for (i in 0 until stepsCount) {
                _analyzingStepIndex.value = i
                delay(450)
            }
            delay(300)
            _isAnalyzing.value = false
            onComplete()
        }
    }

    fun loadSchemeExplanation(schemeId: String, forceReload: Boolean = false) {
        val result = allEvaluationResults.value.firstOrNull { it.scheme.id == schemeId } ?: return
        if (!forceReload && _aiExplanationMap.value.containsKey(schemeId)) return

        viewModelScope.launch {
            _isGeneratingExplanation.value = true
            val explanation = UrimaiAiService.generateSchemeExplanation(
                matchResult = result,
                profile = _userProfile.value,
                language = _selectedLanguage.value
            )
            _aiExplanationMap.value = _aiExplanationMap.value + (schemeId to explanation)
            _isGeneratingExplanation.value = false
        }
    }

    private fun initChatForScheme(schemeId: String) {
        val scheme = SchemeRepository.allSchemes.firstOrNull { it.id == schemeId } ?: return
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "urimai",
                text = "Vanakkam / Namaste! I am Urimai AI Assistant. Ask me anything about **${scheme.shortName}**, its eligibility criteria, benefits, or required documents."
            )
        )
    }

    fun sendChatMessage(question: String) {
        if (question.isBlank()) return
        val currentScheme = selectedSchemeResult.value ?: return

        val userMsg = ChatMessage(sender = "user", text = question)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatLoading.value = true
            val answer = UrimaiAiService.answerCitizenQuestion(
                question = question,
                matchResult = currentScheme,
                profile = _userProfile.value,
                language = _selectedLanguage.value
            )
            val aiMsg = ChatMessage(sender = "urimai", text = answer)
            _chatMessages.value = _chatMessages.value + aiMsg
            _isChatLoading.value = false
        }
    }

    fun setShowHowItWorks(show: Boolean) {
        _showHowItWorks.value = show
    }
}
