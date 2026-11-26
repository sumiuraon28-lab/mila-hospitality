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
import com.example.data.PortfolioItemRecord
import com.example.data.SitePageContent
import com.example.data.TeamMemberRecord
import com.example.data.TestimonialRecord
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
fun AdminWebsiteContentModule(
    actor: AdminUser,
    pages: List<SitePageContent>,
    onSavePage: (SitePageContent) -> Unit,
    onDeletePage: (SitePageContent) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.WEBSITE_CONTENT_MANAGEMENT)
    var editing by remember { mutableStateOf<SitePageContent?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.WEBSITE_CONTENT_MANAGEMENT,
                subtitle = "Edit luxury headlines, editorial copy, hero imagery keys, and CTAs for website pages (${pages.size}).",
                canAdd = canWrite,
                addLabel = "Add Page Section",
                onAddClick = { editing = SitePageContent() },
                isReadOnly = !canWrite
            )
        }
        items(pages, key = { it.id.ifBlank { it.pageKey } }) { page ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusPill(
                            label = if (page.published) "Published" else "Draft",
                            tone = if (page.published) StatusTone.EMERALD else StatusTone.MUTED
                        )
                        Text("Key: ${page.pageKey}", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                    }
                    Text(page.pageTitle, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                    Text(page.headline, style = MaterialTheme.typography.bodyMedium.copy(color = MilaChampagneGold))
                    Text(page.subheadline, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                    if (canWrite) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldOutlineButton("Edit", { editing = page }, "edit_page_${page.id}")
                            GoldOutlineButton(
                                if (page.published) "Unpublish" else "Publish",
                                { onSavePage(page.copy(published = !page.published)) },
                                "toggle_page_${page.id}"
                            )
                            TextButton(onClick = { onDeletePage(page) }) {
                                Text("Delete", color = MilaTerracotta)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { page ->
        var key by remember(page) { mutableStateOf(page.pageKey.ifBlank { "custom_section" }) }
        var title by remember(page) { mutableStateOf(page.pageTitle) }
        var eyebrow by remember(page) { mutableStateOf(page.eyebrow) }
        var headline by remember(page) { mutableStateOf(page.headline) }
        var subheadline by remember(page) { mutableStateOf(page.subheadline) }
        var bodyCopy by remember(page) { mutableStateOf(page.bodyCopy) }
        var imgKey by remember(page) { mutableStateOf(page.heroImageKey) }
        var ctaLabel by remember(page) { mutableStateOf(page.ctaLabel) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Edit Website Content", color = MilaIvory) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { AdminInputField("Page Key", key, { key = it }) }
                    item { AdminInputField("Page Title", title, { title = it }) }
                    item { AdminInputField("Eyebrow", eyebrow, { eyebrow = it }) }
                    item { AdminInputField("Headline", headline, { headline = it }) }
                    item { AdminInputField("Subheadline", subheadline, { subheadline = it }, minLines = 2) }
                    item { AdminInputField("Body Copy", bodyCopy, { bodyCopy = it }, minLines = 3) }
                    item { AdminInputField("Hero Image Key", imgKey, { imgKey = it }) }
                    item { AdminInputField("CTA Button Label", ctaLabel, { ctaLabel = it }) }
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save Page",
                    onClick = {
                        onSavePage(
                            page.copy(
                                pageKey = key,
                                pageTitle = title,
                                eyebrow = eyebrow,
                                headline = headline,
                                subheadline = subheadline,
                                bodyCopy = bodyCopy,
                                heroImageKey = imgKey,
                                ctaLabel = ctaLabel
                            )
                        )
                        editing = null
                    },
                    testTag = "save_site_page"
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
fun AdminPortfolioModule(
    actor: AdminUser,
    itemsList: List<PortfolioItemRecord>,
    onSaveItem: (PortfolioItemRecord) -> Unit,
    onDeleteItem: (PortfolioItemRecord) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.PORTFOLIO_MANAGEMENT)
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<PortfolioItemRecord?>(null) }

    val filtered = remember(itemsList, query) {
        val q = query.trim().lowercase()
        itemsList.filter { p -> q.isEmpty() || p.propertyName.lowercase().contains(q) || p.location.lowercase().contains(q) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.PORTFOLIO_MANAGEMENT,
                subtitle = "Curate hospitality pre-opening and mixology case studies synced to Our Portfolio (${itemsList.size}).",
                canAdd = canWrite,
                addLabel = "Add Case Study",
                onAddClick = { editing = PortfolioItemRecord(sortOrder = (itemsList.size + 1).toLong()) },
                isReadOnly = !canWrite
            )
        }
        item { AdminSearchField(query, { query = it }, "Search case studies...", "admin_portfolio_search") }
        items(filtered, key = { it.id.ifBlank { it.propertyName } }) { cs ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusPill(label = if (cs.published) "Published" else "Draft", tone = if (cs.published) StatusTone.EMERALD else StatusTone.MUTED)
                        Text(cs.filterGroup, style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                    }
                    Text(cs.propertyName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                    Text("${cs.location} • ${cs.scale}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                    Text(cs.summary, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted), maxLines = 2)
                    if (canWrite) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldOutlineButton("Edit", { editing = cs }, "edit_port_${cs.id}")
                            GoldOutlineButton(
                                if (cs.published) "Unpublish" else "Publish",
                                { onSaveItem(cs.copy(published = !cs.published)) },
                                "toggle_port_${cs.id}"
                            )
                            TextButton(onClick = { onDeleteItem(cs) }) { Text("Delete", color = MilaTerracotta) }
                        }
                    }
                }
            }
        }
    }

    editing?.let { cs ->
        var name by remember(cs) { mutableStateOf(cs.propertyName) }
        var loc by remember(cs) { mutableStateOf(cs.location) }
        var cat by remember(cs) { mutableStateOf(cs.category.ifBlank { "Pre-Opening Advisory" }) }
        var grp by remember(cs) { mutableStateOf(cs.filterGroup) }
        var scale by remember(cs) { mutableStateOf(cs.scale.ifBlank { "Luxury Resort" }) }
        var summary by remember(cs) { mutableStateOf(cs.summary) }
        var outcomesText by remember(cs) { mutableStateOf(cs.outcomes.joinToString("\n")) }
        var imgKey by remember(cs) { mutableStateOf(cs.imageKey) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Portfolio Case Study", color = MilaIvory) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { AdminInputField("Property / Project Name", name, { name = it }) }
                    item { AdminInputField("Location", loc, { loc = it }) }
                    item { AdminInputField("Category Label", cat, { cat = it }) }
                    item { AdminInputField("Filter Group", grp, { grp = it }) }
                    item { AdminInputField("Scale", scale, { scale = it }) }
                    item { AdminInputField("Image Key", imgKey, { imgKey = it }) }
                    item { AdminInputField("Summary", summary, { summary = it }, minLines = 2) }
                    item { AdminInputField("Outcomes (1 per line)", outcomesText, { outcomesText = it }, minLines = 2) }
                }
            },
            confirmButton = {
                GoldFilledButton(
                    text = "Save",
                    onClick = {
                        onSaveItem(
                            cs.copy(
                                propertyName = name,
                                location = loc,
                                category = cat,
                                filterGroup = grp,
                                scale = scale,
                                summary = summary.ifBlank { "Hospitality consulting mandate." },
                                outcomes = outcomesText.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Completed operational launch") },
                                imageKey = imgKey
                            )
                        )
                        editing = null
                    },
                    testTag = "save_portfolio_dialog"
                )
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel", color = MilaWarmStone) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminTestimonialsModule(
    actor: AdminUser,
    testimonials: List<TestimonialRecord>,
    onSaveTestimonial: (TestimonialRecord) -> Unit,
    onDeleteTestimonial: (TestimonialRecord) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.TESTIMONIALS)
    var editing by remember { mutableStateOf<TestimonialRecord?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.TESTIMONIALS,
                subtitle = "Publish & edit verified client testimonials displayed on the Home experience (${testimonials.size}).",
                canAdd = canWrite,
                addLabel = "Add Testimonial",
                onAddClick = { editing = TestimonialRecord(sortOrder = (testimonials.size + 1).toLong()) },
                isReadOnly = !canWrite
            )
        }
        items(testimonials, key = { it.id.ifBlank { it.quote.take(20) } }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(label = if (item.published) "Published" else "Draft", tone = if (item.published) StatusTone.EMERALD else StatusTone.MUTED)
                    Text("\"${item.quote}\"", style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory))
                    Text("${item.attributionRole} • ${item.engagementContext}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                    if (canWrite) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldOutlineButton("Edit", { editing = item }, "edit_test_${item.id}")
                            GoldOutlineButton(if (item.published) "Unpublish" else "Publish", { onSaveTestimonial(item.copy(published = !item.published)) }, "toggle_test_${item.id}")
                            TextButton(onClick = { onDeleteTestimonial(item) }) { Text("Delete", color = MilaTerracotta) }
                        }
                    }
                }
            }
        }
    }

    editing?.let { item ->
        var quote by remember(item) { mutableStateOf(item.quote) }
        var roleLabel by remember(item) { mutableStateOf(item.attributionRole) }
        var context by remember(item) { mutableStateOf(item.engagementContext) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("Client Testimonial", color = MilaIvory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminInputField("Quote", quote, { quote = it }, minLines = 3)
                    AdminInputField("Client Role (e.g., Managing Partner)", roleLabel, { roleLabel = it })
                    AdminInputField("Engagement Context", context, { context = it })
                }
            },
            confirmButton = {
                GoldFilledButton("Save", {
                    onSaveTestimonial(item.copy(quote = quote, attributionRole = roleLabel, engagementContext = context))
                    editing = null
                }, "save_testimonial_dialog")
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel", color = MilaWarmStone) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminTeamManagementModule(
    actor: AdminUser,
    team: List<TeamMemberRecord>,
    onSaveMember: (TeamMemberRecord) -> Unit,
    onDeleteMember: (TeamMemberRecord) -> Unit
) {
    val canWrite = actor.resolvedRole.canMutate(AdminModule.TEAM_MANAGEMENT)
    var editing by remember { mutableStateOf<TeamMemberRecord?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminModuleHeader(
                module = AdminModule.TEAM_MANAGEMENT,
                subtitle = "Manage MILA Consultancy, Talent, and Mixology Academy specialist profiles (${team.size}).",
                canAdd = canWrite,
                addLabel = "Add Specialist",
                onAddClick = { editing = TeamMemberRecord(sortOrder = (team.size + 1).toLong()) },
                isReadOnly = !canWrite
            )
        }
        items(team, key = { it.id.ifBlank { it.fullName } }) { m ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusPill(label = if (m.published) "Published" else "Draft", tone = if (m.published) StatusTone.EMERALD else StatusTone.MUTED)
                        Text("${m.yearsExperience}+ yrs • ${m.divisionName}", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                    }
                    Text(m.fullName, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                    Text("${m.designation} • ${m.specialty}", style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                    Text(m.credentials, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                    if (canWrite) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldOutlineButton("Edit", { editing = m }, "edit_team_${m.id}")
                            GoldOutlineButton(if (m.published) "Unpublish" else "Publish", { onSaveMember(m.copy(published = !m.published)) }, "toggle_team_${m.id}")
                            TextButton(onClick = { onDeleteMember(m) }) { Text("Delete", color = MilaTerracotta) }
                        }
                    }
                }
            }
        }
    }

    editing?.let { m ->
        var fullName by remember(m) { mutableStateOf(m.fullName) }
        var designation by remember(m) { mutableStateOf(m.designation) }
        var division by remember(m) { mutableStateOf(m.divisionName) }
        var specialty by remember(m) { mutableStateOf(m.specialty) }
        var expYears by remember(m) { mutableStateOf(m.yearsExperience.toString()) }
        var credentials by remember(m) { mutableStateOf(m.credentials) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = MilaCharcoalSurface,
            title = { Text("MILA Leadership Profile", color = MilaIvory) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { AdminInputField("Full Name / Role Title", fullName, { fullName = it }) }
                    item { AdminInputField("Designation", designation, { designation = it }) }
                    item { AdminInputField("Division", division, { division = it }) }
                    item { AdminInputField("Specialty", specialty, { specialty = it }) }
                    item { AdminInputField("Years Experience", expYears, { expYears = it }) }
                    item { AdminInputField("Credentials", credentials, { credentials = it }, minLines = 2) }
                }
            },
            confirmButton = {
                GoldFilledButton("Save", {
                    onSaveMember(
                        m.copy(
                            fullName = fullName,
                            designation = designation,
                            divisionName = division,
                            specialty = specialty,
                            yearsExperience = (expYears.toLongOrNull() ?: 10L).coerceIn(0L, 60L),
                            credentials = credentials
                        )
                    )
                    editing = null
                }, "save_team_dialog")
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel", color = MilaWarmStone) } }
        )
    }
}
