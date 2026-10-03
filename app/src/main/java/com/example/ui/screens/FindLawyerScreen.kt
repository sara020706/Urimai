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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.remote.DirectoryLawyerResponse
import com.example.data.remote.MyContactRequestResponse
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText

private val SPECIALIZATIONS = listOf(
    "Welfare Law", "Consumer", "Family", "Property",
    "Employment", "Criminal", "Civil", "Documentation"
)

private val LANGUAGES = listOf("Tamil", "English", "Hindi", "Telugu", "Malayalam")

/**
 * Browse verified lawyers.
 *
 * No card here shows a phone number or email: those are released only after a
 * lawyer accepts a specific request. The server does not even send the fields,
 * so a mistake in this file cannot expose them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindLawyerScreen(
    lawyers: List<DirectoryLawyerResponse>,
    isLoading: Boolean,
    searchQuery: String,
    specializationFilter: String?,
    languageFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSpecializationChange: (String?) -> Unit,
    onLanguageChange: (String?) -> Unit,
    onOpenLawyer: (String) -> Unit,
    onViewMyRequests: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a lawyer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onViewMyRequests) { Text("My requests") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search by name") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Area of practice",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                CategoryChips(
                    categories = SPECIALIZATIONS,
                    selected = specializationFilter,
                    onSelect = { onSpecializationChange(if (it == specializationFilter) null else it) }
                )

                Text(
                    "Language",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                CategoryChips(
                    categories = LANGUAGES,
                    selected = languageFilter,
                    onSelect = { onLanguageChange(if (it == languageFilter) null else it) }
                )
            }

            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (lawyers.isEmpty() && !isLoading) {
                EmptyState(
                    title = "No lawyers match",
                    body = "Try removing a filter, or search a different name."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lawyers, key = { it.userId }) { lawyer ->
                        LawyerCard(lawyer = lawyer, onClick = { onOpenLawyer(lawyer.userId) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LawyerCard(
    lawyer: DirectoryLawyerResponse,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    lawyer.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Verified",
                    tint = EmeraldText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                buildString {
                    append("${lawyer.yearsExperience} years' experience")
                    lawyer.practiceDistrict?.let { append(" · $it") }
                    lawyer.practiceState?.let { append(", $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (lawyer.specializations.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lawyer.specializations.take(3).forEach { Chip(it) }
                }
            }

            if (!lawyer.acceptingQuestions) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Not accepting new requests",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberText
                )
            }
        }
    }
}

/**
 * One lawyer's public profile, with the option to request contact.
 *
 * The screen is explicit that details are not published and that the lawyer
 * decides — so the citizen understands why they cannot simply see a number.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerProfileScreen(
    lawyer: DirectoryLawyerResponse?,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onRequestContact: (lawyerId: String, message: String?) -> Unit,
    onBack: () -> Unit
) {
    var showRequestDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lawyer profile") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (lawyer == null) {
                if (!isLoading) {
                    EmptyState(
                        title = "Profile unavailable",
                        body = "This lawyer's profile could not be loaded."
                    )
                }
                return@Column
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            lawyer.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = EmeraldText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Verified with the ${lawyer.barCouncilState} Bar Council",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldText
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    DetailRow("Experience", "${lawyer.yearsExperience} years")
                    lawyer.enrollmentYear?.let { DetailRow("Enrolled", it.toString()) }
                    lawyer.practiceDistrict?.let {
                        DetailRow("Practises in", "$it, ${lawyer.practiceState ?: ""}")
                    }
                    if (lawyer.languages.isNotEmpty()) {
                        DetailRow("Languages", lawyer.languages.joinToString(", "))
                    }
                    if (lawyer.specializations.isNotEmpty()) {
                        DetailRow("Areas", lawyer.specializations.joinToString(", "))
                    }

                    lawyer.bio?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CivicNavy100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "How contact works",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CivicNavy700
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "We do not publish lawyers' phone numbers or email addresses. " +
                            "Send a request, and if they accept, their details appear " +
                            "under \"My requests\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = CivicNavy700
                    )
                }
            }

            Button(
                onClick = { showRequestDialog = true },
                enabled = lawyer.acceptingQuestions && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (lawyer.acceptingQuestions) {
                        "Request contact details"
                    } else {
                        "Not accepting requests"
                    }
                )
            }
        }
    }

    if (showRequestDialog && lawyer != null) {
        ContactRequestDialog(
            lawyerName = lawyer.fullName,
            isSubmitting = isSubmitting,
            onDismiss = { showRequestDialog = false },
            onConfirm = { message ->
                onRequestContact(lawyer.userId, message)
                showRequestDialog = false
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ContactRequestDialog(
    lawyerName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request contact") },
        text = {
            Column {
                Text(
                    "$lawyerName will see your name and a short note. They can " +
                        "accept or decline.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 500) message = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Briefly, what you need help with.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(message.trim().takeIf { it.isNotBlank() }) },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send request")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * The citizen's own contact requests.
 *
 * Details are rendered only when the server actually supplied them, which it
 * does only for an accepted request.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyContactRequestsScreen(
    requests: List<MyContactRequestResponse>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Column(modifier = Modifier.padding(padding)) {
                EmptyState(
                    title = "No requests yet",
                    body = "Find a lawyer and ask for their contact details."
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            request.lawyer.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${request.lawyer.yearsExperience} years' experience",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))
                        StatusLine(request.status)

                        // Present only when the server released them.
                        if (request.contactEmail != null || request.contactPhone != null) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))

                            request.contactPhone?.let {
                                ContactLine(Icons.Filled.Phone, it)
                            }
                            request.contactEmail?.let {
                                Spacer(Modifier.height(6.dp))
                                ContactLine(Icons.Filled.Email, it)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(status: String) {
    val (text, container, content) = when (status) {
        "ACCEPTED" -> Triple("Accepted — details below", EmeraldContainer, EmeraldText)
        "DECLINED" -> Triple("Declined", AmberContainer, AmberText)
        // BLOCKED is never surfaced as such: the server returns it looking like
        // a pending request, and telling the sender would invite evasion.
        else -> Triple("Waiting for a reply", AmberContainer, AmberText)
    }
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ContactLine(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = CivicNavy700)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
