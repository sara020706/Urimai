package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.ui.components.CivicHeader
import com.example.ui.components.PrivacyNoticeCard
import com.example.ui.theme.*

@Composable
fun ProfileViewEditScreen(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onSaveAndRecalculate: () -> Unit,
    onBack: () -> Unit,
    onLogOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var editableProfile by remember { mutableStateOf(profile) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        CivicHeader(
            title = "Citizen Profile",
            subtitle = "Review and edit your details",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Editable Card
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
                    Text(
                        text = "Profile Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )

                    OutlinedTextField(
                        value = editableProfile.name,
                        onValueChange = { editableProfile = editableProfile.copy(name = it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editableProfile.age?.toString() ?: "",
                            onValueChange = { str ->
                                val num = str.filter { it.isDigit() }.toIntOrNull()
                                editableProfile = editableProfile.copy(age = num)
                            },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = editableProfile.gender,
                            onValueChange = { editableProfile = editableProfile.copy(gender = it) },
                            label = { Text("Gender") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editableProfile.state,
                            onValueChange = { editableProfile = editableProfile.copy(state = it) },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = editableProfile.district,
                            onValueChange = { editableProfile = editableProfile.copy(district = it) },
                            label = { Text("District") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = editableProfile.education,
                        onValueChange = { editableProfile = editableProfile.copy(education = it) },
                        label = { Text("Highest Education") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editableProfile.annualIncome?.toString() ?: "",
                        onValueChange = { str ->
                            val num = str.filter { it.isDigit() }.toLongOrNull()
                            editableProfile = editableProfile.copy(annualIncome = num)
                        },
                        label = { Text("Annual Family Income (₹)") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editableProfile.socialCategory ?: "",
                        onValueChange = { editableProfile = editableProfile.copy(socialCategory = it) },
                        label = { Text("Social Category (General / OBC / SC / ST / EWS)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Checkboxes for situation flags
                    HorizontalDivider(color = BorderLight)

                    Text(
                        text = "Citizen Attributes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CivicNavy900
                    )

                    AttributeCheckboxRow(
                        label = "Currently an active student",
                        checked = editableProfile.isStudent == true,
                        onChecked = { editableProfile = editableProfile.copy(isStudent = it) }
                    )

                    AttributeCheckboxRow(
                        label = "Practicing farmer / land cultivator",
                        checked = editableProfile.isFarmer == true,
                        onChecked = { editableProfile = editableProfile.copy(isFarmer = it) }
                    )

                    AttributeCheckboxRow(
                        label = "Business owner / registered enterprise",
                        checked = editableProfile.isBusinessOwner == true,
                        onChecked = { editableProfile = editableProfile.copy(isBusinessOwner = it) }
                    )

                    AttributeCheckboxRow(
                        label = "Benchmark disability cardholder",
                        checked = editableProfile.disabilityStatus != "No" && editableProfile.disabilityStatus != null,
                        onChecked = { editableProfile = editableProfile.copy(disabilityStatus = if (it) "Yes" else "No") }
                    )
                }
            }

            PrivacyNoticeCard()

            OutlinedButton(
                onClick = onLogOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("log_out_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CrimsonPrimary.copy(alpha = 0.4f)))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
        }

        // Bottom Save CTA
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceLight,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        onProfileChange(editableProfile)
                        onSaveAndRecalculate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_and_recalc_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CivicNavy800)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Re-evaluate Schemes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AttributeCheckboxRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onChecked(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = CivicNavy800)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CivicNavy900
        )
    }
}
