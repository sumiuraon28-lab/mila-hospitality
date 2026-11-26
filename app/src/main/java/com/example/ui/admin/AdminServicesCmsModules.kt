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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.AdminModule
import com.example.data.AdminUser
import com.example.data.ConsultationServiceType
import com.example.data.ServiceCategoryRecord
import com.example.data.ServiceOffering
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaTerracotta
import com.example.ui.theme.MilaWarmStone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminServicesModule(
    actor: AdminUser,
    services: List<ServiceOffering>,
    onSaveService: (ServiceOffering) -> Unit,
    onDeleteService: (ServiceOffering) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.SERVICE_MANAGEMENT)
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ServiceOffering?>(null) }

    val filtered = remember(services, query) {
        val q = query.trim().lowercase()
        services.filter { s ->
            q.isEmpty() || s.title.lowercase().contains(q) || s.divisionName.lowercase().contains(q) || s.summary.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.SERVICE_MANAGEMENT,
                subtitle = "Publish, unpublish, edit, or add MILA Consultancy service detail pages (${services.size}).",
                canAdd = canWrite,
                addLabel = "Add Service",
                onAddClick = { editing = ServiceOffering(sortOrder = (services.size + 1).toLong()) },
                isReadOnly = !canWrite
            )
        }
        item {
            AdminSearchField(query, { query = it }, "Search consultancy services...", "admin_service_search")
        }
        if (filtered.isEmpty()) {
            item { AdminEmptyState("No consultancy service documents found.") }
        } else {
            items(filtered, key = { it.id.ifBlank { it.slug } }) { svc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(
                                label = if (svc.published) "Published" else "Draft",
                                tone = if (svc.published) StatusTone.EMERALD else StatusTone.MUTED
                            )
                            Text("#${svc.sortOrder} • ${svc.divisionName}", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                        }
                        Text(svc.title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text(svc.subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text(svc.summary, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted), maxLines = 2)
                        if (canWrite) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoldOutlineButton("Edit", { editing = svc }, "edit_svc_${svc.id}")
                                GoldOutlineButton(
                                    if (svc.published) "Unpublish" else "Publish",
                                    { onSaveService(svc.copy(published = !svc.published)) },
                                    "toggle_svc_${svc.id}"
                                )
                                TextButton(onClick = { onDeleteService(svc) }) {
                                    Text("Delete", color = MilaTerracotta)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { svc ->
        var selectedType by remember(svc) { mutableStateOf(svc.resolvedServiceType) }
        var title by remember(svc) { mutableStateOf(svc.title) }
        var subtitle by remember(svc) { mutableStateOf(svc.subtitle) }
        var division by remember(svc) { mutableStateOf(svc.divisionName) }
        var summary by remember(svc) { mutableStateOf(svc.summary) }
        var details by remember(svc) { mutableStateOf(svc.detailedDescription) }
        var timeline by remember(svc) { mutableStateOf(svc.typicalTimeline.ifBlank { "3 – 6 Months" }) }
        var imageKey by remember(svc) { mutableStateOf(svc.imageKey.ifBlank { "palace_resort" }) }
        var included by remember(svc) { mutableStateOf(svc.servicesIncluded.joinToString("\n")) }
        var benefits by remember(svc) { mutableStateOf(svc.ownerBenefits.joinToString("\n")) }
        var deliverables by remember(svc) { mutableStateOf(svc.deliverables.joinToString("\n")) }

        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Manage Consultancy Service", color = MilaIvory) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ConsultationServiceType.entries.forEach { t ->
                                FilterChip(
                                    selected = selectedType == t,
                                    onClick = { selectedType = t },
                                    label = { Text(t.label) }
                                )
                            }
                        }
                    }
                    item { AdminInputField("Title", title, { title = it }) }
                    item { AdminInputField("Subtitle", subtitle, { subtitle = it }) }
                    item { AdminInputField("Division", division, { division = it }) }
                    item { AdminInputField("Typical Timeline", timeline, { timeline = it }) }
                    item { AdminInputField("Image Asset Key", imageKey, { imageKey = it }) }
                    item { AdminInputField("Catalogue Summary", summary, { summary = it }, minLines = 2) }
                    item { AdminInputField("Detailed Description", details, { details = it }, minLines = 3) }
                    item { AdminInputField("Services Included (1/line)", included, { included = it }, minLines = 2) }
                    item { AdminInputField("Owner Benefits (1/line)", benefits, { benefits = it }, minLines = 2) }
                    item { AdminInputField("Deliverables (1/line)", deliverables, { deliverables = it }, minLines = 2) }
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save Service",
                    onClick = {
                        onSaveService(
                            svc.copy(
                                serviceType = selectedType.name,
                                slug = svc.slug.ifBlank { selectedType.slug },
                                title = title.ifBlank { selectedType.label },
                                subtitle = subtitle.ifBlank { "Advisory Engagement" },
                                divisionName = division.ifBlank { "MILA Consultancy" },
                                summary = summary.ifBlank { "Tailored hospitality advisory engagement across India." },
                                detailedDescription = details.ifBlank { "Comprehensive MILA Consultancy engagement covering operational design, SOPs, and profitability." },
                                servicesIncluded = included.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Operational Advisory") },
                                ownerBenefits = benefits.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Improved Commercial Yield") },
                                deliverables = deliverables.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Master Advisory Dossier") },
                                typicalTimeline = timeline,
                                imageKey = imageKey
                            )
                        )
                        editing = null
                    },
                    testTag = "save_service_dialog"
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
fun AdminServiceCategoriesModule(
    actor: AdminUser,
    categories: List<ServiceCategoryRecord>,
    onSaveCategory: (ServiceCategoryRecord) -> Unit,
    onDeleteCategory: (ServiceCategoryRecord) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.SERVICE_CATEGORIES)
    var editing by remember { mutableStateOf<ServiceCategoryRecord?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.SERVICE_CATEGORIES,
                subtitle = "Manage high-level MILA service taxonomy & parent divisions (${categories.size}).",
                canAdd = canWrite,
                addLabel = "Add Category",
                onAddClick = { editing = ServiceCategoryRecord(sortOrder = (categories.size + 1).toLong()) },
                isReadOnly = !canWrite
            )
        }
        if (categories.isEmpty()) {
            item { AdminEmptyState("No service categories defined yet.") }
        } else {
            items(categories, key = { it.id.ifBlank { it.code } }) { cat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusPill(
                                label = if (cat.published) "Published" else "Hidden",
                                tone = if (cat.published) StatusTone.EMERALD else StatusTone.MUTED
                            )
                            Text("#${cat.sortOrder} • ${cat.divisionName}", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                        }
                        Text(cat.title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text(cat.summary, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        if (canWrite) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoldOutlineButton("Edit", { editing = cat }, "edit_cat_${cat.id}")
                                GoldOutlineButton(
                                    if (cat.published) "Unpublish" else "Publish",
                                    { onSaveCategory(cat.copy(published = !cat.published)) },
                                    "toggle_cat_${cat.id}"
                                )
                                TextButton(onClick = { onDeleteCategory(cat) }) {
                                    Text("Delete", color = MilaTerracotta)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { cat ->
        var code by remember(cat) { mutableStateOf(cat.code.ifBlank { "HOSPITALITY_ADVISORY" }) }
        var title by remember(cat) { mutableStateOf(cat.title) }
        var division by remember(cat) { mutableStateOf(cat.divisionName) }
        var summary by remember(cat) { mutableStateOf(cat.summary) }
        var sortOrder by remember(cat) { mutableStateOf(cat.sortOrder.toString()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Service Category", color = MilaIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminInputField("Code", code, { code = it })
                    AdminInputField("Title", title, { title = it })
                    AdminInputField("Division", division, { division = it })
                    AdminInputField("Sort Order", sortOrder, { sortOrder = it })
                    AdminInputField("Summary", summary, { summary = it }, minLines = 3)
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save",
                    onClick = {
                        onSaveCategory(
                            cat.copy(
                                code = code,
                                title = title,
                                divisionName = division,
                                sortOrder = (sortOrder.toLongOrNull() ?: 1L).coerceIn(1L, 100L),
                                summary = summary
                            )
                        )
                        editing = null
                    },
                    testTag = "save_category_dialog"
                )
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Cancel", color = MilaWarmStone) }
            }
        )
    }
}
