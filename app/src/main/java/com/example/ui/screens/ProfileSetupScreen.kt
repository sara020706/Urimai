package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.engine.EligibilityEngine
import com.example.ui.components.AiInputCard
import com.example.ui.components.CivicHeader
import com.example.ui.components.PrivacyNoticeCard
import com.example.ui.theme.*

@Composable
fun ProfileSetupScreen(
    currentStep: Int,
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onStepChange: (Int) -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onSubmitProfile: () -> Unit,
    // AI Natural language extraction bindings
    aiInputText: String,
    onAiInputChange: (String) -> Unit,
    onAiExtract: () -> Unit,
    isAiExtracting: Boolean,
    extractedProfilePreview: UserProfile?,
    onApplyExtracted: () -> Unit,
    onCancelExtracted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        CivicHeader(
            title = "Profile Setup",
            subtitle = "Step $currentStep of 4 • ${getStepTitle(currentStep)}",
            onBack = if (currentStep > 1) onPreviousStep else null
        )

        // Progress Bar Indicator
        LinearProgressIndicator(
            progress = { currentStep / 4f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = SaffronPrimary,
            trackColor = CivicNavy100
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optional AI Assistant Card (Show prominently at step 1)
            if (currentStep == 1) {
                AiInputCard(
                    inputText = aiInputText,
                    onInputChange = onAiInputChange,
                    onExtract = onAiExtract,
                    isLoading = isAiExtracting,
                    extractedProfile = extractedProfilePreview,
                    onApplyExtracted = onApplyExtracted,
                    onCancelExtracted = onCancelExtracted
                )
            }

            // Step Forms
            when (currentStep) {
                1 -> SectionAboutYou(profile = profile, onProfileChange = onProfileChange)
                2 -> SectionFinancial(profile = profile, onProfileChange = onProfileChange)
                3 -> SectionSituation(profile = profile, onProfileChange = onProfileChange)
                4 -> SectionAdditional(profile = profile, onProfileChange = onProfileChange)
            }

            PrivacyNoticeCard()

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Bottom Fixed Navigation Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = SurfaceLight
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = onPreviousStep,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("step_prev_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CivicNavy800)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < 4) {
                            onNextStep()
                        } else {
                            onSubmitProfile()
                        }
                    },
                    modifier = Modifier
                        .weight(if (currentStep > 1) 1.5f else 1f)
                        .height(48.dp)
                        .testTag("step_next_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (currentStep == 4) EmeraldDark else CivicNavy800)
                ) {
                    Text(
                        text = if (currentStep < 4) "Continue to Step ${currentStep + 1}" else "Evaluate Eligibility",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (currentStep == 4) Icons.Default.FactCheck else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getStepTitle(step: Int): String {
    return when (step) {
        1 -> "About You"
        2 -> "Financial Situation"
        3 -> "Your Situation"
        4 -> "Additional Information"
        else -> ""
    }
}

// ----------------------------------------------------
// SECTION 1: ABOUT YOU
// ----------------------------------------------------
@Composable
private fun SectionAboutYou(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Personal Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicNavy900
            )

            // Age Field
            OutlinedTextField(
                value = profile.age?.toString() ?: "",
                onValueChange = { str ->
                    val num = str.filter { it.isDigit() }.toIntOrNull()
                    onProfileChange(profile.copy(age = num))
                },
                label = { Text("Age") },
                placeholder = { Text("e.g. 21") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_age"),
                shape = RoundedCornerShape(10.dp)
            )

            // Gender Segmented Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                val genders = listOf("Male", "Female", "Other", "Prefer not to say")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    genders.forEach { g ->
                        val isSelected = profile.gender.equals(g, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CivicNavy800 else CivicNavy50)
                                .border(1.dp, if (isSelected) CivicNavy800 else BorderLight, RoundedCornerShape(8.dp))
                                .clickable { onProfileChange(profile.copy(gender = g)) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = g,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else CivicNavy900,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // State Selection
            val states = listOf("Tamil Nadu", "Karnataka", "Kerala", "Maharashtra", "Telangana", "Delhi", "Uttar Pradesh", "Andhra Pradesh")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "State of Residence",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(states) { st ->
                        val isSelected = profile.state.equals(st, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CivicNavy800 else CivicNavy50)
                                .border(1.dp, if (isSelected) CivicNavy800 else BorderLight, RoundedCornerShape(20.dp))
                                .clickable { onProfileChange(profile.copy(state = st)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = st,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.White else CivicNavy900,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // District Input
            OutlinedTextField(
                value = profile.district,
                onValueChange = { onProfileChange(profile.copy(district = it)) },
                label = { Text("District / City") },
                placeholder = { Text("e.g. Chennai, Madurai, Coimbatore") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_district"),
                shape = RoundedCornerShape(10.dp)
            )

            // Education Level
            val educations = listOf("Below 10th", "10th Pass", "12th Pass", "Diploma", "Undergraduate", "Postgraduate", "Doctorate")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Highest Education Level",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(educations) { edu ->
                        val isSelected = profile.education.equals(edu, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CivicNavy800 else CivicNavy50)
                                .border(1.dp, if (isSelected) CivicNavy800 else BorderLight, RoundedCornerShape(8.dp))
                                .clickable { onProfileChange(profile.copy(education = edu)) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = edu,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.White else CivicNavy900,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Sensitive / Why we ask explainer
            WhyWeAskRow(
                title = "Why do we ask location & education?",
                explanation = "Many government scholarships and state-specific subsidies are reserved strictly for residents or students enrolled in accredited institutions."
            )
        }
    }
}

// ----------------------------------------------------
// SECTION 2: FINANCIAL SITUATION
// ----------------------------------------------------
@Composable
private fun SectionFinancial(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Household Financial Situation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicNavy900
            )

            // Annual Income Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Annual Family Income (Total)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                OutlinedTextField(
                    value = profile.annualIncome?.toString() ?: "",
                    onValueChange = { str ->
                        val num = str.filter { it.isDigit() }.toLongOrNull()
                        onProfileChange(profile.copy(annualIncome = num))
                    },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("200000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_income"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Quick Presets
                val presets = listOf(
                    150000L to "₹1.5 Lakh",
                    200000L to "₹2 Lakh",
                    250000L to "₹2.5 Lakh",
                    350000L to "₹3.5 Lakh",
                    500000L to "₹5 Lakh"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { (amount, label) ->
                        val isSelected = profile.annualIncome == amount
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) SaffronContainer else CivicNavy50)
                                .border(1.dp, if (isSelected) SaffronPrimary else BorderLight, RoundedCornerShape(16.dp))
                                .clickable { onProfileChange(profile.copy(annualIncome = amount)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) SaffronText else CivicNavy900,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Family Size Stepper
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Total Family Members (Household Size)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${profile.familySize} Members",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (profile.familySize > 1) {
                                    onProfileChange(profile.copy(familySize = profile.familySize - 1))
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CivicNavy100)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = CivicNavy800)
                        }
                        IconButton(
                            onClick = {
                                onProfileChange(profile.copy(familySize = profile.familySize + 1))
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CivicNavy800)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                        }
                    }
                }
            }

            WhyWeAskRow(
                title = "Why do we ask income & family size?",
                explanation = "Welfare schemes like housing grants, fee subsidies, and pension allowances use family income caps (e.g. ≤ ₹2.5L) to prioritize economically weaker households."
            )
        }
    }
}

// ----------------------------------------------------
// SECTION 3: YOUR SITUATION
// ----------------------------------------------------
@Composable
private fun SectionSituation(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Your Current Situation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicNavy900
            )

            SituationToggleCard(
                title = "Are you currently a student?",
                subtitle = "Enrolled in school, polytechnic, college, or university",
                value = profile.isStudent,
                onValueChange = { onProfileChange(profile.copy(isStudent = it)) }
            )

            SituationToggleCard(
                title = "Are you currently employed?",
                subtitle = "Holding formal or informal salaried employment",
                value = profile.isEmployed,
                onValueChange = { onProfileChange(profile.copy(isEmployed = it)) }
            )

            SituationToggleCard(
                title = "Are you a practicing farmer?",
                subtitle = "Cultivating agricultural land or smallholder",
                value = profile.isFarmer,
                onValueChange = { onProfileChange(profile.copy(isFarmer = it)) }
            )

            SituationToggleCard(
                title = "Do you own a business or aspire to start one?",
                subtitle = "Micro-enterprise, shop, trade, or startup entrepreneur",
                value = profile.isBusinessOwner,
                onValueChange = { onProfileChange(profile.copy(isBusinessOwner = it)) }
            )
        }
    }
}

@Composable
private fun SituationToggleCard(
    title: String,
    subtitle: String,
    value: Boolean?,
    onValueChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CivicNavy50),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CivicNavy900
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isYes = value == true
                val isNo = value == false

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isYes) CivicNavy800 else SurfaceLight)
                        .border(1.dp, if (isYes) CivicNavy800 else BorderLight, RoundedCornerShape(8.dp))
                        .clickable { onValueChange(true) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yes",
                        fontWeight = if (isYes) FontWeight.Bold else FontWeight.Medium,
                        color = if (isYes) Color.White else CivicNavy900,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isNo) CivicNavy800 else SurfaceLight)
                        .border(1.dp, if (isNo) CivicNavy800 else BorderLight, RoundedCornerShape(8.dp))
                        .clickable { onValueChange(false) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No",
                        fontWeight = if (isNo) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNo) Color.White else CivicNavy900,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// SECTION 4: ADDITIONAL INFORMATION
// ----------------------------------------------------
@Composable
private fun SectionAdditional(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Social & Demographic Details (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CivicNavy900
            )

            // Social Category
            val categories = listOf("General", "OBC", "MBC", "SC", "ST", "EWS")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Social Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = profile.socialCategory?.contains(cat, ignoreCase = true) == true
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CivicNavy800 else CivicNavy50)
                                .border(1.dp, if (isSelected) CivicNavy800 else BorderLight, RoundedCornerShape(8.dp))
                                .clickable { onProfileChange(profile.copy(socialCategory = cat)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.White else CivicNavy900,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Disability Status
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Benchmark Disability Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                val disabilityOptions = listOf("No", "Yes")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    disabilityOptions.forEach { opt ->
                        val isSelected = (opt == "Yes" && profile.disabilityStatus != "No" && profile.disabilityStatus != null) ||
                                (opt == "No" && (profile.disabilityStatus == "No" || profile.disabilityStatus == null))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CivicNavy800 else CivicNavy50)
                                .border(1.dp, if (isSelected) CivicNavy800 else BorderLight, RoundedCornerShape(8.dp))
                                .clickable { onProfileChange(profile.copy(disabilityStatus = opt)) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.White else CivicNavy900,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Marital Status
            val maritalOptions = listOf("Single", "Married", "Widowed", "Divorced / Separated")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Marital Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(maritalOptions) { m ->
                        val isSelected = profile.maritalStatus.equals(m, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CivicNavy800 else CivicNavy50)
                                .border(1.dp, if (isSelected) CivicNavy800 else BorderLight, RoundedCornerShape(8.dp))
                                .clickable { onProfileChange(profile.copy(maritalStatus = m)) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = m,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color.White else CivicNavy900,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            WhyWeAskRow(
                title = "Why do we ask social category & disability?",
                explanation = "Affirmative action programs (e.g. Stand-Up India, Divyangjan pension, SC/ST scholarships) provide targeted grants and loan subsidies specifically reserved for these groups."
            )
        }
    }
}

@Composable
private fun WhyWeAskRow(title: String, explanation: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CivicNavy50)
            .clickable { expanded = !expanded }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = CivicNavy700,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CivicNavy800
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = CivicNavy700,
                modifier = Modifier.size(16.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
