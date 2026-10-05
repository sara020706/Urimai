package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonContainer
import com.example.viewmodel.LegalChatMessage

/**
 * General legal question answering.
 *
 * This is NOT the anonymous lawyer path. Answers come from a model, so the
 * screen says so twice: once in the opening state and once as a persistent
 * footnote under the input. The route to a real lawyer is offered from here,
 * because the honest answer to many legal questions is "ask someone who can
 * be accountable for the advice".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalChatScreen(
    messages: List<LegalChatMessage>,
    isReplying: Boolean,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onAskLawyer: () -> Unit,
    // The lawyer Q&A moved under this tab, so its inbox has to be reachable
    // from here or previously-asked questions become unreachable.
    onViewMyQuestions: () -> Unit,
    onBack: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Follow the conversation as it grows, including while a reply is pending.
    LaunchedEffect(messages.size, isReplying) {
        val target = messages.size + if (isReplying) 1 else 0
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_legal_assistant)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onViewMyQuestions) {
                        Text(stringResource(R.string.nav_my_questions_short))
                    }
                    if (messages.isNotEmpty()) {
                        TextButton(onClick = onClear) {
                            Text(stringResource(R.string.action_new_chat))
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { if (it.length <= 2000) input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.legal_chat_hint)) },
                            maxLines = 5,
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (input.isNotBlank() && !isReplying) {
                                        onSend(input)
                                        input = ""
                                    }
                                }
                            )
                        )
                        FilledIconButton(
                            onClick = {
                                if (input.isNotBlank() && !isReplying) {
                                    onSend(input)
                                    input = ""
                                }
                            },
                            enabled = input.isNotBlank() && !isReplying
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.action_send)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Persistent and not dismissible: someone scrolling a long
                    // conversation should never lose track of what this is.
                    Text(
                        stringResource(R.string.legal_chat_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            LegalChatIntro(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAskLawyer = onAskLawyer,
                onSuggestion = onSend
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    LegalChatBubble(message = message, onRetry = onRetry)
                }
                if (isReplying) {
                    item(key = "typing") { LegalChatTyping() }
                }
                item(key = "lawyer_nudge") {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onAskLawyer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.legal_chat_ask_lawyer))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalChatIntro(
    modifier: Modifier = Modifier,
    onAskLawyer: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    val suggestions = listOf(
        stringResource(R.string.legal_chat_suggestion_1),
        stringResource(R.string.legal_chat_suggestion_2),
        stringResource(R.string.legal_chat_suggestion_3)
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = AmberContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.legal_chat_intro_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.legal_chat_intro_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            stringResource(R.string.legal_chat_try_asking),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        suggestions.forEach { suggestion ->
            Card(
                onClick = { onSuggestion(suggestion) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    suggestion,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        OutlinedButton(onClick = onAskLawyer, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.legal_chat_ask_lawyer))
        }
    }
}

@Composable
private fun LegalChatBubble(message: LegalChatMessage, onRetry: () -> Unit) {
    val isError = message.error != null
    val container = when {
        isError -> CrimsonContainer
        message.isFromUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.88f)) {
            Surface(
                color = container,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                )
            ) {
                Text(
                    message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Only a transient failure is worth retrying. A rejected question
            // would be rejected again, so retry is not offered for it.
            if (message.error != null && message.error != "REJECTED") {
                TextButton(onClick = onRetry) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

@Composable
private fun LegalChatTyping() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp
            )
            Text(
                stringResource(R.string.legal_chat_thinking),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
