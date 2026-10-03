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
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.QnaViewModel
import com.example.viewmodel.UrimaiViewModel

object UrimaiDestinations {
    const val LOGIN = "login"
    const val ANALYZING = "analyzing"
    const val DASHBOARD = "dashboard"
    const val SCHEME_DETAIL = "scheme_detail"
    const val SAVED_SCHEMES = "saved_schemes"
    const val PROFILE_VIEW_EDIT = "profile_view_edit"

    // Legal help (citizen)
    const val MY_QUESTIONS = "my_questions"
    const val ASK_QUESTION = "ask_question"
    const val QUESTION_DETAIL = "question_detail"

    // Lawyer directory (citizen)
    const val FIND_LAWYER = "find_lawyer"
    const val LAWYER_PROFILE = "lawyer_profile"
    const val MY_CONTACT_REQUESTS = "my_contact_requests"

    // Lawyer workspace
    const val LAWYER_FEED = "lawyer_feed"
    const val ANSWER_QUESTION = "answer_question"
    const val LAWYER_VERIFICATION = "lawyer_verification"

    // Shared
    const val NOTIFICATIONS = "notifications"

    // Administration
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_LAWYER_QUEUE = "admin_lawyer_queue"
    const val ADMIN_LAWYER_APPLICATION = "admin_lawyer_application"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_REPORTS = "admin_reports"
    const val ADMIN_AUDIT_LOG = "admin_audit_log"
}

@Composable
fun UrimaiApp(
    viewModel: UrimaiViewModel,
    qnaViewModel: QnaViewModel,
    adminViewModel: AdminViewModel,
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

    val myQuestions by qnaViewModel.myQuestions.collectAsState()
    val selectedQuestion by qnaViewModel.selectedQuestion.collectAsState()
    val lawyerFeed by qnaViewModel.lawyerFeed.collectAsState()
    val lawyerQuestion by qnaViewModel.lawyerQuestion.collectAsState()
    val directoryLawyers by qnaViewModel.lawyers.collectAsState()
    val selectedLawyer by qnaViewModel.selectedLawyer.collectAsState()
    val myContactRequests by qnaViewModel.myContactRequests.collectAsState()
    val qnaLoading by qnaViewModel.isLoading.collectAsState()
    val qnaSubmitting by qnaViewModel.isSubmitting.collectAsState()
    val qnaSpecialization by qnaViewModel.specializationFilter.collectAsState()
    val qnaLanguage by qnaViewModel.languageFilter.collectAsState()
    val qnaSearch by qnaViewModel.searchQuery.collectAsState()

    // UI gating only. Every lawyer route is enforced server-side; this
    // merely avoids showing an entry point that would be refused.
    val isVerifiedLawyer by viewModel.isVerifiedLawyer.collectAsState()
    val isLawyerRole by viewModel.isLawyerRole.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    val adminNotifications by adminViewModel.notifications.collectAsState()
    val unreadCount by adminViewModel.unreadCount.collectAsState()
    val myLawyerProfile by adminViewModel.myLawyerProfile.collectAsState()
    val myVerificationDocs by adminViewModel.myVerificationDocs.collectAsState()
    val adminUsers by adminViewModel.users.collectAsState()
    val adminUserQuery by adminViewModel.userQuery.collectAsState()
    val adminLawyerQueue by adminViewModel.pendingLawyers.collectAsState()
    val adminSelectedLawyer by adminViewModel.selectedLawyer.collectAsState()
    val adminSelectedLawyerDocs by adminViewModel.selectedLawyerDocs.collectAsState()
    val adminReports by adminViewModel.reports.collectAsState()
    val adminReportContent by adminViewModel.reportContent.collectAsState()
    val adminAuditLog by adminViewModel.auditLog.collectAsState()
    val adminLoading by adminViewModel.isLoading.collectAsState()
    val adminSubmitting by adminViewModel.isSubmitting.collectAsState()

    // Verification-document picker.
    //
    // The doc type is held in state because ActivityResultContracts.OpenDocument
    // cannot carry it through the system file picker; it is read back when the
    // result arrives.
    val pickerContext = androidx.compose.ui.platform.LocalContext.current
    var pendingDocumentType by remember { mutableStateOf<String?>(null) }

    val verificationPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        val docType = pendingDocumentType
        pendingDocumentType = null
        if (uri != null && docType != null) {
            try {
                pickerContext.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not support persistable permissions; the URI
                // still works for this session, so proceed without persisting it.
            }
            var fileName = uri.lastPathSegment ?: "document"
            pickerContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex =
                    cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            adminViewModel.uploadVerificationDocument(
                docType,
                uri.toString(),
                fileName,
                pickerContext.contentResolver.getType(uri)
            )
        }
    }

    val onPickVerificationDocument: (String) -> Unit = { docType ->
        pendingDocumentType = docType
        // Matches the server's allowlist; anything else is refused with a 415.
        verificationPickerLauncher.launch(
            arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")
        )
    }

    // Poll the unread count only while signed in; an anonymous visitor has no
    // notifications and should not be generating requests.
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) adminViewModel.startNotificationPolling()
    }

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
                },
                onCheckEligibility = {
                    navController.navigate(UrimaiDestinations.PROFILE_VIEW_EDIT)
                },
                onAskLegalQuestion = {
                    navController.navigate(UrimaiDestinations.MY_QUESTIONS)
                },
                onFindLawyer = {
                    navController.navigate(UrimaiDestinations.FIND_LAWYER)
                },
                isVerifiedLawyer = isVerifiedLawyer,
                onOpenLawyerWorkspace = {
                    navController.navigate(UrimaiDestinations.LAWYER_FEED)
                },
                isLawyerRole = isLawyerRole,
                isAdmin = isAdmin,
                unreadNotificationCount = unreadCount,
                onOpenNotifications = {
                    navController.navigate(UrimaiDestinations.NOTIFICATIONS)
                },
                onOpenVerification = {
                    navController.navigate(UrimaiDestinations.LAWYER_VERIFICATION)
                },
                onOpenAdmin = {
                    navController.navigate(UrimaiDestinations.ADMIN_DASHBOARD)
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
                    uploadedDocuments = uploadedDocuments,
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

        composable(UrimaiDestinations.MY_QUESTIONS) {
            LaunchedEffect(Unit) { qnaViewModel.loadMyQuestions() }
            MyQuestionsScreen(
                questions = myQuestions,
                isLoading = qnaLoading,
                onOpenQuestion = { id ->
                    qnaViewModel.openQuestion(id)
                    navController.navigate(UrimaiDestinations.QUESTION_DETAIL)
                },
                onAskNew = { navController.navigate(UrimaiDestinations.ASK_QUESTION) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ASK_QUESTION) {
            AskQuestionScreen(
                isSubmitting = qnaSubmitting,
                defaultState = userProfile.state,
                onSubmit = { title, body, category, state ->
                    qnaViewModel.askQuestion(
                        title, body, category, state, selectedLanguage.code
                    ) { navController.popBackStack() }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.QUESTION_DETAIL) {
            QuestionDetailScreen(
                detail = selectedQuestion,
                isLoading = qnaLoading,
                onClose = { qnaViewModel.closeQuestion(it) },
                onReportAnswer = { answerId, reason ->
                    qnaViewModel.report("ANSWER", answerId, reason)
                },
                onContactLawyer = { lawyerId ->
                    qnaViewModel.openLawyer(lawyerId)
                    navController.navigate(UrimaiDestinations.LAWYER_PROFILE)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.FIND_LAWYER) {
            LaunchedEffect(Unit) { qnaViewModel.searchLawyers() }
            FindLawyerScreen(
                lawyers = directoryLawyers,
                isLoading = qnaLoading,
                searchQuery = qnaSearch,
                specializationFilter = qnaSpecialization,
                languageFilter = qnaLanguage,
                onSearchQueryChange = { qnaViewModel.setSearchQuery(it) },
                onSearch = { qnaViewModel.searchLawyers() },
                onSpecializationChange = { qnaViewModel.setSpecializationFilter(it) },
                onLanguageChange = { qnaViewModel.setLanguageFilter(it) },
                onOpenLawyer = { id ->
                    qnaViewModel.openLawyer(id)
                    navController.navigate(UrimaiDestinations.LAWYER_PROFILE)
                },
                onViewMyRequests = {
                    qnaViewModel.loadMyContactRequests()
                    navController.navigate(UrimaiDestinations.MY_CONTACT_REQUESTS)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.LAWYER_PROFILE) {
            LawyerProfileScreen(
                lawyer = selectedLawyer,
                isLoading = qnaLoading,
                isSubmitting = qnaSubmitting,
                onRequestContact = { id, message -> qnaViewModel.requestContact(id, message) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.MY_CONTACT_REQUESTS) {
            LaunchedEffect(Unit) { qnaViewModel.loadMyContactRequests() }
            MyContactRequestsScreen(
                requests = myContactRequests,
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.LAWYER_FEED) {
            LaunchedEffect(Unit) { qnaViewModel.loadLawyerFeed() }
            LawyerQuestionFeedScreen(
                questions = lawyerFeed,
                isLoading = qnaLoading,
                onOpenQuestion = { id ->
                    qnaViewModel.openLawyerQuestion(id)
                    navController.navigate(UrimaiDestinations.ANSWER_QUESTION)
                },
                onViewMyAnswers = { qnaViewModel.loadMyAnswers() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ANSWER_QUESTION) {
            AnswerQuestionScreen(
                detail = lawyerQuestion,
                isLoading = qnaLoading,
                isSubmitting = qnaSubmitting,
                onSubmit = { questionId, existingId, body ->
                    qnaViewModel.submitAnswer(questionId, existingId, body)
                },
                onWithdraw = { answerId, questionId ->
                    qnaViewModel.deleteAnswer(answerId, questionId)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.NOTIFICATIONS) {
            LaunchedEffect(Unit) { adminViewModel.loadNotifications() }
            NotificationsScreen(
                notifications = adminNotifications,
                isLoading = adminLoading,
                onMarkAllRead = { adminViewModel.markAllRead() },
                onOpen = { notification ->
                    // Route to whatever the notification refers to, where we can.
                    when (notification.targetType) {
                        "QUESTION" -> notification.targetId?.let { id ->
                            qnaViewModel.openQuestion(id)
                            navController.navigate(UrimaiDestinations.QUESTION_DETAIL)
                        }
                        "CONTACT_REQUEST" -> {
                            qnaViewModel.loadMyContactRequests()
                            navController.navigate(UrimaiDestinations.MY_CONTACT_REQUESTS)
                        }
                        "LAWYER" -> navController.navigate(
                            UrimaiDestinations.LAWYER_VERIFICATION
                        )
                        else -> Unit
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.LAWYER_VERIFICATION) {
            LaunchedEffect(Unit) { adminViewModel.loadMyLawyerProfile() }
            LawyerVerificationScreen(
                profile = myLawyerProfile,
                documents = myVerificationDocs,
                isLoading = adminLoading,
                isSubmitting = adminSubmitting,
                onSave = { request -> adminViewModel.saveLawyerProfile(request) {} },
                onPickDocument = { docType -> onPickVerificationDocument(docType) },
                onDeleteDocument = { adminViewModel.deleteVerificationDocument(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ADMIN_DASHBOARD) {
            LaunchedEffect(Unit) {
                adminViewModel.loadLawyerQueue("PENDING")
                adminViewModel.loadReports("OPEN")
            }
            AdminDashboardScreen(
                pendingLawyerCount = adminLawyerQueue.size,
                openReportCount = adminReports.size,
                onOpenLawyerQueue = {
                    adminViewModel.loadLawyerQueue()
                    navController.navigate(UrimaiDestinations.ADMIN_LAWYER_QUEUE)
                },
                onOpenUsers = {
                    adminViewModel.loadUsers()
                    navController.navigate(UrimaiDestinations.ADMIN_USERS)
                },
                onOpenReports = {
                    adminViewModel.loadReports()
                    navController.navigate(UrimaiDestinations.ADMIN_REPORTS)
                },
                onOpenAuditLog = {
                    adminViewModel.loadAuditLog()
                    navController.navigate(UrimaiDestinations.ADMIN_AUDIT_LOG)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ADMIN_LAWYER_QUEUE) {
            LawyerQueueScreen(
                lawyers = adminLawyerQueue,
                isLoading = adminLoading,
                onOpenApplication = { lawyer ->
                    adminViewModel.openLawyerApplication(lawyer)
                    navController.navigate(UrimaiDestinations.ADMIN_LAWYER_APPLICATION)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ADMIN_LAWYER_APPLICATION) {
            LawyerApplicationScreen(
                lawyer = adminSelectedLawyer,
                documents = adminSelectedLawyerDocs,
                isSubmitting = adminSubmitting,
                onDecide = { status, notes ->
                    adminSelectedLawyer?.let { lawyer ->
                        adminViewModel.decideVerification(lawyer.userId, status, notes) {
                            navController.popBackStack()
                        }
                    }
                },
                onViewDocument = { /* Streaming a document to a viewer is not built yet. */ },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ADMIN_USERS) {
            AdminUsersScreen(
                users = adminUsers,
                query = adminUserQuery,
                isLoading = adminLoading,
                onQueryChange = { adminViewModel.setUserQuery(it) },
                onSearch = { adminViewModel.loadUsers() },
                onSetStatus = { id, status, reason ->
                    adminViewModel.setAccountStatus(id, status, reason)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ADMIN_REPORTS) {
            AdminReportsScreen(
                reports = adminReports,
                selectedContent = adminReportContent,
                isLoading = adminLoading,
                onOpenReport = { adminViewModel.openReport(it) },
                onResolve = { id, action, note ->
                    adminViewModel.resolveReport(id, action, note)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ADMIN_AUDIT_LOG) {
            AdminAuditLogScreen(
                entries = adminAuditLog,
                isLoading = adminLoading,
                onBack = { navController.popBackStack() }
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
                },
                uploadedDocuments = uploadedDocuments,
                onUploadDocument = { documentName, fileUri, fileName, mimeType ->
                    viewModel.uploadDocument(documentName, fileUri, fileName, mimeType)
                },
                onRemoveUpload = { viewModel.removeUploadedDocument(it) }
            )
        }
    }

    if (showHowItWorks) {
        HowItWorksBottomSheet(
            onDismiss = { viewModel.setShowHowItWorks(false) }
        )
    }
}
