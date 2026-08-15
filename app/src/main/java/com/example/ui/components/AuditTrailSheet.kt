package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun AuditTrailSection(
    matchResult: SchemeMatchResult,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CivicNavy50),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clickable { expanded = !expanded },
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = CivicNavy800,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "How did we reach this result?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle audit trail",
                    tint = CivicNavy700,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Tap to inspect the transparent step-by-step evaluation trace.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = BorderLight)

                    // Step 1: Citizen Profile snapshot
                    TraceStepCard(
                        stepNum = "1",
                        title = "Your Profile Snapshot",
                        subtitle = "${profile.age ?: "Age ?"} yrs • ${profile.gender} • ${profile.state} • ${profile.education} • ${profile.occupation}",
                        status = "Captured"
                    )

                    // Step 2: Scheme Rules
                    TraceStepCard(
                        stepNum = "2",
                        title = "Verified Scheme Rules",
                        subtitle = "${matchResult.scheme.criteria.size} statutory conditions retrieved from official department rules",
                        status = "Loaded"
                    )

                    // Step 3: Exact Evaluation Checks
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceLight)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Step 3: Deterministic Rule Matrix",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CivicNavy900
                        )

                        matchResult.criteriaResults.forEach { eval ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = eval.criterion.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CivicNavy800
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (eval.status) {
                                            CriterionStatus.PASSED -> Icons.Default.CheckCircle
                                            CriterionStatus.FAILED -> Icons.Default.Cancel
                                            CriterionStatus.MISSING_INFO -> Icons.Default.HelpOutline
                                        },
                                        contentDescription = null,
                                        tint = when (eval.status) {
                                            CriterionStatus.PASSED -> EmeraldDark
                                            CriterionStatus.FAILED -> CrimsonDark
                                            CriterionStatus.MISSING_INFO -> AmberDark
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = when (eval.status) {
                                            CriterionStatus.PASSED -> "Passed"
                                            CriterionStatus.FAILED -> "Failed"
                                            CriterionStatus.MISSING_INFO -> "Missing"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (eval.status) {
                                            CriterionStatus.PASSED -> EmeraldDark
                                            CriterionStatus.FAILED -> CrimsonDark
                                            CriterionStatus.MISSING_INFO -> AmberDark
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Step 4: Final Result
                    TraceStepCard(
                        stepNum = "4",
                        title = "Final Outcome",
                        subtitle = matchResult.status.label,
                        status = "Calculated"
                    )

                    // Responsible AI Principle Guarantee
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SaffronContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = SaffronDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Responsible AI Architecture: AI was used exclusively to understand, summarize, and explain your information in plain language. The eligibility result was deterministically calculated by the structured rule engine above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SaffronText,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceStepCard(
    stepNum: String,
    title: String,
    subtitle: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceLight)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(CivicNavy800),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = CivicNavy900
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(CivicNavy100)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = CivicNavy900,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
