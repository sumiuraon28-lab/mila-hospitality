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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminModule
import com.example.data.AdminUser
import com.example.data.ApplicationStatus
import com.example.data.CandidateProfile
import com.example.data.JobApplication
import com.example.data.JobNotification
import com.example.data.JobVacancyRecord
import com.example.data.RoleCategory
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminVacanciesModule(
    actor: AdminUser,
    vacancies: List<JobVacancyRecord>,
    onSaveVacancy: (JobVacancyRecord) -> Unit,
    onDeleteVacancy: (JobVacancyRecord) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.JOB_VACANCY_MANAGEMENT)
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<JobVacancyRecord?>(null) }

    val filtered = remember(vacancies, query) {
        val q = query.trim().lowercase()
        vacancies.filter { v ->
            q.isEmpty() || v.title.lowercase().contains(q) || v.location.lowercase().contains(q) || v.category.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.JOB_VACANCY_MANAGEMENT,
                subtitle = "Publish, edit, and prioritize open MILA Talent hospitality roles (${vacancies.size} total).",
                canAdd = canWrite,
                addLabel = "New Job Vacancy",
                onAddClick = { editing = JobVacancyRecord() },
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search vacancies by title, city, or category...", "admin_vacancy_search")
        }
        if (filtered.isEmpty()) {
            item { AdminEmptyState("No hospitality vacancies match this filter.") }
        } else {
            items(filtered, key = { it.id.ifBlank { it.title } }) { job ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(
                                label = if (job.published) "Published" else "Unpublished",
                                tone = if (job.published) StatusTone.EMERALD else StatusTone.MUTED
                            )
                            if (job.urgent) StatusPill(label = "Urgent Mandate", tone = StatusTone.GOLD)
                        }
                        Text(job.title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("${job.propertyTier} • ${job.location}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text("${job.compensation} • ${job.experienceRequired}", style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone))
                        if (canWrite) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoldOutlineButton("Edit", { editing = job }, "edit_job_${job.id}")
                                GoldOutlineButton(
                                    if (job.published) "Unpublish" else "Publish",
                                    { onSaveVacancy(job.copy(published = !job.published)) },
                                    "toggle_job_${job.id}"
                                )
                                TextButton(onClick = { onDeleteVacancy(job) }) {
                                    Text("Delete", color = MilaTerracotta)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { job ->
        var title by remember(job) { mutableStateOf(job.title) }
        var tier by remember(job) { mutableStateOf(job.propertyTier.ifBlank { "5-Star Urban Hotel" }) }
        var location by remember(job) { mutableStateOf(job.location.ifBlank { "Mumbai, Maharashtra" }) }
        var region by remember(job) { mutableStateOf(job.cityRegion.ifBlank { "Mumbai" }) }
        var comp by remember(job) { mutableStateOf(job.compensation.ifBlank { "Executive Package" }) }
        var expLabel by remember(job) { mutableStateOf(job.experienceRequired.ifBlank { "8+ Years" }) }
        var minYears by remember(job) { mutableStateOf(job.minExperienceYears.toString()) }
        var summary by remember(job) { mutableStateOf(job.summary) }
        var mandatesText by remember(job) { mutableStateOf(job.keyMandates.joinToString("\n")) }
        var skillsText by remember(job) { mutableStateOf(job.desiredSkills.joinToString(", ")) }
        var category by remember(job) { mutableStateOf(job.resolvedCategory) }
        var urgent by remember(job) { mutableStateOf(job.urgent) }

        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text(if (job.id.isBlank()) "Create Vacancy" else "Edit Vacancy", color = MilaIvory) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { AdminInputField("Role Title", title, { title = it }) }
                    item { AdminInputField("Property Tier", tier, { tier = it }) }
                    item { AdminInputField("Location", location, { location = it }) }
                    item { AdminInputField("City Region Filter", region, { region = it }) }
                    item { AdminInputField("Compensation", comp, { comp = it }) }
                    item { AdminInputField("Experience Label", expLabel, { expLabel = it }) }
                    item { AdminInputField("Min Years Experience", minYears, { minYears = it }) }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RoleCategory.entries.filter { it != RoleCategory.ALL }.forEach { cat ->
                                FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.displayLabel) })
                            }
                        }
                    }
                    item { AdminInputField("Summary", summary, { summary = it }, minLines = 2) }
                    item { AdminInputField("Key Mandates (1 per line)", mandatesText, { mandatesText = it }, minLines = 2) }
                    item { AdminInputField("Desired Skills (comma separated)", skillsText, { skillsText = it }) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = urgent,
                                onCheckedChange = { urgent = it },
                                colors = CheckboxDefaults.colors(checkedColor = MilaChampagneGold)
                            )
                            Text("Mark as Priority / Urgent Search", color = MilaIvory)
                        }
                    }
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save Vacancy",
                    onClick = {
                        onSaveVacancy(
                            job.copy(
                                title = title,
                                propertyTier = tier,
                                location = location,
                                cityRegion = region,
                                compensation = comp,
                                experienceRequired = expLabel,
                                minExperienceYears = (minYears.toLongOrNull() ?: 5L).coerceIn(0L, 60L),
                                category = category.name,
                                summary = summary.ifBlank { "Hospitality leadership mandate managed via MILA Talent." },
                                keyMandates = mandatesText.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Departmental P&L and service excellence") },
                                desiredSkills = skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Luxury Hospitality") },
                                urgent = urgent
                            )
                        )
                        editing = null
                    },
                    testTag = "save_vacancy_dialog"
                )
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCandidatesModule(
    actor: AdminUser,
    candidates: List<CandidateProfile>,
    onToggleAlerts: (CandidateProfile) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.CANDIDATE_MANAGEMENT)
    var query by remember { mutableStateOf("") }
    var inspecting by remember { mutableStateOf<CandidateProfile?>(null) }

    val filtered = remember(candidates, query) {
        val q = query.trim().lowercase()
        candidates.filter { c ->
            q.isEmpty() ||
                c.fullName.lowercase().contains(q) ||
                c.currentDesignation.lowercase().contains(q) ||
                c.location.lowercase().contains(q) ||
                c.skills.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.CANDIDATE_MANAGEMENT,
                subtitle = "15-field MILA Talent candidate dossiers, experience profiles, and job-alert statuses (${candidates.size}).",
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search candidate name, designation, city, or skills...", "admin_candidate_search")
        }
        if (filtered.isEmpty()) {
            item { AdminEmptyState("No registered candidates match this query.") }
        } else {
            items(filtered, key = { it.userId + "_" + it.id }) { cand ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(label = cand.resolvedDepartment.label, tone = StatusTone.GOLD)
                            StatusPill(
                                label = if (cand.hasCvUploaded) "CV On Vault" else "No CV",
                                tone = if (cand.hasCvUploaded) StatusTone.EMERALD else StatusTone.MUTED
                            )
                        }
                        Text(cand.fullName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("${cand.currentDesignation} • ${cand.currentEmployer} (${cand.totalExperienceYears} yrs)", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text("${cand.location} • Expected: ${cand.expectedSalary} • Notice: ${cand.noticePeriod}", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldOutlineButton("Inspect Full Dossier", { inspecting = cand }, "inspect_cand_${cand.userId}")
                            if (canWrite) {
                                GoldOutlineButton(
                                    if (cand.jobAlertsEnabled) "Pause Alerts" else "Enable Alerts",
                                    { onToggleAlerts(cand) },
                                    "alerts_cand_${cand.userId}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    inspecting?.let { cand ->
        AlertDialog(
            onDismissRequest = { inspecting = null },
            containerColor = MilaCharcoalSurface,
            title = { Text(cand.fullName, color = MilaIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Email: ${cand.email}", color = MilaIvoryMuted)
                    Text("Mobile: ${cand.mobileNumber}", color = MilaIvoryMuted)
                    Text("Location: ${cand.location}", color = MilaIvoryMuted)
                    Text("Designation: ${cand.currentDesignation} (${cand.resolvedDepartment.label})", color = MilaChampagneGold)
                    Text("Experience: ${cand.totalExperienceYears} yrs", color = MilaIvory)
                    Text("Current Employer: ${cand.currentEmployer}", color = MilaIvory)
                    Text("Previous Employers: ${cand.previousEmployers}", color = MilaWarmStone)
                    Text("Education: ${cand.education}", color = MilaWarmStone)
                    Text("Skills: ${cand.skills}", color = MilaIvory)
                    Text("Preferred Locations: ${cand.preferredJobLocations}", color = MilaIvoryMuted)
                    Text("Salary & Availability: ${cand.expectedSalary} • ${cand.resolvedAvailability.label} • Notice ${cand.noticePeriod}", color = MilaChampagneGold)
                }
            },
            confirmButton = {
                TextButton(onClick = { inspecting = null }) { Text("Close", color = MilaChampagneGold) }
            }
        )
    }
}

@Composable
fun AdminCvDatabaseModule(
    actor: AdminUser,
    candidates: List<CandidateProfile>,
    onPurgeCv: (CandidateProfile) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.CV_DATABASE)
    var query by remember { mutableStateOf("") }
    val withCv = remember(candidates, query) {
        val q = query.trim().lowercase()
        candidates.filter { c ->
            c.hasCvUploaded && (q.isEmpty() ||
                c.fullName.lowercase().contains(q) ||
                c.cvFileName.orEmpty().lowercase().contains(q) ||
                c.currentDesignation.lowercase().contains(q))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.CV_DATABASE,
                subtitle = "Private encrypted CV vault metadata (${withCv.size} indexed). Public download URLs are strictly disabled.",
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search candidate name, designation, or CV filename...", "admin_cv_search")
        }
        if (withCv.isEmpty()) {
            item { AdminEmptyState("No private candidate CVs stored yet.") }
        } else {
            items(withCv, key = { it.userId }) { cand ->
                val sizeKb = ((cand.cvFileSizeBytes ?: 0L) / 1024L).coerceAtLeast(1L)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(label = "PRIVATE VAULT KEY", tone = StatusTone.GOLD)
                        Text(cand.cvFileName.orEmpty(), style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("Candidate: ${cand.fullName} • ${cand.currentDesignation}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text("MIME: ${cand.cvMimeType ?: "application/pdf"} • Size: $sizeKb KB", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        Text("Private Object Key: ${cand.cvStoragePath}", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                        if (canWrite) {
                            TextButton(onClick = { onPurgeCv(cand) }) {
                                Text("Remove CV Record", color = MilaTerracotta)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminApplicationsAndStatusModule(
    actor: AdminUser,
    module: AdminModule,
    applications: List<JobApplication>,
    onUpdateStatus: (JobApplication, ApplicationStatus, Boolean) -> Unit,
    onDeleteApplication: (JobApplication) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(module)
    var query by remember { mutableStateOf("") }
    var stageFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(applications, query, stageFilter) {
        val q = query.trim().lowercase()
        applications.filter { app ->
            val matchStage = stageFilter == "ALL" || app.status == stageFilter
            val matchQ = q.isEmpty() ||
                app.candidateName.lowercase().contains(q) ||
                app.roleTitle.lowercase().contains(q) ||
                app.location.lowercase().contains(q)
            matchStage && matchQ
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = module,
                subtitle = if (module == AdminModule.RECRUITMENT_STATUS_MANAGEMENT) {
                    "Advance candidates across Shortlisted, Interview Scheduled, Offer Extended, and Hired stages with automatic notifications."
                } else {
                    "Search and review all candidate applications submitted via MILA Talent (${applications.size} total)."
                },
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search candidate, role title, or city...", "admin_app_search")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("ALL") + ApplicationStatus.entries.map { it.name }).forEach { st ->
                    val selected = stageFilter == st
                    FilterChip(
                        selected = selected,
                        onClick = { stageFilter = st },
                        label = { Text(st.replace('_', ' ')) },
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
                        )
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item { AdminEmptyState("No job applications match this filter.") }
        } else {
            items(filtered, key = { it.id.ifBlank { it.userId + it.roleId } }) { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(
                                label = app.resolvedStatus.label,
                                tone = when (app.resolvedStatus) {
                                    ApplicationStatus.HIRED, ApplicationStatus.SHORTLISTED, ApplicationStatus.OFFER_EXTENDED -> StatusTone.EMERALD
                                    ApplicationStatus.WITHDRAWN, ApplicationStatus.DECLINED -> StatusTone.ALERT
                                    else -> StatusTone.GOLD
                                }
                            )
                            Text("${app.yearsExperience} yrs", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                        }
                        Text("${app.candidateName} — ${app.roleTitle}", style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("${app.currentProperty} • ${app.location}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text("${app.candidateEmail} • ${app.candidatePhone}", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        if (!app.cvFileName.isNullOrBlank()) {
                            Text("Attached Private CV: ${app.cvFileName}", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                        }
                        if (canWrite) {
                            HorizontalDivider(color = MilaGoldSubtle)
                            Text("UPDATE STAGE & NOTIFY CANDIDATE", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.1.sp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    ApplicationStatus.UNDER_REVIEW,
                                    ApplicationStatus.SHORTLISTED,
                                    ApplicationStatus.INTERVIEW_SCHEDULED,
                                    ApplicationStatus.OFFER_EXTENDED,
                                    ApplicationStatus.HIRED,
                                    ApplicationStatus.DECLINED
                                ).forEach { nextStatus ->
                                    FilterChip(
                                        selected = app.resolvedStatus == nextStatus,
                                        onClick = { onUpdateStatus(app, nextStatus, true) },
                                        label = { Text(nextStatus.label) }
                                    )
                                }
                            }
                            if (module == AdminModule.JOB_APPLICATIONS) {
                                TextButton(onClick = { onDeleteApplication(app) }) {
                                    Text("Delete Application", color = MilaTerracotta)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminNotificationsModule(
    actor: AdminUser,
    notifications: List<JobNotification>,
    candidates: List<CandidateProfile>,
    onSendNotification: (String, String, String, String, String, String) -> Unit,
    onDeleteNotification: (JobNotification) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.NOTIFICATION_MANAGEMENT)
    var showCompose by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.NOTIFICATION_MANAGEMENT,
                subtitle = "Monitor candidate job alerts and dispatch direct interview or vacancy invitations (${notifications.size}).",
                canAdd = canWrite && candidates.isNotEmpty(),
                addLabel = "Send Candidate Notification",
                onAddClick = { showCompose = true },
                isReadOnly = !canWrite
            )
        }
        if (notifications.isEmpty()) {
            item { AdminEmptyState("No candidate notifications sent yet.") }
        } else {
            items(notifications, key = { it.id.ifBlank { it.userId + it.title } }) { notif ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(label = notif.department, tone = StatusTone.GOLD)
                            StatusPill(label = if (notif.isRead) "Read" else "Unread", tone = StatusTone.MUTED)
                        }
                        Text(notif.title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text(notif.message, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        Text("Recipient UID: ${notif.userId} • ${notif.location}", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
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
                        onSendNotification(
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
