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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
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
import com.example.data.remote.LawyerQuestionDetailResponse
import com.example.data.remote.LawyerQuestionSummaryResponse
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText

private const val MIN_ANSWER = 20
private const val MAX_ANSWER = 10000

/**
 * The question feed a verified lawyer answers from.
 *
 * Each row shows whether THIS lawyer has already answered. It deliberately does
 * not show how many others have — that is a timing signal, it encourages racing
 * for fresh questions, and it does not help anyone write a better answer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerQuestionFeedScreen(
    questions: List<LawyerQuestionSummaryResponse>,
    isLoading: Boolean,
    onRefresh: () -> Unit = {},
    onOpenQuestion: (String) -> Unit,
    onViewMyAnswers: () -> Unit,
    onBack: () -> Unit,
    // Verification is a blocking account state, not a transient error, so it
    // gets a banner that stays put rather than a snackbar that disappears
    // before the lawyer has finished reading an empty list.
    accessDenied: Boolean = false,
    onOpenVerification: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_citizen_questions)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(onClick = onViewMyAnswers) {
                        Text(stringResource(R.string.nav_my_answers))
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
            if (accessDenied) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Your account is not verified yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "You cannot open or answer citizen questions until an " +
                                "administrator approves your professional details.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.material3.TextButton(onClick = onOpenVerification) {
                            Text("Check verification status")
                        }
                    }
                }
            }
            if (questions.isEmpty() && !isLoading) {
                EmptyState(
                    title = "No open questions",
                    body = "When citizens ask for help, their questions appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questions, key = { it.id }) { question ->
                        Card(
                            onClick = { onOpenQuestion(question.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    question.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (question.iHaveAnswered) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = EmeraldContainer
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp, vertical = 5.dp
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = EmeraldText,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    "You answered",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = EmeraldText
                                                )
                                            }
                                        }
                                    }
                                    question.category?.let { Chip(it) }
                                    question.state?.let { Chip(it) }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        formatShortDate(question.createdAt),
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
}

/**
 * Read a question and write or revise an answer.
 *
 * A lawyer sees only their own answer here. Other lawyers' replies are not
 * withheld by this screen — the server never sends them — so there is nothing
 * for a UI mistake to expose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerQuestionScreen(
    detail: LawyerQuestionDetailResponse?,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onSubmit: (questionId: String, existingAnswerId: String?, body: String) -> Unit,
    onWithdraw: (answerId: String, questionId: String) -> Unit,
    onBack: () -> Unit
) {
    var draft by remember(detail?.myAnswer?.id) {
        mutableStateOf(detail?.myAnswer?.body ?: "")
    }
    val isEditing = detail?.myAnswer != null
    val valid = draft.trim().length >= MIN_ANSWER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Your answer" else "Answer question") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        detail.question.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(detail.question.body, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail.question.category?.let { Chip(it) }
                        detail.question.state?.let { Chip(it) }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = AmberContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = AmberText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "This citizen is anonymous. Other lawyers answering the same " +
                            "question cannot see your reply, and you cannot see theirs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberText
                    )
                }
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= MAX_ANSWER) draft = it },
                label = { Text("Your answer") },
                placeholder = {
                    Text("Explain the position in plain language, and the practical next step.")
                },
                minLines = 8,
                isError = draft.isNotEmpty() && !valid,
                supportingText = {
                    Text(
                        if (draft.isNotEmpty() && !valid) {
                            "At least $MIN_ANSWER characters"
                        } else {
                            "${draft.length} / $MAX_ANSWER"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSubmit(detail.question.id, detail.myAnswer?.id, draft.trim())
                },
                enabled = valid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEditing) "Update answer" else "Send answer")
                }
            }

            if (isEditing) {
                OutlinedButton(
                    onClick = {
                        detail.myAnswer?.let { onWithdraw(it.id, detail.question.id) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Withdraw my answer")
                }
                HorizontalDivider()
                Text(
                    "Posted ${formatShortDate(detail.myAnswer!!.createdAt)}. " +
                        "You may revise it; the citizen sees the current version.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
