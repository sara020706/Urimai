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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.InboxRequestResponse
import com.example.data.remote.LawyerProfileResponse
import com.example.data.remote.LawyerVerificationStatus
import com.example.data.remote.MyAnswerSummaryResponse
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CivicNavy800
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import com.example.ui.theme.SaffronPrimary

/**
 * A verified lawyer's home.
 *
 * Lawyers used to land on the citizen scheme dashboard, which has nothing to do
 * with their work. This is their actual workspace: answer questions, manage
 * contact requests, and control availability.
 *
 * An unverified lawyer sees the same screen with the working sections replaced
 * by an explanation of what is blocking them, rather than buttons that fail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerHomeScreen(
    profile: LawyerProfileResponse?,
    pendingQuestionCount: Int,
    answeredCount: Int,
    pendingContactCount: Int,
    unreadNotificationCount: Int,
    isAcceptingRequests: Boolean,
    isLoading: Boolean = false,
    currentLanguage: com.example.data.model.AppLanguage,
    onLanguageChange: (com.example.data.model.AppLanguage) -> Unit,
    onOpenQuestionFeed: () -> Unit,
    onOpenMyAnswers: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenNotifications: () -> Unit,
    onToggleAvailability: (Boolean) -> Unit,
    onLogOut: () -> Unit
) {
    val status = LawyerVerificationStatus.from(profile?.verificationStatus)
    val verified = status.unlocksLawyerFeatures

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_legal_specialist)) },
                actions = {
                    com.example.ui.components.LanguageSelector(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageChange
                    )
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(containerColor = SaffronPrimary) {
                                        Text("$unreadNotificationCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                profile?.fullName ?: "Welcome",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = CivicNavy800
            )

            StatusCard(status, profile?.verificationNotes, onOpenVerification)

            if (verified) {
                WorkTile(
                    icon = Icons.Filled.QuestionAnswer,
                    title = "Citizen questions",
                    subtitle = when {
                        isLoading -> "Checking…"
                        pendingQuestionCount > 0 -> "$pendingQuestionCount waiting for a reply"
                        else -> "No open questions right now"
                    },
                    highlight = pendingQuestionCount > 0,
                    onClick = onOpenQuestionFeed
                )
                WorkTile(
                    icon = Icons.Filled.Description,
                    title = stringResource(R.string.nav_my_answers),
                    subtitle = when {
                        isLoading -> "Checking…"
                        answeredCount > 0 ->
                            "$answeredCount answer${if (answeredCount == 1) "" else "s"} given"
                        else -> "You have not answered anything yet"
                    },
                    onClick = onOpenMyAnswers
                )
                WorkTile(
                    icon = Icons.Filled.Email,
                    title = stringResource(R.string.title_contact_requests),
                    subtitle = when {
                        isLoading -> "Checking…"
                        pendingContactCount > 0 -> "$pendingContactCount citizen${
                            if (pendingContactCount == 1) "" else "s"
                        } asking to reach you"
                        else -> "No new requests"
                    },
                    highlight = pendingContactCount > 0,
                    onClick = onOpenInbox
                )

                HorizontalDivider()

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Accepting new requests",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (isAcceptingRequests) {
                                    "Citizens can ask to contact you."
                                } else {
                                    "You are hidden from contact requests. " +
                                        "You can still answer questions."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAcceptingRequests,
                            onCheckedChange = onToggleAvailability
                        )
                    }
                }
            }

            WorkTile(
                icon = Icons.Filled.Person,
                title = "My professional details",
                subtitle = "Credentials, areas of practice and documents",
                onClick = onOpenVerification
            )
        }
    }
}

@Composable
private fun StatusCard(
    status: LawyerVerificationStatus,
    notes: String?,
    onOpenVerification: () -> Unit
) {
    if (status.unlocksLawyerFeatures) {
        Card(
            colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldText,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Verified. You appear in the lawyer directory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldText
                )
            }
        }
        return
    }

    val (headline, detail, container, content) = when (status) {
        LawyerVerificationStatus.PENDING -> Quadruple(
            "Awaiting verification",
            "An administrator is reviewing your credentials. Until that is done " +
                "you cannot answer citizen questions or appear in the directory.",
            AmberContainer, AmberText
        )
        LawyerVerificationStatus.MORE_INFO_REQUESTED -> Quadruple(
            "More information needed",
            notes ?: "Add the documents requested, then save to resubmit.",
            AmberContainer, AmberText
        )
        LawyerVerificationStatus.REJECTED -> Quadruple(
            "Not approved",
            notes ?: "Your application was not approved. You can correct your " +
                "details and resubmit.",
            CrimsonContainer, CrimsonText
        )
        LawyerVerificationStatus.SUSPENDED -> Quadruple(
            "Suspended",
            notes ?: "Your verification is suspended. Contact support.",
            CrimsonContainer, CrimsonText
        )
        else -> Quadruple(
            "Complete your profile",
            "Add your Bar Council details and documents so an administrator " +
                "can verify you.",
            AmberContainer, AmberText
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        onClick = onOpenVerification,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = content
                )
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = content)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap to review your details",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = content
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (highlight) AmberText else CivicNavy800
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) AmberText else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlight) {
                        AmberText
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/** The lawyer's own answers, with the question each one belongs to. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAnswersScreen(
    answers: List<MyAnswerSummaryResponse>,
    isLoading: Boolean,
    onOpenQuestion: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_my_answers)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            if (isLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (answers.isEmpty() && !isLoading) {
                EmptyState(
                    title = "No answers yet",
                    body = "Answers you give to citizen questions appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(answers, key = { it.id }) { answer ->
                        Card(
                            onClick = { onOpenQuestion(answer.questionId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    answer.questionTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    answer.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    answer.questionCategory?.let { Chip(it) }
                                    StatusPill(answer.questionStatus)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        formatShortDate(answer.createdAt),
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
}

/**
 * Contact requests sent to this lawyer.
 *
 * The citizen's name is shown here, unlike anonymous Q&A: someone who asks for
 * direct contact is choosing to identify themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInboxScreen(
    requests: List<InboxRequestResponse>,
    isLoading: Boolean,
    onRespond: (requestId: String, action: String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_contact_requests)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            if (isLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (requests.isEmpty() && !isLoading) {
                EmptyState(
                    title = "No requests",
                    body = "When a citizen asks to contact you, it appears here."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests, key = { it.id }) { request ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    request.citizenName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (request.citizenDistrict != null ||
                                    request.citizenState != null
                                ) {
                                    Text(
                                        listOfNotNull(
                                            request.citizenDistrict,
                                            request.citizenState
                                        ).joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                request.message?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(10.dp))
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }

                                Spacer(Modifier.height(12.dp))
                                StatusPill(request.status)

                                if (request.status == "PENDING") {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Accepting shares your email and phone with this " +
                                            "person only.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.material3.Button(
                                            onClick = { onRespond(request.id, "ACCEPTED") }
                                        ) { Text(stringResource(R.string.action_accept)) }
                                        androidx.compose.material3.TextButton(
                                            onClick = { onRespond(request.id, "DECLINED") }
                                        ) { Text(stringResource(R.string.action_decline)) }
                                        androidx.compose.material3.TextButton(
                                            onClick = { onRespond(request.id, "BLOCKED") }
                                        ) { Text(stringResource(R.string.action_block)) }
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
