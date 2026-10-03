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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.LawyerProfileResponse
import com.example.data.remote.LawyerVerificationStatus
import com.example.data.remote.UpdateLawyerProfileRequest
import com.example.data.remote.VerificationDocumentResponse
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldText

private val DOC_TYPES = listOf(
    "BAR_COUNCIL_CERTIFICATE" to "Bar Council certificate",
    "ID_PROOF" to "Photo ID",
    "PRACTICE_PROOF" to "Proof of practice",
    "OTHER" to "Other supporting document"
)

private val SPECIALIZATION_OPTIONS = listOf(
    "Welfare Law", "Consumer", "Family", "Property",
    "Employment", "Criminal", "Civil", "Documentation"
)

private val LANGUAGE_OPTIONS = listOf("Tamil", "English", "Hindi", "Telugu", "Malayalam")

/**
 * A lawyer's own verification status and professional details.
 *
 * The status banner is written to be useful rather than reassuring: a pending
 * lawyer is told plainly that they cannot answer questions yet, and a rejected
 * one is shown the reviewer's actual notes. Hiding either would leave someone
 * repeatedly trying a feature that will not work.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerVerificationScreen(
    profile: LawyerProfileResponse?,
    documents: List<VerificationDocumentResponse>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onSave: (UpdateLawyerProfileRequest) -> Unit,
    onPickDocument: (docType: String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    onBack: () -> Unit
) {
    val status = LawyerVerificationStatus.from(profile?.verificationStatus)

    var fullName by remember(profile?.userId) { mutableStateOf(profile?.fullName ?: "") }
    var regNumber by remember(profile?.userId) {
        mutableStateOf(profile?.barCouncilRegNumber ?: "")
    }
    var barState by remember(profile?.userId) {
        mutableStateOf(profile?.barCouncilState ?: "Tamil Nadu")
    }
    var experience by remember(profile?.userId) {
        mutableStateOf(profile?.yearsExperience?.toString() ?: "")
    }
    var practiceDistrict by remember(profile?.userId) {
        mutableStateOf(profile?.practiceDistrict ?: "")
    }
    var bio by remember(profile?.userId) { mutableStateOf(profile?.bio ?: "") }
    var contactEmail by remember(profile?.userId) { mutableStateOf(profile?.contactEmail ?: "") }
    var contactPhone by remember(profile?.userId) { mutableStateOf(profile?.contactPhone ?: "") }
    var specializations by remember(profile?.userId) {
        mutableStateOf(profile?.specializations?.toSet() ?: emptySet())
    }
    var languages by remember(profile?.userId) {
        mutableStateOf(profile?.languages?.toSet() ?: emptySet())
    }

    // Locked once approved: these are the facts the approval rests on, so a
    // change must go back through review rather than being edited in place.
    val identityLocked = status == LawyerVerificationStatus.VERIFIED ||
        status == LawyerVerificationStatus.SUSPENDED

    val canSave = fullName.isNotBlank() && regNumber.isNotBlank() &&
        barState.isNotBlank() && !isSubmitting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Professional verification") },
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
            VerificationStatusBanner(status, profile?.verificationNotes)

            Text(
                "Your details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name as enrolled") },
                singleLine = true,
                enabled = !identityLocked,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = regNumber,
                onValueChange = { regNumber = it },
                label = { Text("Bar Council registration number") },
                singleLine = true,
                enabled = !identityLocked,
                supportingText = {
                    if (identityLocked) Text("Locked after approval. Contact support to change it.")
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = barState,
                onValueChange = { barState = it },
                label = { Text("Bar Council state") },
                singleLine = true,
                enabled = !identityLocked,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = experience,
                onValueChange = { input -> experience = input.filter { it.isDigit() }.take(2) },
                label = { Text("Years of experience") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = practiceDistrict,
                onValueChange = { practiceDistrict = it },
                label = { Text("District where you practise") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Areas of practice", style = MaterialTheme.typography.labelLarge)
            MultiSelectChips(
                options = SPECIALIZATION_OPTIONS,
                selected = specializations,
                onToggle = { value ->
                    specializations = if (value in specializations) {
                        specializations - value
                    } else {
                        specializations + value
                    }
                }
            )

            Text("Languages you work in", style = MaterialTheme.typography.labelLarge)
            MultiSelectChips(
                options = LANGUAGE_OPTIONS,
                selected = languages,
                onToggle = { value ->
                    languages = if (value in languages) languages - value else languages + value
                }
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 1000) bio = it },
                label = { Text("Short professional summary") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text(
                "Contact details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicNavy100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = CivicNavy700,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "These are never shown in the directory. A citizen must send a " +
                            "request, and they only see these details if you accept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CivicNavy700
                    )
                }
            }
            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        UpdateLawyerProfileRequest(
                            fullName = fullName.trim(),
                            barCouncilRegNumber = regNumber.trim(),
                            barCouncilState = barState.trim(),
                            yearsExperience = experience.toIntOrNull() ?: 0,
                            specializations = specializations.toList(),
                            languages = languages.toList(),
                            practiceState = barState.trim(),
                            practiceDistrict = practiceDistrict.trim().ifBlank { null },
                            bio = bio.trim().ifBlank { null },
                            contactEmail = contactEmail.trim().ifBlank { null },
                            contactPhone = contactPhone.trim().ifBlank { null }
                        )
                    )
                },
                enabled = canSave,
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
                    Text("Save details")
                }
            }

            HorizontalDivider()

            Text(
                "Verification documents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Upload your Bar Council certificate and a photo ID. " +
                    "JPEG, PNG or PDF, up to 10 MB each.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            documents.forEach { document ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                DOC_TYPES.firstOrNull { it.first == document.docType }?.second
                                    ?: document.docType,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                document.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!identityLocked) {
                            IconButton(onClick = { onDeleteDocument(document.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            if (identityLocked) {
                Text(
                    "Documents cannot be removed after review; they are the record the " +
                        "decision was based on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DOC_TYPES.forEach { (type, label) ->
                    OutlinedButton(
                        onClick = { onPickDocument(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload $label")
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun VerificationStatusBanner(
    status: LawyerVerificationStatus,
    notes: String?
) {
    val (title, detail, container, content) = when (status) {
        LawyerVerificationStatus.VERIFIED -> Quad(
            "Verified",
            "You can answer citizen questions and appear in the lawyer directory.",
            EmeraldContainer, EmeraldText
        )
        LawyerVerificationStatus.PENDING -> Quad(
            "Awaiting review",
            "An administrator is checking your details. You cannot answer questions " +
                "or appear in the directory until that is complete.",
            AmberContainer, AmberText
        )
        LawyerVerificationStatus.MORE_INFO_REQUESTED -> Quad(
            "More information needed",
            notes ?: "Please add the documents requested, then save to resubmit.",
            AmberContainer, AmberText
        )
        LawyerVerificationStatus.REJECTED -> Quad(
            "Not approved",
            notes ?: "Your application was not approved. You may correct your details " +
                "and save to resubmit.",
            CrimsonContainer, CrimsonText
        )
        LawyerVerificationStatus.SUSPENDED -> Quad(
            "Suspended",
            notes ?: "Your verification is suspended. Contact support for details.",
            CrimsonContainer, CrimsonText
        )
        LawyerVerificationStatus.UNKNOWN -> Quad(
            "Not yet submitted",
            "Fill in your professional details and upload your documents to apply.",
            CivicNavy100, CivicNavy700
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (status == LawyerVerificationStatus.VERIFIED) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Info
                },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = content
                )
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = content)
            }
        }
    }
}

/** Small 4-tuple so the banner can destructure its configuration. */
private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
private fun MultiSelectChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option in selected
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CivicNavy700 else CivicNavy100
                ),
                onClick = { onToggle(option) }
            ) {
                Text(
                    option,
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
