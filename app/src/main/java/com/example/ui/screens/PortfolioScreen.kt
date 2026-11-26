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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaseStudy
import com.example.data.ConsultationServiceType
import com.example.data.MilaCatalog
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.SectionHeading
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalBorder
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaWarmStone

private val portfolioFilters = listOf("All", "Pre-Opening", "Café & Restaurant", "Mixology", "Executive Search")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OurPortfolioScreen(
    onCommissionSimilar: (ConsultationServiceType) -> Unit,
    caseStudies: List<CaseStudy> = MilaCatalog.caseStudies,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var activeStudy by remember { mutableStateOf<CaseStudy?>(null) }

    val filteredStudies = remember(selectedFilter, caseStudies) {
        if (selectedFilter == "All") {
            caseStudies
        } else {
            caseStudies.filter { it.filterGroup == selectedFilter }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeading(
                eyebrow = "Our Portfolio",
                title = "30+ Pre-Openings & 20+ Setups",
                subtitle = "Representative luxury hotels, heritage palaces, craft cocktail bars, and specialty cafés across India."
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(portfolioFilters) { filter ->
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
                        modifier = Modifier.testTag("portfolio_filter_$filter")
                    )
                }
            }
        }

        items(filteredStudies, key = { it.id }) { study ->
            PortfolioCaseCard(
                study = study,
                onClick = { activeStudy = study }
            )
        }
    }

    activeStudy?.let { study ->
        ModalBottomSheet(
            onDismissRequest = { activeStudy = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MilaCharcoalSurface,
            contentColor = MilaIvory
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = study.imageRes),
                    contentDescription = study.propertyName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                StatusPill(label = study.category, tone = StatusTone.GOLD)
                Text(
                    text = study.propertyName,
                    style = MaterialTheme.typography.headlineMedium.copy(color = MilaIvory)
                )
                Text(
                    text = "${study.location} • ${study.scale}",
                    style = MaterialTheme.typography.titleSmall.copy(color = MilaChampagneGold)
                )
                Text(
                    text = study.summary,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MilaIvoryMuted)
                )
                HorizontalDivider(color = MilaGoldSubtle)
                study.outcomes.forEach { outcome ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MilaChampagneGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(outcome, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                GoldFilledButton(
                    text = "Commission a Similar Project",
                    onClick = {
                        val mappedType = when (study.filterGroup) {
                            "Mixology" -> ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY
                            "Café & Restaurant" -> ConsultationServiceType.RESTAURANT_CAFE_DEVELOPMENT
                            "Executive Search" -> ConsultationServiceType.RECRUITMENT_TALENT_ACQUISITION
                            else -> ConsultationServiceType.HOSPITALITY_PRE_OPENING
                        }
                        activeStudy = null
                        onCommissionSimilar(mappedType)
                    },
                    testTag = "portfolio_sheet_commission",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PortfolioCaseCard(
    study: CaseStudy,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_card_${study.id}"),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                Image(
                    painter = painterResource(id = study.imageRes),
                    contentDescription = study.propertyName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC191D1B)))
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    StatusPill(label = study.category, tone = StatusTone.GOLD)
                }
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = study.propertyName,
                    style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${study.location} • ${study.scale}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                    )
                }
                Text(
                    text = study.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
                )
            }
        }
    }
}
