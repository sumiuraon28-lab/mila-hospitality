package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.auth.GoogleSignInButton
import com.example.auth.SupabaseAuthClient
import com.example.auth.SupabaseAuthValidator
import com.example.data.AdminRole
import com.example.data.AdminSessionState
import com.example.data.AdminUser
import com.example.data.SupabaseAdminSession
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
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
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * Completely separate, dedicated full-screen Executive Administration Login Console.
 * Strictly isolated from the Member/Client Mobile OTP & registration journey.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminLoginScreen(
    supabaseAuthClient: SupabaseAuthClient,
    firebaseAvailable: Boolean,
    currentFirebaseUser: FirebaseUser?,
    adminSessionState: AdminSessionState,
    onSupabaseAdminVerified: (SupabaseAdminSession) -> Unit,
    onRetryFirebaseAdminVerify: () -> Unit,
    onEnterAuthorizedDashboard: (AdminUser) -> Unit,
    onBackToMemberApp: () -> Unit
) {
    var staffEmail by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var recoveryNotice by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val supabaseConfigured = remember(supabaseAuthClient) { supabaseAuthClient.isConfigured() }

    BackHandler {
        if (isRecoveryMode) {
            isRecoveryMode = false
            inlineError = null
        } else {
            onBackToMemberApp()
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
            // Luxury architectural backdrop with darker executive charcoal scrim
            Image(
                painter = painterResource(id = R.drawable.img_hero_hotel_1795637232836),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xEB0A0908),
                            0.45f to Color(0xDE0E0D0B),
                            1.0f to Color(0xFA090807)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x24D6BD96), Color.Transparent),
                            radius = 850f
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top bar separating Admin Console from Member App
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (isRecoveryMode) {
                                    isRecoveryMode = false
                                    inlineError = null
                                } else {
                                    onBackToMemberApp()
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("admin_login_back_to_member_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to Member Login",
                                tint = MilaChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecoveryMode) "Admin Sign In" else "Member App",
                                style = MaterialTheme.typography.labelLarge.copy(color = MilaChampagneGold)
                            )
                        }

                        StatusPill(label = "Staff Only", tone = StatusTone.GOLD)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Executive Admin Branding Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_crest_1795637231719),
                                contentDescription = "MILA Executive Crest",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .border(1.5.dp, MilaChampagneGold, MaterialTheme.shapes.medium)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MilaCharcoalDeep)
                                    .border(1.dp, MilaChampagneGold, MaterialTheme.shapes.extraSmall),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MilaChampagneGold,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                color = MilaGoldMuted,
                                thickness = 0.75.dp,
                                modifier = Modifier.width(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MILA EXECUTIVE CONSOLE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MilaChampagneGold,
                                    letterSpacing = 2.6.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            HorizontalDivider(
                                color = MilaGoldMuted,
                                thickness = 0.75.dp,
                                modifier = Modifier.width(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isRecoveryMode) "Admin Password Recovery" else "Administrator Sign In",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = MilaIvory,
                                textAlign = TextAlign.Center
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Dedicated command portal for MILA leadership, consultants, recruitment leads, and content governance.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MilaIvoryMuted,
                                textAlign = TextAlign.Center
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Main Admin Authentication Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // If an authorized admin session is already verified, show quick direct-entry card
                        if (adminSessionState is AdminSessionState.Authorized) {
                            val verifiedAdmin = adminSessionState.adminUser
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
                                border = BorderStroke(1.25.dp, MilaChampagneGold),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VerifiedUser,
                                            contentDescription = null,
                                            tint = MilaChampagneGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        StatusPill(label = verifiedAdmin.resolvedRole.displayLabel, tone = StatusTone.GOLD)
                                    }
                                    Text(
                                        text = "Verified Staff Session Active",
                                        style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory)
                                    )
                                    Text(
                                        text = "${verifiedAdmin.displayName} • ${verifiedAdmin.email}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold)
                                    )
                                    Text(
                                        text = verifiedAdmin.resolvedRole.scopeDescription,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                                    )
                                    GoldFilledButton(
                                        text = "Open Admin Dashboard",
                                        onClick = { onEnterAuthorizedDashboard(verifiedAdmin) },
                                        testTag = "admin_login_enter_verified_dashboard",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Dedicated Staff Email & Password Card
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
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = MilaChampagneGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isRecoveryMode) "STAFF ACCOUNT RECOVERY" else "STAFF CREDENTIALS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MilaChampagneGold,
                                            letterSpacing = 1.6.sp
                                        )
                                    )
                                }

                                if (isRecoveryMode) {
                                    Text(
                                        text = "Enter your registered MILA staff email address to receive password recovery instructions.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                                    )
                                    OutlinedTextField(
                                        value = staffEmail,
                                        onValueChange = {
                                            staffEmail = it
                                            inlineError = null
                                        },
                                        label = { Text("Administrator Email *", color = MilaWarmStone) },
                                        placeholder = { Text("admin@milahospitality.in", color = MilaWarmStone) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        colors = adminLoginFieldColors(),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_recovery_email_input")
                                    )

                                    recoveryNotice?.let { notice ->
                                        Text(
                                            text = notice,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold)
                                        )
                                    }

                                    inlineError?.let { err ->
                                        Text(
                                            text = err,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                                        )
                                    }

                                    GoldFilledButton(
                                        text = if (isVerifying) "Sending Reset Link..." else "Send Recovery Link",
                                        enabled = !isVerifying,
                                        testTag = "admin_send_recovery_button",
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            val err = SupabaseAuthValidator.validateResetEmail(staffEmail)
                                            if (err != null) {
                                                inlineError = err
                                                return@GoldFilledButton
                                            }
                                            scope.launch {
                                                inlineError = null
                                                recoveryNotice = null
                                                isVerifying = true
                                                val res = supabaseAuthClient.sendPasswordReset(staffEmail)
                                                isVerifying = false
                                                res.fold(
                                                    onSuccess = { msg ->
                                                        recoveryNotice = msg
                                                        snackbarHostState.showSnackbar(msg)
                                                    },
                                                    onFailure = { e ->
                                                        inlineError = e.localizedMessage ?: "Unable to send recovery email."
                                                    }
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = staffEmail,
                                        onValueChange = {
                                            staffEmail = it
                                            inlineError = null
                                        },
                                        label = { Text("Administrator Email *", color = MilaWarmStone) },
                                        placeholder = { Text("name@milahospitality.in", color = MilaWarmStone) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        colors = adminLoginFieldColors(),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_login_email_input")
                                    )

                                    OutlinedTextField(
                                        value = staffPassword,
                                        onValueChange = {
                                            staffPassword = it
                                            inlineError = null
                                        },
                                        label = { Text("Password *", color = MilaWarmStone) },
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
                                        colors = adminLoginFieldColors(),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_login_password_input")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                inlineError = null
                                                recoveryNotice = null
                                                isRecoveryMode = true
                                            },
                                            modifier = Modifier.testTag("admin_forgot_password_button")
                                        ) {
                                            Text("Forgot Password?", color = MilaChampagneGold)
                                        }
                                    }

                                    if (isVerifying || adminSessionState is AdminSessionState.Loading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = MilaChampagneGold,
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "Verifying administrator role & permissions...",
                                                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory)
                                            )
                                        }
                                    }

                                    inlineError?.let { err ->
                                        Text(
                                            text = err,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)
                                        )
                                    }

                                    if (!supabaseConfigured && !firebaseAvailable) {
                                        Text(
                                            text = "Cloud authentication is currently unavailable in this build.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                                        )
                                    }

                                    GoldFilledButton(
                                        text = if (isVerifying) "Verifying Staff Access..." else "Sign In to Admin Dashboard",
                                        enabled = !isVerifying,
                                        testTag = "admin_login_submit_button",
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            val validationErr = SupabaseAuthValidator.validateLogin(staffEmail, staffPassword)
                                            if (validationErr != null) {
                                                inlineError = validationErr
                                                return@GoldFilledButton
                                            }
                                            scope.launch {
                                                inlineError = null
                                                isVerifying = true
                                                val res = supabaseAuthClient.signInAdminWithSession(staffEmail, staffPassword)
                                                isVerifying = false
                                                res.fold(
                                                    onSuccess = { session ->
                                                        onSupabaseAdminVerified(session)
                                                    },
                                                    onFailure = { err ->
                                                        inlineError = err.localizedMessage
                                                            ?: "Administrator verification failed."
                                                    }
                                                )
                                            }
                                        }
                                    )

                                    // Optional Firebase Google Staff Sign-In when Firebase is provisioned in this build
                                    if (firebaseAvailable) {
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

                                        if (currentFirebaseUser != null && adminSessionState is AdminSessionState.NotAdmin) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
                                                border = BorderStroke(1.dp, MilaTerracotta.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    StatusPill(label = "Not Authorised for Google Admin", tone = StatusTone.ALERT)
                                                    Text(
                                                        text = "${currentFirebaseUser.email ?: "Current Google account"} is not in the authorised MILA admin roster. Use your staff email & password above or switch to an authorised Google staff account.",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                                                    )
                                                    GoldOutlineButton(
                                                        text = "Re-check Google Staff Role",
                                                        onClick = onRetryFirebaseAdminVerify,
                                                        testTag = "admin_recheck_google_role_button",
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        } else {
                                            GoogleSignInButton(
                                                label = "Sign in with Authorized Staff Google",
                                                outlined = true,
                                                onAuthSuccess = onRetryFirebaseAdminVerify,
                                                onAuthError = { msg -> inlineError = msg }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Governance & RBAC Roles Footer
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface.copy(alpha = 0.85f)),
                            border = BorderStroke(1.dp, MilaGoldSubtle),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
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
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = MilaChampagneGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "ROLE-BASED ACCESS CONTROL",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MilaChampagneGold,
                                            letterSpacing = 1.4.sp
                                        )
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AdminRole.entries.forEach { role ->
                                        StatusPill(label = role.displayLabel, tone = StatusTone.MUTED)
                                    }
                                }
                                Text(
                                    text = "Public self-registration is disabled for administration. Access is verified server-side against authorised MILA staff records and every action is audit-logged.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MilaWarmStone,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun adminLoginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MilaChampagneGold,
    unfocusedBorderColor = MilaCharcoalBorder,
    focusedContainerColor = MilaCharcoalDeep,
    unfocusedContainerColor = MilaCharcoalDeep,
    focusedTextColor = MilaIvory,
    unfocusedTextColor = MilaIvory
)
