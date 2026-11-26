package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.ClientRegistrationRequest
import com.example.auth.SupabaseAuthValidator
import com.example.auth.SupabaseClientMetadata
import com.example.data.BusinessCategory
import com.example.data.EnquiryConfirmation
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

enum class ClientAuthMode(val label: String) {
    LOGIN("Log In"),
    REGISTER("Create Account"),
    RESET_PASSWORD("Reset Password")
}

@Composable
fun ClientAuthModeTabs(
    selectedMode: ClientAuthMode,
    onSelectMode: (ClientAuthMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ClientAuthMode.entries.forEach { mode ->
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
                modifier = Modifier.testTag("client_mode_${mode.name}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientRegistrationForm(
    isLoading: Boolean,
    supabaseConfigured: Boolean,
    onSubmitRegistration: (ClientRegistrationRequest) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var businessCategory by remember { mutableStateOf(BusinessCategory.LUXURY_HOTEL_RESORT) }
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
                text = "CLIENT REGISTRATION",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "Create Your MILA Client Account",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = if (supabaseConfigured) {
                    "Complete your hospitality profile below. Once registered, you can immediately explore all services and submit enquiries."
                } else {
                    "Complete your hospitality profile below. Email/password registration connects to Supabase Auth once configured in Secrets, or pair with Google below for immediate access."
                },
                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
            )

            ClientTextField(
                value = fullName,
                onValueChange = { if (it.length <= 100) fullName = it },
                label = "Full Name *",
                placeholder = "e.g., Rohan Kapoor",
                testTag = "reg_full_name"
            )
            ClientTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = "Business Email *",
                placeholder = "name@hospitalitygroup.in",
                keyboardType = KeyboardType.Email,
                testTag = "reg_email"
            )
            ClientTextField(
                value = mobileNumber,
                onValueChange = { if (it.length <= 25) mobileNumber = it },
                label = "Mobile Number *",
                placeholder = "+91 98200 00000",
                keyboardType = KeyboardType.Phone,
                testTag = "reg_mobile_number"
            )
            ClientTextField(
                value = companyName,
                onValueChange = { if (it.length <= 120) companyName = it },
                label = "Company / Property Name *",
                placeholder = "e.g., Heritage Hospitality Pvt. Ltd.",
                testTag = "reg_company_name"
            )
            ClientTextField(
                value = designation,
                onValueChange = { if (it.length <= 80) designation = it },
                label = "Designation *",
                placeholder = "e.g., Managing Director, Founder, GM",
                testTag = "reg_designation"
            )
            ClientTextField(
                value = city,
                onValueChange = { if (it.length <= 80) city = it },
                label = "City *",
                placeholder = "e.g., Mumbai, New Delhi, Bengaluru, Goa",
                testTag = "reg_city"
            )

            Text(
                text = "BUSINESS CATEGORY *",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.2.sp
                )
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BusinessCategory.entries.forEach { category ->
                    val selected = businessCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { businessCategory = category },
                        label = { Text(category.label) },
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
                        modifier = Modifier.testTag("reg_business_category_${category.name}")
                    )
                }
            }

            ClientPasswordField(
                value = password,
                onValueChange = { if (it.length <= 128) password = it },
                label = "Password (min 8 chars, letters & numbers) *",
                showPassword = showPassword,
                onToggleVisibility = { showPassword = !showPassword },
                testTag = "reg_password"
            )

            validationError?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }

            GoldFilledButton(
                text = if (isLoading) "Creating Account..." else "Create Account",
                enabled = !isLoading,
                testTag = "client_register_submit_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val req = ClientRegistrationRequest(
                        fullName = fullName,
                        email = email,
                        mobileNumber = mobileNumber,
                        companyName = companyName,
                        designation = designation,
                        city = city,
                        businessCategory = businessCategory,
                        password = password
                    )
                    val err = SupabaseAuthValidator.validateRegistration(req)
                    validationError = err
                    if (err == null) {
                        onSubmitRegistration(req)
                    }
                }
            )
        }
    }
}

@Composable
fun ClientPasswordLoginForm(
    isLoading: Boolean,
    supabaseConfigured: Boolean,
    onLoginSubmit: (email: String, password: String) -> Unit,
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
                text = "EMAIL & PASSWORD LOGIN",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "Log In Securely",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            if (!supabaseConfigured) {
                Text(
                    text = "Supabase Auth ready — add SUPABASE_URL and SUPABASE_ANON_KEY in Secrets to enable email/password sign-in, or use Continue with Google below.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                )
            }
            ClientTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = "Email Address",
                placeholder = "name@hospitalitygroup.in",
                keyboardType = KeyboardType.Email,
                testTag = "login_email"
            )
            ClientPasswordField(
                value = password,
                onValueChange = { if (it.length <= 128) password = it },
                label = "Password",
                showPassword = showPassword,
                onToggleVisibility = { showPassword = !showPassword },
                testTag = "login_password"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.labelLarge.copy(color = MilaChampagneGold)
                    )
                }
            }

            validationError?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }

            GoldFilledButton(
                text = if (isLoading) "Signing In..." else "Log In",
                enabled = !isLoading,
                testTag = "client_login_submit_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val err = SupabaseAuthValidator.validateLogin(email, password)
                    validationError = err
                    if (err == null) {
                        onLoginSubmit(email.trim(), password)
                    }
                }
            )
        }
    }
}

@Composable
fun ClientPasswordResetForm(
    isLoading: Boolean,
    statusMessage: String?,
    onResetSubmit: (email: String) -> Unit,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LockReset,
                    contentDescription = null,
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "RESET PASSWORD",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.6.sp
                    )
                )
            }
            Text(
                text = "Recover Client Account Access",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = "Enter your registered email address to receive a secure Supabase password recovery link.",
                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
            )
            ClientTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = "Registered Email *",
                placeholder = "name@hospitalitygroup.in",
                keyboardType = KeyboardType.Email,
                testTag = "reset_email_input"
            )
            validationError?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }
            statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaEmerald)
                )
            }
            GoldFilledButton(
                text = if (isLoading) "Sending Recovery Email..." else "Send Password Reset Link",
                enabled = !isLoading,
                testTag = "client_reset_submit_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val err = SupabaseAuthValidator.validateResetEmail(email)
                    validationError = err
                    if (err == null) {
                        onResetSubmit(email.trim())
                    }
                }
            )
            GoldOutlineButton(
                text = "Back to Log In",
                onClick = onBackToLogin,
                testTag = "reset_back_to_login_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientProfileEditorCard(
    initialMetadata: SupabaseClientMetadata,
    isSaving: Boolean,
    onSaveProfile: (SupabaseClientMetadata) -> Unit
) {
    var fullName by remember(initialMetadata) { mutableStateOf(initialMetadata.fullName) }
    var email by remember(initialMetadata) { mutableStateOf(initialMetadata.email) }
    var mobileNumber by remember(initialMetadata) { mutableStateOf(initialMetadata.mobileNumber) }
    var companyName by remember(initialMetadata) { mutableStateOf(initialMetadata.companyName) }
    var designation by remember(initialMetadata) { mutableStateOf(initialMetadata.designation) }
    var city by remember(initialMetadata) { mutableStateOf(initialMetadata.city) }
    var businessCategory by remember(initialMetadata) { mutableStateOf(initialMetadata.businessCategory) }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonOutline,
                    contentDescription = null,
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MY PRIVATE CLIENT PROFILE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
            Text(
                text = "Only visible to you. Used to pre-fill service enquiries faster.",
                style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
            )

            ClientTextField(
                value = fullName,
                onValueChange = { if (it.length <= 100) fullName = it },
                label = "Full Name *",
                placeholder = "Full Name",
                testTag = "profile_full_name"
            )
            ClientTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = "Email *",
                placeholder = "name@company.in",
                keyboardType = KeyboardType.Email,
                testTag = "profile_email"
            )
            ClientTextField(
                value = mobileNumber,
                onValueChange = { if (it.length <= 25) mobileNumber = it },
                label = "Mobile Number *",
                placeholder = "+91 98200 00000",
                keyboardType = KeyboardType.Phone,
                testTag = "profile_mobile"
            )
            ClientTextField(
                value = companyName,
                onValueChange = { if (it.length <= 120) companyName = it },
                label = "Company Name *",
                placeholder = "Hotel / Restaurant / Group",
                testTag = "profile_company"
            )
            ClientTextField(
                value = designation,
                onValueChange = { if (it.length <= 80) designation = it },
                label = "Designation *",
                placeholder = "Director / Founder / GM",
                testTag = "profile_designation"
            )
            ClientTextField(
                value = city,
                onValueChange = { if (it.length <= 80) city = it },
                label = "City *",
                placeholder = "City",
                testTag = "profile_city"
            )
            Text(
                text = "BUSINESS CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BusinessCategory.entries.forEach { category ->
                    val selected = businessCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { businessCategory = category },
                        label = { Text(category.label) },
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
                        modifier = Modifier.testTag("profile_category_${category.name}")
                    )
                }
            }

            validationError?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }

            GoldFilledButton(
                text = if (isSaving) "Saving Profile..." else "Save Profile",
                enabled = !isSaving,
                testTag = "save_client_profile_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val updated = SupabaseClientMetadata(
                        fullName = fullName.trim(),
                        email = email.trim(),
                        mobileNumber = mobileNumber.trim(),
                        companyName = companyName.trim(),
                        designation = designation.trim(),
                        city = city.trim(),
                        businessCategory = businessCategory
                    )
                    val err = SupabaseAuthValidator.validateProfileUpdate(updated)
                    validationError = err
                    if (err == null) {
                        onSaveProfile(updated)
                    }
                }
            )
        }
    }
}

@Composable
fun EnquiryConfirmationDialog(
    confirmation: EnquiryConfirmation,
    onExploreServices: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MilaCharcoalSurface,
        icon = {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Enquiry confirmed",
                tint = MilaEmerald,
                modifier = Modifier.size(38.dp)
            )
        },
        title = {
            Text(
                text = "Enquiry Confirmed",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reference: ${confirmation.referenceCode}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.2.sp
                    )
                )
                HorizontalDivider(color = MilaGoldSubtle)
                Text(
                    text = confirmation.serviceType.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MilaIvory,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "${confirmation.propertyName} • ${confirmation.city} (${confirmation.scaleSummary})",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                )
                Text(
                    text = "Confirmation recorded for ${confirmation.contactName} (${confirmation.contactEmail}). A senior MILA partner will respond within 1 business day.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onExploreServices()
                },
                modifier = Modifier.testTag("enquiry_confirmation_explore_services")
            ) {
                Text("Explore Services", color = MilaChampagneGold, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("enquiry_confirmation_done")
            ) {
                Text("Done", color = MilaIvoryMuted)
            }
        }
    )
}

@Composable
private fun ClientTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MilaWarmStone) },
        placeholder = { Text(placeholder, color = MilaWarmStone) },
        singleLine = true,
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
private fun ClientPasswordField(
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
