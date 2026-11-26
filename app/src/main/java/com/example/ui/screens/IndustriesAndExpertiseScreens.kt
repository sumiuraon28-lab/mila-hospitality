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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.ConsultationServiceType
import com.example.data.ExpertisePillar
import com.example.data.IndustrySector
import com.example.data.MilaCatalog
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.SectionHeading
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalDeep
import com.example.ui.theme.MilaCharcoalElevated
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldMuted
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaIvoryMuted
import com.example.ui.theme.MilaWarmStone

@Composable
fun IndustriesWeServeScreen(
    onRequestMandate: (ConsultationServiceType) -> Unit,
    onViewPortfolio: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            SectionHeading(
                eyebrow = "Industries We Serve",
                title = "Sector-Specific Hospitality Mastery",
                subtitle = "Tailored operational playbooks for India's luxury hotels, heritage resorts, fine dining rooms, and specialty café groups."
            )
        }

        items(MilaCatalog.industries, key = { it.id }) { sector ->
            IndustrySectorCard(
                sector = sector,
                onConsultClick = {
                    val targetService = when (sector.id) {
                        "ind_cafes_roasteries" -> ConsultationServiceType.RESTAURANT_CAFE_DEVELOPMENT
                        "ind_fine_dining" -> ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY
                        else -> ConsultationServiceType.HOSPITALITY_PRE_OPENING
                    }
                    onRequestMandate(targetService)
                }
            )
        }

        item {
            GoldOutlineButton(
                text = "Explore Our Portfolio",
                onClick = onViewPortfolio,
                testTag = "industries_view_portfolio_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IndustrySectorCard(
    sector: IndustrySector,
    onConsultClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("industry_card_${sector.id}"),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
            ) {
                Image(
                    painter = painterResource(id = sector.imageRes),
                    contentDescription = sector.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9191D1B)))
                        )
                )
                Text(
                    text = sector.regions.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.2.sp
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(sector.name, style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory))
                Text(sector.overview, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted))
                HorizontalDivider(color = MilaGoldSubtle)
                sector.advisoryFocus.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MilaChampagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(point, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                GoldFilledButton(
                    text = "Request Sector Advisory",
                    onClick = onConsultClick,
                    testTag = "industry_consult_${sector.id}",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun OurExpertiseScreen(
    onRequestMandate: (ConsultationServiceType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            SectionHeading(
                eyebrow = "MILA Academy & Expertise",
                title = "Specialist Mixology, Training & Pre-Opening Mastery",
                subtitle = "Combining 15+ years of operational execution with award-winning three-time champion mixology labs and service training academies."
            )
        }

        items(MilaCatalog.expertisePillars, key = { it.id }) { pillar ->
            ExpertisePillarCard(
                pillar = pillar,
                onCommission = {
                    val type = when (pillar.id) {
                        "exp_mixology" -> ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY
                        "exp_cafe_fnb" -> ConsultationServiceType.RESTAURANT_CAFE_DEVELOPMENT
                        "exp_academy_talent" -> ConsultationServiceType.HOSPITALITY_TRAINING
                        else -> ConsultationServiceType.HOSPITALITY_PRE_OPENING
                    }
                    onRequestMandate(type)
                }
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MilaChampagneGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "MILA MIXOLOGY LAB & ACADEMY BENCHMARKS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MilaChampagneGold,
                                letterSpacing = 1.4.sp
                            )
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.img_mixology_bar_1795637233060),
                        contentDescription = "MILA Signature Cocktail Lab",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    MilaCatalog.mixologySignatures.forEach { sig ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MilaCharcoalDeep)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalBar,
                                        contentDescription = null,
                                        tint = MilaChampagneGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = sig.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = MilaChampagneGold,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                Text(sig.tastingNotes, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                                Text(sig.glassAndGarnish, style = MaterialTheme.typography.labelSmall.copy(color = MilaWarmStone))
                            }
                        }
                    }
                    GoldFilledButton(
                        text = "Commission Beverage or Academy Training",
                        onClick = { onRequestMandate(ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY) },
                        testTag = "expertise_mixology_cta",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpertisePillarCard(
    pillar: ExpertisePillar,
    onCommission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expertise_card_${pillar.id}"),
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
                Text(
                    text = "DISCIPLINE ${pillar.code}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.5.sp
                    )
                )
                StatusPill(label = pillar.headlineMetric, tone = StatusTone.GOLD)
            }
            Text(pillar.title, style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory))
            Text(pillar.summary, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted))
            HorizontalDivider(color = MilaGoldSubtle)
            pillar.capabilities.forEach { capability ->
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
                    Text(capability, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            GoldOutlineButton(
                text = "Engage This Discipline",
                onClick = onCommission,
                testTag = "engage_${pillar.id}",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
