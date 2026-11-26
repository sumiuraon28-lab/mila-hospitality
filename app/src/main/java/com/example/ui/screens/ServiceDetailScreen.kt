package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConsultancyService
import com.example.data.ConsultationServiceType
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoal
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaWarmStone

@Composable
fun ServiceDetailScreen(
    service: ConsultancyService,
    allServices: List<ConsultancyService>,
    onSelectServicePage: (ConsultancyService) -> Unit,
    onEnquireNow: (ConsultationServiceType) -> Unit,
    onBackToCatalogue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = allServices.indexOfFirst { it.slug == service.slug }.coerceAtLeast(0)
    val nextService = if (allServices.size > 1) {
        allServices[(currentIndex + 1) % allServices.size]
    } else {
        null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("service_detail_${service.slug}"),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                Image(
                    painter = painterResource(id = service.imageRes),
                    contentDescription = service.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x88191D1B),
                                    Color(0x99191D1B),
                                    MilaCharcoal
                                )
                            )
                        )
                )
                Surface(
                    onClick = onBackToCatalogue,
                    color = Color(0xCC111412),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("service_detail_back")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Our Services",
                            tint = MilaChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "All Services",
                            style = MaterialTheme.typography.labelMedium.copy(color = MilaIvory)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            label = "${service.numberBadge} • ${service.divisionName}",
                            tone = StatusTone.GOLD
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MilaChampagneGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = service.typicalTimeline,
                                style = MaterialTheme.typography.labelSmall.copy(color = MilaIvory)
                            )
                        }
                    }
                    Text(
                        text = service.title,
                        style = MaterialTheme.typography.headlineLarge.copy(color = MilaIvory)
                    )
                    Text(
                        text = service.subtitle,
                        style = MaterialTheme.typography.titleSmall.copy(color = MilaChampagneGold)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CONSULTANCY SERVICE DIRECTORY (01 – 08)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allServices, key = { it.id }) { item ->
                        val selected = item.slug == service.slug
                        FilterChip(
                            selected = selected,
                            onClick = { if (!selected) onSelectServicePage(item) },
                            label = { Text("${item.numberBadge}. ${item.title}") },
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
                            modifier = Modifier.testTag("service_switcher_${item.slug}")
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "OVERVIEW & ADVISORY MANDATE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = service.detailedDescription,
                        style = MaterialTheme.typography.bodyLarge.copy(color = MilaIvoryMuted)
                    )
                }
            }
        }

        item {
            EditorialServiceListCard(
                eyebrow = "Scope of Engagement",
                title = "Services Included",
                icon = Icons.Rounded.Layers,
                items = service.servicesIncluded,
                elevated = false,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            EditorialServiceListCard(
                eyebrow = "Commercial & Strategic Value",
                title = "Benefits for Business Owners",
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                items = service.ownerBenefits,
                elevated = true,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            EditorialServiceListCard(
                eyebrow = "Tangible Outputs",
                title = "Deliverables",
                icon = Icons.Rounded.AssignmentTurnedIn,
                items = service.deliverables,
                elevated = false,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
                border = BorderStroke(1.dp, MilaGoldMuted),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "COMMISSION THIS PRACTICE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = "Ready to Discuss ${service.title}?",
                        style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                    )
                    Text(
                        text = "Select your property scale, Indian city, and business requirements directly in our enquiry form. A MILA partner responds within one business day.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
                    )
                    GoldFilledButton(
                        text = "Enquire Now",
                        onClick = { onEnquireNow(service.type) },
                        testTag = "service_detail_enquire_now",
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nextService != null) {
                        HorizontalDivider(color = MilaGoldSubtle)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NEXT SERVICE CATEGORY",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                                )
                                Text(
                                    text = "${nextService.numberBadge}. ${nextService.title}",
                                    style = MaterialTheme.typography.titleSmall.copy(color = MilaIvory)
                                )
                            }
                            TextButton(
                                onClick = { onSelectServicePage(nextService) },
                                modifier = Modifier.testTag("service_detail_next")
                            ) {
                                Text("Open", color = MilaChampagneGold, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Next service",
                                    tint = MilaChampagneGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialServiceListCard(
    eyebrow: String,
    title: String,
    icon: ImageVector,
    items: List<String>,
    elevated: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (elevated) MilaCharcoalElevated else MilaCharcoalSurface
        ),
        border = BorderStroke(1.dp, if (elevated) MilaGoldMuted else MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.4.sp
                        )
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                    )
                }
            }
            HorizontalDivider(color = MilaGoldSubtle)
            items.forEachIndexed { index, point ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(17.dp)
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory)
                    )
                }
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
