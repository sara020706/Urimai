package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.ChatMessage

@Composable
fun SchemeDetailScreen(
    matchResult: SchemeMatchResult,
    profile: UserProfile,
    isSaved: Boolean,
    onSaveToggle: () -> Unit,
    onBack: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    // AI Explanation
    aiExplanation: String?,
    isLoadingExplanation: Boolean,
    onReloadExplanation: () -> Unit,
    // Document toggle
    onToggleDocument: (String) -> Unit,
    // Navigation to edit profile for missing criteria
    onNavigateToEditProfile: () -> Unit,
    // AI Q&A Chat
    chatMessages: List<ChatMessage>,
    isChatLoading: Boolean,
    onSendChatMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = matchResult.scheme
    var questionInput by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        "Why do I qualify?",
        "What document am I missing?",
        "How do I apply on the portal?",
        "Explain in simple Tamil"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceLight,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CivicNavy50)
                            .testTag("scheme_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CivicNavy900
                        )
                    }
                    Text(
                        text = "Scheme Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onSaveToggle,
                        modifier = Modifier.size(36.dp).testTag("save_scheme_detail_btn")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isSaved) SaffronPrimary else CivicNavy800
                        )
                    }

                    LanguageSelector(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageChange
                    )
                }
            }
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            StatusBadge(status = matchResult.status)
                        }

                        Text(
                            text = scheme.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CivicNavy900,
                            lineHeight = 26.sp
                        )

                        if (currentLanguage == AppLanguage.TAMIL && scheme.tamilName.isNotBlank()) {
                            Text(
                                text = scheme.tamilName,
                                style = MaterialTheme.typography.titleSmall,
                                color = SaffronDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (currentLanguage == AppLanguage.HINDI && scheme.hindiName.isNotBlank()) {
                            Text(
                                text = scheme.hindiName,
                                style = MaterialTheme.typography.titleSmall,
                                color = SaffronDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = TextSecondaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = scheme.department,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                        }

                        Text(
                            text = scheme.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CivicNavy900,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Benefit Highlight Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CivicNavy900)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Scheme Benefits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = scheme.benefitHighlight,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            scheme.detailedBenefits.forEach { benefit ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("•", color = CivicNavy100, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = benefit,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CivicNavy100,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Plain Language Explanation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SaffronPrimary.copy(alpha = 0.5f)))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(SaffronContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = SaffronDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Urimai AI Plain Language Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicNavy900
                                )
                            }

                            IconButton(
                                onClick = onReloadExplanation,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = CivicNavy700,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isLoadingExplanation) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = SaffronPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Generating personalized explanation...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryLight
                                )
                            }
                        } else {
                            Text(
                                text = aiExplanation ?: matchResult.ruleSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CivicNavy900,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Criteria Breakdown Matrix
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Eligibility Criteria Breakdown (${matchResult.passedCount}/${matchResult.totalEvaluatedCount} Passed)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                    Text(
                        text = "Each condition is strictly evaluated against government rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchResult.criteriaResults.forEach { eval ->
                            CriterionRow(
                                evaluation = eval,
                                onAnswerClicked = { onNavigateToEditProfile() }
                            )
                        }
                    }
                }
            }

            // Document Readiness Checklist
            item {
                DocumentChecklist(
                    requiredDocuments = matchResult.requiredDocuments,
                    ownedDocuments = profile.ownedDocuments,
                    onToggleDocument = onToggleDocument
                )
            }

            // Application Procedure Steps
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = null,
                                tint = CivicNavy800,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "How to Apply",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CivicNavy900
                            )
                        }

                        scheme.applicationSteps.forEachIndexed { index, step ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(CivicNavy800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CivicNavy900,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Official Source & Portal Link Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CivicNavy50),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Official Government Source",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CivicNavy900
                            )
                        }

                        Text(
                            text = "${scheme.officialSourceLabel}\nAdministered by: ${scheme.department}\nLast verified: ${scheme.lastVerifiedDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme.sourceUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("open_official_portal_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CivicNavy800)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Official Website", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Audit Trail Transparency Section
            item {
                AuditTrailSection(matchResult = matchResult, profile = profile)
            }

            // Interactive Q&A Assistant
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
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
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = SaffronDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Ask Urimai Assistant",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicNavy900
                                )
                                Text(
                                    text = "Ask questions specifically about this scheme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryLight,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Quick prompt chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickQuestions) { q ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CivicNavy50)
                                        .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                        .clickable { onSendChatMessage(q) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = q,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CivicNavy800,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Chat Messages Stream
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CivicNavy50)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chatMessages.forEach { msg ->
                                val isUser = msg.sender == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isUser) 12.dp else 2.dp,
                                                    bottomEnd = if (isUser) 2.dp else 12.dp
                                                )
                                            )
                                            .background(if (isUser) CivicNavy800 else SurfaceLight)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isUser) Color.White else CivicNavy900,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            if (isChatLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = SaffronPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Urimai is checking verified rules...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = TextSecondaryLight
                                    )
                                }
                            }
                        }

                        // Input Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = questionInput,
                                onValueChange = { questionInput = it },
                                placeholder = { Text("Ask a question about this scheme...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scheme_chat_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            IconButton(
                                onClick = {
                                    if (questionInput.isNotBlank()) {
                                        onSendChatMessage(questionInput)
                                        questionInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SaffronPrimary)
                                    .testTag("scheme_chat_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = CivicNavy900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Disclaimer Banner at Bottom
            item {
                DisclaimerBanner()
            }
        }
    }
}
