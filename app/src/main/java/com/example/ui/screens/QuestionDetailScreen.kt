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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.AnswerResponse
import com.example.data.remote.QuestionDetailResponse
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText

/**
 * One question and every answer it received.
 *
 * Answers are shown in the order they arrived, with no ranking or "best answer"
 * marker. Several verified lawyers may reasonably disagree, and presenting one
 * as authoritative would be the platform making a judgement it is not in a
 * position to make.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    detail: QuestionDetailResponse?,
    isLoading: Boolean,
    onClose: (String) -> Unit,
    onReportAnswer: (answerId: String, reason: String) -> Unit,
    onContactLawyer: (lawyerId: String) -> Unit,
    onBack: () -> Unit
) {
    var reportTarget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your question") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val question = detail?.question
                    if (question != null && question.status != "CLOSED") {
                        TextButton(onClick = { onClose(question.id) }) { Text("Close") }
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

            if (detail == null) {
                if (!isLoading) {
                    EmptyState(
                        title = "Question unavailable",
                        body = "This question could not be loaded."
                    )
                }
                return@Column
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                detail.question.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                detail.question.body,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                detail.question.category?.let { Chip(it) }
                                Chip(formatShortDate(detail.question.createdAt))
                            }
                        }
                    }
                }

                item {
                    Text(
                        when (detail.answers.size) {
                            0 -> "No replies yet"
                            1 -> "1 reply from a verified lawyer"
                            else -> "${detail.answers.size} replies from verified lawyers"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (detail.answers.isEmpty()) {
                    item {
                        Text(
                            "Verified lawyers can see your question and may reply. " +
                                "You will be notified when they do.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(detail.answers, key = { it.id }) { answer ->
                        AnswerCard(
                            answer = answer,
                            onReport = { reportTarget = answer.id },
                            onContact = { onContactLawyer(answer.lawyerUserId) }
                        )
                    }

                    item {
                        Text(
                            "Lawyers answer independently and cannot see each other's " +
                                "replies, so differing views are normal. These are general " +
                                "legal information, not formal advice on your case.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    reportTarget?.let { answerId ->
        ReportDialog(
            onDismiss = { reportTarget = null },
            onConfirm = { reason ->
                onReportAnswer(answerId, reason)
                reportTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnswerCard(
    answer: AnswerResponse,
    onReport: () -> Unit,
    onContact: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            answer.lawyerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Verified lawyer",
                            tint = EmeraldText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        buildString {
                            append("${answer.lawyerYearsExperience} years' experience")
                            if (answer.lawyerSpecializations.isNotEmpty()) {
                                append(" · ")
                                append(answer.lawyerSpecializations.take(2).joinToString(", "))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Contact this lawyer") },
                            onClick = { menuOpen = false; onContact() }
                        )
                        DropdownMenuItem(
                            text = { Text("Report this answer") },
                            leadingIcon = {
                                Icon(Icons.Filled.Flag, contentDescription = null)
                            },
                            onClick = { menuOpen = false; onReport() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(answer.body, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            Text(
                formatShortDate(answer.createdAt) +
                    if (answer.updatedAt > answer.createdAt + 1000L) " · edited" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Minimal box wrapper so the dropdown anchors correctly. */
@Composable
private fun Box(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box { content() }
}

private val REPORT_REASONS = listOf(
    "Inappropriate or offensive",
    "Appears to be spam or advertising",
    "Looks like incorrect legal information",
    "Asks for money or personal details"
)

@Composable
private fun ReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selected by remember { mutableStateOf(REPORT_REASONS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this answer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "A moderator will review it. The lawyer is not told who reported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                REPORT_REASONS.forEach { reason ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (reason == selected) CivicNavy100 else
                                MaterialTheme.colorScheme.surface
                        ),
                        onClick = { selected = reason },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reason == selected) CivicNavy700 else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("Report") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
