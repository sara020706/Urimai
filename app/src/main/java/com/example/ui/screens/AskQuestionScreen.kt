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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText

/** Minimums mirrored from the server so the user is told before a round trip. */
private const val MIN_TITLE = 10
private const val MIN_BODY = 20
private const val MAX_TITLE = 200
private const val MAX_BODY = 5000

private val CATEGORIES = listOf(
    "Welfare", "Education", "Employment", "Housing",
    "Agriculture", "Pension", "Documents", "Other"
)

/**
 * Compose an anonymous question for verified lawyers.
 *
 * The anonymity notice is prominent and honest about its one limitation: the
 * platform withholds identity, but it cannot redact what someone types into the
 * body. Saying so is more useful than a blanket "you are anonymous".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskQuestionScreen(
    isSubmitting: Boolean,
    defaultState: String?,
    onSubmit: (title: String, body: String, category: String?, state: String?) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }

    val titleValid = title.trim().length >= MIN_TITLE
    val bodyValid = body.trim().length >= MIN_BODY
    val canSubmit = titleValid && bodyValid && !isSubmitting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ask anonymously") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = EmeraldText,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            "Your name is never shown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldText
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Lawyers see only your question. They cannot see who you are, " +
                                "and other citizens cannot see this question at all.\n\n" +
                                "One thing to watch: we cannot remove details you type " +
                                "yourself, so avoid putting your name, phone number or " +
                                "address in the text below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldText
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE) title = it },
                label = { Text("What is your question about?") },
                placeholder = { Text("e.g. My scheme application was rejected") },
                singleLine = true,
                isError = title.isNotEmpty() && !titleValid,
                supportingText = {
                    Text(
                        if (title.isNotEmpty() && !titleValid) {
                            "At least $MIN_TITLE characters"
                        } else {
                            "${title.length} / $MAX_TITLE"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = body,
                onValueChange = { if (it.length <= MAX_BODY) body = it },
                label = { Text("Describe your situation") },
                placeholder = {
                    Text("What happened, what you were told, and what you want to achieve.")
                },
                minLines = 6,
                isError = body.isNotEmpty() && !bodyValid,
                supportingText = {
                    Text(
                        if (body.isNotEmpty() && !bodyValid) {
                            "At least $MIN_BODY characters so a lawyer can help"
                        } else {
                            "${body.length} / $MAX_BODY"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Topic (optional)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            CategoryChips(
                categories = CATEGORIES,
                selected = category,
                onSelect = { category = if (category == it) null else it }
            )

            Button(
                onClick = { onSubmit(title.trim(), body.trim(), category, defaultState) },
                enabled = canSubmit,
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
                    Text("Send to verified lawyers")
                }
            }

            Text(
                "Answers are general legal information, not a substitute for formal " +
                    "legal advice on your specific case.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Simple wrapping chip row. Extracted so the directory filters can reuse it. */
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun CategoryChips(
    categories: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { item ->
            val isSelected = item == selected
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CivicNavy700 else CivicNavy100
                ),
                onClick = { onSelect(item) }
            ) {
                Text(
                    item,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        CivicNavy700
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/** Shared empty state, so every list screen reads the same way. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
