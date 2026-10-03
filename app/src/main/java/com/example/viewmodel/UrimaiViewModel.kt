package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.UrimaiAiService
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.DocumentRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.SavedSchemesRepository
import com.example.data.repository.CatalogSource
import com.example.data.repository.SchemeCatalogRepository
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
    private val profileRepository = ProfileRepository(application)
    private val savedSchemesRepository = SavedSchemesRepository(application)
    private val schemeCatalogRepository = SchemeCatalogRepository(application)

    init {
        // The AI service reaches the backend through this; without it every AI
        // call takes its deterministic path, which is a supported mode.
        UrimaiAiService.initialize(application)
    }

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    /**
     * Whether to offer lawyer entry points.
     *
     * UI gating only: the server re-checks the role and verification status
     * on every request, so this cannot grant access, only hide a button that
     * would be refused anyway.
     */
    private val _isLawyerRole = MutableStateFlow(false)
    val isLawyerRole: StateFlow<Boolean> = _isLawyerRole.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isVerifiedLawyer = MutableStateFlow(false)
    val isVerifiedLawyer: StateFlow<Boolean> = _isVerifiedLawyer.asStateFlow()

    private val _uploadedDocuments = MutableStateFlow<List<UploadedDocumentEntity>>(emptyList())
    val uploadedDocuments: StateFlow<List<UploadedDocumentEntity>> = _uploadedDocuments.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = authRepository.restoreSession()
            if (restored is AuthResult.Success) {
                _authState.value = AuthUiState(
                    isLoggedIn = true,
                    userId = restored.userId,
                    displayName = restored.displayName
                )
                loadRemoteData()
            }
        }
    }

    /**
     * Fetch the catalog from the backend, falling back to cache then to the
     * bundled copy. Safe to call at any time; never leaves the list empty.
     */
    fun refreshSchemeCatalog() {
        viewModelScope.launch {
            val result = schemeCatalogRepository.loadSchemes()
            _schemes.value = result.schemes
            _catalogSource.value = result.source
        }
    }

    private fun refreshRoleFlag() {
        viewModelScope.launch {
            val session = com.example.data.remote.SessionManager(getApplication())
            val role = session.currentRole()
            _isAdmin.value = role == "ADMIN"
            _isVerifiedLawyer.value =
                role == "LAWYER" && session.currentLawyerStatus() == "VERIFIED"
            // A lawyer who is not yet approved still needs the
            // verification screen, so surface that separately.
            _isLawyerRole.value = role == "LAWYER"
        }
    }

    private fun loadRemoteData() {
        refreshRoleFlag()
        viewModelScope.launch {
            profileRepository.getProfile()?.let { _userProfile.value = it }
        }
        viewModelScope.launch {
            _uploadedDocuments.value = documentRepository.listDocuments()
        }
        viewModelScope.launch {
            _savedSchemeIds.value = savedSchemesRepository.getSavedSchemeIds()
        }
    }

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
                    loadRemoteData()
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
                    loadRemoteData()
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
        viewModelScope.launch { authRepository.logOut() }
        _authState.value = AuthUiState()
        _userProfile.value = UserProfile()
        _uploadedDocuments.value = emptyList()
        _savedSchemeIds.value = emptySet()
    }

    fun uploadDocument(documentName: String, fileUri: String, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            val uploaded = documentRepository.uploadDocument(documentName, fileUri, fileName, mimeType)
            if (uploaded != null) {
                _uploadedDocuments.value = _uploadedDocuments.value + uploaded
                toggleDocumentOwnedIfMissing(documentName)
                persistProfile()
            }
        }
    }

    fun removeUploadedDocument(document: UploadedDocumentEntity) {
        viewModelScope.launch {
            documentRepository.removeUpload(document)
            _uploadedDocuments.value = _uploadedDocuments.value.filterNot { it.id == document.id }
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

    // The scheme catalog. Starts as the catalog bundled in the APK so the app is
    // usable immediately and offline, then swaps to the backend copy once it
    // loads. Because results derive from this flow, that swap re-evaluates
    // everything automatically — there is no cache to invalidate.
    private val _schemes = MutableStateFlow(SchemeRepository.allSchemes)
    val schemes: StateFlow<List<Scheme>> = _schemes.asStateFlow()

    private val _catalogSource = MutableStateFlow(CatalogSource.BUNDLED)
    val catalogSource: StateFlow<CatalogSource> = _catalogSource.asStateFlow()

    // Declared after _schemes/_catalogSource on purpose: Kotlin runs property
    // initializers and init blocks in declaration order, so refreshing any
    // earlier would write to a not-yet-initialized flow.
    init {
        refreshSchemeCatalog()
    }

    // Computed: Scheme Evaluation Results recalculated automatically whenever
    // the profile OR the catalog changes.
    val allEvaluationResults: StateFlow<List<SchemeMatchResult>> =
        combine(_userProfile, _schemes) { profile, schemes ->
            EligibilityEngine.evaluateAll(profile, schemes)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            EligibilityEngine.evaluateAll(UserProfile(), SchemeRepository.allSchemes)
        )

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
        val nowSaved: Boolean
        if (current.contains(schemeId)) {
            current.remove(schemeId)
            nowSaved = false
        } else {
            current.add(schemeId)
            nowSaved = true
        }
        _savedSchemeIds.value = current
        viewModelScope.launch {
            if (nowSaved) savedSchemesRepository.saveScheme(schemeId) else savedSchemesRepository.unsaveScheme(schemeId)
        }
    }

    fun updateProfile(newProfile: UserProfile) {
        _userProfile.value = newProfile
        persistProfile()
    }

    private fun persistProfile() {
        viewModelScope.launch {
            profileRepository.updateProfile(_userProfile.value)
        }
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
        val scheme = _schemes.value.firstOrNull { it.id == schemeId } ?: return
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
