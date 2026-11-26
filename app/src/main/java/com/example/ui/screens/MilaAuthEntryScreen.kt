package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.ClientAuthOutcome
import com.example.auth.GoogleSignInButton
import com.example.auth.SupabaseAuthClient
import com.example.auth.SupabaseAuthValidator
import com.example.auth.SupabaseTalentClient
import com.example.auth.VerifiedPhoneOtpSession
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaEmerald
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone
import kotlinx.coroutines.launch

private enum class AuthEntryStep {
    GET_STARTED,
    MEMBER_AUTH,
    CREATE_MILA_ACCOUNT,
    PASSWORD_RESET
}

private enum class AuthMethodTab {
    PHONE,
    EMAIL
}

private enum class EmailAuthMode {
    SIGN_IN,
    CREATE_ACCOUNT
}

@Composable
fun MilaAuthEntryScreen(
    supabaseAuthClient: SupabaseAuthClient,
    @Suppress("UNUSED_PARAMETER") supabaseTalentClient: SupabaseTalentClient
) {
    var currentStep by remember { mutableStateOf(AuthEntryStep.GET_STARTED) }
    var authMethodTab by remember { mutableStateOf(AuthMethodTab.PHONE) }
    var emailAuthMode by remember { mutableStateOf(EmailAuthMode.SIGN_IN) }

    var mobileInput by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSentToPhone by remember { mutableStateOf<String?>(null) }
    var verifiedPhoneSession by remember { mutableStateOf<VerifiedPhoneOtpSession?>(null) }

    var isBusy by remember { mutableStateOf(false) }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var inlineInfo by remember { mutableStateOf<String?>(null) }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val supabaseConfigured = remember(supabaseAuthClient) { supabaseAuthClient.isConfigured() }

    BackHandler(enabled = currentStep != AuthEntryStep.GET_STARTED) {
        inlineError = null
        inlineInfo = null
        currentStep = when (currentStep) {
            AuthEntryStep.CREATE_MILA_ACCOUNT -> AuthEntryStep.MEMBER_AUTH
            AuthEntryStep.PASSWORD_RESET -> {
                authMethodTab = AuthMethodTab.EMAIL
                emailAuthMode = EmailAuthMode.SIGN_IN
                AuthEntryStep.MEMBER_AUTH
            }
            AuthEntryStep.MEMBER_AUTH -> AuthEntryStep.GET_STARTED
            AuthEntryStep.GET_STARTED -> AuthEntryStep.GET_STARTED
        }
    }

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

                when (currentStep) {
                    AuthEntryStep.CREATE_MILA_ACCOUNT -> {
                        val verified = verifiedPhoneSession
                        if (verified == null) {
                            currentStep = AuthEntryStep.MEMBER_AUTH
                        } else {
                            CreateMilaAccountStep(
                                verifiedPhone = verified,
                                isLoading = isBusy,
                                onBack = {
                                    inlineError = null
                                    currentStep = AuthEntryStep.MEMBER_AUTH
                                },
                                onRegisterSubmit = { fullName, email, password, updatesOptIn ->
                                    scope.launch {
                                        isBusy = true
                                        val outcome = supabaseAuthClient.completeMilaAccountAfterOtp(
                                            verifiedPhone = verified,
                                            fullName = fullName,
                                            email = email,
                                            password = password,
                                            updatesOptIn = updatesOptIn
                                        )
                                        isBusy = false
                                        when (outcome) {
                                            is ClientAuthOutcome.Authenticated -> snackbarHostState.showSnackbar(outcome.message)
                                            is ClientAuthOutcome.PendingEmailVerification -> snackbarHostState.showSnackbar(outcome.message)
                                            is ClientAuthOutcome.RequiresGoogleSyncOrSecrets -> snackbarHostState.showSnackbar(outcome.message)
                                            is ClientAuthOutcome.Failure -> snackbarHostState.showSnackbar(outcome.message)
                                        }
                                    }
                                },
                                onGoogleAuthError = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .heightIn(min = viewportMinHeight)
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top back arrow when inside Member Auth or Password Reset
                            if (currentStep == AuthEntryStep.MEMBER_AUTH || currentStep == AuthEntryStep.PASSWORD_RESET) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 460.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            inlineError = null
                                            currentStep = if (currentStep == AuthEntryStep.PASSWORD_RESET) {
                                                authMethodTab = AuthMethodTab.EMAIL
                                                emailAuthMode = EmailAuthMode.SIGN_IN
                                                AuthEntryStep.MEMBER_AUTH
                                            } else {
                                                AuthEntryStep.GET_STARTED
                                            }
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("auth_back_to_welcome")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MilaIvory
                                        )
                                    }
                                    Text(
                                        text = if (currentStep == AuthEntryStep.PASSWORD_RESET) "Back to Sign In" else "Back",
                                        style = MaterialTheme.typography.labelLarge.copy(color = MilaIvoryMuted)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Classy editorial luxury hospitality header
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 460.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_crest_1795637231719),
                                    contentDescription = "MILA Hospitality Crest",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(if (currentStep == AuthEntryStep.GET_STARTED) 74.dp else 58.dp)
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

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 460.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (currentStep) {
                                    AuthEntryStep.GET_STARTED -> {
                                        GetStartedCard(
                                            onGetStartedEmail = {
                                                inlineError = null
                                                inlineInfo = null
                                                authMethodTab = AuthMethodTab.EMAIL
                                                emailAuthMode = EmailAuthMode.CREATE_ACCOUNT
                                                currentStep = AuthEntryStep.MEMBER_AUTH
                                            },
                                            onSignInEmail = {
                                                inlineError = null
                                                inlineInfo = null
                                                authMethodTab = AuthMethodTab.EMAIL
                                                emailAuthMode = EmailAuthMode.SIGN_IN
                                                currentStep = AuthEntryStep.MEMBER_AUTH
                                            },
                                            onVerifyPhoneOtp = {
                                                inlineError = null
                                                inlineInfo = null
                                                authMethodTab = AuthMethodTab.PHONE
                                                currentStep = AuthEntryStep.MEMBER_AUTH
                                            }
                                        )
                                    }

                                    AuthEntryStep.MEMBER_AUTH -> {
                                        AuthMethodToggleBar(
                                            selectedTab = authMethodTab,
                                            onSelectTab = { tab ->
                                                authMethodTab = tab
                                                inlineError = null
                                                inlineInfo = null
                                            }
                                        )

                                        when (authMethodTab) {
                                            AuthMethodTab.PHONE -> {
                                                MobileOtpCard(
                                                    mobileInput = mobileInput,
                                                    onMobileChange = { input ->
                                                        if (input.length <= 15) {
                                                            mobileInput = input
                                                            inlineError = null
                                                        }
                                                    },
                                                    otpSentToPhone = otpSentToPhone,
                                                    otpCode = otpCode,
                                                    onOtpChange = { input ->
                                                        if (input.length <= 6 && input.all { it.isDigit() }) {
                                                            otpCode = input
                                                            inlineError = null
                                                        }
                                                    },
                                                    isLoading = isBusy,
                                                    errorText = inlineError,
                                                    onGetOtp = {
                                                        val normalized = SupabaseAuthValidator.normalizePhoneE164(mobileInput)
                                                        if (normalized == null) {
                                                            inlineError = "Please enter a valid 10-digit mobile number."
                                                            return@MobileOtpCard
                                                        }
                                                        scope.launch {
                                                            inlineError = null
                                                            isBusy = true
                                                            val result = supabaseAuthClient.sendPhoneOtp(normalized)
                                                            isBusy = false
                                                            result.fold(
                                                                onSuccess = { sentPhone ->
                                                                    otpSentToPhone = sentPhone
                                                                    otpCode = ""
                                                                    snackbarHostState.showSnackbar("OTP sent to $sentPhone")
                                                                },
                                                                onFailure = { err ->
                                                                    inlineError = err.localizedMessage
                                                                        ?: "Could not send OTP. Please try again."
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onVerifyOtp = {
                                                        val targetPhone = otpSentToPhone ?: return@MobileOtpCard
                                                        if (otpCode.length < 4) {
                                                            inlineError = "Please enter the OTP sent to your mobile."
                                                            return@MobileOtpCard
                                                        }
                                                        scope.launch {
                                                            inlineError = null
                                                            isBusy = true
                                                            val result = supabaseAuthClient.verifyPhoneOtp(targetPhone, otpCode)
                                                            isBusy = false
                                                            result.fold(
                                                                onSuccess = { session ->
                                                                    verifiedPhoneSession = session
                                                                    currentStep = AuthEntryStep.CREATE_MILA_ACCOUNT
                                                                },
                                                                onFailure = { err ->
                                                                    inlineError = err.localizedMessage
                                                                        ?: "Invalid OTP. Please check and try again."
                                                                }
                                                            )
                                                        }
                                                    },
                                                    onChangeNumber = {
                                                        otpSentToPhone = null
                                                        otpCode = ""
                                                        inlineError = null
                                                    },
                                                    onSwitchToEmail = {
                                                        inlineError = null
                                                        inlineInfo = null
                                                        authMethodTab = AuthMethodTab.EMAIL
                                                        emailAuthMode = EmailAuthMode.CREATE_ACCOUNT
                                                    }
                                                )
                                            }

                                            AuthMethodTab.EMAIL -> {
                                                EmailModeSubToggle(
                                                    selectedMode = emailAuthMode,
                                                    onSelectMode = { mode ->
                                                        emailAuthMode = mode
                                                        inlineError = null
                                                        inlineInfo = null
                                                    }
                                                )

                                                inlineInfo?.let { infoMsg ->
                                                    AuthStatusBanner(
                                                        message = infoMsg,
                                                        isError = false
                                                    )
                                                }
                                                inlineError?.let { errMsg ->
                                                    AuthStatusBanner(
                                                        message = errMsg,
                                                        isError = true
                                                    )
                                                }

                                                when (emailAuthMode) {
                                                    EmailAuthMode.SIGN_IN -> {
                                                        ClientPasswordLoginForm(
                                                            isLoading = isBusy,
                                                            supabaseConfigured = supabaseConfigured,
                                                            onLoginSubmit = { email, password ->
                                                                scope.launch {
                                                                    inlineError = null
                                                                    inlineInfo = null
                                                                    isBusy = true
                                                                    val outcome = supabaseAuthClient.signInWithPassword(email, password)
                                                                    isBusy = false
                                                                    when (outcome) {
                                                                        is ClientAuthOutcome.Authenticated -> {
                                                                            snackbarHostState.showSnackbar(outcome.message)
                                                                        }
                                                                        is ClientAuthOutcome.RequiresGoogleSyncOrSecrets -> {
                                                                            inlineError = outcome.message
                                                                        }
                                                                        is ClientAuthOutcome.PendingEmailVerification -> {
                                                                            inlineInfo = outcome.message
                                                                        }
                                                                        is ClientAuthOutcome.Failure -> {
                                                                            inlineError = if (outcome.message.contains("not confirmed", ignoreCase = true)) {
                                                                                "Email is not confirmed yet. Click the verification link in your inbox, or turn off 'Confirm email' in Supabase Dashboard -> Authentication -> Providers -> Email for instant sign-in."
                                                                            } else {
                                                                                outcome.message
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onForgotPasswordClick = {
                                                                inlineError = null
                                                                inlineInfo = null
                                                                resetStatusMessage = null
                                                                currentStep = AuthEntryStep.PASSWORD_RESET
                                                            }
                                                        )

                                                        OrDividerRow()

                                                        GoogleSignInButton(
                                                            label = "Continue with Google",
                                                            outlined = true,
                                                            onAuthSuccess = {},
                                                            onAuthError = { msg ->
                                                                inlineError = msg
                                                            }
                                                        )
                                                    }

                                                    EmailAuthMode.CREATE_ACCOUNT -> {
                                                        DirectEmailSignUpCard(
                                                            isLoading = isBusy,
                                                            onRegisterSubmit = { fullName, email, mobile, password, updatesOptIn ->
                                                                scope.launch {
                                                                    inlineError = null
                                                                    inlineInfo = null
                                                                    isBusy = true
                                                                    val outcome = supabaseAuthClient.quickEmailSignUp(
                                                                        fullName = fullName,
                                                                        email = email,
                                                                        mobileNumber = mobile,
                                                                        password = password,
                                                                        updatesOptIn = updatesOptIn
                                                                    )
                                                                    isBusy = false
                                                                    when (outcome) {
                                                                        is ClientAuthOutcome.Authenticated -> {
                                                                            snackbarHostState.showSnackbar(outcome.message)
                                                                        }
                                                                        is ClientAuthOutcome.PendingEmailVerification -> {
                                                                            inlineInfo = outcome.message + " (Tip: For instant testing without inbox links, disable 'Confirm email' in Supabase -> Authentication -> Providers -> Email.)"
                                                                            emailAuthMode = EmailAuthMode.SIGN_IN
                                                                        }
                                                                        is ClientAuthOutcome.RequiresGoogleSyncOrSecrets -> {
                                                                            inlineError = outcome.message
                                                                        }
                                                                        is ClientAuthOutcome.Failure -> {
                                                                            inlineError = outcome.message
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onGoogleAuthError = { msg ->
                                                                inlineError = msg
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    AuthEntryStep.PASSWORD_RESET -> {
                                        ClientPasswordResetForm(
                                            isLoading = isBusy,
                                            statusMessage = resetStatusMessage,
                                            onResetSubmit = { email ->
                                                scope.launch {
                                                    isBusy = true
                                                    val result = supabaseAuthClient.sendPasswordReset(email)
                                                    isBusy = false
                                                    result.fold(
                                                        onSuccess = { msg ->
                                                            resetStatusMessage = msg
                                                            snackbarHostState.showSnackbar(msg)
                                                        },
                                                        onFailure = { error ->
                                                            snackbarHostState.showSnackbar(
                                                                error.localizedMessage
                                                                    ?: "Unable to request password reset."
                                                            )
                                                        }
                                                    )
                                                }
                                            },
                                            onBackToLogin = {
                                                authMethodTab = AuthMethodTab.EMAIL
                                                emailAuthMode = EmailAuthMode.SIGN_IN
                                                currentStep = AuthEntryStep.MEMBER_AUTH
                                            }
                                        )
                                    }

                                    AuthEntryStep.CREATE_MILA_ACCOUNT -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GetStartedCard(
    onGetStartedEmail: () -> Unit,
    onSignInEmail: () -> Unit,
    onVerifyPhoneOtp: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WELCOME TO MILA",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "Your Private Hospitality Suite",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MilaIvory,
                    textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create your MILA client account or sign in to explore consultancy services, submit business enquiries, and discover executive hospitality roles.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MilaIvoryMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            GoldFilledButton(
                text = "Get Started",
                onClick = onGetStartedEmail,
                testTag = "auth_get_started_button",
                modifier = Modifier.fillMaxWidth()
            )

            GoldOutlineButton(
                text = "Sign In with Email & Password",
                onClick = onSignInEmail,
                testTag = "auth_get_started_email_signin",
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = onVerifyPhoneOtp,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("auth_get_started_phone_otp")
            ) {
                Text(
                    text = "Or verify with Mobile Number (OTP)",
                    style = MaterialTheme.typography.labelLarge.copy(color = MilaChampagneGold)
                )
            }
        }
    }
}

@Composable
private fun AuthStatusBanner(
    message: String,
    isError: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) Color(0xFF2B1715) else Color(0xFF14241D)
        ),
        border = BorderStroke(1.dp, if (isError) MilaTerracotta else MilaEmerald),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isError) "auth_inline_error_banner" else "auth_inline_info_banner")
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isError) Color(0xFFF6B7B0) else Color(0xFFC4F0DA),
                lineHeight = 18.sp
            ),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun AuthMethodToggleBar(
    selectedTab: AuthMethodTab,
    onSelectTab: (AuthMethodTab) -> Unit
) {
    Surface(
        color = MilaCharcoalSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val phoneSelected = selectedTab == AuthMethodTab.PHONE
            Surface(
                color = if (phoneSelected) MilaChampagneGold else Color.Transparent,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSelectTab(AuthMethodTab.PHONE) }
                    .testTag("auth_tab_phone")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = if (phoneSelected) MilaCharcoalDeep else MilaIvoryMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Phone Number",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (phoneSelected) MilaCharcoalDeep else MilaIvoryMuted,
                            fontWeight = if (phoneSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }

            val emailSelected = selectedTab == AuthMethodTab.EMAIL
            Surface(
                color = if (emailSelected) MilaChampagneGold else Color.Transparent,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSelectTab(AuthMethodTab.EMAIL) }
                    .testTag("auth_tab_email")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = if (emailSelected) MilaCharcoalDeep else MilaIvoryMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Email & Password",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (emailSelected) MilaCharcoalDeep else MilaIvoryMuted,
                            fontWeight = if (emailSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailModeSubToggle(
    selectedMode: EmailAuthMode,
    onSelectMode: (EmailAuthMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val signInSelected = selectedMode == EmailAuthMode.SIGN_IN
        Surface(
            color = if (signInSelected) MilaCharcoalElevated else MilaCharcoalSurface,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, if (signInSelected) MilaChampagneGold else MilaCharcoalBorder),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable { onSelectMode(EmailAuthMode.SIGN_IN) }
                .testTag("email_mode_sign_in")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (signInSelected) MilaChampagneGold else MilaIvoryMuted,
                        fontWeight = if (signInSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }

        val createSelected = selectedMode == EmailAuthMode.CREATE_ACCOUNT
        Surface(
            color = if (createSelected) MilaCharcoalElevated else MilaCharcoalSurface,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, if (createSelected) MilaChampagneGold else MilaCharcoalBorder),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable { onSelectMode(EmailAuthMode.CREATE_ACCOUNT) }
                .testTag("email_mode_create_account")
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (createSelected) MilaChampagneGold else MilaIvoryMuted,
                        fontWeight = if (createSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun DirectEmailSignUpCard(
    isLoading: Boolean,
    onRegisterSubmit: (
        fullName: String,
        email: String,
        mobileNumber: String,
        password: String,
        updatesOptIn: Boolean
    ) -> Unit,
    onGoogleAuthError: (String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var updatesOptIn by remember { mutableStateOf(true) }
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
                text = "CREATE ACCOUNT",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "Create Your MILA Account",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { if (it.length <= 100) fullName = it; validationError = null },
                label = { Text("Full name*", color = MilaWarmStone) },
                placeholder = { Text("Enter your full name", color = MilaWarmStone) },
                singleLine = true,
                colors = milaFieldColors(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("direct_signup_full_name")
            )

            OutlinedTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it; validationError = null },
                label = { Text("Email ID*", color = MilaWarmStone) },
                placeholder = { Text("name@example.com", color = MilaWarmStone) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = milaFieldColors(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("direct_signup_email")
            )

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { if (it.length <= 15) mobileNumber = it; validationError = null },
                label = { Text("Mobile number (Optional)", color = MilaWarmStone) },
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
                    .testTag("direct_signup_mobile")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= 128) password = it; validationError = null },
                label = { Text("Password* (Minimum 6 characters)", color = MilaWarmStone) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = MilaWarmStone
                        )
                    }
                },
                colors = milaFieldColors(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("direct_signup_password")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = updatesOptIn,
                    onCheckedChange = { updatesOptIn = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MilaChampagneGold,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedColor = MilaWarmStone
                    ),
                    modifier = Modifier.testTag("direct_signup_updates_checkbox")
                )
                Text(
                    text = "Send me important updates & hospitality mandates via email, SMS and WhatsApp",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            validationError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                )
            }

            GoldFilledButton(
                text = if (isLoading) "Creating Account..." else "Register",
                enabled = !isLoading,
                testTag = "direct_signup_submit_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val err = SupabaseAuthValidator.validateQuickMilaRegistration(
                        fullName = fullName,
                        email = email,
                        password = password
                    )
                    validationError = err
                    if (err == null) {
                        onRegisterSubmit(
                            fullName.trim(),
                            email.trim(),
                            mobileNumber.trim(),
                            password,
                            updatesOptIn
                        )
                    }
                }
            )

            OrDividerRow()

            GoogleSignInButton(
                label = "Sign up with Google",
                outlined = true,
                onAuthSuccess = {},
                onAuthError = onGoogleAuthError
            )
        }
    }
}

@Composable
private fun MobileOtpCard(
    mobileInput: String,
    onMobileChange: (String) -> Unit,
    otpSentToPhone: String?,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    errorText: String?,
    onGetOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onChangeNumber: () -> Unit,
    onSwitchToEmail: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (otpSentToPhone == null) Icons.Rounded.PhoneAndroid else Icons.Rounded.Sms,
                    contentDescription = null,
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (otpSentToPhone == null) "MOBILE VERIFICATION" else "ENTER VERIFICATION CODE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.6.sp
                    )
                )
            }

            Text(
                text = if (otpSentToPhone == null) {
                    "Begin With Your Mobile Number"
                } else {
                    "Verify OTP to Continue"
                },
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )

            Text(
                text = if (otpSentToPhone == null) {
                    "Enter your mobile number to receive a one-time verification code and create your MILA Hospitality account."
                } else {
                    "We sent a 6-digit verification code to $otpSentToPhone."
                },
                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
            )

            if (otpSentToPhone == null) {
                OutlinedTextField(
                    value = mobileInput,
                    onValueChange = onMobileChange,
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
                        .testTag("otp_mobile_number_input")
                )
            } else {
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = onOtpChange,
                    label = { Text("6-Digit OTP *", color = MilaWarmStone) },
                    placeholder = { Text("Enter 6-digit code", color = MilaWarmStone) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = milaFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_code_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onChangeNumber,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("otp_change_number_button")
                    ) {
                        Text("Change Number", color = MilaIvoryMuted)
                    }
                    TextButton(
                        onClick = onGetOtp,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("otp_resend_button")
                    ) {
                        Text("Resend OTP", color = MilaChampagneGold)
                    }
                }
            }

            errorText?.let { msg ->
                AuthStatusBanner(message = msg, isError = true)
                GoldOutlineButton(
                    text = "Use Email & Password Instead",
                    onClick = onSwitchToEmail,
                    testTag = "otp_switch_to_email_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            GoldFilledButton(
                text = when {
                    isLoading && otpSentToPhone == null -> "Sending OTP..."
                    isLoading && otpSentToPhone != null -> "Verifying OTP..."
                    otpSentToPhone == null -> "Get OTP"
                    else -> "Submit OTP"
                },
                enabled = !isLoading,
                testTag = if (otpSentToPhone == null) "get_otp_button" else "submit_otp_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = if (otpSentToPhone == null) onGetOtp else onVerifyOtp
            )
        }
    }
}

@Composable
private fun CreateMilaAccountStep(
    verifiedPhone: VerifiedPhoneOtpSession,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRegisterSubmit: (fullName: String, email: String, password: String, updatesOptIn: Boolean) -> Unit,
    onGoogleAuthError: (String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var updatesOptIn by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("create_account_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to mobile verification",
                    tint = MilaIvory
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Create your MILA account",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MilaIvory,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Explore bespoke hospitality consultancy services & executive opportunities",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { if (it.length <= 100) fullName = it },
                        label = { Text("Full name*", color = MilaWarmStone) },
                        placeholder = { Text("Enter your full name", color = MilaWarmStone) },
                        singleLine = true,
                        colors = milaFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_mila_full_name")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { if (it.length <= 120) email = it },
                            label = { Text("Email ID*", color = MilaWarmStone) },
                            placeholder = { Text("name@example.com", color = MilaWarmStone) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = milaFieldColors(),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_mila_email")
                        )
                        Text(
                            text = "We'll send relevant service updates and confirmations to this email",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { if (it.length <= 128) password = it },
                            label = { Text("Password* (Minimum 6 characters)", color = MilaWarmStone) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password",
                                        tint = MilaWarmStone
                                    )
                                }
                            },
                            colors = milaFieldColors(),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_mila_password")
                        )
                        Text(
                            text = "This helps your account stay protected",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = formatDisplayPhone(verifiedPhone.phone),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mobile number*", color = MilaWarmStone) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Verified mobile number",
                                    tint = MilaEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            colors = milaFieldColors(),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_mila_verified_mobile")
                        )
                        Text(
                            text = "MILA Hospitality partners will contact you on this verified number",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = updatesOptIn,
                            onCheckedChange = { updatesOptIn = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MilaChampagneGold,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                uncheckedColor = MilaWarmStone
                            ),
                            modifier = Modifier.testTag("create_mila_updates_checkbox")
                        )
                        Text(
                            text = "Send me important updates & hospitality mandates via email, SMS and WhatsApp",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Text(
                        text = "By clicking Register, you agree to MILA Hospitality's Terms and Conditions and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MilaWarmStone,
                            fontSize = 11.sp
                        )
                    )

                    validationError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                        )
                    }

                    GoldFilledButton(
                        text = if (isLoading) "Creating Account..." else "Register",
                        enabled = !isLoading,
                        testTag = "create_mila_register_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val err = SupabaseAuthValidator.validateQuickMilaRegistration(
                                fullName = fullName,
                                email = email,
                                password = password
                            )
                            validationError = err
                            if (err == null) {
                                onRegisterSubmit(fullName.trim(), email.trim(), password, updatesOptIn)
                            }
                        }
                    )

                    OrDividerRow()

                    GoogleSignInButton(
                        label = "Sign up with Google",
                        outlined = true,
                        onAuthSuccess = {},
                        onAuthError = onGoogleAuthError
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OrDividerRow() {
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
            style = MaterialTheme.typography.bodySmall.copy(
                color = MilaWarmStone
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            color = MilaGoldSubtle,
            modifier = Modifier.weight(1f)
        )
    }
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

private fun formatDisplayPhone(e164: String): String {
    return if (e164.startsWith("+91") && e164.length == 13) {
        "+91 ${e164.substring(3)}"
    } else {
        e164
    }
}
