package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminModule
import com.example.data.AdminUser
import com.example.data.CandidateProfile
import com.example.data.JobNotification
import com.example.data.JobVacancyRecord
import com.example.notifications.CandidateDeviceToken
import com.example.notifications.DeliveryStatus
import com.example.notifications.JobMatchingEngine
import com.example.notifications.NotificationChannelType
import com.example.notifications.NotificationControlSettings
import com.example.notifications.NotificationDeliveryLog
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
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone

private enum class NotificationCenterTab(val label: String) {
    CONTROLS_AND_MATCHING("Controls & Auto-Match"),
    DELIVERY_LOGS("Delivery Logs"),
    SENT_ALERTS("In-App Alerts")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminNotificationsCenterModule(
    actor: AdminUser,
    controls: NotificationControlSettings,
    vacancies: List<JobVacancyRecord>,
    candidates: List<CandidateProfile>,
    deviceTokens: List<CandidateDeviceToken>,
    notifications: List<JobNotification>,
    deliveryLogs: List<NotificationDeliveryLog>,
    onSaveControls: (NotificationControlSettings) -> Unit,
    onDispatchMatchedVacancy: (JobVacancyRecord) -> Unit,
    onSendDirectNotification: (String, String, String, String, String, String) -> Unit,
    onDeleteNotification: (JobNotification) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.NOTIFICATION_MANAGEMENT)
    var activeTab by remember { mutableStateOf(NotificationCenterTab.CONTROLS_AND_MATCHING) }
    var showCompose by remember { mutableStateOf(false) }

    val publishedVacancies = remember(vacancies) { vacancies.filter { it.published } }
    var selectedVacancyId by remember(publishedVacancies) {
        mutableStateOf(publishedVacancies.firstOrNull()?.id.orEmpty())
    }
    val selectedVacancy = remember(publishedVacancies, selectedVacancyId) {
        publishedVacancies.firstOrNull { it.id == selectedVacancyId } ?: publishedVacancies.firstOrNull()
    }
    val matchPreview = remember(selectedVacancy, candidates, controls) {
        if (selectedVacancy != null) {
            JobMatchingEngine.evaluateAllCandidates(selectedVacancy, candidates, controls)
        } else {
            emptyList()
        }
    }
    val eligibleMatches = remember(matchPreview) { matchPreview.filter { it.eligible } }
    val excludedMatches = remember(matchPreview) { matchPreview.filter { !it.eligible } }

    var logChannelFilter by remember { mutableStateOf("ALL") }
    val filteredLogs = remember(deliveryLogs, logChannelFilter) {
        if (logChannelFilter == "ALL") deliveryLogs
        else deliveryLogs.filter { it.channel.equals(logChannelFilter, ignoreCase = true) }
    }

    val androidTokenCount = remember(deviceTokens) {
        deviceTokens.count { it.active && it.platform.equals("ANDROID", ignoreCase = true) }
    }
    val iosTokenCount = remember(deviceTokens) {
        deviceTokens.count { it.active && it.platform.equals("IOS", ignoreCase = true) }
    }
    val emailOptInCount = remember(deviceTokens) {
        deviceTokens.count { it.active && it.emailEnabled }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.NOTIFICATION_MANAGEMENT,
                subtitle = "Automated candidate matching (department, location, experience, preferences & active status) across FCM Android/iOS push, in-app, and optional email with full delivery logs.",
                canAdd = canWrite && candidates.isNotEmpty(),
                addLabel = "Send Direct Alert",
                onAddClick = { showCompose = true },
                isReadOnly = !canWrite
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiTile(
                    figure = deliveryLogs.size.toString(),
                    label = "Delivery Logs",
                    detail = "${deliveryLogs.count { it.resolvedStatus == DeliveryStatus.DELIVERED }} delivered",
                    modifier = Modifier.weight(1f)
                )
                AdminKpiTile(
                    figure = "${androidTokenCount}/${iosTokenCount}",
                    label = "FCM Devices",
                    detail = "Android / iOS • $emailOptInCount email opt-in",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NotificationCenterTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    val label = when (tab) {
                        NotificationCenterTab.DELIVERY_LOGS -> "${tab.label} (${deliveryLogs.size})"
                        NotificationCenterTab.SENT_ALERTS -> "${tab.label} (${notifications.size})"
                        else -> tab.label
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { activeTab = tab },
                        label = { Text(label) },
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
                        modifier = Modifier.testTag("admin_notif_tab_${tab.name}")
                    )
                }
            }
        }

        when (activeTab) {
            NotificationCenterTab.CONTROLS_AND_MATCHING -> {
                item {
                    NotificationControlsCard(
                        controls = controls,
                        canWrite = canWrite,
                        onSaveControls = onSaveControls
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                        border = BorderStroke(1.dp, MilaGoldSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "TARGETED VACANCY MATCH PREVIEW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MilaChampagneGold,
                                    letterSpacing = 1.4.sp
                                )
                            )
                            Text(
                                text = "Irrelevant vacancies are never broadcast to all candidates. Select a published mandate to inspect eligible candidates before dispatching.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                            )
                            if (publishedVacancies.isEmpty()) {
                                Text(
                                    text = "Publish a job vacancy first to preview eligible candidate matches.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                                )
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    publishedVacancies.take(8).forEach { vac ->
                                        val isSelected = selectedVacancy?.id == vac.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedVacancyId = vac.id },
                                            label = { Text("${vac.title} (${vac.cityRegion})") }
                                        )
                                    }
                                }

                                selectedVacancy?.let { vac ->
                                    HorizontalDivider(color = MilaGoldSubtle)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(
                                            label = "${eligibleMatches.size} Eligible Matched",
                                            tone = if (eligibleMatches.isNotEmpty()) StatusTone.EMERALD else StatusTone.MUTED
                                        )
                                        StatusPill(
                                            label = "${excludedMatches.size} Filtered Out",
                                            tone = StatusTone.GOLD
                                        )
                                    }

                                    if (eligibleMatches.isNotEmpty()) {
                                        eligibleMatches.take(6).forEach { match ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
                                                border = BorderStroke(0.5.dp, MilaEmerald),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "${match.candidate.fullName} • ${match.candidate.currentDesignation}",
                                                            style = MaterialTheme.typography.titleSmall.copy(color = MilaIvory)
                                                        )
                                                        StatusPill(label = "${match.matchScore}%", tone = StatusTone.EMERALD)
                                                    }
                                                    Text(
                                                        text = match.matchedCriteria.joinToString(" • "),
                                                        style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "No registered candidates currently match all active criteria for ${vac.title}.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                                        )
                                    }

                                    if (excludedMatches.isNotEmpty()) {
                                        Text(
                                            text = "EXCLUDED CANDIDATES (PROTECTED FROM IRRELEVANT ALERTS)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MilaWarmStone,
                                                letterSpacing = 1.1.sp
                                            )
                                        )
                                        excludedMatches.take(4).forEach { ex ->
                                            Text(
                                                text = "• ${ex.candidate.fullName}: ${ex.exclusionReasons.joinToString("; ")}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                                            )
                                        }
                                    }

                                    if (canWrite) {
                                        GoldFilledButton(
                                            text = "Dispatch Alerts to ${eligibleMatches.size} Eligible Candidate(s)",
                                            onClick = { onDispatchMatchedVacancy(vac) },
                                            enabled = eligibleMatches.isNotEmpty(),
                                            testTag = "admin_dispatch_matched_vacancy",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            NotificationCenterTab.DELIVERY_LOGS -> {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf("ALL") + NotificationChannelType.entries.map { it.name }).forEach { ch ->
                            val selected = logChannelFilter == ch
                            FilterChip(
                                selected = selected,
                                onClick = { logChannelFilter = ch },
                                label = { Text(ch.replace('_', ' ')) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MilaChampagneGold,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MilaCharcoalSurface,
                                    labelColor = MilaIvoryMuted
                                )
                            )
                        }
                    }
                }
                if (filteredLogs.isEmpty()) {
                    item {
                        AdminEmptyState("No notification delivery logs recorded yet. Publish a vacancy or update an application status to generate per-channel audit entries.")
                    }
                } else {
                    items(filteredLogs, key = { it.id.ifBlank { it.recipientUserId + it.channel + it.roleId } }) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                            border = BorderStroke(1.dp, MilaGoldSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatusPill(label = log.resolvedChannel.label, tone = StatusTone.GOLD)
                                    StatusPill(
                                        label = log.resolvedStatus.label,
                                        tone = when (log.resolvedStatus) {
                                            DeliveryStatus.DELIVERED -> StatusTone.EMERALD
                                            DeliveryStatus.FAILED -> StatusTone.ALERT
                                            DeliveryStatus.QUEUED -> StatusTone.GOLD
                                            DeliveryStatus.SKIPPED -> StatusTone.MUTED
                                        }
                                    )
                                }
                                Text(
                                    text = "${log.resolvedEventType.label}: ${log.roleTitle}",
                                    style = MaterialTheme.typography.titleSmall.copy(color = MilaIvory)
                                )
                                Text(
                                    text = "Recipient: ${log.recipientName} (${log.recipientEmail.ifBlank { log.recipientUserId }})",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold)
                                )
                                if (log.matchSummary.isNotBlank()) {
                                    Text(
                                        text = "Match (${log.matchScore}%): ${log.matchSummary}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                                    )
                                }
                                Text(
                                    text = "${log.detail} • Actor: ${log.actorEmail}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                                )
                            }
                        }
                    }
                }
            }

            NotificationCenterTab.SENT_ALERTS -> {
                if (notifications.isEmpty()) {
                    item { AdminEmptyState("No in-app candidate notifications dispatched yet.") }
                } else {
                    items(notifications, key = { it.id.ifBlank { it.userId + it.title } }) { notif ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                            border = BorderStroke(1.dp, MilaGoldSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatusPill(label = notif.department, tone = StatusTone.GOLD)
                                    StatusPill(
                                        label = if (notif.isRead) "Read" else "Unread",
                                        tone = if (notif.isRead) StatusTone.MUTED else StatusTone.EMERALD
                                    )
                                }
                                Text(notif.title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                                Text(notif.message, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                                Text(
                                    "Recipient UID: ${notif.userId} • ${notif.location}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                                )
                                if (canWrite) {
                                    TextButton(onClick = { onDeleteNotification(notif) }) {
                                        Text("Delete", color = MilaTerracotta)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCompose && candidates.isNotEmpty()) {
        var selectedCand by remember { mutableStateOf(candidates.first()) }
        var title by remember { mutableStateOf("Executive Search Invitation") }
        var message by remember { mutableStateOf("MILA Talent has an active leadership mandate matching your profile.") }
        var location by remember { mutableStateOf(selectedCand.location) }
        AlertDialog(
            onDismissRequest = { showCompose = false },
            containerColor = MilaCharcoalSurface,
            title = { Text("Dispatch Candidate Notification", color = MilaIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RECIPIENT CANDIDATE", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        candidates.take(8).forEach { cand ->
                            FilterChip(
                                selected = selectedCand.userId == cand.userId,
                                onClick = { selectedCand = cand; location = cand.location },
                                label = { Text(cand.fullName) }
                            )
                        }
                    }
                    AdminInputField("Notification Title", title, { title = it })
                    AdminInputField("Message", message, { message = it }, minLines = 3)
                    AdminInputField("Location", location, { location = it })
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Send Notification",
                    onClick = {
                        onSendDirectNotification(
                            selectedCand.userId,
                            "direct_admin_alert",
                            title,
                            message,
                            selectedCand.resolvedDepartment.label,
                            location
                        )
                        showCompose = false
                    },
                    testTag = "send_notif_confirm"
                )
            },
            dismissButton = {
                TextButton(onClick = { showCompose = false }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationControlsCard(
    controls: NotificationControlSettings,
    canWrite: Boolean,
    onSaveControls: (NotificationControlSettings) -> Unit
) {
    var draft by remember(controls) { mutableStateOf(controls) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ADMINISTRATOR CHANNEL & MATCHING CONTROLS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.4.sp
                )
            )
            ControlSwitchRow("Auto-Dispatch on Vacancy Publish", draft.autoDispatchOnPublish, canWrite) {
                draft = draft.copy(autoDispatchOnPublish = it)
            }
            ControlSwitchRow("Android Push Notifications (FCM)", draft.enableAndroidPush, canWrite) {
                draft = draft.copy(enableAndroidPush = it)
            }
            ControlSwitchRow("iOS Push Notifications (FCM / APNs)", draft.enableIosPush, canWrite) {
                draft = draft.copy(enableIosPush = it)
            }
            ControlSwitchRow("In-App Notifications", draft.enableInApp, canWrite) {
                draft = draft.copy(enableInApp = it)
            }
            ControlSwitchRow("Optional Email Notifications", draft.enableEmailNotifications, canWrite) {
                draft = draft.copy(enableEmailNotifications = it)
            }
            ControlSwitchRow("Application Status Notifications", draft.enableApplicationStatusAlerts, canWrite) {
                draft = draft.copy(enableApplicationStatusAlerts = it)
            }
            HorizontalDivider(color = MilaGoldSubtle)
            ControlSwitchRow("Strict Department Matching", draft.requireDepartmentMatch, canWrite) {
                draft = draft.copy(requireDepartmentMatch = it)
            }
            ControlSwitchRow("Strict Preferred Location Matching", draft.requireLocationMatch, canWrite) {
                draft = draft.copy(requireLocationMatch = it)
            }
            Text(
                text = "Experience Tolerance (Years Below Minimum)",
                style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L, 1L, 2L).forEach { tol ->
                    FilterChip(
                        selected = draft.experienceToleranceYears == tol,
                        onClick = { if (canWrite) draft = draft.copy(experienceToleranceYears = tol) },
                        label = { Text(if (tol == 0L) "Exact (0 yrs)" else "±$tol yr") }
                    )
                }
            }
            if (canWrite) {
                GoldOutlineButton(
                    text = "Save Notification Controls",
                    onClick = { onSaveControls(draft) },
                    testTag = "save_notification_controls_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ControlSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MilaCharcoalDeep,
                checkedTrackColor = MilaChampagneGold,
                uncheckedThumbColor = MilaWarmStone,
                uncheckedTrackColor = MilaCharcoalElevated
            )
        )
    }
}
