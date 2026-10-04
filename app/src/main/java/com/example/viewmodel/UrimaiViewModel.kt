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

/**
 * One message in the general legal chat.
 *
 * Kept separate from ChatMessage, which the per-scheme chat uses: this one
 * carries an error code so a failed turn can be retried in place instead of
 * leaving a dead end in the transcript.
 */
data class LegalChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val error: String? = null,
    val id: String = java.util.UUID.randomUUID().toString()
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

    // --- Google sign-in ---------------------------------------------------

    private val _googleSignInAvailable = MutableStateFlow(false)
    val googleSignInAvailable: StateFlow<Boolean> = _googleSignInAvailable.asStateFlow()

    init {
        // Asked once at startup so the login screen can decide whether to show
        // the button. Defaults to false, so a failure here hides it.
        viewModelScope.launch {
            _googleSignInAvailable.value = authRepository.googleSignInAvailable()
        }
    }

    /**
     * @param activityContext must be an Activity context -- Credential Manager
     *        needs one to present its sheet.
     */
    fun signInWithGoogle(activityContext: android.content.Context, role: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signInWithGoogle(activityContext, role)) {
                // Null means the person dismissed the sheet. Clear the spinner
                // and say nothing: they know what they did.
                null -> _authState.value = _authState.value.copy(isLoading = false)
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

    fun signUp(
        username: String,
        password: String,
        displayName: String,
        role: String? = null
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signUp(username, password, displayName, role)) {
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

    // --- Certificate reading (OCR) -----------------------------------------
    //
    // The result is advisory. It is held here for the review screen and never
    // written to the profile until the citizen confirms it field by field.

    private val aiRepository = com.example.data.repository.AiRepository(application)

    private val _extraction =
        MutableStateFlow<com.example.data.remote.ExtractProfileResponse?>(null)
    val extraction: StateFlow<com.example.data.remote.ExtractProfileResponse?> =
        _extraction.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractionError = MutableStateFlow<String?>(null)
    val extractionError: StateFlow<String?> = _extractionError.asStateFlow()

    fun clearExtraction() {
        _extraction.value = null
        _extractionError.value = null
    }

    // --- General legal chat -----------------------------------------------
    //
    // Separate from asking a verified lawyer, which still goes through
    // POST /questions and the answer-isolation path. This is general legal
    // information from the model, and the screen says so.

    private val _legalChat = MutableStateFlow<List<LegalChatMessage>>(emptyList())
    val legalChat: StateFlow<List<LegalChatMessage>> = _legalChat.asStateFlow()

    private val _isLegalChatReplying = MutableStateFlow(false)
    val isLegalChatReplying: StateFlow<Boolean> = _isLegalChatReplying.asStateFlow()

    fun sendLegalChatMessage(text: String) {
        val question = text.trim()
        if (question.isBlank() || _isLegalChatReplying.value) return

        // History is what preceded this question, so it is captured before the
        // new message is appended.
        val history = _legalChat.value
            .filter { it.error == null }
            .map {
                com.example.data.remote.LegalChatTurn(
                    role = if (it.isFromUser) "user" else "assistant",
                    text = it.text
                )
            }

        _legalChat.value = _legalChat.value + LegalChatMessage(question, isFromUser = true)
        _isLegalChatReplying.value = true

        viewModelScope.launch {
            val outcome = aiRepository.legalChat(
                question = question,
                history = history,
                language = _selectedLanguage.value.code
            )
            val reply = when (outcome) {
                is com.example.data.repository.LegalChatOutcome.Success ->
                    LegalChatMessage(outcome.answer, isFromUser = false)
                com.example.data.repository.LegalChatOutcome.RateLimited ->
                    LegalChatMessage(
                        "You have asked a lot of questions in a short time. " +
                            "Please wait a little and try again.",
                        isFromUser = false,
                        error = "RATE_LIMITED"
                    )
                com.example.data.repository.LegalChatOutcome.Offline ->
                    LegalChatMessage(
                        "We could not reach the assistant. Check your connection " +
                            "and try again.",
                        isFromUser = false,
                        error = "OFFLINE"
                    )
                is com.example.data.repository.LegalChatOutcome.Rejected ->
                    LegalChatMessage(outcome.message, isFromUser = false, error = "REJECTED")
                com.example.data.repository.LegalChatOutcome.Unavailable ->
                    LegalChatMessage(
                        "The legal assistant is unavailable right now. You can " +
                            "still ask verified lawyers your question anonymously.",
                        isFromUser = false,
                        error = "UNAVAILABLE"
                    )
            }
            _legalChat.value = _legalChat.value + reply
            _isLegalChatReplying.value = false
        }
    }

    /** Drop the last failed exchange and ask again. */
    fun retryLastLegalChatMessage() {
        val messages = _legalChat.value
        val lastUser = messages.lastOrNull { it.isFromUser } ?: return
        // Remove the failed reply and the question, so resending does not
        // duplicate the question in the transcript.
        val trimmed = messages.dropLastWhile { !it.isFromUser }.dropLast(1)
        _legalChat.value = trimmed
        sendLegalChatMessage(lastUser.text)
    }

    fun clearLegalChat() {
        _legalChat.value = emptyList()
    }

    // --- Viewing an uploaded document -------------------------------------
    // Reuses OpenDocument so the existing DocumentViewerScreen renders both the
    // admin's verification documents and a citizen's own certificates.

    private val _openDocument = MutableStateFlow<OpenDocument?>(null)
    val openDocument: StateFlow<OpenDocument?> = _openDocument.asStateFlow()

    private val _isDownloadingDocument = MutableStateFlow(false)
    val isDownloadingDocument: StateFlow<Boolean> = _isDownloadingDocument.asStateFlow()

    fun viewOwnDocument(document: com.example.data.model.UploadedDocumentEntity) {
        viewModelScope.launch {
            _isDownloadingDocument.value = true
            _openDocument.value = null
            val file = documentRepository.downloadOwnDocument(document.id, document.fileName)
            if (file == null) {
                // Surfaced by the same snackbar that reports extraction failures.
                _extractionError.value = "We could not open that document."
            } else {
                _openDocument.value = OpenDocument(
                    file = file,
                    meta = com.example.data.remote.VerificationDocumentResponse(
                        id = document.id,
                        docType = document.documentName,
                        fileName = document.fileName,
                        mimeType = document.mimeType,
                        byteSize = null,
                        uploadedAt = document.uploadedAt
                    )
                )
            }
            _isDownloadingDocument.value = false
        }
    }

    fun closeOwnDocument() {
        _openDocument.value = null
    }

    /** Read a certificate the citizen has already uploaded. */
    fun extractFromDocument(documentId: String) {
        viewModelScope.launch {
            _isExtracting.value = true
            _extraction.value = null
            _extractionError.value = null
            handleExtraction(aiRepository.extractFromUploadedDocument(documentId))
            _isExtracting.value = false
        }
    }

    /** Read a certificate the citizen just picked, without storing it. */
    fun extractFromFile(fileUri: String, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            _isExtracting.value = true
            _extraction.value = null
            _extractionError.value = null
            handleExtraction(aiRepository.extractFromFile(fileUri, fileName, mimeType))
            _isExtracting.value = false
        }
    }

    private fun handleExtraction(outcome: com.example.data.repository.ExtractionOutcome) {
        when (outcome) {
            is com.example.data.repository.ExtractionOutcome.Success ->
                _extraction.value = outcome.response
            // Distinguished so the message is actionable: "try a clearer photo"
            // is something the citizen can act on; "AI is unavailable" is not.
            com.example.data.repository.ExtractionOutcome.Unreadable ->
                _extractionError.value =
                    "We could not read that document. Try a clearer photo."
            com.example.data.repository.ExtractionOutcome.UnsupportedType ->
                _extractionError.value = "Use a JPEG, PNG or PDF."
            com.example.data.repository.ExtractionOutcome.TooLarge ->
                _extractionError.value = "That file is too large. The limit is 10 MB."
            com.example.data.repository.ExtractionOutcome.RateLimited ->
                _extractionError.value = "Too many attempts. Try again later."
            com.example.data.repository.ExtractionOutcome.Unavailable ->
                _extractionError.value =
                    "Automatic reading is unavailable. You can enter details by hand."
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
