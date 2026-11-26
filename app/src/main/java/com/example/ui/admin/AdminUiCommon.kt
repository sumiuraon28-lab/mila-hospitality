package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.GoogleSignInButton
import com.example.data.AdminModule
import com.example.data.AdminSessionState
import com.example.data.AdminUser
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaWarmStone
import com.google.firebase.auth.FirebaseUser

@Composable
fun AdminModuleHeader(
    module: AdminModule,
    subtitle: String,
    canAdd: Boolean = false,
    addLabel: String = "Add Record",
    onAddClick: (() -> Unit)? = null,
    isReadOnly: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MODULE ${module.number.toString().padStart(2, '0')} • ${module.group.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.4.sp
                )
            )
            if (isReadOnly) {
                StatusPill(label = "Read-Only", tone = StatusTone.MUTED)
            }
        }
        Text(
            text = module.title,
            style = MaterialTheme.typography.headlineMedium.copy(color = MilaIvory)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
        )
        if (canAdd && !isReadOnly && onAddClick != null) {
            GoldFilledButton(
                text = "+ $addLabel",
                onClick = onAddClick,
                testTag = "admin_add_${module.name}",
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AdminSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = MilaWarmStone) },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MilaChampagneGold)
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MilaChampagneGold,
            unfocusedBorderColor = MilaCharcoalBorder,
            focusedContainerColor = MilaCharcoalSurface,
            unfocusedContainerColor = MilaCharcoalSurface,
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
fun AdminInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = minLines == 1,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MilaWarmStone) },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MilaChampagneGold,
            unfocusedBorderColor = MilaCharcoalBorder,
            focusedContainerColor = MilaCharcoalElevated,
            unfocusedContainerColor = MilaCharcoalElevated,
            focusedTextColor = MilaIvory,
            unfocusedTextColor = MilaIvory
        ),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AdminKpiTile(
    figure: String,
    label: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = figure,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MilaChampagneGold,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(color = MilaIvory)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
            )
        }
    }
}

@Composable
fun AdminEmptyState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "No matching database records",
                style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
            )
        }
    }
}

@Composable
fun AdminStaffGateDialog(
    currentUser: FirebaseUser?,
    adminSessionState: AdminSessionState,
    onRetryVerify: () -> Unit,
    onEnterPortal: (AdminUser) -> Unit,
    onAuthError: (String) -> Unit,
    onDismiss: () -> Unit,
    authError: String? = null,
    supabaseConfigured: Boolean = false,
    isSupabaseVerifying: Boolean = false,
    onSupabaseAdminLogin: ((String, String) -> Unit)? = null
) {
    var staffEmail by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MilaCharcoalSurface,
        titleContentColor = MilaIvory,
        textContentColor = MilaIvoryMuted,
        icon = {
            Icon(
                imageVector = if (adminSessionState is AdminSessionState.Authorized) {
                    Icons.Rounded.AdminPanelSettings
                } else {
                    Icons.Rounded.Lock
                },
                contentDescription = "Authorised MILA Staff Gate",
                tint = MilaChampagneGold
            )
        },
        title = {
            Text(
                text = "MILA Administration Access",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    adminSessionState is AdminSessionState.Authorized -> {
                        val admin = adminSessionState.adminUser
                        StatusPill(label = admin.resolvedRole.displayLabel, tone = StatusTone.GOLD)
                        Text(
                            text = "Verified as ${admin.displayName} (${admin.email}).",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory)
                        )
                        Text(
                            text = admin.resolvedRole.scopeDescription,
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                        GoldFilledButton(
                            text = "Enter MILA Admin Portal",
                            onClick = { onEnterPortal(admin) },
                            testTag = "enter_admin_portal_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    adminSessionState is AdminSessionState.Loading || isSupabaseVerifying -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MilaChampagneGold,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Verifying staff role & permissions...", color = MilaIvory)
                        }
                    }
                    else -> {
                        if (currentUser != null) {
                            StatusPill(label = "Access Denied", tone = StatusTone.ALERT)
                            Text(
                                text = "Signed in account (${currentUser.email ?: "unknown"}) is not authorised as a MILA administrator. Ordinary clients and job seekers cannot access administrative tools.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                            )
                            GoldOutlineButton(
                                text = "Re-check Authorization",
                                onClick = onRetryVerify,
                                testTag = "recheck_admin_auth_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "This administration console is strictly restricted to verified MILA Hospitality staff. Verify with an authorised staff account.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                            )
                            GoogleSignInButton(
                                onAuthSuccess = onRetryVerify,
                                onAuthError = onAuthError
                            )
                        }

                        if (supabaseConfigured && onSupabaseAdminLogin != null) {
                            Text(
                                text = "Or verify via Supabase Staff Role Check (server-side RPC):",
                                style = MaterialTheme.typography.labelMedium.copy(color = MilaChampagneGold)
                            )
                            OutlinedTextField(
                                value = staffEmail,
                                onValueChange = { staffEmail = it },
                                label = { Text("Staff Email", color = MilaWarmStone) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MilaChampagneGold,
                                    unfocusedBorderColor = MilaCharcoalBorder,
                                    focusedContainerColor = MilaCharcoalElevated,
                                    unfocusedContainerColor = MilaCharcoalElevated,
                                    focusedTextColor = MilaIvory,
                                    unfocusedTextColor = MilaIvory
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_supabase_email")
                            )
                            OutlinedTextField(
                                value = staffPassword,
                                onValueChange = { staffPassword = it },
                                label = { Text("Staff Password", color = MilaWarmStone) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MilaChampagneGold,
                                    unfocusedBorderColor = MilaCharcoalBorder,
                                    focusedContainerColor = MilaCharcoalElevated,
                                    unfocusedContainerColor = MilaCharcoalElevated,
                                    focusedTextColor = MilaIvory,
                                    unfocusedTextColor = MilaIvory
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_supabase_password")
                            )
                            GoldFilledButton(
                                text = "Verify Staff Role",
                                onClick = { onSupabaseAdminLogin(staffEmail, staffPassword) },
                                testTag = "admin_supabase_verify_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!authError.isNullOrBlank()) {
                            StatusPill(label = authError, tone = StatusTone.ALERT)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MilaChampagneGold)
            }
        }
    )
}
