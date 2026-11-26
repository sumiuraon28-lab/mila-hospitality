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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityLog
import com.example.data.AdminModule
import com.example.data.AdminRole
import com.example.data.AdminUser
import com.example.data.RealAnalyticsSnapshot
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
import com.example.ui.theme.MilaWarmStone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardOverviewModule(
    actor: AdminUser,
    analytics: RealAnalyticsSnapshot,
    recentLogs: List<ActivityLog>,
    onSelectModule: (AdminModule) -> Unit
) {
    val role = actor.resolvedRole
    val allowedModules = remember(role) {
        role.allowedModules().filter { it != AdminModule.DASHBOARD_OVERVIEW }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.DASHBOARD_OVERVIEW,
                subtitle = "Live database telemetry scoped to ${role.displayLabel}. Every metric reflects real Firestore records.",
                isReadOnly = role.isReadOnly
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
                border = BorderStroke(1.dp, MilaGoldMuted),
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
                        StatusPill(label = role.displayLabel, tone = StatusTone.GOLD)
                        StatusPill(
                            label = if (role.isReadOnly) "Read-Only Audit" else "Active Write Scope",
                            tone = if (role.isReadOnly) StatusTone.MUTED else StatusTone.EMERALD
                        )
                    }
                    Text(actor.displayName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                    Text(actor.email, style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                    Text(role.scopeDescription, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LIVE DATABASE METRICS",
                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.4.sp)
                )
                if (role == AdminRole.SUPER_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminKpiTile("${analytics.totalClients}", "Clients", "Registered owners", Modifier.weight(1f))
                        AdminKpiTile("${analytics.totalEnquiries}", "Enquiries", "${analytics.activeEnquiries} active", Modifier.weight(1f))
                    }
                }
                if (role == AdminRole.CONSULTANT) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminKpiTile("${analytics.totalEnquiries}", "Assigned Enquiries", "${analytics.activeEnquiries} active", Modifier.weight(1f))
                        AdminKpiTile("${analytics.activityLogsCount}", "Audit Events", "Recorded actions", Modifier.weight(1f))
                    }
                }
                if (role == AdminRole.SUPER_ADMIN || role == AdminRole.RECRUITMENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminKpiTile("${analytics.totalCandidates}", "Candidates", "${analytics.candidatesWithCv} private CVs", Modifier.weight(1f))
                        AdminKpiTile("${analytics.totalApplications}", "Applications", "${analytics.publishedVacanciesCount}/${analytics.totalVacanciesCount} live jobs", Modifier.weight(1f))
                    }
                }
                if (role == AdminRole.SUPER_ADMIN || role == AdminRole.CONTENT_ADMIN || role == AdminRole.REPORTING_ADMIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminKpiTile("${analytics.publishedServicesCount}/${analytics.totalServicesCount}", "Services Live", "Consultancy pages", Modifier.weight(1f))
                        AdminKpiTile("${analytics.publishedPortfolioCount}", "Portfolio", "${analytics.publishedTestimonialsCount} testimonials", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AUTHORISED MODULES (${allowedModules.size})",
                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.4.sp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allowedModules.forEach { mod ->
                        GoldOutlineButton(
                            text = "${mod.number.toString().padStart(2, '0')}. ${mod.title}",
                            onClick = { onSelectModule(mod) },
                            testTag = "dash_nav_${mod.name}"
                        )
                    }
                }
            }
        }

        if (recentLogs.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT AUDIT TRAIL",
                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.4.sp)
                )
            }
            items(recentLogs.take(5), key = { it.id.ifBlank { it.summary + it.createdAt?.seconds } }) { log ->
                ActivityLogRow(log)
            }
        }
    }
}

@Composable
fun AdminReportsAnalyticsModule(
    actor: AdminUser,
    analytics: RealAnalyticsSnapshot
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.REPORTS_AND_ANALYTICS,
                subtitle = "Zero fabricated figures — every breakdown below aggregates live Firestore documents.",
                isReadOnly = actor.resolvedRole.isReadOnly
            )
        }

        if (analytics.enquiriesByStatus.isNotEmpty() || analytics.enquiriesByService.isNotEmpty()) {
            item {
                AnalyticsBreakdownCard(
                    title = "CLIENT ENQUIRIES BY STAGE (${analytics.totalEnquiries} TOTAL)",
                    counts = analytics.enquiriesByStatus,
                    total = analytics.totalEnquiries
                )
            }
            item {
                AnalyticsBreakdownCard(
                    title = "ENQUIRIES BY CONSULTANCY PRACTICE",
                    counts = analytics.enquiriesByService,
                    total = analytics.totalEnquiries
                )
            }
        }

        if (analytics.totalCandidates > 0 || analytics.totalApplications > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "CANDIDATE & CV VAULT COVERAGE",
                            style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.3.sp)
                        )
                        val cvRate = if (analytics.totalCandidates == 0) 0f else analytics.candidatesWithCv.toFloat() / analytics.totalCandidates
                        Text(
                            "${analytics.candidatesWithCv} of ${analytics.totalCandidates} registered candidates have uploaded a private CV (${(cvRate * 100).toInt()}%).",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory)
                        )
                        LinearProgressIndicator(
                            progress = { cvRate },
                            color = MilaChampagneGold,
                            trackColor = MilaCharcoalDeep,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Job Alerts Enabled: ${analytics.jobAlertsEnabledCount} candidates",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }
                }
            }
            item {
                AnalyticsBreakdownCard(
                    title = "RECRUITMENT APPLICATION FUNNEL (${analytics.totalApplications} TOTAL)",
                    counts = analytics.applicationsByStatus,
                    total = analytics.totalApplications
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "CMS & GOVERNANCE INVENTORY",
                        style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.3.sp)
                    )
                    Text("Published Services: ${analytics.publishedServicesCount} / ${analytics.totalServicesCount}", color = MilaIvory)
                    Text("Published Vacancies: ${analytics.publishedVacanciesCount} / ${analytics.totalVacanciesCount}", color = MilaIvory)
                    Text("Published Portfolio Case Studies: ${analytics.publishedPortfolioCount}", color = MilaIvory)
                    Text("Published Testimonials: ${analytics.publishedTestimonialsCount} • Team Profiles: ${analytics.publishedTeamCount}", color = MilaIvory)
                    Text("Active Admins: ${analytics.activeAdminsCount} • Audit Events: ${analytics.activityLogsCount}", color = MilaWarmStone)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsBreakdownCard(
    title: String,
    counts: Map<String, Int>,
    total: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.3.sp))
            if (counts.isEmpty()) {
                Text("No records recorded yet.", style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone))
            } else {
                counts.entries.sortedByDescending { it.value }.forEach { (label, count) ->
                    val ratio = if (total <= 0) 0f else count.toFloat() / total
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory))
                            Text("$count", style = MaterialTheme.typography.labelMedium.copy(color = MilaChampagneGold))
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = MilaChampagneGold,
                            trackColor = MilaCharcoalDeep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminActivityLogsModule(
    logs: List<ActivityLog>
) {
    var query by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(logs, query, roleFilter) {
        val q = query.trim().lowercase()
        logs.filter { log ->
            val matchesRole = roleFilter == "ALL" || log.actorRole == roleFilter
            val matchesQ = q.isEmpty() ||
                log.summary.lowercase().contains(q) ||
                log.actorEmail.lowercase().contains(q) ||
                log.module.lowercase().contains(q) ||
                log.action.lowercase().contains(q)
            matchesRole && matchesQ
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.ACTIVITY_LOGS,
                subtitle = "Immutable administrative audit log across all MILA portals.",
                isReadOnly = true
            )
        }
        item {
            AdminSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search actor, module, or summary...",
                testTag = "admin_logs_search"
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("ALL") + AdminRole.entries.map { it.name }).forEach { r ->
                    val selected = roleFilter == r
                    FilterChip(
                        selected = selected,
                        onClick = { roleFilter = r },
                        label = { Text(r.replace('_', ' ')) },
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
            item { AdminEmptyState("No audit events recorded matching this filter.") }
        } else {
            items(filtered, key = { it.id.ifBlank { it.summary + it.createdAt?.nanoseconds } }) { log ->
                ActivityLogRow(log)
            }
        }
    }
}

@Composable
private fun ActivityLogRow(log: ActivityLog) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusPill(label = "${log.module} • ${log.action}", tone = StatusTone.GOLD)
                StatusPill(label = log.actorRole, tone = StatusTone.MUTED)
            }
            Text(log.summary, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory))
            HorizontalDivider(color = MilaGoldSubtle)
            Text(
                text = "Actor: ${log.actorEmail} • Target: ${log.targetId}",
                style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
            )
        }
    }
}
