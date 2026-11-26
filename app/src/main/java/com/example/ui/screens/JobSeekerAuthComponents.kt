package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.CandidateRegistrationRequest
import com.example.auth.CandidateValidator
import com.example.auth.SupabaseAuthValidator
import com.example.data.CandidateAvailability
import com.example.data.HospitalityDepartment
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaEmerald
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone

enum class JobSeekerAuthMode(val label: String) {
    LOGIN("Log In"),
    REGISTER("Register"),
    RESET_PASSWORD("Reset")
}

val noticePeriodOptions = listOf(
    "Immediate",
    "15 Days",
    "30 Days",
    "45 Days",
    "60 Days",
    "90 Days"
)

@Composable
fun JobSeekerAuthModeTabs(
    selectedMode: JobSeekerAuthMode,
    onSelectMode: (JobSeekerAuthMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JobSeekerAuthMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            FilterChip(
                selected = selected,
                onClick = { onSelectMode(mode) },
                label = { Text(mode.label, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MilaChampagneGold,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MilaCharcoalSurface,
                    labelColor = MilaIvoryMuted
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MilaCharcoalBorder,
                    selectedBorderColor = MilaChampagneGold
                ),
                modifier = Modifier.testTag("job_seeker_mode_${mode.name}")
            )
        }
    }
}

@Composable
fun JobSeekerPasswordLoginForm(
    isLoading: Boolean,
    supabaseConfigured: Boolean,
    onLoginSubmit: (String, String) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "MILA TALENT • CANDIDATE LOGIN",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "Sign In to Your Career Dossier",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = "Access leadership vacancies across India's finest hotels, resorts, and fine-dining destinations.",
                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
            )
            CandidateTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = "Candidate Email *",
                placeholder = "candidate@hospitality.in",
                keyboardType = KeyboardType.Email,
                testTag = "cand_login_email"
            )
            CandidatePasswordField(
                value = password,
                onValueChange = { if (it.length <= 128) password = it },
                label = "Password *",
                showPassword = showPassword,
                onToggleVisibility = { showPassword = !showPassword },
                testTag = "cand_login_password"
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier.testTag("cand_forgot_password")
                ) {
                    Text("Forgot Password?", color = MilaChampagneGold)
                }
            }
            validationError?.let { msg ->
                Text(msg, style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta))
            }
            GoldFilledButton(
                text = if (isLoading) "Signing In..." else "Log In to MILA Talent",
                enabled = !isLoading,
                testTag = "cand_login_submit",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val err = SupabaseAuthValidator.validateLogin(email, password)
                    validationError = err
                    if (err == null) onLoginSubmit(email.trim(), password)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobSeekerRegistrationForm(
    isLoading: Boolean,
    supabaseConfigured: Boolean,
    onSubmitRegistration: (CandidateRegistrationRequest) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var currentDesignation by remember { mutableStateOf("") }
    var department by remember { mutableStateOf(HospitalityDepartment.CULINARY) }
    var totalExperienceText by remember { mutableStateOf("") }
    var currentEmployer by remember { mutableStateOf("") }
    var previousEmployers by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var preferredLocations by remember { mutableStateOf("") }
    var expectedSalary by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf(CandidateAvailability.WITHIN_30_DAYS) }
    var noticePeriod by remember { mutableStateOf("30 Days") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "MILA TALENT • JOB SEEKER REGISTRATION",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "Register Your Hospitality Career Profile",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = "Complete your executive hospitality profile to unlock bespoke career mandates, private CV storage, and live alerts.",
                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
            )

            CandidateTextField(fullName, { if (it.length <= 100) fullName = it }, "Full Name *", "e.g., Kabir Malhotra", "reg_cand_full_name")
            CandidateTextField(email, { if (it.length <= 120) email = it }, "Email Address *", "candidate@hospitality.in", "reg_cand_email", KeyboardType.Email)
            CandidateTextField(mobileNumber, { if (it.length <= 25) mobileNumber = it }, "Mobile Number *", "+91 98100 00000", "reg_cand_mobile", KeyboardType.Phone)
            CandidateTextField(location, { if (it.length <= 80) location = it }, "Current Location *", "e.g., Mumbai, Maharashtra", "reg_cand_location")

            HorizontalDivider(color = MilaGoldSubtle)
            CandidateTextField(currentDesignation, { if (it.length <= 100) currentDesignation = it }, "Current Designation *", "e.g., Executive Sous Chef / General Manager", "reg_cand_designation")

            Text("DEPARTMENT *", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HospitalityDepartment.entries.forEach { dept ->
                    val selected = department == dept
                    FilterChip(
                        selected = selected,
                        onClick = { department = dept },
                        label = { Text(dept.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MilaChampagneGold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MilaCharcoalDeep,
                            labelColor = MilaIvoryMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MilaCharcoalBorder,
                            selectedBorderColor = MilaChampagneGold
                        ),
                        modifier = Modifier.testTag("reg_cand_dept_${dept.name}")
                    )
                }
            }

            CandidateTextField(
                value = totalExperienceText,
                onValueChange = { input -> if (input.length <= 2 && input.all { it.isDigit() }) totalExperienceText = input },
                label = "Total Experience (Years) *",
                placeholder = "e.g., 9",
                testTag = "reg_cand_experience",
                keyboardType = KeyboardType.Number
            )
            CandidateTextField(currentEmployer, { if (it.length <= 120) currentEmployer = it }, "Current Employer *", "e.g., Luxury Urban Hotel Group", "reg_cand_current_employer")
            CandidateTextField(previousEmployers, { if (it.length <= 500) previousEmployers = it }, "Previous Employers", "e.g., Heritage Palace Resort; Boutique Coastal Villas", "reg_cand_prev_employers", minLines = 2)
            CandidateTextField(education, { if (it.length <= 200) education = it }, "Education & Certifications *", "e.g., IHM Pusa B.Sc. Hospitality, WSET Level 3", "reg_cand_education")
            CandidateTextField(skills, { if (it.length <= 400) skills = it }, "Core Hospitality Skills *", "e.g., Pre-Opening SOPs, P&L Ownership, Craft Mixology", "reg_cand_skills", minLines = 2)

            HorizontalDivider(color = MilaGoldSubtle)
            CandidateTextField(preferredLocations, { if (it.length <= 200) preferredLocations = it }, "Preferred Job Locations *", "e.g., Mumbai, Goa, New Delhi NCR, Bengaluru", "reg_cand_pref_locations")
            CandidateTextField(expectedSalary, { if (it.length <= 80) expectedSalary = it }, "Expected Salary (Annual CTC) *", "e.g., ₹24 – 30 LPA", "reg_cand_salary")

            Text("AVAILABILITY *", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CandidateAvailability.entries.forEach { avail ->
                    val selected = availability == avail
                    FilterChip(
                        selected = selected,
                        onClick = { availability = avail },
                        label = { Text(avail.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MilaChampagneGold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MilaCharcoalDeep,
                            labelColor = MilaIvoryMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MilaCharcoalBorder,
                            selectedBorderColor = MilaChampagneGold
                        ),
                        modifier = Modifier.testTag("reg_cand_avail_${avail.name}")
                    )
                }
            }

            Text("NOTICE PERIOD *", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                noticePeriodOptions.forEach { option ->
                    val selected = noticePeriod == option
                    FilterChip(
                        selected = selected,
                        onClick = { noticePeriod = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MilaChampagneGold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MilaCharcoalDeep,
                            labelColor = MilaIvoryMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MilaCharcoalBorder,
                            selectedBorderColor = MilaChampagneGold
                        )
                    )
                }
            }

            CandidatePasswordField(
                value = password,
                onValueChange = { if (it.length <= 128) password = it },
                label = "Password (min 8 chars, letters & numbers) *",
                showPassword = showPassword,
                onToggleVisibility = { showPassword = !showPassword },
                testTag = "reg_cand_password"
            )

            validationError?.let { msg ->
                Text(msg, style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta))
            }

            GoldFilledButton(
                text = if (isLoading) "Creating Candidate Profile..." else "Create Job Seeker Account",
                enabled = !isLoading,
                testTag = "reg_cand_submit",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val expYears = totalExperienceText.toLongOrNull() ?: -1L
                    val req = CandidateRegistrationRequest(
                        fullName = fullName,
                        email = email,
                        mobileNumber = mobileNumber,
                        location = location,
                        currentDesignation = currentDesignation,
                        department = department,
                        totalExperienceYears = expYears,
                        currentEmployer = currentEmployer,
                        previousEmployers = previousEmployers,
                        education = education,
                        skills = skills,
                        preferredJobLocations = preferredLocations,
                        expectedSalary = expectedSalary,
                        availability = availability,
                        noticePeriod = noticePeriod,
                        password = password
                    )
                    val err = CandidateValidator.validateRegistration(req)
                    validationError = err
                    if (err == null) onSubmitRegistration(req)
                }
            )
        }
    }
}

@Composable
fun JobSeekerPasswordResetForm(
    isLoading: Boolean,
    statusMessage: String?,
    onResetSubmit: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.LockReset, contentDescription = null, tint = MilaChampagneGold, modifier = Modifier.size(22.dp))
                Text("CANDIDATE PASSWORD RESET", style = MaterialTheme.typography.labelMedium.copy(color = MilaChampagneGold, letterSpacing = 1.5.sp))
            }
            CandidateTextField(email, { if (it.length <= 120) email = it }, "Registered Email *", "candidate@hospitality.in", "cand_reset_email", KeyboardType.Email)
            validationError?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)) }
            statusMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = MilaEmerald)) }
            GoldFilledButton(
                text = if (isLoading) "Sending..." else "Send Reset Link",
                enabled = !isLoading,
                testTag = "cand_reset_submit",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val err = SupabaseAuthValidator.validateResetEmail(email)
                    validationError = err
                    if (err == null) onResetSubmit(email.trim())
                }
            )
            GoldOutlineButton("Back to Log In", onBackToLogin, "cand_reset_back", Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun CandidateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MilaWarmStone) },
        placeholder = { Text(placeholder, color = MilaWarmStone) },
        minLines = minLines,
        maxLines = if (minLines > 1) 5 else 1,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MilaChampagneGold,
            unfocusedBorderColor = MilaCharcoalBorder,
            focusedContainerColor = MilaCharcoalDeep,
            unfocusedContainerColor = MilaCharcoalDeep,
            focusedTextColor = MilaIvory,
            unfocusedTextColor = MilaIvory
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun CandidatePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleVisibility: () -> Unit,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MilaWarmStone) },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password",
                    tint = MilaWarmStone
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MilaChampagneGold,
            unfocusedBorderColor = MilaCharcoalBorder,
            focusedContainerColor = MilaCharcoalDeep,
            unfocusedContainerColor = MilaCharcoalDeep,
            focusedTextColor = MilaIvory,
            unfocusedTextColor = MilaIvory
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
