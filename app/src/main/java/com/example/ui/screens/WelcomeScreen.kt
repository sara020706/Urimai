package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.UserProfile
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.LanguageSelector
import com.example.ui.components.TrustPill
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onStartEligibilityCheck: () -> Unit,
    onExploreHowItWorks: () -> Unit,
    onSelectDemoProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top App Bar / Brand + Language Selector
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CivicNavy900, CivicNavy700)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Urimai Logo",
                        tint = SaffronPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Urimai",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                    Text(
                        text = "உரிமை • नागरिक अधिकार",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryLight,
                        fontSize = 10.sp
                    )
                }
            }

            LanguageSelector(
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageChange
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hero Graphic Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CivicNavy900),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(SaffronContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SaffronDark,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = when (currentLanguage) {
                        AppLanguage.TAMIL -> "உங்களுக்குரிய அரசு நலத்திட்டங்களை கண்டறியுங்கள்."
                        AppLanguage.HINDI -> "सरकारी योजनाओं की पात्रता आसानी से समझें।"
                        AppLanguage.ENGLISH -> "Find government support that's relevant to you."
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Text(
                    text = "Tell us a little about yourself. We'll help you discover government schemes and understand their eligibility requirements in simple language.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CivicNavy100,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Trust Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TrustPill(text = "Personalized", icon = Icons.Default.PersonSearch)
            TrustPill(text = "Easy to understand", icon = Icons.Default.Translate)
            TrustPill(text = "Source-backed", icon = Icons.Default.Verified)
        }

        // CTAs
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartEligibilityCheck,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("check_eligibility_cta"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) {
                Text(
                    text = "Check My Eligibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            OutlinedButton(
                onClick = onExploreHowItWorks,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("how_it_works_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CivicNavy800),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explore how it works", fontWeight = FontWeight.SemiBold)
            }
        }

        // Quick Test Scenarios Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = CivicNavy800,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Instant Demo Scenarios:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DemoChip(
                        name = "Arun (Student)",
                        desc = "21 yrs • TN",
                        onClick = { onSelectDemoProfile(UserProfile.DEMO_ARUN_STUDENT) },
                        modifier = Modifier.weight(1f)
                    )
                    DemoChip(
                        name = "Meena (Farmer)",
                        desc = "32 yrs • Madurai",
                        onClick = { onSelectDemoProfile(UserProfile.DEMO_MEENA_FARMER) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DemoChip(
                        name = "Rajesh (Business)",
                        desc = "29 yrs • Bengaluru",
                        onClick = { onSelectDemoProfile(UserProfile.DEMO_RAJESH_ENTREPRENEUR) },
                        modifier = Modifier.weight(1f)
                    )
                    DemoChip(
                        name = "Priya (Missing info)",
                        desc = "23 yrs • Needs review",
                        onClick = { onSelectDemoProfile(UserProfile.DEMO_PRIYA_PARTIAL_DATA) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Disclaimer Note
        DisclaimerBanner()
    }
}

@Composable
private fun DemoChip(
    name: String,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CivicNavy50)
            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CivicNavy900
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = TextSecondaryLight
            )
        }
    }
}
