package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.AppLanguage
import com.example.data.model.CriterionStatus
import com.example.data.model.EligibilityStatus
import com.example.data.model.SchemeMatchResult
import com.example.ui.theme.*

@Composable
fun SchemeCard(
    matchResult: SchemeMatchResult,
    isSaved: Boolean,
    onSaveToggle: () -> Unit,
    onClick: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val scheme = matchResult.scheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("scheme_card_${scheme.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (matchResult.status) {
                    EligibilityStatus.LIKELY_ELIGIBLE -> EmeraldLight.copy(alpha = 0.6f)
                    EligibilityStatus.MORE_INFO_NEEDED -> AmberPrimary.copy(alpha = 0.5f)
                    EligibilityStatus.NOT_ELIGIBLE -> BorderLight
                }
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Category Pill + Save Button + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CivicNavy100)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = scheme.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicNavy800,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onSaveToggle,
                        modifier = Modifier.size(32.dp).testTag("save_scheme_${scheme.id}")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isSaved) "Saved" else "Save",
                            tint = if (isSaved) SaffronPrimary else TextTertiaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Title & Department
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = scheme.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CivicNavy900,
                    lineHeight = 22.sp
                )

                if (language == AppLanguage.TAMIL && scheme.tamilName.isNotBlank()) {
                    Text(
                        text = scheme.tamilName,
                        style = MaterialTheme.typography.bodySmall,
                        color = SaffronDark,
                        fontWeight = FontWeight.Medium
                    )
                } else if (language == AppLanguage.HINDI && scheme.hindiName.isNotBlank()) {
                    Text(
                        text = scheme.hindiName,
                        style = MaterialTheme.typography.bodySmall,
                        color = SaffronDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = scheme.department,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            // Status Banner
            StatusBadge(status = matchResult.status)

            // Benefit Highlight Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CivicNavy50)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = CivicNavy700,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = scheme.benefitHighlight,
                    style = MaterialTheme.typography.labelSmall,
                    color = CivicNavy900,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Criteria match summary
            val passed = matchResult.criteriaResults.count { it.status == CriterionStatus.PASSED }
            val total = matchResult.totalEvaluatedCount
            val failed = matchResult.criteriaResults.filter { it.status == CriterionStatus.FAILED }
            val missing = matchResult.criteriaResults.filter { it.status == CriterionStatus.MISSING_INFO }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (matchResult.status) {
                            EligibilityStatus.LIKELY_ELIGIBLE -> "✓ All $total criteria passed"
                            EligibilityStatus.MORE_INFO_NEEDED -> "⚠ $passed of $total verified (${missing.size} info missing)"
                            EligibilityStatus.NOT_ELIGIBLE -> "✕ ${failed.size} requirement(s) not met"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (matchResult.status) {
                            EligibilityStatus.LIKELY_ELIGIBLE -> EmeraldDark
                            EligibilityStatus.MORE_INFO_NEEDED -> AmberDark
                            EligibilityStatus.NOT_ELIGIBLE -> CrimsonDark
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "View details",
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicNavy800,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = CivicNavy800,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Why it matches snippet
                Text(
                    text = matchResult.ruleSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }
    }
}
