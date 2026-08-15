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
import com.example.data.model.CriterionEvaluation
import com.example.data.model.CriterionStatus
import com.example.ui.theme.*

@Composable
fun CriterionRow(
    evaluation: CriterionEvaluation,
    onAnswerClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val criterion = evaluation.criterion

    val (bgColor, iconColor, statusIcon, statusTitle) = when (evaluation.status) {
        CriterionStatus.PASSED -> Quad(
            EmeraldContainer.copy(alpha = 0.5f),
            EmeraldDark,
            Icons.Default.CheckCircle,
            "Passed"
        )
        CriterionStatus.FAILED -> Quad(
            CrimsonContainer.copy(alpha = 0.5f),
            CrimsonDark,
            Icons.Default.Cancel,
            "Not Met"
        )
        CriterionStatus.MISSING_INFO -> Quad(
            AmberContainer.copy(alpha = 0.5f),
            AmberDark,
            Icons.Default.HelpOutline,
            "Needs Info"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when (evaluation.status) {
                    CriterionStatus.PASSED -> EmeraldLight.copy(alpha = 0.4f)
                    CriterionStatus.FAILED -> CrimsonPrimary.copy(alpha = 0.3f)
                    CriterionStatus.MISSING_INFO -> AmberPrimary.copy(alpha = 0.4f)
                }
            )
        )
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .clickable { expanded = !expanded },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusTitle,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = criterion.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Required vs You Comparison Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceLight.copy(alpha = 0.8f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Required:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = criterion.requirementDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CivicNavy900
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your info:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = evaluation.userValueDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (evaluation.status == CriterionStatus.FAILED) CrimsonDark else CivicNavy900
                    )
                }

                if (evaluation.failureReason != null) {
                    Text(
                        text = "⚠ ${evaluation.failureReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonDark,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // If missing info, show interactive CTA button
            if (evaluation.status == CriterionStatus.MISSING_INFO && onAnswerClicked != null) {
                Button(
                    onClick = onAnswerClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Answer this question", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Expandable "Why do we ask this?" Section
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CivicNavy700,
                            modifier = Modifier.size(14.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Why do we ask this?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CivicNavy800
                            )
                            Text(
                                text = criterion.whyWeAskReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight,
                                fontSize = 11.sp
                            )
                            Text(
                                text = criterion.explanationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiaryLight,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
