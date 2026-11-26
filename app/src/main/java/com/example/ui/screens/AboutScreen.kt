package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MilaCatalog
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.LuxuryMetricTile
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
fun AboutUsScreen(
    onContactUs: () -> Unit,
    onViewExpertise: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SectionHeading(
                    eyebrow = "About Us • MILA Hospitality Consultant",
                    title = "Creating Excellence in Hospitality",
                    subtitle = "A serious business platform serving the Indian hospitality industry across luxury pre-openings, café & restaurant setups, executive recruitment, and specialist mixology."
                )
            }

            item {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_hotel_1795637232836),
                    contentDescription = "MILA Hospitality Luxury Atrium",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(MaterialTheme.shapes.large)
                )
            }

            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MilaCatalog.impactMetrics.forEach { metric ->
                            LuxuryMetricTile(metric = metric, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LuxuryMetricTile(MilaCatalog.impactMetrics[0], Modifier.weight(1f))
                            LuxuryMetricTile(MilaCatalog.impactMetrics[1], Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LuxuryMetricTile(MilaCatalog.impactMetrics[2], Modifier.weight(1f))
                            LuxuryMetricTile(MilaCatalog.impactMetrics[3], Modifier.weight(1f))
                        }
                    }
                }
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
                            StatusPill(label = "Three-Time Champion Mixologist", tone = StatusTone.GOLD)
                        }
                        Text(
                            text = "Three Integrated Divisions Under One Advisory Standard",
                            style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                        )
                        Text(
                            text = "With 15+ years of hospitality industry experience, 30+ pre-opening projects, and 20+ café and restaurant setups, MILA unites MILA Consultancy, MILA Talent, and MILA Academy & Expertise.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
                        )
                    }
                }
            }

            item {
                SectionHeading(
                    eyebrow = "Our Charter",
                    title = "How We Partner With Owners"
                )
            }

            items(MilaCatalog.leadershipPrinciples) { (title, desc) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = MilaChampagneGold,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                            Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = MilaIvoryMuted))
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = MilaChampagneGold
                            )
                            Text(
                                text = "PAN-INDIA HOSPITALITY COVERAGE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MilaChampagneGold,
                                    letterSpacing = 1.4.sp
                                )
                            )
                        }
                        HorizontalDivider(color = MilaGoldSubtle)
                        MilaCatalog.serviceRegions.forEach { (region, detail) ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = region,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MilaIvory,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GoldFilledButton(
                                text = "Contact Us",
                                onClick = onContactUs,
                                testTag = "about_contact_button",
                                modifier = Modifier.weight(1f)
                            )
                            GoldOutlineButton(
                                text = "MILA Academy",
                                onClick = onViewExpertise,
                                testTag = "about_expertise_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
