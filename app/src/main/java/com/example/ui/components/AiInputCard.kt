package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.engine.EligibilityEngine
import com.example.ui.theme.*

@Composable
fun AiInputCard(
    inputText: String,
    onInputChange: (String) -> Unit,
    onExtract: () -> Unit,
    isLoading: Boolean,
    extractedProfile: UserProfile?,
    onApplyExtracted: () -> Unit,
    onCancelExtracted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val samplePrompts = listOf(
        "I'm 21, studying engineering in Chennai. Family income is ₹2 lakh.",
        "I'm a 32-year-old female farmer in Madurai with ₹1.8L annual income.",
        "I'm 29, starting a micro business in Bengaluru with Diploma.",
        "23 y/o female student in Coimbatore, SC category, ₹2.2L income."
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaffronPrimary.copy(alpha = 0.4f)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SaffronContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = SaffronDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI Quick Profile Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                    Text(
                        text = "Describe yourself in your own words",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
            }

            // Quick Example Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Try an example:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiaryLight
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(samplePrompts) { prompt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CivicNavy50)
                                .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                .clickable { onInputChange(prompt) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = CivicNavy800,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_natural_input"),
                placeholder = {
                    Text(
                        "e.g., I'm a 21-year-old undergraduate student from Chennai with a family income of around ₹2 lakh.",
                        fontSize = 13.sp,
                        color = TextTertiaryLight
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaffronPrimary,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = CivicNavy50.copy(alpha = 0.3f),
                    unfocusedContainerColor = CivicNavy50.copy(alpha = 0.3f)
                )
            )

            // Extract Action Button
            Button(
                onClick = onExtract,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("ai_extract_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronPrimary,
                    disabledContainerColor = SaffronPrimary.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Understanding your profile...")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use AI to Fill My Profile", fontWeight = FontWeight.Bold)
                }
            }

            // Extracted Profile Confirmation Dialog / Box
            AnimatedVisibility(visible = extractedProfile != null) {
                if (extractedProfile != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "AI Extracted Attributes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldText
                                )
                            }

                            // Extracted Attributes grid
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ExtractedAttributeRow("Age", "${extractedProfile.age ?: "Not specified"} years")
                                ExtractedAttributeRow("Location", "${extractedProfile.district}, ${extractedProfile.state}")
                                ExtractedAttributeRow("Occupation", extractedProfile.occupation)
                                ExtractedAttributeRow("Education", extractedProfile.education)
                                ExtractedAttributeRow(
                                    "Annual Income",
                                    extractedProfile.annualIncome?.let { EligibilityEngine.formatInr(it) } ?: "Not specified"
                                )
                                ExtractedAttributeRow(
                                    "Status",
                                    listOfNotNull(
                                        if (extractedProfile.isStudent == true) "Student" else null,
                                        if (extractedProfile.isFarmer == true) "Farmer" else null,
                                        if (extractedProfile.isBusinessOwner == true) "Business Owner" else null
                                    ).joinToString(", ").ifBlank { "Individual" }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCancelExtracted,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryLight)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Discard")
                                }
                                Button(
                                    onClick = onApplyExtracted,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("confirm_extracted_profile"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply Profile", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractedAttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldText.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = EmeraldText
        )
    }
}
