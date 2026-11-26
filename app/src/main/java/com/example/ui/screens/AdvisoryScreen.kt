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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ConsultancyService
import com.example.data.ConsultationServiceType
import com.example.data.MilaCatalog
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.SectionHeading
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

@Composable
fun AdvisoryScreen(
    services: List<ConsultancyService>,
    onOpenServiceDetail: (ConsultancyService) -> Unit,
    onRequestMandate: (ConsultationServiceType) -> Unit,
    modifier: Modifier = Modifier
) {
    val divisionFilters = remember {
        listOf("All (8)", "MILA Consultancy", "MILA Academy & Expertise", "MILA Talent")
    }
    var selectedFilter by remember { mutableStateOf("All (8)") }

    val visibleServices = remember(services, selectedFilter) {
        if (selectedFilter == "All (8)") {
            services
        } else {
            services.filter { it.divisionName == selectedFilter }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            SectionHeading(
                eyebrow = "MILA Consultancy • Complete Service Catalogue",
                title = "Eight Specialized Hospitality Practices",
                subtitle = "Select any practice to open its dedicated advisory page, inspect deliverables and owner benefits, or submit an enquiry."
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(divisionFilters) { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
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
                        modifier = Modifier.testTag("services_filter_$filter")
                    )
                }
            }
        }

        items(visibleServices, key = { it.id }) { service ->
            ConsultancyServiceCard(
                service = service,
                onViewDetails = { onOpenServiceDetail(service) },
                onRequestMandate = { onRequestMandate(service.type) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            MixologyAtelierSection(
                onCommissionBeverage = {
                    onRequestMandate(ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY)
                }
            )
        }
    }
}

@Composable
private fun ConsultancyServiceCard(
    service: ConsultancyService,
    onViewDetails: () -> Unit,
    onRequestMandate: () -> Unit
) {
    Card(
        onClick = onViewDetails,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("advisory_card_${service.type.name}"),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
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
                                listOf(Color.Transparent, Color(0xD9191D1B))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = "${service.numberBadge} • ${service.divisionName}", tone = StatusTone.GOLD)
                    Text(
                        text = service.typicalTimeline,
                        style = MaterialTheme.typography.labelSmall.copy(color = MilaIvory)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                )
                Text(
                    text = service.subtitle,
                    style = MaterialTheme.typography.titleSmall.copy(color = MilaChampagneGold)
                )
                Text(
                    text = service.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
                )
                HorizontalDivider(color = MilaGoldSubtle)
                Text(
                    text = "HIGHLIGHTED DELIVERABLES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.4.sp
                    )
                )
                service.deliverables.take(3).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MilaChampagneGold,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldOutlineButton(
                        text = "View Details",
                        onClick = onViewDetails,
                        testTag = "view_service_${service.slug}",
                        modifier = Modifier.weight(1f)
                    )
                    GoldFilledButton(
                        text = "Enquire Now",
                        onClick = onRequestMandate,
                        testTag = "commission_${service.type.name}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MixologyAtelierSection(
    onCommissionBeverage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mixology_atelier_section"),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
        border = BorderStroke(1.dp, MilaGoldMuted),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Three-Time Champion Mixologist",
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = "SPECIALIST MIXOLOGY LEADERSHIP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.6.sp
                        )
                    )
                    Text(
                        text = "Three-Time Champion Mixologist",
                        style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.img_mixology_bar_1795637233060),
                contentDescription = "Signature Cocktail Atelier",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            MilaCatalog.mixologyHonors.forEach { honor ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalBar,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = honor,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MilaIvory,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            HorizontalDivider(color = MilaGoldSubtle)

            Text(
                text = "SIGNATURE INDIAN BOTANICAL BENCHMARKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.4.sp
                )
            )

            MilaCatalog.mixologySignatures.forEach { signature ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MilaCharcoalDeep)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = signature.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MilaChampagneGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = signature.inspiration,
                            style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone)
                        )
                        Text(
                            text = signature.tastingNotes,
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted)
                        )
                        Text(
                            text = signature.glassAndGarnish,
                            style = MaterialTheme.typography.labelSmall.copy(color = MilaIvory)
                        )
                    }
                }
            }

            GoldFilledButton(
                text = "Enquire About Bar & Beverage Consultancy",
                onClick = onCommissionBeverage,
                testTag = "book_beverage_architecture_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
