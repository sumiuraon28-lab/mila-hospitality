package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.CandidateValidator
import com.example.auth.PrivateCvMetadata
import com.example.auth.SupabaseTalentClient
import com.example.data.CandidateAvailability
import com.example.data.CandidateProfile
import com.example.data.HospitalityDepartment
import com.example.data.JobNotification
import com.example.data.PrivateCvLocalVault
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
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

@Composable
fun IAmLookingForJobAlertsCard(
    profile: CandidateProfile,
    onToggleAlerts: (Boolean) -> Unit,
    onBrowseVacancies: () -> Unit,
    pushEnabled: Boolean = true,
    emailAlertsEnabled: Boolean = false,
    onUpdateChannelPreferences: ((pushEnabled: Boolean, emailEnabled: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val active = profile.jobAlertsEnabled
    var localPush by remember(pushEnabled) { mutableStateOf(pushEnabled) }
    var localEmail by remember(emailAlertsEnabled) { mutableStateOf(emailAlertsEnabled) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        localPush = granted
        onUpdateChannelPreferences?.invoke(granted, localEmail)
    }

    fun requestPushIfNeededAndSync(nextPush: Boolean, nextEmail: Boolean) {
        if (nextPush && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !com.example.notifications.MilaNotificationChannels.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onUpdateChannelPreferences?.invoke(nextPush, nextEmail)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
        border = BorderStroke(1.dp, if (active) MilaEmerald else MilaGoldMuted),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (active) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = if (active) MilaEmerald else MilaWarmStone,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = if (active) "JOB ALERTS ACTIVE" else "JOB ALERTS PAUSED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (active) MilaEmerald else MilaChampagneGold,
                                letterSpacing = 1.4.sp
                            )
                        )
                        Text(
                            text = "${profile.resolvedDepartment.label} • ${profile.preferredJobLocations.ifBlank { profile.location.ifBlank { "All India" } }}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                        )
                    }
                }
                Switch(
                    checked = active,
                    onCheckedChange = { enabled ->
                        onToggleAlerts(enabled)
                        if (enabled && localPush) {
                            requestPushIfNeededAndSync(true, localEmail)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MilaCharcoalDeep,
                        checkedTrackColor = MilaEmerald,
                        uncheckedThumbColor = MilaWarmStone,
                        uncheckedTrackColor = MilaCharcoalSurface
                    ),
                    modifier = Modifier.testTag("job_alerts_switch")
                )
            }

            if (active && onUpdateChannelPreferences != null) {
                HorizontalDivider(color = MilaGoldSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mobile Push Alerts (FCM)",
                            style = MaterialTheme.typography.labelLarge.copy(color = MilaIvory)
                        )
                        Text(
                            text = "Matched vacancies & application stage updates on this device",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }
                    Switch(
                        checked = localPush,
                        onCheckedChange = { enabled ->
                            localPush = enabled
                            requestPushIfNeededAndSync(enabled, localEmail)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MilaCharcoalDeep,
                            checkedTrackColor = MilaChampagneGold,
                            uncheckedThumbColor = MilaWarmStone,
                            uncheckedTrackColor = MilaCharcoalSurface
                        ),
                        modifier = Modifier.testTag("candidate_fcm_push_switch")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Optional Email Alerts",
                            style = MaterialTheme.typography.labelLarge.copy(color = MilaIvory)
                        )
                        Text(
                            text = "Receive matched mandate digests at ${profile.email.ifBlank { "your email" }}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }
                    Switch(
                        checked = localEmail,
                        onCheckedChange = { enabled ->
                            localEmail = enabled
                            onUpdateChannelPreferences(localPush, enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MilaCharcoalDeep,
                            checkedTrackColor = MilaChampagneGold,
                            uncheckedThumbColor = MilaWarmStone,
                            uncheckedTrackColor = MilaCharcoalSurface
                        ),
                        modifier = Modifier.testTag("candidate_email_alerts_switch")
                    )
                }
            }

            GoldFilledButton(
                text = if (active) "I AM LOOKING FOR A JOB • BROWSE ROLES" else "I AM LOOKING FOR A JOB",
                onClick = {
                    if (!active) {
                        onToggleAlerts(true)
                        if (localPush) requestPushIfNeededAndSync(true, localEmail)
                    } else {
                        onBrowseVacancies()
                    }
                },
                testTag = "portal_iam_looking_for_job_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PrivateCvVaultCard(
    userId: String,
    profile: CandidateProfile,
    supabaseTalentClient: SupabaseTalentClient,
    onCvUploaded: (PrivateCvMetadata) -> Unit,
    onCvDeleted: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessing = true
            val readResult = PrivateCvLocalVault.readPickedCv(context, uri)
            readResult.fold(
                onSuccess = { picked ->
                    val uploadResult = supabaseTalentClient.uploadOrReplacePrivateCv(
                        userId = userId,
                        fileName = picked.fileName,
                        mimeType = picked.mimeType,
                        bytes = picked.bytes,
                        previousStoragePath = profile.cvStoragePath
                    )
                    uploadResult.fold(
                        onSuccess = { metadata ->
                            PrivateCvLocalVault.saveOwnerOnlyCopy(
                                context = context,
                                userId = userId,
                                storagePath = metadata.storagePath,
                                bytes = picked.bytes,
                                previousStoragePath = profile.cvStoragePath
                            )
                            onCvUploaded(metadata)
                        },
                        onFailure = { err ->
                            onError(err.localizedMessage ?: "Private CV upload failed.")
                        }
                    )
                },
                onFailure = { err ->
                    onError(err.localizedMessage ?: "Unsupported CV file. Use PDF, DOC, or DOCX.")
                }
            )
            isProcessing = false
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        text = "PRIVATE CV VAULT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }
                StatusPill(label = "No Public Links", tone = StatusTone.EMERALD)
            }

            Text(
                text = "Upload your CV in PDF, DOC, or DOCX (max 10 MB). Stored strictly in owner-scoped private storage with zero public download URLs.",
                style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
            )

            if (profile.hasCvUploaded) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
                    border = BorderStroke(1.dp, MilaCharcoalBorder),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = "Uploaded CV",
                            tint = MilaChampagneGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.cvFileName.orEmpty(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MilaIvory,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            val sizeKb = ((profile.cvFileSizeBytes ?: 0L) / 1024L).coerceAtLeast(1L)
                            val extLabel = profile.cvFileName.orEmpty().substringAfterLast('.', "PDF").uppercase()
                            Text(
                                text = "$extLabel • $sizeKb KB • Private Bucket Vault",
                                style = MaterialTheme.typography.bodySmall.copy(color = MilaEmerald)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldOutlineButton(
                        text = if (isProcessing) "Replacing..." else "Replace CV",
                        onClick = {
                            if (!isProcessing) {
                                documentPicker.launch(CandidateValidator.ALLOWED_CV_MIME_TYPES.toTypedArray())
                            }
                        },
                        testTag = "replace_cv_button",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = !isProcessing,
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                supabaseTalentClient.deletePrivateCv(profile.cvStoragePath)
                                PrivateCvLocalVault.deleteOwnerOnlyCopy(context, userId, profile.cvStoragePath)
                                onCvDeleted()
                                isProcessing = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_cv_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MilaTerracotta, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete CV", color = MilaTerracotta)
                    }
                }
            } else {
                GoldFilledButton(
                    text = if (isProcessing) "Uploading CV..." else "Upload CV (PDF, DOC, DOCX)",
                    enabled = !isProcessing,
                    testTag = "upload_cv_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        documentPicker.launch(CandidateValidator.ALLOWED_CV_MIME_TYPES.toTypedArray())
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandidateProfileEditorCard(
    initialProfile: CandidateProfile,
    isSaving: Boolean,
    onSaveProfile: (CandidateProfile) -> Unit
) {
    var fullName by remember(initialProfile) { mutableStateOf(initialProfile.fullName) }
    var email by remember(initialProfile) { mutableStateOf(initialProfile.email) }
    var mobileNumber by remember(initialProfile) { mutableStateOf(initialProfile.mobileNumber) }
    var location by remember(initialProfile) { mutableStateOf(initialProfile.location) }
    var currentDesignation by remember(initialProfile) { mutableStateOf(initialProfile.currentDesignation) }
    var department by remember(initialProfile) { mutableStateOf(initialProfile.resolvedDepartment) }
    var totalExpText by remember(initialProfile) {
        mutableStateOf(if (initialProfile.totalExperienceYears > 0L) initialProfile.totalExperienceYears.toString() else "")
    }
    var currentEmployer by remember(initialProfile) { mutableStateOf(initialProfile.currentEmployer) }
    var previousEmployers by remember(initialProfile) { mutableStateOf(initialProfile.previousEmployers) }
    var education by remember(initialProfile) { mutableStateOf(initialProfile.education) }
    var skills by remember(initialProfile) { mutableStateOf(initialProfile.skills) }
    var preferredLocations by remember(initialProfile) { mutableStateOf(initialProfile.preferredJobLocations) }
    var expectedSalary by remember(initialProfile) { mutableStateOf(initialProfile.expectedSalary) }
    var availability by remember(initialProfile) { mutableStateOf(initialProfile.resolvedAvailability) }
    var noticePeriod by remember(initialProfile) { mutableStateOf(initialProfile.noticePeriod.ifBlank { "30 Days" }) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = MilaChampagneGold, modifier = Modifier.size(18.dp))
                Text(
                    text = "CANDIDATE CAREER DOSSIER (15 FIELDS)",
                    style = MaterialTheme.typography.labelMedium.copy(color = MilaChampagneGold, letterSpacing = 1.4.sp)
                )
            }

            CandidateTextField(fullName, { if (it.length <= 100) fullName = it }, "1. Full Name *", "Full Name", "edit_cand_full_name")
            CandidateTextField(email, { if (it.length <= 120) email = it }, "2. Email *", "candidate@hospitality.in", "edit_cand_email", KeyboardType.Email)
            CandidateTextField(mobileNumber, { if (it.length <= 25) mobileNumber = it }, "3. Mobile Number *", "+91 98100 00000", "edit_cand_mobile", KeyboardType.Phone)
            CandidateTextField(location, { if (it.length <= 80) location = it }, "4. Current Location *", "e.g., Mumbai, Maharashtra", "edit_cand_location")
            CandidateTextField(currentDesignation, { if (it.length <= 100) currentDesignation = it }, "5. Current Designation *", "e.g., General Manager / Head Chef", "edit_cand_designation")

            Text("6. DEPARTMENT *", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        modifier = Modifier.testTag("edit_cand_dept_${dept.name}")
                    )
                }
            }

            CandidateTextField(
                value = totalExpText,
                onValueChange = { input -> if (input.length <= 2 && input.all { it.isDigit() }) totalExpText = input },
                label = "7. Total Experience (Years) *",
                placeholder = "e.g., 10",
                testTag = "edit_cand_experience",
                keyboardType = KeyboardType.Number
            )
            CandidateTextField(currentEmployer, { if (it.length <= 120) currentEmployer = it }, "8. Current Employer *", "Hotel or Hospitality Group", "edit_cand_current_employer")
            CandidateTextField(previousEmployers, { if (it.length <= 500) previousEmployers = it }, "9. Previous Employers", "Prior hotels, resorts, or F&B groups", "edit_cand_prev_employers", minLines = 2)
            CandidateTextField(education, { if (it.length <= 200) education = it }, "10. Education *", "Degree, Hospitality Institute & Certifications", "edit_cand_education")
            CandidateTextField(skills, { if (it.length <= 400) skills = it }, "11. Key Skills *", "Pre-opening, Mixology, P&L, HACCP", "edit_cand_skills", minLines = 2)
            CandidateTextField(preferredLocations, { if (it.length <= 200) preferredLocations = it }, "12. Preferred Job Locations *", "Mumbai, Goa, Bengaluru, Udaipur", "edit_cand_pref_locations")
            CandidateTextField(expectedSalary, { if (it.length <= 80) expectedSalary = it }, "13. Expected Salary *", "e.g., ₹24 – 30 LPA", "edit_cand_salary")

            Text("14. AVAILABILITY *", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        modifier = Modifier.testTag("edit_cand_avail_${avail.name}")
                    )
                }
            }

            Text("15. NOTICE PERIOD *", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

            validationError?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(color = MilaTerracotta)) }

            GoldFilledButton(
                text = if (isSaving) "Saving Career Dossier..." else "Save Candidate Profile",
                enabled = !isSaving,
                testTag = "save_candidate_profile_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val updated = initialProfile.copy(
                        fullName = fullName.trim(),
                        email = email.trim(),
                        mobileNumber = mobileNumber.trim(),
                        location = location.trim(),
                        currentDesignation = currentDesignation.trim(),
                        department = department.name,
                        totalExperienceYears = totalExpText.toLongOrNull() ?: -1L,
                        currentEmployer = currentEmployer.trim(),
                        previousEmployers = previousEmployers.trim(),
                        education = education.trim(),
                        skills = skills.trim(),
                        preferredJobLocations = preferredLocations.trim(),
                        expectedSalary = expectedSalary.trim(),
                        availability = availability.name,
                        noticePeriod = noticePeriod.trim()
                    )
                    val err = CandidateValidator.validateProfileFields(updated)
                    validationError = err
                    if (err == null) onSaveProfile(updated)
                }
            )
        }
    }
}

@Composable
fun JobNotificationCard(
    notification: JobNotification,
    onMarkRead: () -> Unit,
    onViewCareers: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MilaCharcoalSurface else MilaCharcoalElevated
        ),
        border = BorderStroke(1.dp, if (notification.isRead) MilaCharcoalBorder else MilaChampagneGold),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = if (notification.isRead) notification.department else "NEW • ${notification.department}",
                    tone = if (notification.isRead) StatusTone.MUTED else StatusTone.GOLD
                )
                IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Dismiss notification", tint = MilaWarmStone)
                }
            }
            Text(notification.title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory, fontWeight = FontWeight.SemiBold))
            Text(notification.message, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
            HorizontalDivider(color = MilaGoldSubtle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!notification.isRead) {
                    TextButton(onClick = onMarkRead) {
                        Text("Mark Read", color = MilaChampagneGold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onViewCareers) {
                    Text("View Vacancy ->", color = MilaChampagneGold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
