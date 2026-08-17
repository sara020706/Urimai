package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.HowItWorksBottomSheet
import com.example.ui.screens.*
import com.example.viewmodel.UrimaiViewModel

object UrimaiDestinations {
    const val LOGIN = "login"
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
    val authState by viewModel.authState.collectAsState()
    val uploadedDocuments by viewModel.uploadedDocuments.collectAsState()

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(UrimaiDestinations.ANALYZING) {
                popUpTo(UrimaiDestinations.LOGIN) { inclusive = true }
            }
            viewModel.startAnalysisAnimation {
                navController.navigate(UrimaiDestinations.DASHBOARD) {
                    popUpTo(UrimaiDestinations.ANALYZING) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.isLoggedIn) UrimaiDestinations.ANALYZING else UrimaiDestinations.LOGIN,
        modifier = modifier
    ) {
        composable(UrimaiDestinations.LOGIN) {
            var mode by remember { mutableStateOf(AuthMode.LOGIN) }
            AuthScreen(
                mode = mode,
                onModeChange = {
                    mode = it
                    viewModel.clearAuthError()
                },
                errorMessage = authState.errorMessage,
                isLoading = authState.isLoading,
                onLogin = { username, password -> viewModel.logIn(username, password) },
                onSignUp = { username, password, displayName -> viewModel.signUp(username, password, displayName) }
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
                    uploadedDocuments = uploadedDocuments,
                    onUploadDocument = { documentName, fileUri, fileName, mimeType ->
                        viewModel.uploadDocument(documentName, fileUri, fileName, mimeType)
                    },
                    onRemoveUpload = { viewModel.removeUploadedDocument(it) },
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
                onBack = { navController.popBackStack() },
                onLogOut = {
                    viewModel.logOut()
                    navController.navigate(UrimaiDestinations.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }
    }

    if (showHowItWorks) {
        HowItWorksBottomSheet(
            onDismiss = { viewModel.setShowHowItWorks(false) }
        )
    }
}
