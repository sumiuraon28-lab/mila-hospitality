package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.auth.SupabaseAuthClient
import com.example.auth.SupabaseAuthValidator
import com.example.auth.SupabaseTalentClient
import com.example.ui.components.GoldFilledButton
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone
import kotlinx.coroutines.launch

@Composable
fun MilaAuthEntryScreen(
    supabaseAuthClient: SupabaseAuthClient,
    @Suppress("UNUSED_PARAMETER") supabaseTalentClient: SupabaseTalentClient
) {
    val savedDraft = remember(supabaseAuthClient) { supabaseAuthClient.loadPendingEmailOtpDraft() }
    var nameInput by rememberSaveable { mutableStateOf(savedDraft?.fullName.orEmpty()) }
    var mobileInput by rememberSaveable {
        mutableStateOf(savedDraft?.mobileNumber?.removePrefix("+91").orEmpty())
    }
    var emailInput by rememberSaveable { mutableStateOf(savedDraft?.email.orEmpty()) }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var otpSentToEmail by rememberSaveable { mutableStateOf<String?>(savedDraft?.email) }
    var normalizedMobileForVerify by rememberSaveable {
        mutableStateOf(savedDraft?.mobileNumber.orEmpty())
    }
    var isBusy by remember { mutableStateOf(false) }
    var inlineError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MilaCharcoalDeep,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MilaCharcoalElevated,
                    contentColor = MilaIvory,
                    actionColor = MilaChampagneGold
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed luxury architectural backdrop
            Image(
                painter = painterResource(id = R.drawable.img_hero_hotel_1795637232836),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Deep luxury charcoal & champagne-gold atmospheric scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xD40D0C0A),
                            0.35f to Color(0xBD11100D),
                            0.72f to Color(0xE8100F0C),
                            1.0f to Color(0xFA0C0B09)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x28D6BD96),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                val viewportMinHeight = maxHeight

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = viewportMinHeight)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Editorial luxury hospitality brand header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_crest_1795637231719),
                            contentDescription = "MILA Hospitality Crest",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(1.25.dp, MilaChampagneGold, MaterialTheme.shapes.medium)
                                .testTag("auth_brand_crest_container")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                color = MilaGoldMuted,
                                thickness = 0.75.dp,
                                modifier = Modifier.width(34.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MILA HOSPITALITY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MilaChampagneGold,
                                    letterSpacing = 3.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            HorizontalDivider(
                                color = MilaGoldMuted,
                                thickness = 0.75.dp,
                                modifier = Modifier.width(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Crafting Icons of\nLuxury Hospitality",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = MilaIvory,
                                textAlign = TextAlign.Center,
                                lineHeight = 36.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Bespoke Pre-Opening Advisory  •  Culinary & Bar Direction  •  Executive Talent",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MilaIvoryMuted,
                                letterSpacing = 0.4.sp,
                                textAlign = TextAlign.Center
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Free Email OTP (6-digit code via Email) + Name & Mobile card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, MilaGoldSubtle),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (otpSentToEmail != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MarkEmailRead,
                                        contentDescription = null,
                                        tint = MilaChampagneGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "VERIFY EMAIL OTP",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MilaChampagneGold,
                                            letterSpacing = 1.6.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Text(
                                text = if (otpSentToEmail == null) {
                                    "Sign In with Email OTP"
                                } else {
                                    "Enter 6-Digit Email Code"
                                },
                                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                            )

                            Text(
                                text = if (otpSentToEmail == null) {
                                    "Enter your name, mobile number, and email address. We'll send a free 6-digit verification code to your email inbox."
                                } else {
                                    "We sent a 6-digit verification code to $otpSentToEmail. Check your inbox or spam folder."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                            )

                            if (otpSentToEmail == null) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { input ->
                                        if (input.length <= 80) {
                                            nameInput = input
                                            inlineError = null
                                        }
                                    },
                                    label = { Text("Your Name *", color = MilaWarmStone) },
                                    placeholder = { Text("e.g., Rahul Sharma", color = MilaWarmStone) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = MilaChampagneGold
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    colors = milaFieldColors(),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_otp_full_name_input")
                                )

                                OutlinedTextField(
                                    value = mobileInput,
                                    onValueChange = { input ->
                                        if (input.length <= 15) {
                                            mobileInput = input
                                            inlineError = null
                                        }
                                    },
                                    label = { Text("Mobile Number *", color = MilaWarmStone) },
                                    placeholder = { Text("98200 00000", color = MilaWarmStone) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.PhoneAndroid,
                                            contentDescription = null,
                                            tint = MilaChampagneGold
                                        )
                                    },
                                    prefix = {
                                        Text(
                                            text = "+91 ",
                                            color = MilaChampagneGold,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = milaFieldColors(),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_otp_mobile_number_input")
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { input ->
                                        if (input.length <= 120) {
                                            emailInput = input
                                            inlineError = null
                                        }
                                    },
                                    label = { Text("Email Address *", color = MilaWarmStone) },
                                    placeholder = { Text("you@example.com", color = MilaWarmStone) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Email,
                                            contentDescription = null,
                                            tint = MilaChampagneGold
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = milaFieldColors(),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_otp_address_input")
                                )
                            } else {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { input ->
                                        if (input.length <= 8 && input.all { it.isDigit() }) {
                                            otpCode = input
                                            inlineError = null
                                        }
                                    },
                                    label = { Text("6-Digit Email OTP *", color = MilaWarmStone) },
                                    placeholder = { Text("Enter 6-digit code", color = MilaWarmStone) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = milaFieldColors(),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_otp_code_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            supabaseAuthClient.clearPendingEmailOtpDraft()
                                            otpSentToEmail = null
                                            otpCode = ""
                                            inlineError = null
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("email_otp_change_details_button")
                                    ) {
                                        Text("Change Details", color = MilaIvoryMuted)
                                    }
                                    TextButton(
                                        onClick = {
                                            val targetEmail = otpSentToEmail ?: return@TextButton
                                            scope.launch {
                                                inlineError = null
                                                isBusy = true
                                                val res = supabaseAuthClient.sendEmailOtp(
                                                    email = targetEmail,
                                                    fullName = nameInput.trim(),
                                                    rawPhone = normalizedMobileForVerify.ifBlank { mobileInput }
                                                )
                                                isBusy = false
                                                res.fold(
                                                    onSuccess = { sent ->
                                                        snackbarHostState.showSnackbar("New OTP code sent to $sent")
                                                    },
                                                    onFailure = { err ->
                                                        inlineError = err.localizedMessage
                                                            ?: "Could not resend code right now."
                                                    }
                                                )
                                            }
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("email_otp_resend_button")
                                    ) {
                                        Text("Resend Code", color = MilaChampagneGold)
                                    }
                                }
                            }

                            inlineError?.let { errMsg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1715)),
                                    border = BorderStroke(1.dp, MilaTerracotta),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_inline_error_banner")
                                ) {
                                    Text(
                                        text = errMsg,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFF6B7B0),
                                            lineHeight = 18.sp
                                        ),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            GoldFilledButton(
                                text = when {
                                    isBusy && otpSentToEmail == null -> "Sending Email OTP..."
                                    isBusy && otpSentToEmail != null -> "Verifying Code..."
                                    otpSentToEmail == null -> "Send 6-Digit Email OTP"
                                    else -> "Verify & Sign In"
                                },
                                enabled = !isBusy,
                                testTag = if (otpSentToEmail == null) "send_email_otp_button" else "verify_email_otp_button",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (otpSentToEmail == null) {
                                        val cleanName = nameInput.trim()
                                        if (cleanName.length < 2) {
                                            inlineError = "Please enter your full name."
                                            return@GoldFilledButton
                                        }
                                        val normalizedPhone = SupabaseAuthValidator.normalizePhoneE164(mobileInput)
                                        if (normalizedPhone == null) {
                                            inlineError = "Please enter a valid 10-digit mobile number."
                                            return@GoldFilledButton
                                        }
                                        val cleanEmail = emailInput.trim().lowercase()
                                        if (!SupabaseAuthValidator.isValidEmail(cleanEmail)) {
                                            inlineError = "Please enter a valid email address."
                                            return@GoldFilledButton
                                        }
                                        scope.launch {
                                            inlineError = null
                                            isBusy = true
                                            val result = supabaseAuthClient.sendEmailOtp(
                                                email = cleanEmail,
                                                fullName = cleanName,
                                                rawPhone = normalizedPhone
                                            )
                                            isBusy = false
                                            result.fold(
                                                onSuccess = { sentEmail ->
                                                    normalizedMobileForVerify = normalizedPhone
                                                    otpSentToEmail = sentEmail
                                                    otpCode = ""
                                                    snackbarHostState.showSnackbar("6-digit OTP sent to $sentEmail")
                                                },
                                                onFailure = { err ->
                                                    inlineError = err.localizedMessage
                                                        ?: "Could not send Email OTP. Please try again or use Continue with Google."
                                                }
                                            )
                                        }
                                    } else {
                                        val targetEmail = otpSentToEmail ?: return@GoldFilledButton
                                        if (otpCode.trim().length < 6) {
                                            inlineError = "Please enter the 6-digit OTP sent to your email."
                                            return@GoldFilledButton
                                        }
                                        scope.launch {
                                            inlineError = null
                                            isBusy = true
                                            val result = supabaseAuthClient.verifyEmailOtp(
                                                email = targetEmail,
                                                otpCode = otpCode.trim(),
                                                fullName = nameInput.trim(),
                                                rawPhone = normalizedMobileForVerify.ifBlank { mobileInput }
                                            )
                                            isBusy = false
                                            result.onFailure { err ->
                                                inlineError = err.localizedMessage
                                                    ?: "Invalid or expired OTP code. Please check your inbox and try again."
                                            }
                                        }
                                    }
                                }
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HorizontalDivider(
                                    color = MilaGoldSubtle,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "or",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                HorizontalDivider(
                                    color = MilaGoldSubtle,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            GoogleSignInButton(
                                label = "Continue with Google",
                                outlined = true,
                                onAuthSuccess = {},
                                onAuthError = { msg -> inlineError = msg }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleMobilePromptDialog(
    initialName: String,
    userEmail: String,
    isSaving: Boolean,
    errorMessage: String?,
    onSave: (fullName: String, normalizedMobile: String) -> Unit,
    onSkip: () -> Unit
) {
    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    var mobileInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayedError = localError ?: errorMessage

    AlertDialog(
        onDismissRequest = { if (!isSaving) onSkip() },
        containerColor = MilaCharcoalSurface,
        titleContentColor = MilaIvory,
        textContentColor = MilaIvoryMuted,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ONE QUICK STEP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Text(
                    text = "Add Your Mobile Number",
                    style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Add your mobile number so MILA Hospitality consultants can reach you for openings, advisory, and career updates.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                )

                if (userEmail.isNotBlank()) {
                    Text(
                        text = "Signed in as $userEmail",
                        style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold)
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { input ->
                        if (input.length <= 80) {
                            nameInput = input
                            localError = null
                        }
                    },
                    label = { Text("Your Name *", color = MilaWarmStone) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = milaFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_mobile_prompt_name")
                )

                OutlinedTextField(
                    value = mobileInput,
                    onValueChange = { input ->
                        if (input.length <= 15) {
                            mobileInput = input
                            localError = null
                        }
                    },
                    label = { Text("Mobile Number *", color = MilaWarmStone) },
                    placeholder = { Text("98200 00000", color = MilaWarmStone) },
                    prefix = {
                        Text(
                            text = "+91 ",
                            color = MilaChampagneGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = milaFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_mobile_prompt_number")
                )

                displayedError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF6B7B0))
                    )
                }
            }
        },
        confirmButton = {
            GoldFilledButton(
                text = if (isSaving) "Saving..." else "Save & Continue",
                enabled = !isSaving,
                testTag = "google_mobile_prompt_save_button",
                onClick = {
                    val cleanName = nameInput.trim()
                    if (cleanName.length < 2) {
                        localError = "Please enter your name."
                        return@GoldFilledButton
                    }
                    val normalized = SupabaseAuthValidator.normalizePhoneE164(mobileInput)
                    if (normalized == null) {
                        localError = "Please enter a valid 10-digit mobile number."
                        return@GoldFilledButton
                    }
                    localError = null
                    onSave(cleanName, normalized)
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !isSaving,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("google_mobile_prompt_skip_button")
            ) {
                Text("Not Now", color = MilaIvoryMuted)
            }
        }
    )
}

@Composable
private fun milaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MilaChampagneGold,
    unfocusedBorderColor = MilaCharcoalBorder,
    focusedContainerColor = MilaCharcoalDeep,
    unfocusedContainerColor = MilaCharcoalDeep,
    focusedTextColor = MilaIvory,
    unfocusedTextColor = MilaIvory
)
