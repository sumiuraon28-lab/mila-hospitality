package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExperienceFilter
import com.example.data.HospitalityRole
import com.example.data.MilaCatalog
import com.example.data.RoleCategory
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.SectionHeading
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

private val talentLocationFilters = listOf(
    "All Locations",
    "Mumbai",
    "New Delhi NCR",
    "Bengaluru",
    "Udaipur & Rajasthan",
    "Goa",
    "Hyderabad"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentScreen(
    savedRoleIds: Set<String>,
    appliedRoleIds: Set<String>,
    onToggleSaveRole: (HospitalityRole, Boolean) -> Unit,
    onApplyRole: (HospitalityRole) -> Unit,
    onHireTalentMandate: () -> Unit,
    onOpenJobSeekerPortal: () -> Unit,
    jobAlertsEnabled: Boolean = false,
    roles: List<HospitalityRole> = MilaCatalog.openRoles,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("All Locations") }
    var selectedCategory by remember { mutableStateOf(RoleCategory.ALL) }
    var selectedExperience by remember { mutableStateOf(ExperienceFilter.ALL) }
    var inspectingRole by remember { mutableStateOf<HospitalityRole?>(null) }

    val filteredRoles = remember(searchQuery, selectedLocation, selectedCategory, selectedExperience, roles) {
        roles.filter { role ->
            val matchesLocation = selectedLocation == "All Locations" ||
                role.cityRegion.equals(selectedLocation, ignoreCase = true) ||
                role.location.contains(selectedLocation, ignoreCase = true)
            val matchesCategory = selectedCategory == RoleCategory.ALL || role.category == selectedCategory
            val matchesExperience = selectedExperience.matches(role.minExperienceYears)
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                role.title.lowercase().contains(q) ||
                role.location.lowercase().contains(q) ||
                role.cityRegion.lowercase().contains(q) ||
                role.propertyTier.lowercase().contains(q) ||
                role.desiredSkills.any { it.lowercase().contains(q) }
            matchesLocation && matchesCategory && matchesExperience && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeading(
                eyebrow = "MILA Talent • Hospitality Recruitment Portal",
                title = "Executive Search & Open Hospitality Roles",
                subtitle = "Confidential placements for luxury hotels, heritage resorts, craft cocktail bars, and multi-unit café groups across India."
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
                border = BorderStroke(1.dp, MilaGoldMuted),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MILA TALENT • CANDIDATE & EMPLOYER ACCESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MilaChampagneGold,
                                letterSpacing = 1.4.sp
                            )
                        )
                        if (jobAlertsEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = "Job alerts active",
                                    tint = MilaChampagneGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Alerts On",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold)
                                )
                            }
                        }
                    }

                    GoldFilledButton(
                        text = "I AM LOOKING FOR A JOB",
                        onClick = onOpenJobSeekerPortal,
                        testTag = "talent_iam_looking_for_job_button",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldOutlineButton(
                            text = "Candidate Login / CV",
                            onClick = onOpenJobSeekerPortal,
                            testTag = "careers_job_seeker_portal_button",
                            modifier = Modifier.weight(1f)
                        )
                        GoldOutlineButton(
                            text = "Hire Talent",
                            onClick = onHireTalentMandate,
                            testTag = "talent_hire_mandate_button",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search roles, cities, or skills (e.g., Chef, Mixology, Udaipur)...", color = MilaWarmStone) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search jobs",
                        tint = MilaChampagneGold
                    )
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
                    .testTag("talent_search_input")
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "FILTER BY LOCATION",
                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.2.sp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(talentLocationFilters) { city ->
                        val selected = selectedLocation == city
                        FilterChip(
                            selected = selected,
                            onClick = { selectedLocation = city },
                            label = { Text(city) },
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
                            modifier = Modifier.testTag("filter_location_$city")
                        )
                    }
                }

                Text(
                    text = "FILTER BY DEPARTMENT",
                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.2.sp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RoleCategory.entries) { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayLabel) },
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
                            modifier = Modifier.testTag("filter_chip_${category.name}")
                        )
                    }
                }

                Text(
                    text = "FILTER BY EXPERIENCE",
                    style = MaterialTheme.typography.labelSmall.copy(color = MilaChampagneGold, letterSpacing = 1.2.sp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExperienceFilter.entries) { exp ->
                        val selected = selectedExperience == exp
                        FilterChip(
                            selected = selected,
                            onClick = { selectedExperience = exp },
                            label = { Text(exp.label) },
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
                            modifier = Modifier.testTag("filter_exp_${exp.name}")
                        )
                    }
                }
            }
        }

        if (filteredRoles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkOutline,
                            contentDescription = null,
                            tint = MilaChampagneGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No matching vacancies",
                            style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory)
                        )
                        Text(
                            text = "Try resetting location, department, or experience filters.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }
                }
            }
        } else {
            items(filteredRoles, key = { it.id }) { role ->
                val isSaved = role.id in savedRoleIds
                val isApplied = role.id in appliedRoleIds
                RoleMandateCard(
                    role = role,
                    isSaved = isSaved,
                    isApplied = isApplied,
                    onToggleSave = { onToggleSaveRole(role, isSaved) },
                    onInspect = { inspectingRole = role },
                    onApply = { onApplyRole(role) }
                )
            }
        }
    }

    inspectingRole?.let { role ->
        val isSaved = role.id in savedRoleIds
        ModalBottomSheet(
            onDismissRequest = { inspectingRole = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MilaCharcoalSurface,
            contentColor = MilaIvory
        ) {
            RoleDetailSheet(
                role = role,
                isSaved = isSaved,
                isApplied = role.id in appliedRoleIds,
                onToggleSave = { onToggleSaveRole(role, isSaved) },
                onApply = {
                    inspectingRole = null
                    onApplyRole(role)
                }
            )
        }
    }
}

@Composable
private fun RoleMandateCard(
    role: HospitalityRole,
    isSaved: Boolean,
    isApplied: Boolean,
    onToggleSave: () -> Unit,
    onInspect: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        onClick = onInspect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("role_card_${role.id}"),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = role.category.displayLabel, tone = StatusTone.GOLD)
                    if (role.isUrgent) {
                        StatusPill(label = "Priority Search", tone = StatusTone.EMERALD)
                    }
                    if (isApplied) {
                        StatusPill(label = "Applied", tone = StatusTone.EMERALD)
                    }
                }
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bookmark_${role.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from shortlist" else "Shortlist role",
                        tint = if (isSaved) MilaChampagneGold else MilaWarmStone
                    )
                }
            }

            Text(
                text = role.title,
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = role.propertyTier,
                style = MaterialTheme.typography.bodyMedium.copy(color = MilaChampagneGold)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MilaWarmStone,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${role.location} • ${role.experienceRequired}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                )
            }
            Text(
                text = role.summary,
                style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted),
                maxLines = 2
            )

            HorizontalDivider(color = MilaGoldSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMPENSATION",
                        style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                    )
                    Text(
                        text = role.compensation,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MilaIvory,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                GoldFilledButton(
                    text = if (isApplied) "Applied" else "Apply Now",
                    onClick = onApply,
                    enabled = !isApplied,
                    testTag = "apply_${role.id}"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleDetailSheet(
    role: HospitalityRole,
    isSaved: Boolean,
    isApplied: Boolean,
    onToggleSave: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatusPill(label = role.category.displayLabel, tone = StatusTone.GOLD)
        Text(
            text = role.title,
            style = MaterialTheme.typography.headlineMedium.copy(color = MilaIvory)
        )
        Text(
            text = "${role.propertyTier} • ${role.location}",
            style = MaterialTheme.typography.titleSmall.copy(color = MilaChampagneGold)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("COMPENSATION", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                Text(role.compensation, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("EXPERIENCE", style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                Text(role.experienceRequired, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
            }
        }
        HorizontalDivider(color = MilaGoldSubtle)
        Text(
            text = role.summary,
            style = MaterialTheme.typography.bodyLarge.copy(color = MilaIvoryMuted)
        )
        if (role.desiredSkills.isNotEmpty()) {
            Text(
                text = "DESIRED COMPETENCIES",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.4.sp
                )
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                role.desiredSkills.forEach { skill ->
                    StatusPill(label = skill, tone = StatusTone.MUTED)
                }
            }
        }
        Text(
            text = "PRIMARY MANDATES",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MilaChampagneGold,
                letterSpacing = 1.4.sp
            )
        )
        role.keyMandates.forEach { mandate ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MilaChampagneGold,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Text(
                    text = mandate,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoldOutlineButton(
                text = if (isSaved) "Saved" else "Save Role",
                onClick = onToggleSave,
                testTag = "sheet_save_${role.id}",
                modifier = Modifier.weight(0.9f)
            )
            GoldFilledButton(
                text = if (isApplied) "Applied" else "Apply Now",
                onClick = onApply,
                enabled = !isApplied,
                testTag = "sheet_apply_${role.id}",
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}
