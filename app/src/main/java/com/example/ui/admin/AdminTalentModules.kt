package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.data.CandidateAvailability
import com.example.data.CandidateProfile
import com.example.data.HospitalityDepartment
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
    var deleting by remember { mutableStateOf<JobVacancyRecord?>(null) }

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
                subtitle = "MILA Admin Exclusive — Post, edit, publish/unpublish, or delete open hospitality roles (${vacancies.size} total).",
                canAdd = canWrite,
                addLabel = "Post New Job",
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
                                GoldOutlineButton("Edit Job", { editing = job }, "edit_job_${job.id}")
                                GoldOutlineButton(
                                    if (job.published) "Unpublish" else "Publish",
                                    { onSaveVacancy(job.copy(published = !job.published)) },
                                    "toggle_job_${job.id}"
                                )
                                TextButton(onClick = { deleting = job }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete job",
                                        tint = MilaTerracotta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
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
                    text = if (job.id.isBlank()) "Post Job" else "Save Job",
                    enabled = title.trim().isNotEmpty(),
                    onClick = {
                        if (title.trim().isEmpty()) return@GoldFilledButton
                        onSaveVacancy(
                            job.copy(
                                title = title.trim(),
                                propertyTier = tier.trim(),
                                location = location.trim(),
                                cityRegion = region.trim(),
                                compensation = comp.trim(),
                                experienceRequired = expLabel.trim(),
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

    deleting?.let { job ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Delete Job Vacancy?", color = MilaIvory) },
            text = {
                Text(
                    "Permanently delete '${job.title}' (${job.location})? It will be removed from the MILA App Careers tab immediately.",
                    color = MilaIvoryMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVacancy(job)
                        deleting = null
                    }
                ) {
                    Text("Delete Permanently", color = MilaTerracotta)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCandidatesModule(
    actor: AdminUser,
    candidates: List<CandidateProfile>,
    onUpdateCandidate: (CandidateProfile) -> Unit,
    onDeleteCandidate: (CandidateProfile) -> Unit,
    onToggleAlerts: (CandidateProfile) -> Unit,
    onDownloadCv: (CandidateProfile) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.CANDIDATE_MANAGEMENT)
    var query by remember { mutableStateOf("") }
    var inspecting by remember { mutableStateOf<CandidateProfile?>(null) }
    var editing by remember { mutableStateOf<CandidateProfile?>(null) }
    var deleting by remember { mutableStateOf<CandidateProfile?>(null) }

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
                subtitle = "15-field MILA Talent candidate dossiers, edit/delete management, and instant CV downloads (${candidates.size}).",
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
                                label = if (cand.hasCvUploaded) "CV Ready" else "No CV",
                                tone = if (cand.hasCvUploaded) StatusTone.EMERALD else StatusTone.MUTED
                            )
                        }
                        Text(cand.fullName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("${cand.currentDesignation} • ${cand.currentEmployer} (${cand.totalExperienceYears} yrs)", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text("${cand.location} • Expected: ${cand.expectedSalary} • Notice: ${cand.noticePeriod}", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        if (cand.hasCvUploaded) {
                            Text("CV: ${cand.cvFileName}", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoldOutlineButton("Inspect Dossier", { inspecting = cand }, "inspect_cand_${cand.userId}")
                            if (cand.hasCvUploaded) {
                                GoldFilledButton(
                                    text = "Download CV",
                                    onClick = { onDownloadCv(cand) },
                                    testTag = "download_cand_cv_${cand.userId}"
                                )
                            }
                            if (canWrite) {
                                GoldOutlineButton("Edit", { editing = cand }, "edit_cand_${cand.userId}")
                                GoldOutlineButton(
                                    if (cand.jobAlertsEnabled) "Pause Alerts" else "Enable Alerts",
                                    { onToggleAlerts(cand) },
                                    "alerts_cand_${cand.userId}"
                                )
                                TextButton(onClick = { deleting = cand }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete candidate",
                                        tint = MilaTerracotta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", color = MilaTerracotta)
                                }
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
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item { Text("Email: ${cand.email}", color = MilaIvoryMuted) }
                    item { Text("Mobile: ${cand.mobileNumber}", color = MilaIvoryMuted) }
                    item { Text("Location: ${cand.location}", color = MilaIvoryMuted) }
                    item { Text("Designation: ${cand.currentDesignation} (${cand.resolvedDepartment.label})", color = MilaChampagneGold) }
                    item { Text("Experience: ${cand.totalExperienceYears} yrs", color = MilaIvory) }
                    item { Text("Current Employer: ${cand.currentEmployer}", color = MilaIvory) }
                    item { Text("Previous Employers: ${cand.previousEmployers}", color = MilaWarmStone) }
                    item { Text("Education: ${cand.education}", color = MilaWarmStone) }
                    item { Text("Skills: ${cand.skills}", color = MilaIvory) }
                    item { Text("Preferred Locations: ${cand.preferredJobLocations}", color = MilaIvoryMuted) }
                    item { Text("Salary & Availability: ${cand.expectedSalary} • ${cand.resolvedAvailability.label} • Notice ${cand.noticePeriod}", color = MilaChampagneGold) }
                    if (cand.hasCvUploaded) {
                        item {
                            Text(
                                "Uploaded CV: ${cand.cvFileName}",
                                style = MaterialTheme.typography.labelMedium.copy(color = MilaChampagneGold)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cand.hasCvUploaded) {
                        GoldFilledButton(
                            text = "Download CV",
                            onClick = { onDownloadCv(cand) },
                            testTag = "inspect_download_cv_${cand.userId}"
                        )
                    }
                    if (canWrite) {
                        GoldOutlineButton(
                            text = "Edit",
                            onClick = {
                                inspecting = null
                                editing = cand
                            },
                            testTag = "inspect_edit_cand_${cand.userId}"
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { inspecting = null }) { Text("Close", color = MilaWarmStone) }
            }
        )
    }

    editing?.let { cand ->
        var fullName by remember(cand) { mutableStateOf(cand.fullName) }
        var email by remember(cand) { mutableStateOf(cand.email) }
        var mobile by remember(cand) { mutableStateOf(cand.mobileNumber) }
        var location by remember(cand) { mutableStateOf(cand.location) }
        var designation by remember(cand) { mutableStateOf(cand.currentDesignation) }
        var department by remember(cand) { mutableStateOf(cand.resolvedDepartment) }
        var expYears by remember(cand) { mutableStateOf(cand.totalExperienceYears.toString()) }
        var currentEmployer by remember(cand) { mutableStateOf(cand.currentEmployer) }
        var previousEmployers by remember(cand) { mutableStateOf(cand.previousEmployers) }
        var education by remember(cand) { mutableStateOf(cand.education) }
        var skills by remember(cand) { mutableStateOf(cand.skills) }
        var preferredLocations by remember(cand) { mutableStateOf(cand.preferredJobLocations) }
        var expectedSalary by remember(cand) { mutableStateOf(cand.expectedSalary) }
        var availability by remember(cand) { mutableStateOf(cand.resolvedAvailability) }
        var noticePeriod by remember(cand) { mutableStateOf(cand.noticePeriod) }

        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = MilaChampagneGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Candidate Dossier", color = MilaIvory)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { AdminInputField("Full Name", fullName, { fullName = it }) }
                    item { AdminInputField("Email", email, { email = it }) }
                    item { AdminInputField("Mobile Number", mobile, { mobile = it }) }
                    item { AdminInputField("Current City / Location", location, { location = it }) }
                    item { AdminInputField("Current Designation", designation, { designation = it }) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("DEPARTMENT", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HospitalityDepartment.entries.forEach { dept ->
                                    FilterChip(
                                        selected = department == dept,
                                        onClick = { department = dept },
                                        label = { Text(dept.label) }
                                    )
                                }
                            }
                        }
                    }
                    item { AdminInputField("Total Experience (Years)", expYears, { expYears = it }) }
                    item { AdminInputField("Current Employer", currentEmployer, { currentEmployer = it }) }
                    item { AdminInputField("Previous Employers", previousEmployers, { previousEmployers = it }, minLines = 2) }
                    item { AdminInputField("Education", education, { education = it }) }
                    item { AdminInputField("Skills", skills, { skills = it }, minLines = 2) }
                    item { AdminInputField("Preferred Locations", preferredLocations, { preferredLocations = it }) }
                    item { AdminInputField("Expected Salary", expectedSalary, { expectedSalary = it }) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("AVAILABILITY", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CandidateAvailability.entries.forEach { avail ->
                                    FilterChip(
                                        selected = availability == avail,
                                        onClick = { availability = avail },
                                        label = { Text(avail.label) }
                                    )
                                }
                            }
                        }
                    }
                    item { AdminInputField("Notice Period", noticePeriod, { noticePeriod = it }) }
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save Candidate",
                    onClick = {
                        onUpdateCandidate(
                            cand.copy(
                                fullName = fullName.trim().ifBlank { cand.fullName },
                                email = email.trim().ifBlank { cand.email },
                                mobileNumber = mobile.trim().ifBlank { cand.mobileNumber },
                                location = location.trim(),
                                currentDesignation = designation.trim(),
                                department = department.name,
                                totalExperienceYears = (expYears.toLongOrNull() ?: cand.totalExperienceYears).coerceIn(0L, 60L),
                                currentEmployer = currentEmployer.trim(),
                                previousEmployers = previousEmployers.trim(),
                                education = education.trim(),
                                skills = skills.trim(),
                                preferredJobLocations = preferredLocations.trim(),
                                expectedSalary = expectedSalary.trim(),
                                availability = availability.name,
                                noticePeriod = noticePeriod.trim()
                            )
                        )
                        editing = null
                    },
                    testTag = "save_edit_candidate_dialog"
                )
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }

    deleting?.let { cand ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Delete Candidate Profile?", color = MilaIvory) },
            text = {
                Text(
                    "Permanently delete ${cand.fullName}'s candidate record (${cand.currentDesignation})? This action cannot be undone.",
                    color = MilaIvoryMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCandidate(cand)
                        deleting = null
                    }
                ) {
                    Text("Delete Permanently", color = MilaTerracotta)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCvDatabaseModule(
    actor: AdminUser,
    candidates: List<CandidateProfile>,
    onDownloadCv: (CandidateProfile) -> Unit,
    onPurgeCv: (CandidateProfile) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.CV_DATABASE)
    var query by remember { mutableStateOf("") }
    var purging by remember { mutableStateOf<CandidateProfile?>(null) }

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
                subtitle = "Private encrypted CV vault (${withCv.size} indexed). Authorized MILA admins can securely download any candidate CV directly to Downloads/MILA_CVs or remove records.",
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search candidate name, designation, or CV filename...", "admin_cv_search")
        }
        if (withCv.isEmpty()) {
            item { AdminEmptyState("No private candidate CVs stored yet.") }
        } else {
            items(withCv, key = { it.userId + "_" + it.id }) { cand ->
                val sizeKb = ((cand.cvFileSizeBytes ?: 0L) / 1024L).coerceAtLeast(1L)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(label = "PRIVATE VAULT CV", tone = StatusTone.GOLD)
                        Text(cand.cvFileName.orEmpty(), style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("Candidate: ${cand.fullName} • ${cand.currentDesignation}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text("MIME: ${cand.cvMimeType ?: "application/pdf"} • Size: $sizeKb KB", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        Text("Private Object Key: ${cand.cvStoragePath}", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoldFilledButton(
                                text = "Download CV",
                                onClick = { onDownloadCv(cand) },
                                testTag = "cv_db_download_${cand.userId}"
                            )
                            if (canWrite) {
                                TextButton(onClick = { purging = cand }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Remove CV record",
                                        tint = MilaTerracotta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove CV Record", color = MilaTerracotta)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    purging?.let { cand ->
        AlertDialog(
            onDismissRequest = { purging = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Remove CV Record?", color = MilaIvory) },
            text = {
                Text(
                    "Clear ${cand.cvFileName ?: "the uploaded CV"} from ${cand.fullName}'s profile?",
                    color = MilaIvoryMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPurgeCv(cand)
                        purging = null
                    }
                ) {
                    Text("Remove CV", color = MilaTerracotta)
                }
            },
            dismissButton = {
                TextButton(onClick = { purging = null }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminApplicationsAndStatusModule(
    actor: AdminUser,
    module: AdminModule,
    applications: List<JobApplication>,
    candidates: List<CandidateProfile>,
    onUpdateStatus: (JobApplication, ApplicationStatus, Boolean) -> Unit,
    onDownloadCv: (CandidateProfile) -> Unit,
    onDeleteApplication: (JobApplication) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(module)
    var query by remember { mutableStateOf("") }
    var stageFilter by remember { mutableStateOf("ALL") }
    var deletingApp by remember { mutableStateOf<JobApplication?>(null) }

    val candidateByUserId = remember(candidates) {
        candidates.associateBy { it.userId }
    }
    val candidateByEmail = remember(candidates) {
        candidates.associateBy { it.email.trim().lowercase() }
    }

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
                    "Advance candidates across Shortlisted, Interview Scheduled, Offer Extended, and Hired stages, download CVs, and manage applications."
                } else {
                    "Search, download candidate CVs, update statuses, or remove applications submitted via MILA Talent (${applications.size} total)."
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
                val matchedCandidate = candidateByUserId[app.userId]
                    ?: candidateByEmail[app.candidateEmail.trim().lowercase()]

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
                        if (matchedCandidate != null && matchedCandidate.hasCvUploaded) {
                            GoldFilledButton(
                                text = "Download Candidate CV",
                                onClick = { onDownloadCv(matchedCandidate) },
                                testTag = "app_download_cv_${app.id}"
                            )
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
                            TextButton(onClick = { deletingApp = app }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete application",
                                    tint = MilaTerracotta,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Delete Application", color = MilaTerracotta)
                            }
                        }
                    }
                }
            }
        }
    }

    deletingApp?.let { app ->
        AlertDialog(
            onDismissRequest = { deletingApp = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Delete Application?", color = MilaIvory) },
            text = {
                Text(
                    "Remove ${app.candidateName}'s application for ${app.roleTitle}? This action cannot be undone.",
                    color = MilaIvoryMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteApplication(app)
                        deletingApp = null
                    }
                ) {
                    Text("Delete Application", color = MilaTerracotta)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingApp = null }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
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
