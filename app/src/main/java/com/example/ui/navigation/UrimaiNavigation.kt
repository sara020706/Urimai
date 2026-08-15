package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.HowItWorksBottomSheet
import com.example.ui.screens.*
import com.example.viewmodel.UrimaiViewModel

object UrimaiDestinations {
    const val WELCOME = "welcome"
    const val PROFILE_SETUP = "profile_setup"
    const val ANALYZING = "analyzing"
    const val DASHBOARD = "dashboard"
    const val SCHEME_DETAIL = "scheme_detail"
    const val SAVED_SCHEMES = "saved_schemes"
    const val PROFILE_VIEW_EDIT = "profile_view_edit"
}

@Composable
fun UrimaiApp(
    viewModel: UrimaiViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val savedSchemeIds by viewModel.savedSchemeIds.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onboardingStep by viewModel.onboardingStep.collectAsState()
    val aiInputText by viewModel.naturalLanguageInput.collectAsState()
    val isAiExtracting by viewModel.isAiExtracting.collectAsState()
    val extractedPreview by viewModel.extractedProfilePreview.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analyzingStepIndex by viewModel.analyzingStepIndex.collectAsState()
    val filteredMatches by viewModel.filteredMatches.collectAsState()
    val allResults by viewModel.allEvaluationResults.collectAsState()
    val dashboardSummary by viewModel.dashboardSummary.collectAsState()
    val selectedSchemeResult by viewModel.selectedSchemeResult.collectAsState()
    val aiExplanationMap by viewModel.aiExplanationMap.collectAsState()
    val isGeneratingExplanation by viewModel.isGeneratingExplanation.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val showHowItWorks by viewModel.showHowItWorks.collectAsState()

    NavHost(
        navController = navController,
        startDestination = UrimaiDestinations.WELCOME,
        modifier = modifier
    ) {
        composable(UrimaiDestinations.WELCOME) {
            WelcomeScreen(
                currentLanguage = selectedLanguage,
                onLanguageChange = { viewModel.setLanguage(it) },
                onStartEligibilityCheck = {
                    viewModel.setOnboardingStep(1)
                    navController.navigate(UrimaiDestinations.PROFILE_SETUP)
                },
                onExploreHowItWorks = {
                    viewModel.setShowHowItWorks(true)
                },
                onSelectDemoProfile = { demoProfile ->
                    viewModel.loadDemoProfile(demoProfile)
                    viewModel.startAnalysisAnimation {
                        navController.navigate(UrimaiDestinations.DASHBOARD) {
                            popUpTo(UrimaiDestinations.WELCOME)
                        }
                    }
                    navController.navigate(UrimaiDestinations.ANALYZING)
                }
            )
        }

        composable(UrimaiDestinations.PROFILE_SETUP) {
            ProfileSetupScreen(
                currentStep = onboardingStep,
                profile = userProfile,
                onProfileChange = { viewModel.updateProfile(it) },
                onStepChange = { viewModel.setOnboardingStep(it) },
                onPreviousStep = { viewModel.previousOnboardingStep() },
                onNextStep = { viewModel.nextOnboardingStep() },
                onSubmitProfile = {
                    viewModel.startAnalysisAnimation {
                        navController.navigate(UrimaiDestinations.DASHBOARD) {
                            popUpTo(UrimaiDestinations.WELCOME)
                        }
                    }
                    navController.navigate(UrimaiDestinations.ANALYZING)
                },
                aiInputText = aiInputText,
                onAiInputChange = { viewModel.setNaturalLanguageInput(it) },
                onAiExtract = { viewModel.extractProfileFromText() },
                isAiExtracting = isAiExtracting,
                extractedProfilePreview = extractedPreview,
                onApplyExtracted = { viewModel.applyExtractedProfile() },
                onCancelExtracted = { viewModel.cancelExtractedProfile() }
            )
        }

        composable(UrimaiDestinations.ANALYZING) {
            AnalyzingScreen(currentStepIndex = analyzingStepIndex)
        }

        composable(UrimaiDestinations.DASHBOARD) {
            MatchesDashboardScreen(
                summary = dashboardSummary,
                matches = filteredMatches,
                profile = userProfile,
                savedSchemeIds = savedSchemeIds,
                selectedCategory = selectedCategory,
                onCategorySelect = { viewModel.setCategory(it) },
                selectedStatusFilter = selectedStatusFilter,
                onStatusFilterSelect = { viewModel.setStatusFilter(it) },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                currentLanguage = selectedLanguage,
                onLanguageChange = { viewModel.setLanguage(it) },
                onSchemeClick = { schemeId ->
                    viewModel.selectScheme(schemeId)
                    navController.navigate(UrimaiDestinations.SCHEME_DETAIL)
                },
                onToggleSaveScheme = { viewModel.toggleSaveScheme(it) },
                onEditProfile = {
                    navController.navigate(UrimaiDestinations.PROFILE_VIEW_EDIT)
                },
                onViewSavedSchemes = {
                    navController.navigate(UrimaiDestinations.SAVED_SCHEMES)
                },
                onExploreHowItWorks = {
                    viewModel.setShowHowItWorks(true)
                }
            )
        }

        composable(UrimaiDestinations.SCHEME_DETAIL) {
            val schemeResult = selectedSchemeResult
            if (schemeResult != null) {
                SchemeDetailScreen(
                    matchResult = schemeResult,
                    profile = userProfile,
                    isSaved = savedSchemeIds.contains(schemeResult.scheme.id),
                    onSaveToggle = { viewModel.toggleSaveScheme(schemeResult.scheme.id) },
                    onBack = { navController.popBackStack() },
                    currentLanguage = selectedLanguage,
                    onLanguageChange = {
                        viewModel.setLanguage(it)
                        viewModel.loadSchemeExplanation(schemeResult.scheme.id, forceReload = true)
                    },
                    aiExplanation = aiExplanationMap[schemeResult.scheme.id],
                    isLoadingExplanation = isGeneratingExplanation,
                    onReloadExplanation = { viewModel.loadSchemeExplanation(schemeResult.scheme.id, forceReload = true) },
                    onToggleDocument = { viewModel.toggleDocumentOwned(it) },
                    onNavigateToEditProfile = { navController.navigate(UrimaiDestinations.PROFILE_VIEW_EDIT) },
                    chatMessages = chatMessages,
                    isChatLoading = isChatLoading,
                    onSendChatMessage = { viewModel.sendChatMessage(it) }
                )
            }
        }

        composable(UrimaiDestinations.SAVED_SCHEMES) {
            val savedMatches = allResults.filter { savedSchemeIds.contains(it.scheme.id) }
            SavedSchemesScreen(
                savedResults = savedMatches,
                savedSchemeIds = savedSchemeIds,
                profile = userProfile,
                onToggleSave = { viewModel.toggleSaveScheme(it) },
                onSchemeClick = { schemeId ->
                    viewModel.selectScheme(schemeId)
                    navController.navigate(UrimaiDestinations.SCHEME_DETAIL)
                },
                onBack = { navController.popBackStack() },
                language = selectedLanguage
            )
        }

        composable(UrimaiDestinations.PROFILE_VIEW_EDIT) {
            ProfileViewEditScreen(
                profile = userProfile,
                onProfileChange = { viewModel.updateProfile(it) },
                onSaveAndRecalculate = {
                    viewModel.startAnalysisAnimation {
                        navController.navigate(UrimaiDestinations.DASHBOARD) {
                            popUpTo(UrimaiDestinations.DASHBOARD) { inclusive = true }
                        }
                    }
                    navController.navigate(UrimaiDestinations.ANALYZING)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showHowItWorks) {
        HowItWorksBottomSheet(
            onDismiss = { viewModel.setShowHowItWorks(false) }
        )
    }
}
