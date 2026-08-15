package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AnalyzingScreen(
    currentStepIndex: Int,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "Reading citizen profile details...",
        "Evaluating age, income, and education criteria...",
        "Checking state and central government rules...",
        "Verifying document readiness requirements...",
        "Preparing personalized recommendations..."
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Central Emblem
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(CivicNavy50),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(CivicNavy800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SaffronPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analyzing Schemes for You",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CivicNavy900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Evaluating against verified central and state eligibility rules",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progressive steps card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                steps.forEachIndexed { index, stepText ->
                    val isDone = index < currentStepIndex
                    val isCurrent = index == currentStepIndex
                    val isPending = index > currentStepIndex

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (isCurrent) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SaffronPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(BorderLight)
                            )
                        }

                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isDone -> CivicNavy900
                                isCurrent -> CivicNavy900
                                else -> TextTertiaryLight
                            },
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
