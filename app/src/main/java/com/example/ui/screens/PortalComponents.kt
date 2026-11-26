package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.signOut
import com.example.data.ApplicationStatus
import com.example.data.ConsultationInquiry
import com.example.data.InquiryStatus
import com.example.data.JobApplication
import com.example.data.SavedRole
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone
import com.google.firebase.auth.FirebaseUser

enum class AuthIntentMode(val label: String) {
    LOGIN("Log In"),
    REGISTER("Register")
}

@Composable
fun AuthIntentToggle(
    selectedMode: AuthIntentMode,
    onSelectMode: (AuthIntentMode) -> Unit,
    prefixTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AuthIntentMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            FilterChip(
                selected = selected,
                onClick = { onSelectMode(mode) },
                label = { Text(mode.label) },
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
                modifier = Modifier.testTag("${prefixTag}_${mode.name}")
            )
        }
    }
}

@Composable
fun PortalUserHeader(
    eyebrow: String,
    user: FirebaseUser?,
    testTag: String,
    displayNameOverride: String? = null,
    emailOverride: String? = null,
    onAdditionalSignOut: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val resolvedName = displayNameOverride?.takeIf { it.isNotBlank() }
        ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: "Authenticated Member"
    val resolvedEmail = emailOverride?.takeIf { it.isNotBlank() }
        ?: user?.email.orEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.4.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resolvedName,
                    style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory)
                )
                if (resolvedEmail.isNotBlank()) {
                    Text(
                        text = resolvedEmail,
                        style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    signOut(credentialManager, scope) {
                        onAdditionalSignOut?.invoke()
                    }
                },
                border = BorderStroke(1.dp, MilaCharcoalBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MilaIvoryMuted),
                modifier = Modifier.testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Sign out",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun PortalHighlightList(
    title: String,
    bullets: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.4.sp
                )
            )
            bullets.forEach { item ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Text(item, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                }
            }
        }
    }
}

@Composable
fun ClientInquiryCard(
    inquiry: ConsultationInquiry,
    onEditNotes: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit
) {
    val status = inquiry.resolvedStatus
    val tone = when (status) {
        InquiryStatus.SUBMITTED, InquiryStatus.PROPOSAL_SHARED -> StatusTone.GOLD
        InquiryStatus.UNDER_REVIEW, InquiryStatus.CONVERTED -> StatusTone.EMERALD
        InquiryStatus.WITHDRAWN -> StatusTone.MUTED
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(label = status.label, tone = tone)
                IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete enquiry", tint = MilaWarmStone)
                }
            }
            Text(inquiry.propertyName, style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory))
            Text("${inquiry.resolvedServiceType.label} • ${inquiry.city}", style = MaterialTheme.typography.titleSmall.copy(color = MilaChampagneGold))
            Text("Scale: ${inquiry.scaleSummary}", style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone))
            if (!inquiry.briefNotes.isNullOrBlank()) {
                Text(inquiry.briefNotes, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted))
            }
            if (status != InquiryStatus.WITHDRAWN) {
                HorizontalDivider(color = MilaGoldSubtle)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GoldOutlineButton("Edit Notes", onEditNotes, "edit_inquiry_${inquiry.id}", Modifier.weight(1f))
                    TextButton(onClick = onWithdraw, modifier = Modifier.weight(1f)) {
                        Text("Withdraw", color = MilaTerracotta)
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateApplicationCard(
    application: JobApplication,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit
) {
    val status = application.resolvedStatus
    val tone = when (status) {
        ApplicationStatus.APPLIED, ApplicationStatus.OFFER_EXTENDED -> StatusTone.GOLD
        ApplicationStatus.UNDER_REVIEW, ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.HIRED -> StatusTone.EMERALD
        ApplicationStatus.DECLINED -> StatusTone.ALERT
        ApplicationStatus.WITHDRAWN -> StatusTone.MUTED
    }
    val stageSummary = when (status) {
        ApplicationStatus.APPLIED -> "Stage 1 of 5 • Submitted to MILA Talent"
        ApplicationStatus.UNDER_REVIEW -> "Stage 2 of 5 • Consultant Review"
        ApplicationStatus.SHORTLISTED -> "Stage 3 of 5 • Shortlisted for Client"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "Stage 4 of 5 • Executive Interview"
        ApplicationStatus.OFFER_EXTENDED -> "Stage 5 of 5 • Offer Extended"
        ApplicationStatus.HIRED -> "Placement Completed • Hired"
        ApplicationStatus.DECLINED -> "Application Closed"
        ApplicationStatus.WITHDRAWN -> "Application Withdrawn"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = status.label,
                    tone = tone
                )
                IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete application", tint = MilaWarmStone)
                }
            }
            Text(application.roleTitle, style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory))
            Text("${application.location} • ${application.yearsExperience} yrs experience", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
            Text("Current Group: ${application.currentProperty}", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
            Text(
                text = stageSummary,
                style = MaterialTheme.typography.labelMedium.copy(color = MilaWarmStone)
            )
            if (!application.cvFileName.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = "Private CV attached",
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Private CV Attached: ${application.cvFileName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold)
                    )
                }
            }
            if (status != ApplicationStatus.WITHDRAWN) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onWithdraw) {
                        Text("Withdraw Application", color = MilaTerracotta)
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateSavedRoleCard(
    saved: SavedRole,
    onApply: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(label = saved.category, tone = StatusTone.GOLD)
            Text(saved.roleTitle, style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory))
            Text("${saved.location} • ${saved.compensation}", style = MaterialTheme.typography.bodyMedium.copy(color = MilaChampagneGold))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldFilledButton("Apply Now", onApply, "portal_apply_${saved.roleId}", Modifier.weight(1f))
                GoldOutlineButton("Remove", onRemove, "portal_remove_${saved.roleId}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PortalLoadingIndicator() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = MilaChampagneGold)
        Text("Loading your records...", style = MaterialTheme.typography.bodyMedium.copy(color = MilaWarmStone))
    }
}

@Composable
fun PortalErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaTerracotta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = MilaTerracotta),
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
fun PortalEmptyCard(
    icon: ImageVector,
    title: String,
    body: String,
    ctaLabel: String,
    ctaTag: String,
    onCta: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MilaChampagneGold, modifier = Modifier.size(30.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory))
            Text(body, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted))
            GoldFilledButton(ctaLabel, onCta, ctaTag, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PortalEditBriefDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notes by remember { mutableStateOf(initialNotes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MilaCharcoalSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = MilaChampagneGold)
                Text("Update Enquiry Notes", color = MilaIvory)
            }
        },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 1000) notes = it },
                label = { Text("Project Scope & Launch Notes") },
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MilaChampagneGold,
                    unfocusedBorderColor = MilaCharcoalBorder,
                    focusedTextColor = MilaIvory,
                    unfocusedTextColor = MilaIvory
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes) }) {
                Text("Save Notes", color = MilaChampagneGold, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MilaWarmStone)
            }
        }
    )
}
