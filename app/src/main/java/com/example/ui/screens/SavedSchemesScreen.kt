package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.SchemeMatchResult
import com.example.data.model.UserProfile
import com.example.ui.components.CivicHeader
import com.example.ui.components.SchemeCard
import com.example.ui.theme.*

@Composable
fun SavedSchemesScreen(
    savedResults: List<SchemeMatchResult>,
    savedSchemeIds: Set<String>,
    profile: UserProfile,
    onToggleSave: (String) -> Unit,
    onSchemeClick: (String) -> Unit,
    onBack: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val aggregatedDocs = savedResults.flatMap { it.requiredDocuments }.distinctBy { it.name }
    val readyDocs = aggregatedDocs.filter { doc ->
        profile.ownedDocuments.any { owned -> owned.contains(doc.name, ignoreCase = true) || doc.name.contains(owned, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        CivicHeader(
            title = "Saved Schemes",
            subtitle = "${savedResults.size} Bookmarked programs",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            if (savedResults.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = TextTertiaryLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Saved Schemes Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CivicNavy900
                            )
                            Text(
                                text = "Tap the bookmark icon on any scheme card to save it for easy access, document readiness tracking, and follow-up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Aggregated Document Readiness for Saved Schemes
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CivicNavy50),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = CivicNavy800,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Aggregated Document Readiness",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicNavy900
                                )
                            }
                            Text(
                                text = "You have ${readyDocs.size} of ${aggregatedDocs.size} unique documents required across your saved schemes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight,
                                fontSize = 12.sp
                            )
                            LinearProgressIndicator(
                                progress = { if (aggregatedDocs.isNotEmpty()) readyDocs.size.toFloat() / aggregatedDocs.size else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = EmeraldDark,
                                trackColor = CivicNavy100
                            )
                        }
                    }
                }

                items(savedResults, key = { it.scheme.id }) { result ->
                    SchemeCard(
                        matchResult = result,
                        isSaved = true,
                        onSaveToggle = { onToggleSave(result.scheme.id) },
                        onClick = { onSchemeClick(result.scheme.id) },
                        language = language
                    )
                }
            }
        }
    }
}
