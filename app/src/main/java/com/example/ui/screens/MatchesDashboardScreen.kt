package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import com.example.ui.theme.EmeraldText
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CloudOff
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberText
import com.example.ui.components.ActionTile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.engine.EligibilityEngine
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MatchesDashboardScreen(
    summary: DashboardSummary,
    matches: List<SchemeMatchResult>,
    profile: UserProfile,
    savedSchemeIds: Set<String>,
    selectedCategory: SchemeCategory,
    onCategorySelect: (SchemeCategory) -> Unit,
    selectedStatusFilter: EligibilityStatus?,
    onStatusFilterSelect: (EligibilityStatus?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onSchemeClick: (String) -> Unit,
    onToggleSaveScheme: (String) -> Unit,
    onEditProfile: () -> Unit,
    onViewSavedSchemes: () -> Unit,
    onExploreHowItWorks: () -> Unit,
    onCheckEligibility: () -> Unit,
    onAskLegalQuestion: () -> Unit,
    onFindLawyer: () -> Unit,
    onAskAssistant: () -> Unit,
    isVerifiedLawyer: Boolean = false,
    isLawyerRole: Boolean = false,
    isAdmin: Boolean = false,
    unreadNotificationCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenVerification: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    isCatalogStale: Boolean = false,
    onOpenLawyerWorkspace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CivicNavy800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = SaffronPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Urimai",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Eligible Scheme Matches",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = onOpenNotifications) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(containerColor = SaffronPrimary) {
                                            Text("$unreadNotificationCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = CivicNavy800
                                )
                            }
                        }

                        IconButton(
                            onClick = onViewSavedSchemes,
                            modifier = Modifier.size(36.dp).testTag("saved_schemes_nav")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (savedSchemeIds.isNotEmpty()) {
                                        Badge(containerColor = SaffronPrimary) {
                                            Text("${savedSchemeIds.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Saved Schemes",
                                    tint = CivicNavy800
                                )
                            }
                        }

                        LanguageSelector(
                            currentLanguage = currentLanguage,
                            onLanguageSelected = onLanguageChange
                        )

                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier.size(36.dp).testTag("profile_icon_nav")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = CivicNavy800
                            )
                        }
                    }
                }

                // Profile Summary Bar with Quick Edit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CivicNavy50)
                        .clickable { onEditProfile() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CivicNavy700,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${profile.name} • ${profile.age ?: "?"} yrs • ${profile.state} • ${profile.annualIncome?.let { EligibilityEngine.formatInr(it) } ?: "Income not set"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SaffronDark
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SaffronDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Search and Filters
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)
        ) {
            // Check Eligibility CTA
            item {
                Button(
                    onClick = onCheckEligibility,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_eligibility_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = CivicNavy900)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check Eligibility", fontWeight = FontWeight.Bold)
                }
            }

            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scheme_search_input"),
                    placeholder = { Text("Search by scheme name, ministry, keyword...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = CivicNavy700,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }

            // Summary Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricSummaryCard(
                        count = summary.likelyEligibleCount,
                        label = "Likely Eligible",
                        bgColor = EmeraldContainer,
                        textColor = EmeraldText,
                        icon = Icons.Default.CheckCircle,
                        isSelected = selectedStatusFilter == EligibilityStatus.LIKELY_ELIGIBLE,
                        onClick = {
                            onStatusFilterSelect(
                                if (selectedStatusFilter == EligibilityStatus.LIKELY_ELIGIBLE) null else EligibilityStatus.LIKELY_ELIGIBLE
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MetricSummaryCard(
                        count = summary.moreInfoNeededCount,
                        label = "Needs Info",
                        bgColor = AmberContainer,
                        textColor = AmberText,
                        icon = Icons.Default.HelpOutline,
                        isSelected = selectedStatusFilter == EligibilityStatus.MORE_INFO_NEEDED,
                        onClick = {
                            onStatusFilterSelect(
                                if (selectedStatusFilter == EligibilityStatus.MORE_INFO_NEEDED) null else EligibilityStatus.MORE_INFO_NEEDED
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MetricSummaryCard(
                        count = summary.missingDocumentsCount,
                        label = "Docs Needed",
                        bgColor = CivicNavy50,
                        textColor = CivicNavy900,
                        icon = Icons.Default.Description,
                        isSelected = false,
                        // Read-only: there is no "missing documents" filter, and
                        // a tile that looks tappable but is not is worse than
                        // one that plainly is not.
                        onClick = null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Chips Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy800
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(SchemeCategory.entries) { cat ->
                            val isSelected = cat == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) CivicNavy800 else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (isSelected) CivicNavy800 else MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                    .clickable { onCategorySelect(cat) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("cat_${cat.name}")
                            ) {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else CivicNavy900
                                )
                            }
                        }
                    }
                }
            }

            // Status Filter Clear Banner (if active)
            if (selectedStatusFilter != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CivicNavy100)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtered by: ${selectedStatusFilter.label}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Clear filter",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SaffronDark,
                            modifier = Modifier.clickable { onStatusFilterSelect(null) }
                        )
                    }
                }
            }

            // Schemes List Header
item {
    if (isCatalogStale) {
        // Scheme rules change. A citizen reading cached data deserves to know
        // the verdicts may be out of date rather than discovering it later.
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = AmberContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = AmberText
                )
                Text(
                    "Showing saved scheme information. It may be out of date until " +
                        "you are back online.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberText
                )
            }
        }
    }
}

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matching Schemes (${matches.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Why this matches?",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SaffronDark,
                        modifier = Modifier.clickable { onExploreHowItWorks() }
                    )
                }
            }

            // Schemes List
            if (matches.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No matching schemes found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Try clearing filters or updating your profile criteria.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(matches, key = { it.scheme.id }) { matchResult ->
                    SchemeCard(
                        matchResult = matchResult,
                        isSaved = savedSchemeIds.contains(matchResult.scheme.id),
                        onSaveToggle = { onToggleSaveScheme(matchResult.scheme.id) },
                        onClick = { onSchemeClick(matchResult.scheme.id) },
                        language = currentLanguage
                    )
                }
            }

            // Legal help sits BELOW the schemes, not above them. The screen
            // promises scheme matches; a citizen should reach those first and
            // find legal help as the natural next step when a scheme does not
            // work out.
            item {
                LegalHelpSection(
                    onAskQuestion = onAskLegalQuestion,
                    onFindLawyer = onFindLawyer,
                    onAskAssistant = onAskAssistant
                )
            }

            // Disclaimer Banner at Bottom
            item {
                DisclaimerBanner()
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    count: Int,
    label: String,
    bgColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    /** Null for a read-only tile: no ripple, and not announced as a button. */
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("metric_${label.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(textColor)) else null
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


/**
 * Legal help, for citizens.
 *
 * Lawyer and admin entries used to render here too, which meant a citizen
 * dashboard carried three other products' entry points. Those roles now land on
 * their own workspaces with their own navigation, so the branches were dead
 * code that still cost a reader's attention.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LegalHelpSection(
    onAskQuestion: () -> Unit,
    onFindLawyer: () -> Unit,
    onAskAssistant: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Need legal help?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = CivicNavy800
        )

        // The assistant is first because it is the fastest answer and costs a
        // lawyer nothing. The two human paths stay directly beneath it, so
        // nothing about asking a real lawyer becomes harder to find.
        ActionTile(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = stringResource(R.string.legal_help_chat_title),
            subtitle = stringResource(R.string.legal_help_chat_subtitle),
            onClick = onAskAssistant
        )
        ActionTile(
            icon = Icons.Default.Lock,
            title = "Ask a question anonymously",
            subtitle = "Verified lawyers reply. Your name is never shown.",
            onClick = onAskQuestion
        )
        ActionTile(
            icon = Icons.Default.Search,
            title = "Find a verified lawyer",
            subtitle = "Search by area of practice, language and location.",
            onClick = onFindLawyer
        )
    }
}
