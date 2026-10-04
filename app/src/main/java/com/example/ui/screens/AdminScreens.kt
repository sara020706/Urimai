package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Logout
import com.example.ui.components.NotificationAction
import com.example.ui.components.UrimaiLoadingBar
import com.example.ui.components.UrimaiTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.AdminLawyerResponse
import com.example.data.remote.AdminUserResponse
import com.example.data.remote.AuditLogEntryResponse
import com.example.data.remote.ContentReportResponse
import com.example.data.remote.ReportedContentResponse
import com.example.data.remote.VerificationDocumentResponse
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.CivicNavy800
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText

/** Entry point to the administrative sections. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    pendingLawyerCount: Int,
    openReportCount: Int,
    unreadNotificationCount: Int,
    isLoading: Boolean,
    onOpenLawyerQueue: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAuditLog: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogOut: () -> Unit,
    currentLanguage: com.example.data.model.AppLanguage,
    onLanguageChange: (com.example.data.model.AppLanguage) -> Unit
) {
    Scaffold(
        topBar = {
            // No back button: this is a landing screen reached with popUpTo(0),
            // so back would have popped an empty stack and exited the app.
            UrimaiTopBar(
                title = stringResource(R.string.title_administration),
                actions = {
                    com.example.ui.components.LanguageSelector(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageChange
                    )
                    NotificationAction(
                        unreadCount = unreadNotificationCount,
                        onClick = onOpenNotifications
                    )
                    IconButton(onClick = onLogOut) {
                        Icon(Icons.Filled.Logout, contentDescription = stringResource(R.string.action_log_out))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UrimaiLoadingBar(isLoading)

            AdminTile(
                title = stringResource(R.string.title_lawyer_applications),
                subtitle = when {
                    isLoading -> "Checking…"
                    pendingLawyerCount > 0 -> "$pendingLawyerCount awaiting review"
                    else -> "Nothing awaiting review"
                },
                highlight = pendingLawyerCount > 0,
                onClick = onOpenLawyerQueue
            )
            AdminTile(
                title = stringResource(R.string.title_reported_content),
                subtitle = when {
                    isLoading -> "Checking…"
                    openReportCount > 0 ->
                        "$openReportCount open report${if (openReportCount == 1) "" else "s"}"
                    else -> "No open reports"
                },
                highlight = openReportCount > 0,
                onClick = onOpenReports
            )
            AdminTile(
                title = stringResource(R.string.nav_users),
                subtitle = "Search, suspend or block accounts",
                onClick = onOpenUsers
            )
            AdminTile(
                title = stringResource(R.string.title_audit_log),
                subtitle = "Every administrative action, with who and why",
                onClick = onOpenAuditLog
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTile(
    title: String,
    subtitle: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) AmberContainer else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) AmberText else CivicNavy800
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlight) AmberText else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The lawyer verification queue.
 *
 * Pending and more-info applications sort first, because an application left
 * waiting is a lawyer who cannot help anyone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerQueueScreen(
    lawyers: List<AdminLawyerResponse>,
    isLoading: Boolean,
    onOpenApplication: (AdminLawyerResponse) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_lawyer_applications)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (lawyers.isEmpty() && !isLoading) {
                EmptyState(
                    title = "No applications",
                    body = "Lawyer applications appear here for review."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lawyers, key = { it.userId }) { lawyer ->
                        Card(
                            onClick = { onOpenApplication(lawyer) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    lawyer.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${lawyer.barCouncilRegNumber} · ${lawyer.barCouncilState}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusPill(lawyer.verificationStatus)
                                    Chip(
                                        "${lawyer.documentCount} document" +
                                            if (lawyer.documentCount == 1) "" else "s"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Review one application and decide.
 *
 * Rejecting or asking for more information requires a note — the server
 * enforces it too, but asking here means the admin is not surprised by a 400
 * after composing a decision.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerApplicationScreen(
    lawyer: AdminLawyerResponse?,
    documents: List<VerificationDocumentResponse>,
    isSubmitting: Boolean,
    onDecide: (status: String, notes: String?) -> Unit,
    onViewDocument: (docId: String) -> Unit,
    onBack: () -> Unit
) {
    var pendingDecision by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_review_application)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (lawyer == null) {
            Column(modifier = Modifier.padding(padding)) {
                EmptyState(title = "No application selected", body = "")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        lawyer.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusPill(lawyer.verificationStatus)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    AdminDetailRow("Account", lawyer.username)
                    AdminDetailRow("Registration", lawyer.barCouncilRegNumber)
                    AdminDetailRow("Bar Council", lawyer.barCouncilState)
                    AdminDetailRow("Experience", "${lawyer.yearsExperience} years")
                    lawyer.enrollmentYear?.let { AdminDetailRow("Enrolled", it.toString()) }
                    lawyer.practiceDistrict?.let { AdminDetailRow("Practises in", it) }
                    if (lawyer.specializations.isNotEmpty()) {
                        AdminDetailRow("Areas", lawyer.specializations.joinToString(", "))
                    }
                    if (lawyer.languages.isNotEmpty()) {
                        AdminDetailRow("Languages", lawyer.languages.joinToString(", "))
                    }
                    lawyer.bio?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                "Documents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (documents.isEmpty()) {
                Text(
                    "No documents uploaded. Ask for them before approving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberText
                )
            }
            documents.forEach { document ->
                Card(
                    onClick = { onViewDocument(document.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(document.docType, style = MaterialTheme.typography.titleSmall)
                            Text(
                                document.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Text(
                "Opening a document is recorded in the audit log.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Button(
                onClick = { pendingDecision = "VERIFIED" },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.action_approve))
            }
            OutlinedButton(
                onClick = { pendingDecision = "MORE_INFO_REQUESTED" },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request more information")
            }
            OutlinedButton(
                onClick = { pendingDecision = "REJECTED" },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_reject))
            }
            if (lawyer.verificationStatus == "VERIFIED") {
                OutlinedButton(
                    onClick = { pendingDecision = "SUSPENDED" },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Suspend verification")
                }
            }
        }
    }

    pendingDecision?.let { decision ->
        DecisionDialog(
            decision = decision,
            notesRequired = decision == "REJECTED" || decision == "MORE_INFO_REQUESTED",
            onDismiss = { pendingDecision = null },
            onConfirm = { notes ->
                onDecide(decision, notes)
                pendingDecision = null
            }
        )
    }
}

@Composable
private fun DecisionDialog(
    decision: String,
    notesRequired: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    val canConfirm = !notesRequired || notes.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (decision) {
                    "VERIFIED" -> "Approve this lawyer?"
                    "REJECTED" -> "Reject this application?"
                    "MORE_INFO_REQUESTED" -> "Request more information"
                    else -> "Suspend verification?"
                }
            )
        },
        text = {
            Column {
                Text(
                    when (decision) {
                        "VERIFIED" ->
                            "They will be able to answer citizen questions and appear " +
                                "in the directory."
                        "SUSPENDED" ->
                            "They will immediately stop appearing in the directory and " +
                                "cannot answer questions."
                        else -> "They will see your note and can resubmit."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = {
                        Text(if (notesRequired) "Reason (required)" else "Note (optional)")
                    },
                    minLines = 3,
                    isError = notesRequired && notes.isNotEmpty() && notes.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(notes.trim().ifBlank { null }) },
                enabled = canConfirm
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** User management: search, then suspend, block or reactivate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    users: List<AdminUserResponse>,
    query: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSetStatus: (userId: String, status: String, reason: String?) -> Unit,
    onBack: () -> Unit
) {
    var target by remember { mutableStateOf<Pair<AdminUserResponse, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_users)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search by username or name") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                user.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(user.role)
                                StatusPill(user.accountStatus)
                                user.lawyerVerificationStatus?.let { StatusPill(it) }
                            }
                            user.statusReason?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CrimsonText
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (user.accountStatus != "ACTIVE") {
                                    TextButton(onClick = {
                                        onSetStatus(user.id, "ACTIVE", null)
                                    }) { Text(stringResource(R.string.action_reactivate)) }
                                }
                                if (user.accountStatus != "SUSPENDED") {
                                    TextButton(onClick = {
                                        target = user to "SUSPENDED"
                                    }) { Text(stringResource(R.string.action_suspend)) }
                                }
                                if (user.accountStatus != "BLOCKED") {
                                    TextButton(onClick = {
                                        target = user to "BLOCKED"
                                    }) { Text(stringResource(R.string.action_block)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    target?.let { (user, status) ->
        var reason by remember(user.id, status) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { target = null },
            title = {
                Text(if (status == "BLOCKED") "Block this account?" else "Suspend this account?")
            },
            text = {
                Column {
                    Text(
                        "Their current session ends immediately and they cannot sign " +
                            "in again until this is reversed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason shown to the user") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetStatus(user.id, status, reason.trim().ifBlank { null })
                    target = null
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

/** Moderation queue for reported questions, answers and lawyers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    reports: List<ContentReportResponse>,
    selectedContent: ReportedContentResponse?,
    isLoading: Boolean,
    onRefresh: () -> Unit = {},
    onOpenReport: (String) -> Unit,
    onResolve: (reportId: String, action: String, note: String?) -> Unit,
    onBack: () -> Unit
) {
    var expanded by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_reported_content)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        com.example.ui.components.UrimaiRefreshable(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (reports.isEmpty() && !isLoading) {
                EmptyState(title = "Nothing reported", body = "The queue is empty.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports, key = { it.id }) { report ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Chip(report.targetType)
                                    StatusPill(report.status)
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    report.reason,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                report.details?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Reported by ${report.reporterUsername} on " +
                                        formatShortDate(report.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (expanded == report.id && selectedContent?.content != null) {
                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(Modifier.height(12.dp))
                                    selectedContent.content.forEach { (key, value) ->
                                        if (value != null) {
                                            Text(
                                                "$key: $value",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        expanded = if (expanded == report.id) null else report.id
                                        if (expanded == report.id) onOpenReport(report.id)
                                    }) {
                                        Text(if (expanded == report.id) "Hide" else "View content")
                                    }
                                    if (report.status == "OPEN") {
                                        TextButton(onClick = {
                                            onResolve(report.id, "HIDE", null)
                                        }) { Text("Hide") }
                                        TextButton(onClick = {
                                            onResolve(report.id, "DISMISS", null)
                                        }) { Text("Dismiss") }
                                    } else {
                                        TextButton(onClick = {
                                            onResolve(report.id, "UNHIDE", null)
                                        }) { Text("Restore") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/** Audit log. Read-only by design: an editable audit trail is not one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogScreen(
    entries: List<AuditLogEntryResponse>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_audit_log)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (entries.isEmpty() && !isLoading) {
                EmptyState(title = "No entries", body = "Administrative actions appear here.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    entry.action.replace('_', ' ').lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${entry.targetType}${
                                        entry.targetId?.let { " #$it" } ?: ""
                                    } · by ${entry.adminUsername}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                entry.reason?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(6.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    formatShortDate(entry.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(width = 130.dp, height = 20.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Colour-codes a status so a queue can be scanned without reading every word. */
@Composable
internal fun StatusPill(status: String) {
    val (container, content) = when (status.uppercase()) {
        "VERIFIED", "ACTIVE", "RESOLVED" -> EmeraldContainer to EmeraldText
        "PENDING", "MORE_INFO_REQUESTED", "OPEN", "SUSPENDED" -> AmberContainer to AmberText
        "REJECTED", "BLOCKED", "REMOVED" -> CrimsonContainer to CrimsonText
        else -> CivicNavy100 to CivicNavy700
    }
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Text(
            status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
