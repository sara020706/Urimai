package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UploadedDocumentEntity
import com.example.data.model.SchemeDocument
import com.example.ui.theme.*

@Composable
fun DocumentChecklist(
    requiredDocuments: List<SchemeDocument>,
    ownedDocuments: Set<String>,
    onToggleDocument: (String) -> Unit,
    uploadedDocuments: List<UploadedDocumentEntity> = emptyList(),
    onUploadDocument: (documentName: String, fileUri: String, fileName: String, mimeType: String?) -> Unit = { _, _, _, _ -> },
    onRemoveUpload: (UploadedDocumentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val readyDocs = requiredDocuments.filter { doc ->
        ownedDocuments.any { owned -> owned.contains(doc.name, ignoreCase = true) || doc.name.contains(owned, ignoreCase = true) }
    }
    val missingDocs = requiredDocuments.filter { doc ->
        ownedDocuments.none { owned -> owned.contains(doc.name, ignoreCase = true) || doc.name.contains(owned, ignoreCase = true) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = CivicNavy800,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Document Readiness",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (missingDocs.isEmpty()) EmeraldContainer else AmberContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${readyDocs.size}/${requiredDocuments.size} Ready",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (missingDocs.isEmpty()) EmeraldText else AmberText
                    )
                }
            }

            // Readiness summary banner
            if (missingDocs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberContainer)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "You may need ${missingDocs.size} additional document(s) before applying.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "Check off documents you currently have available:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )

            // Document items list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                requiredDocuments.forEach { doc ->
                    val isOwned = ownedDocuments.any { owned ->
                        owned.contains(doc.name, ignoreCase = true) || doc.name.contains(owned, ignoreCase = true)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isOwned) EmeraldContainer.copy(alpha = 0.3f) else CivicNavy50)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isOwned,
                            onCheckedChange = { onToggleDocument(doc.name) },
                            modifier = Modifier.testTag("doc_check_${doc.id}"),
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldDark,
                                checkmarkColor = SurfaceLight
                            )
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = doc.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isOwned) FontWeight.SemiBold else FontWeight.Medium,
                                    color = CivicNavy900
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (doc.isMandatoryForEligibility) SaffronContainer else CivicNavy100)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (doc.isMandatoryForEligibility) "Mandatory" else "Optional",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = if (doc.isMandatoryForEligibility) SaffronText else CivicNavy800
                                    )
                                }
                            }
                            Text(
                                text = "Stage: ${doc.stage} • ${doc.tip}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = TextTertiaryLight
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val existingUpload = uploadedDocuments.firstOrNull { it.documentName == doc.name }
                            DocumentUploadRow(
                                documentName = doc.name,
                                existingUpload = existingUpload,
                                onUpload = { fileUri, fileName, mimeType ->
                                    onUploadDocument(doc.name, fileUri, fileName, mimeType)
                                },
                                onRemove = onRemoveUpload
                            )
                        }
                    }
                }
            }
        }
    }
}
