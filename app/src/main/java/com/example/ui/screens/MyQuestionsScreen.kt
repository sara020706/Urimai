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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.QuestionSummaryResponse
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Short, local date for list rows. */
internal fun formatShortDate(epochMillis: Long): String = try {
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
} catch (_: Exception) {
    ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuestionsScreen(
    questions: List<QuestionSummaryResponse>,
    isLoading: Boolean,
    onOpenQuestion: (String) -> Unit,
    onAskNew: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_my_questions)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAskNew,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_ask)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (questions.isEmpty() && !isLoading) {
                EmptyState(
                    title = "No questions yet",
                    body = "Ask anonymously and verified lawyers can reply. " +
                        "Your name is never shown to them."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questions, key = { it.id }) { question ->
                        QuestionRow(question = question, onClick = { onOpenQuestion(question.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionRow(
    question: QuestionSummaryResponse,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                question.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnswerCountPill(question.answerCount)
                question.category?.let { Chip(it) }
                Spacer(Modifier.weight(1f))
                Text(
                    formatShortDate(question.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (question.status == "CLOSED") {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Closed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The answer count, shown only to the person who asked.
 *
 * Lawyers never see this: a live count of how many others have replied is a
 * timing signal and encourages racing rather than better answers.
 */
@Composable
private fun AnswerCountPill(count: Int) {
    val answered = count > 0
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (answered) EmeraldContainer else AmberContainer
        )
    ) {
        Text(
            when (count) {
                0 -> "Awaiting replies"
                1 -> "1 reply"
                else -> "$count replies"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (answered) EmeraldText else AmberText,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
internal fun Chip(label: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CivicNavy100)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = CivicNavy700,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
