package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.CatalogSource
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
    const val LAWYER_HOME = "lawyer_home"
    const val LAWYER_MY_ANSWERS = "lawyer_my_answers"
    const val LAWYER_INBOX = "lawyer_inbox"

    // Shared
    const val NOTIFICATIONS = "notifications"

    // Administration
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_LAWYER_QUEUE = "admin_lawyer_queue"
    const val ADMIN_LAWYER_APPLICATION = "admin_lawyer_application"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_REPORTS = "admin_reports"
    const val ADMIN_AUDIT_LOG = "admin_audit_log"
    const val ADMIN_DOCUMENT_VIEWER = "admin_document_viewer"
    const val EXTRACT_REVIEW = "extract_review"
    const val MY_DOCUMENT_VIEWER = "my_document_viewer"
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
    val qnaMyAnswers by qnaViewModel.myAnswers.collectAsState()
    val qnaContactInbox by qnaViewModel.contactInbox.collectAsState()
    val qnaAccepting by qnaViewModel.isAcceptingRequests.collectAsState()
    val openDocument by adminViewModel.openDocument.collectAsState()
    val isDownloadingDocument by adminViewModel.isDownloadingDocument.collectAsState()
    val extraction by viewModel.extraction.collectAsState()
    val isExtracting by viewModel.isExtracting.collectAsState()
    val extractionError by viewModel.extractionError.collectAsState()
    val catalogSource by viewModel.catalogSource.collectAsState()

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

    // Where a session lands depends on the role.
    //
    // The analysis animation and scheme dashboard are the CITIZEN flow; running
    // a lawyer or an admin through them shows work that has nothing to do with
    // their job. Role is read from the session, which login populates, so this
    // waits for both the login flag and the role flags to settle.
    LaunchedEffect(authState.isLoggedIn, isAdmin, isLawyerRole) {
        if (!authState.isLoggedIn) return@LaunchedEffect

        when {
            isAdmin -> navController.navigate(UrimaiDestinations.ADMIN_DASHBOARD) {
                popUpTo(0)
            }
            isLawyerRole -> navController.navigate(UrimaiDestinations.LAWYER_HOME) {
                popUpTo(0)
            }
            else -> {
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
    }

    // Messages from the ViewModels had nowhere to go: 30 assignment sites across
    // QnaViewModel and AdminViewModel were written and never read, so a failed
    // post or a refused action looked like nothing happening at all.
    val snackbarHostState = remember { SnackbarHostState() }
    val myOpenDocument by viewModel.openDocument.collectAsState()
    val isDownloadingMyDocument by viewModel.isDownloadingDocument.collectAsState()
    val lawyerAccessDenied by qnaViewModel.lawyerAccessDenied.collectAsState()
    val qnaMessage by qnaViewModel.message.collectAsState()
    val adminMessage by adminViewModel.message.collectAsState()

    LaunchedEffect(qnaMessage) {
        qnaMessage?.let {
            snackbarHostState.showSnackbar(it)
            qnaViewModel.clearMessage()
        }
    }
    LaunchedEffect(extractionError) {
        extractionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExtraction()
        }
    }
    LaunchedEffect(adminMessage) {
        adminMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessage()
        }
    }

    // The bottom bar is the main thing that makes the three roles feel like
    // different apps rather than one app with hidden rooms. Which set of tabs
    // appears is presentation only; the server still decides what each role may
    // actually do.
    val tabs = when {
        isAdmin -> adminTabs(
            pendingLawyers = adminLawyerQueue.count { it.verificationStatus == "PENDING" },
            openReports = adminReports.count { it.status == "OPEN" },
            unread = unreadCount
        )
        isLawyerRole -> lawyerTabs(
            pendingQuestions = lawyerFeed.count { !it.iHaveAnswered },
            pendingContacts = qnaContactInbox.count { it.status == "PENDING" },
            unread = unreadCount
        )
        else -> citizenTabs(unread = unreadCount)
    }

    Box(modifier = modifier) {
    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (authState.isLoggedIn) {
                UrimaiBottomBar(tabs = tabs, navController = navController)
            }
        }
    ) { scaffoldPadding ->
    NavHost(
        modifier = Modifier.padding(scaffoldPadding),
        // A short slide + fade. Snapping between screens made the app feel
        // like a set of unrelated pages; 220ms is fast enough not to be felt
        // as waiting.
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                animationSpec = androidx.compose.animation.core.tween(220),
                initialOffsetX = { it / 8 }
            ) + androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(220)
            )
        },
        exitTransition = {
            androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(160)
            )
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(180)
            )
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(200),
                targetOffsetX = { it / 8 }
            ) + androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(200)
            )
        },
        navController = navController,
        startDestination = if (authState.isLoggedIn) UrimaiDestinations.ANALYZING else UrimaiDestinations.LOGIN
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
                onSignUp = { username, password, displayName, role ->
                    viewModel.signUp(username, password, displayName, role)
                }
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
                    navController.navigate(UrimaiDestinations.ASK_QUESTION)
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
                },
                isCatalogStale = catalogSource != CatalogSource.NETWORK
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
                onReport = { targetType, targetId, reason ->
                    qnaViewModel.report(targetType, targetId, reason)
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
                isLoading = qnaLoading,
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.LAWYER_FEED) {
            LaunchedEffect(Unit) { qnaViewModel.loadLawyerFeed() }
            LawyerQuestionFeedScreen(
                questions = lawyerFeed,
                isLoading = qnaLoading,
                onRefresh = { qnaViewModel.loadLawyerFeed() },
                onOpenQuestion = { id ->
                    qnaViewModel.openLawyerQuestion(id)
                    navController.navigate(UrimaiDestinations.ANSWER_QUESTION)
                },
                onViewMyAnswers = {
                    qnaViewModel.loadMyAnswers()
                    navController.navigate(UrimaiDestinations.LAWYER_MY_ANSWERS)
                },
                onBack = { navController.popBackStack() },
                accessDenied = lawyerAccessDenied,
                onOpenVerification = {
                    navController.navigate(UrimaiDestinations.LAWYER_VERIFICATION)
                }
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
                unreadNotificationCount = unreadCount,
                isLoading = adminLoading,
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
                onOpenNotifications = {
                    navController.navigate(UrimaiDestinations.NOTIFICATIONS)
                },
                onLogOut = {
                    viewModel.logOut()
                    navController.navigate(UrimaiDestinations.LOGIN) { popUpTo(0) }
                },
                currentLanguage = selectedLanguage,
                onLanguageChange = { viewModel.setLanguage(it) }
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
                onViewDocument = { docId ->
                    adminSelectedLawyerDocs.firstOrNull { it.id == docId }?.let { doc ->
                        adminViewModel.openVerificationDocument(doc)
                        navController.navigate(UrimaiDestinations.ADMIN_DOCUMENT_VIEWER)
                    }
                },
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
                onRefresh = { adminViewModel.loadReports() },
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

        composable(UrimaiDestinations.LAWYER_HOME) {
            LaunchedEffect(Unit) {
                adminViewModel.loadMyLawyerProfile()
                qnaViewModel.loadLawyerFeed()
                qnaViewModel.loadMyAnswers()
                qnaViewModel.loadContactInbox()
            }
            // Seed the availability switch from the server's copy.
            LaunchedEffect(myLawyerProfile?.userId) {
                myLawyerProfile?.let {
                    qnaViewModel.setAcceptingRequestsLocal(it.acceptingQuestions)
                }
            }
            LawyerHomeScreen(
                profile = myLawyerProfile,
                pendingQuestionCount = lawyerFeed.count { !it.iHaveAnswered },
                answeredCount = qnaMyAnswers.size,
                pendingContactCount = qnaContactInbox.count { it.status == "PENDING" },
                unreadNotificationCount = unreadCount,
                isAcceptingRequests = qnaAccepting,
                isLoading = qnaLoading || adminLoading,
                currentLanguage = selectedLanguage,
                onLanguageChange = { viewModel.setLanguage(it) },
                onOpenQuestionFeed = {
                    navController.navigate(UrimaiDestinations.LAWYER_FEED)
                },
                onOpenMyAnswers = {
                    qnaViewModel.loadMyAnswers()
                    navController.navigate(UrimaiDestinations.LAWYER_MY_ANSWERS)
                },
                onOpenInbox = {
                    qnaViewModel.loadContactInbox()
                    navController.navigate(UrimaiDestinations.LAWYER_INBOX)
                },
                onOpenVerification = {
                    navController.navigate(UrimaiDestinations.LAWYER_VERIFICATION)
                },
                onOpenNotifications = {
                    navController.navigate(UrimaiDestinations.NOTIFICATIONS)
                },
                onToggleAvailability = { qnaViewModel.setAvailability(it) },
                onLogOut = {
                    viewModel.logOut()
                    navController.navigate(UrimaiDestinations.LOGIN) { popUpTo(0) }
                }
            )
        }

        composable(UrimaiDestinations.LAWYER_MY_ANSWERS) {
            MyAnswersScreen(
                answers = qnaMyAnswers,
                isLoading = qnaLoading,
                onOpenQuestion = { questionId ->
                    qnaViewModel.openLawyerQuestion(questionId)
                    navController.navigate(UrimaiDestinations.ANSWER_QUESTION)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.LAWYER_INBOX) {
            LaunchedEffect(Unit) { qnaViewModel.loadContactInbox() }
            ContactInboxScreen(
                requests = qnaContactInbox,
                isLoading = qnaLoading,
                onRespond = { id, action -> qnaViewModel.respondToContact(id, action) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(UrimaiDestinations.ADMIN_DOCUMENT_VIEWER) {
            DocumentViewerScreen(
                document = openDocument,
                isLoading = isDownloadingDocument,
                onBack = {
                    adminViewModel.closeDocument()
                    navController.popBackStack()
                }
            )
        }

        composable(UrimaiDestinations.MY_DOCUMENT_VIEWER) {
            DocumentViewerScreen(
                document = myOpenDocument,
                isLoading = isDownloadingMyDocument,
                onBack = {
                    viewModel.closeOwnDocument()
                    navController.popBackStack()
                }
            )
        }

        composable(UrimaiDestinations.EXTRACT_REVIEW) {
            ExtractedInfoReviewScreen(
                extraction = extraction,
                currentProfile = userProfile,
                isLoading = isExtracting,
                onApply = { updated ->
                    viewModel.updateProfile(updated)
                    viewModel.clearExtraction()
                    navController.popBackStack()
                },
                onBack = {
                    viewModel.clearExtraction()
                    navController.popBackStack()
                }
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
                onRemoveUpload = { viewModel.removeUploadedDocument(it) },
                onReadDocument = { document ->
                    viewModel.extractFromDocument(document.id)
                    navController.navigate(UrimaiDestinations.EXTRACT_REVIEW)
                },
                onViewDocument = { document ->
                    viewModel.viewOwnDocument(document)
                    navController.navigate(UrimaiDestinations.MY_DOCUMENT_VIEWER)
                }
            )
        }
    }

    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }

    if (showHowItWorks) {
        HowItWorksBottomSheet(
            onDismiss = { viewModel.setShowHowItWorks(false) }
        )
    }
}
