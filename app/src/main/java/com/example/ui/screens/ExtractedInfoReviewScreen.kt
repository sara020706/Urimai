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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.data.remote.ExtractProfileResponse
import com.example.data.remote.ExtractionConfidence
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.theme.CivicNavy100
import com.example.ui.theme.CivicNavy700
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText

/**
 * Review what was read from a certificate before any of it reaches the profile.
 *
 * Nothing here is applied automatically. OCR misreads often enough that a wrong
 * income or category figure would silently sit behind an eligibility verdict,
 * and the citizen would have no reason to doubt it. So every field arrives
 * unchecked, with the extracted value shown beside the value it would replace,
 * and the citizen decides field by field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractedInfoReviewScreen(
    extraction: ExtractProfileResponse?,
    currentProfile: UserProfile,
    isLoading: Boolean,
    onApply: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    // Which fields the citizen has accepted, and the possibly-edited value.
    val accepted = remember(extraction) { mutableStateMapOf<String, Boolean>() }
    val edited = remember(extraction) { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_check_details)) },
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
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Reading your document…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Column
            }

            if (extraction == null) {
                EmptyState(
                    title = "Could not read that document",
                    body = "Try a clearer photo, or enter your details by hand."
                )
                return@Column
            }

            val fields = buildFieldList(extraction, currentProfile)
            val confidence = ExtractionConfidence.from(extraction.confidence)

            ConfidenceBanner(confidence, extraction.documentType)

            if (fields.isEmpty()) {
                EmptyState(
                    title = "Nothing usable found",
                    body = "We could not read any profile details from this document. " +
                        "You can still enter them by hand."
                )
                return@Column
            }

            Text(
                "Tick only what is correct",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Nothing is saved until you choose. Correct anything that is wrong — " +
                    "these details decide which schemes you are shown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            fields.forEach { field ->
                ExtractedFieldRow(
                    field = field,
                    isAccepted = accepted[field.key] ?: false,
                    editedValue = edited[field.key] ?: field.extracted,
                    onToggle = { accepted[field.key] = it },
                    onEdit = { edited[field.key] = it }
                )
            }

            HorizontalDivider()

            val acceptedCount = fields.count { accepted[it.key] == true }

            Button(
                onClick = {
                    var updated = currentProfile
                    fields.forEach { field ->
                        if (accepted[field.key] == true) {
                            val value = edited[field.key] ?: field.extracted
                            updated = field.apply(updated, value)
                        }
                    }
                    onApply(updated)
                },
                enabled = acceptedCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    when (acceptedCount) {
                        0 -> "Select what to save"
                        1 -> "Save 1 detail"
                        else -> "Save $acceptedCount details"
                    }
                )
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Discard and go back")
            }
        }
    }
}

@Composable
private fun ConfidenceBanner(confidence: ExtractionConfidence, documentType: String?) {
    val (container, content, message) = when (confidence) {
        ExtractionConfidence.HIGH -> Triple(
            CivicNavy100, CivicNavy700,
            "The document was clear. Please still check each value."
        )
        ExtractionConfidence.MEDIUM -> Triple(
            AmberContainer, AmberText,
            "Some of this was hard to read. Check each value carefully."
        )
        else -> Triple(
            CrimsonContainer, CrimsonText,
            "The scan was unclear, so these may well be wrong. " +
                "A clearer photo would give a better result."
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (confidence == ExtractionConfidence.HIGH) {
                    Icons.Filled.Info
                } else {
                    Icons.Filled.Warning
                },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp)
            )
            Column {
                documentType?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = content
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(message, style = MaterialTheme.typography.bodySmall, color = content)
            }
        }
    }
}

@Composable
private fun ExtractedFieldRow(
    field: ExtractedField,
    isAccepted: Boolean,
    editedValue: String,
    onToggle: (Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAccepted, onCheckedChange = onToggle)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        field.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Showing what it replaces makes a silent overwrite visible.
                    Text(
                        "Currently: ${field.current}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isAccepted) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = onEdit,
                    label = { Text("Value to save") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Read from document: ${field.extracted}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** One reviewable field: what was read, what it replaces, and how to apply it. */
private data class ExtractedField(
    val key: String,
    val label: String,
    val extracted: String,
    val current: String,
    val apply: (UserProfile, String) -> UserProfile
)

/**
 * Build the review list, skipping anything the extractor did not return.
 *
 * A field the model had no opinion about must not appear as a blank row the
 * citizen might tick by accident.
 */
private fun buildFieldList(
    extraction: ExtractProfileResponse,
    profile: UserProfile
): List<ExtractedField> {
    val f = extraction.fields
    val out = mutableListOf<ExtractedField>()

    f.name?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField("name", "Name", it, profile.name) { p, v -> p.copy(name = v) }
    }
    f.age?.let {
        out += ExtractedField(
            "age", "Age", it.toString(), profile.age?.toString() ?: "Not set"
        ) { p, v -> p.copy(age = v.toIntOrNull() ?: p.age) }
    }
    f.gender?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField("gender", "Gender", it, profile.gender) { p, v ->
            p.copy(gender = v)
        }
    }
    f.state?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField("state", "State", it, profile.state) { p, v -> p.copy(state = v) }
    }
    f.district?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField("district", "District", it, profile.district) { p, v ->
            p.copy(district = v)
        }
    }
    f.annualIncome?.let {
        out += ExtractedField(
            "income",
            "Annual family income",
            formatRupees(it),
            profile.annualIncome?.let(::formatRupees) ?: "Not set"
        ) { p, v ->
            // Accept a typed-in figure with separators or a rupee sign.
            val digits = v.filter { ch -> ch.isDigit() }
            p.copy(annualIncome = digits.toLongOrNull() ?: p.annualIncome)
        }
    }
    f.occupation?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField("occupation", "Occupation", it, profile.occupation) { p, v ->
            p.copy(occupation = v)
        }
    }
    f.education?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField("education", "Education", it, profile.education) { p, v ->
            p.copy(education = v)
        }
    }
    f.familySize?.let {
        out += ExtractedField(
            "familySize", "Family size", it.toString(), profile.familySize.toString()
        ) { p, v -> p.copy(familySize = v.toIntOrNull() ?: p.familySize) }
    }
    f.socialCategory?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField(
            "socialCategory", "Social category", it, profile.socialCategory ?: "Not set"
        ) { p, v -> p.copy(socialCategory = v) }
    }
    f.disabilityStatus?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField(
            "disabilityStatus", "Disability status", it, profile.disabilityStatus ?: "Not set"
        ) { p, v -> p.copy(disabilityStatus = v) }
    }
    f.maritalStatus?.takeIf { it.isNotBlank() }?.let {
        out += ExtractedField(
            "maritalStatus", "Marital status", it, profile.maritalStatus ?: "Not set"
        ) { p, v -> p.copy(maritalStatus = v) }
    }

    return out
}

/** Indian digit grouping, so a figure reads the way the citizen wrote it. */
private fun formatRupees(amount: Long): String = try {
    java.text.NumberFormat
        .getCurrencyInstance(java.util.Locale("en", "IN"))
        .format(amount)
} catch (_: Exception) {
    "₹$amount"
}
