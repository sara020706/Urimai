package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.UrimaiAiService
import com.example.data.model.*
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

class UrimaiViewModel : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile.DEMO_ARUN_STUDENT)
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _savedSchemeIds = MutableStateFlow<Set<String>>(setOf("sch_vidya_lakshmi"))
    val savedSchemeIds: StateFlow<Set<String>> = _savedSchemeIds.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SchemeCategory.ALL)
    val selectedCategory: StateFlow<SchemeCategory> = _selectedCategory.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<EligibilityStatus?>(null)
    val selectedStatusFilter: StateFlow<EligibilityStatus?> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSchemeId = MutableStateFlow<String?>("sch_vidya_lakshmi")
    val selectedSchemeId: StateFlow<String?> = _selectedSchemeId.asStateFlow()

    // Onboarding & AI extraction state
    private val _onboardingStep = MutableStateFlow(1)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

    private val _naturalLanguageInput = MutableStateFlow("")
    val naturalLanguageInput: StateFlow<String> = _naturalLanguageInput.asStateFlow()

    private val _isAiExtracting = MutableStateFlow(false)
    val isAiExtracting: StateFlow<Boolean> = _isAiExtracting.asStateFlow()

    private val _extractedProfilePreview = MutableStateFlow<UserProfile?>(null)
    val extractedProfilePreview: StateFlow<UserProfile?> = _extractedProfilePreview.asStateFlow()

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EligibilityEngine.evaluateAll(UserProfile.DEMO_ARUN_STUDENT, SchemeRepository.allSchemes))

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

    fun loadDemoProfile(profile: UserProfile) {
        _userProfile.value = profile
        _extractedProfilePreview.value = null
    }

    fun setOnboardingStep(step: Int) {
        _onboardingStep.value = step.coerceIn(1, 4)
    }

    fun nextOnboardingStep() {
        if (_onboardingStep.value < 4) {
            _onboardingStep.value += 1
        }
    }

    fun previousOnboardingStep() {
        if (_onboardingStep.value > 1) {
            _onboardingStep.value -= 1
        }
    }

    fun setNaturalLanguageInput(text: String) {
        _naturalLanguageInput.value = text
    }

    fun extractProfileFromText() {
        val input = _naturalLanguageInput.value.trim()
        if (input.isBlank()) return

        viewModelScope.launch {
            _isAiExtracting.value = true
            val extracted = UrimaiAiService.extractProfileFromNaturalLanguage(input)
            _extractedProfilePreview.value = extracted
            _isAiExtracting.value = false
        }
    }

    fun applyExtractedProfile() {
        val extracted = _extractedProfilePreview.value ?: return
        _userProfile.value = extracted
        _extractedProfilePreview.value = null
        _naturalLanguageInput.value = ""
    }

    fun cancelExtractedProfile() {
        _extractedProfilePreview.value = null
    }

    fun toggleDocumentOwned(docName: String) {
        val current = _userProfile.value.ownedDocuments.toMutableSet()
        if (current.contains(docName)) {
            current.remove(docName)
        } else {
            current.add(docName)
        }
        _userProfile.value = _userProfile.value.copy(ownedDocuments = current)
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
