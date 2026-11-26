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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminInvite
import com.example.data.AdminModule
import com.example.data.AdminRole
import com.example.data.AdminSettings
import com.example.data.AdminUser
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.InquiryStatus
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
fun AdminClientManagementModule(
    actor: AdminUser,
    clients: List<ClientProfile>,
    onUpdateClient: (ClientProfile) -> Unit,
    onDeleteClient: (ClientProfile) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.CLIENT_MANAGEMENT)
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ClientProfile?>(null) }

    val filtered = remember(clients, query) {
        val q = query.trim().lowercase()
        clients.filter { c ->
            q.isEmpty() ||
                c.fullName.lowercase().contains(q) ||
                c.companyName.lowercase().contains(q) ||
                c.city.lowercase().contains(q) ||
                c.email.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.CLIENT_MANAGEMENT,
                subtitle = "Manage registered hospitality clients, owners, and organization profiles (${clients.size} records).",
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search client name, company, city, or email...", "admin_client_search")
        }
        if (filtered.isEmpty()) {
            item { AdminEmptyState("No registered client profiles found.") }
        } else {
            items(filtered, key = { it.userId + "_" + it.id }) { client ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(label = client.resolvedBusinessCategory.label, tone = StatusTone.GOLD)
                            Text(client.city, style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                        }
                        Text(client.companyName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("${client.fullName} • ${client.designation}", style = MaterialTheme.typography.bodyMedium.copy(color = MilaChampagneGold))
                        Text("${client.email} • ${client.mobileNumber}", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        if (canWrite) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoldOutlineButton("Edit", { editing = client }, "edit_client_${client.userId}")
                                TextButton(onClick = { onDeleteClient(client) }) {
                                    Text("Delete", color = MilaTerracotta)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { client ->
        var fullName by remember(client) { mutableStateOf(client.fullName) }
        var companyName by remember(client) { mutableStateOf(client.companyName) }
        var designation by remember(client) { mutableStateOf(client.designation) }
        var city by remember(client) { mutableStateOf(client.city) }
        var mobile by remember(client) { mutableStateOf(client.mobileNumber) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Edit Client Dossier", color = MilaIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminInputField("Full Name", fullName, { fullName = it })
                    AdminInputField("Company / Property", companyName, { companyName = it })
                    AdminInputField("Designation", designation, { designation = it })
                    AdminInputField("City", city, { city = it })
                    AdminInputField("Mobile Number", mobile, { mobile = it })
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save",
                    onClick = {
                        onUpdateClient(
                            client.copy(
                                fullName = fullName,
                                companyName = companyName,
                                designation = designation,
                                city = city,
                                mobileNumber = mobile
                            )
                        )
                        editing = null
                    },
                    testTag = "save_client_dialog"
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
fun AdminClientEnquiriesModule(
    actor: AdminUser,
    inquiries: List<ConsultationInquiry>,
    consultants: List<AdminUser>,
    onUpdateInquiry: (ConsultationInquiry, InquiryStatus, String, AdminUser?) -> Unit,
    onDeleteInquiry: (ConsultationInquiry) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.CLIENT_ENQUIRIES)
    val isSuperAdmin = actor.resolvedRole == AdminRole.SUPER_ADMIN
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var editing by remember { mutableStateOf<ConsultationInquiry?>(null) }

    val filtered = remember(inquiries, query, statusFilter) {
        val q = query.trim().lowercase()
        inquiries.filter { inq ->
            val matchStatus = statusFilter == "ALL" || inq.status == statusFilter
            val matchQ = q.isEmpty() ||
                inq.propertyName.lowercase().contains(q) ||
                inq.city.lowercase().contains(q) ||
                inq.contactName.lowercase().contains(q) ||
                inq.resolvedServiceType.label.lowercase().contains(q)
            matchStatus && matchQ
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val scopeNote = if (actor.resolvedRole == AdminRole.CONSULTANT) {
                "Showing client enquiries assigned to ${actor.displayName} (${inquiries.size} assigned)."
            } else {
                "Review consultancy enquiries, assign MILA consultants, and progress mandate status (${inquiries.size} total)."
            }
            AdminModuleHeader(
                module = AdminModule.CLIENT_ENQUIRIES,
                subtitle = scopeNote,
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search property, city, service, or stakeholder...", "admin_enquiry_search")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("ALL") + InquiryStatus.entries.map { it.name }).forEach { st ->
                    val selected = statusFilter == st
                    FilterChip(
                        selected = selected,
                        onClick = { statusFilter = st },
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
            item { AdminEmptyState("No client enquiries match this filter or assignment scope.") }
        } else {
            items(filtered, key = { it.id.ifBlank { it.userId + it.propertyName } }) { inq ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(
                                label = inq.resolvedStatus.label,
                                tone = when (inq.resolvedStatus) {
                                    InquiryStatus.CONVERTED -> StatusTone.EMERALD
                                    InquiryStatus.WITHDRAWN -> StatusTone.ALERT
                                    else -> StatusTone.GOLD
                                }
                            )
                            Text(inq.city, style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                        }
                        Text(inq.propertyName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text("${inq.resolvedServiceType.label} • ${inq.scaleSummary}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text("Contact: ${inq.contactName} (${inq.contactEmail} • ${inq.contactPhone})", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        if (!inq.assignedConsultantEmail.isNullOrBlank()) {
                            Text("Assigned Consultant: ${inq.assignedConsultantEmail}", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                        }
                        if (!inq.briefNotes.isNullOrBlank()) {
                            Text("Brief: ${inq.briefNotes}", style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone))
                        }
                        if (!inq.consultantNotes.isNullOrBlank()) {
                            Text("Advisory Notes: ${inq.consultantNotes}", style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory))
                        }
                        if (canWrite) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoldOutlineButton("Manage & Update", { editing = inq }, "manage_inq_${inq.id}")
                                if (isSuperAdmin) {
                                    TextButton(onClick = { onDeleteInquiry(inq) }) {
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

    editing?.let { inq ->
        var selectedStatus by remember(inq) { mutableStateOf(inq.resolvedStatus) }
        var notes by remember(inq) { mutableStateOf(inq.consultantNotes.orEmpty()) }
        var selectedConsultant by remember(inq) {
            mutableStateOf(consultants.firstOrNull { it.uid == inq.assignedConsultantUid })
        }
        val assignableStaff = remember(consultants) {
            consultants.filter { it.active && (it.resolvedRole == AdminRole.CONSULTANT || it.resolvedRole == AdminRole.SUPER_ADMIN) }
        }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Manage Enquiry: ${inq.propertyName}", color = MilaIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PROGRESSION STAGE", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InquiryStatus.entries.forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    if (isSuperAdmin && assignableStaff.isNotEmpty()) {
                        Text("ASSIGN CONSULTANT", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            assignableStaff.forEach { staff ->
                                FilterChip(
                                    selected = selectedConsultant?.uid == staff.uid,
                                    onClick = { selectedConsultant = staff },
                                    label = { Text(staff.displayName) }
                                )
                            }
                        }
                    }
                    AdminInputField("Consultant Notes", notes, { notes = it }, minLines = 3)
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save Update",
                    onClick = {
                        onUpdateInquiry(inq, selectedStatus, notes, selectedConsultant)
                        editing = null
                    },
                    testTag = "save_inquiry_admin"
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
fun AdminUsersModule(
    actor: AdminUser,
    adminUsers: List<AdminUser>,
    adminInvites: List<AdminInvite>,
    onInviteAdmin: (String, String, AdminRole) -> Unit,
    onRevokeInvite: (AdminInvite) -> Unit,
    onUpdateAdmin: (AdminUser, AdminRole, Boolean) -> Unit
) {
    val canWrite = actor.resolvedRole == AdminRole.SUPER_ADMIN
    var showInviteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.ADMIN_USER_MANAGEMENT,
                subtitle = "Configure administrator roles (Super, Recruitment, Content, Consultant, Reporting).",
                canAdd = canWrite,
                addLabel = "Invite Staff Admin",
                onAddClick = { showInviteDialog = true },
                isReadOnly = !canWrite
            )
        }
        item {
            Text(
                "ACTIVE ADMINISTRATORS (${adminUsers.size})",
                style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.4.sp)
            )
        }
        items(adminUsers, key = { it.uid.ifBlank { it.email } }) { adm ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusPill(label = adm.resolvedRole.displayLabel, tone = StatusTone.GOLD)
                        StatusPill(
                            label = if (adm.active) "Active" else "Suspended",
                            tone = if (adm.active) StatusTone.EMERALD else StatusTone.ALERT
                        )
                    }
                    Text(adm.displayName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                    Text(adm.email, style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                    if (canWrite && adm.uid != actor.uid) {
                        HorizontalDivider(color = MilaGoldSubtle)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AdminRole.entries.forEach { r ->
                                FilterChip(
                                    selected = adm.resolvedRole == r,
                                    onClick = { onUpdateAdmin(adm, r, adm.active) },
                                    label = { Text(r.displayLabel) }
                                )
                            }
                        }
                        GoldOutlineButton(
                            text = if (adm.active) "Suspend Admin" else "Reactivate Admin",
                            onClick = { onUpdateAdmin(adm, adm.resolvedRole, !adm.active) },
                            testTag = "toggle_admin_${adm.uid}"
                        )
                    }
                }
            }
        }
        if (adminInvites.isNotEmpty()) {
            item {
                Text(
                    "STAFF ALLOWLIST INVITES (${adminInvites.size})",
                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.4.sp)
                )
            }
            items(adminInvites, key = { it.email }) { inv ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(inv.displayName, style = MaterialTheme.typography.titleSmall.copy(color = MilaIvory))
                            Text("${inv.email} • ${inv.resolvedRole.displayLabel}", style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone))
                        }
                        if (canWrite) {
                            TextButton(onClick = { onRevokeInvite(inv) }) {
                                Text("Revoke", color = MilaTerracotta)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        var email by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(AdminRole.CONSULTANT) }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = MilaCharcoalSurface,
            title = { Text("Authorize Staff Email", color = MilaIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminInputField("Staff Google Email", email, { email = it })
                    AdminInputField("Staff Name", name, { name = it })
                    Text("ASSIGN ROLE", style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AdminRole.entries.forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.displayLabel) })
                        }
                    }
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save Invite",
                    onClick = {
                        if (email.contains("@") && name.isNotBlank()) {
                            onInviteAdmin(email, name, role)
                            showInviteDialog = false
                        }
                    },
                    testTag = "confirm_invite_admin"
                )
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }
}

@Composable
fun AdminSettingsModule(
    actor: AdminUser,
    settings: AdminSettings,
    onSaveSettings: (AdminSettings) -> Unit
) {
    val canWrite = actor.resolvedRole == AdminRole.SUPER_ADMIN
    var maintenance by remember(settings) { mutableStateOf(settings.maintenanceMode) }
    var allowClients by remember(settings) { mutableStateOf(settings.allowClientRegistrations) }
    var allowCandidates by remember(settings) { mutableStateOf(settings.allowCandidateRegistrations) }
    var requireCv by remember(settings) { mutableStateOf(settings.requireCvForApplications) }
    var slaHours by remember(settings) { mutableStateOf(settings.defaultEnquirySlaHours.toString()) }
    var supportEmail by remember(settings) { mutableStateOf(settings.supportEmail) }
    var regions by remember(settings) { mutableStateOf(settings.officeRegions) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.SETTINGS,
                subtitle = "Platform governance toggles, enquiry response SLA, and regional advisory offices.",
                isReadOnly = !canWrite
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingSwitchRow("Maintenance Mode", maintenance, canWrite) { maintenance = it }
                    SettingSwitchRow("Allow Client Registrations", allowClients, canWrite) { allowClients = it }
                    SettingSwitchRow("Allow Candidate Registrations", allowCandidates, canWrite) { allowCandidates = it }
                    SettingSwitchRow("Require CV Before Applying", requireCv, canWrite) { requireCv = it }
                    HorizontalDivider(color = MilaGoldSubtle)
                    AdminInputField("Enquiry Response SLA (Hours)", slaHours, { slaHours = it }, enabled = canWrite)
                    AdminInputField("Advisory Support Email", supportEmail, { supportEmail = it }, enabled = canWrite)
                    AdminInputField("Regional Coverage", regions, { regions = it }, minLines = 2, enabled = canWrite)
                    if (canWrite) {
                        GoldFilledButton(
                            text = "Save Platform Settings",
                            onClick = {
                                onSaveSettings(
                                    AdminSettings(
                                        maintenanceMode = maintenance,
                                        allowClientRegistrations = allowClients,
                                        allowCandidateRegistrations = allowCandidates,
                                        requireCvForApplications = requireCv,
                                        defaultEnquirySlaHours = (slaHours.toLongOrNull() ?: 24L).coerceIn(1L, 168L),
                                        supportEmail = supportEmail,
                                        officeRegions = regions
                                    )
                                )
                            },
                            testTag = "save_platform_settings",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
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
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MilaCharcoalDeep,
                checkedTrackColor = MilaChampagneGold
            )
        )
    }
}
