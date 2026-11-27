package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.WineBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BusinessDivision
import com.example.data.CaseStudy
import com.example.data.ConsultationServiceType
import com.example.data.HospitalityRole
import com.example.data.MilaCatalog
import com.example.data.Testimonial
import com.example.ui.components.GoldFilledButton
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.SectionHeading
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
fun HeroAtelierBanner(
    onExploreServicesClick: () -> Unit = {},
    onExploreCareersClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_hotel_1795637232836),
            contentDescription = "Luxury Indian Heritage Hotel Lobby",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66191D1B),
                            Color(0xB8191D1B),
                            MilaCharcoal
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(
                label = "Indian Hospitality Consultancy & Recruitment",
                tone = StatusTone.GOLD
            )
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.displayLarge.copy(color = MilaIvory)
            )
            Text(
                text = "Turnkey hotel pre-openings, café and restaurant setups, champion mixology curation, and executive hospitality recruitment.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted),
                modifier = Modifier.widthIn(max = 560.dp)
            )
        }
    }
}

@Composable
fun CompanyIntroductionCard(
    onReadAboutUs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "MILA HOSPITALITY CONSULTANT",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "International Consulting Standards Tailored for Indian Hospitality",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = "Built on 15+ years of operational leadership, MILA partners with hotel owners, heritage palace trusts, restaurateurs, and hospitality leaders across three focused divisions: MILA Consultancy, MILA Talent, and MILA Academy & Expertise.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
            )
            GoldOutlineButton(
                text = "Discover About Us",
                onClick = onReadAboutUs,
                testTag = "home_intro_about_button"
            )
        }
    }
}

@Composable
fun BusinessDivisionCard(
    division: BusinessDivision,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("division_card_${division.id}"),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
        border = BorderStroke(1.dp, MilaGoldMuted),
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
                    text = "DIVISION ${division.code}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MilaChampagneGold,
                        letterSpacing = 1.5.sp
                    )
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open ${division.name}",
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = division.name,
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Text(
                text = division.tagline,
                style = MaterialTheme.typography.titleSmall.copy(color = MilaChampagneGold)
            )
            Text(
                text = division.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
            )
            HorizontalDivider(color = MilaGoldSubtle)
            division.highlights.forEach { bullet ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory)
                    )
                }
            }
        }
    }
}

@Composable
fun MixologySpotlightCard(
    onExploreExpertise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onExploreExpertise,
        modifier = modifier
            .fillMaxWidth()
            .testTag("mixology_spotlight_card"),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalElevated),
        border = BorderStroke(1.dp, MilaGoldMuted),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mixology_bar_1795637233060),
                    contentDescription = "Award-Winning Mixology Craft Cocktail Bar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC191D1B))))
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WineBar,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AWARD-WINNING THREE-TIME CHAMPION MIXOLOGIST",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Specialist Mixology & Beverage Lab",
                    style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
                )
                Text(
                    text = "Bespoke cocktail programs, ergonomic speed-bar engineering, zero-waste botanical prep workflows, and hands-on bar-team mastery.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Explore MILA Academy & Expertise",
                        style = MaterialTheme.typography.labelLarge.copy(color = MilaChampagneGold)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeRecruitmentSection(
    onBrowseCareers: () -> Unit,
    openRoles: List<HospitalityRole> = MilaCatalog.openRoles,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeading(
            eyebrow = "MILA Talent • Recruitment",
            title = "Hospitality Executive Search & Careers",
            subtitle = "Connecting properties with vetted General Managers, Executive Chefs, Mixologists, and pre-opening hospitality teams."
        )

        openRoles.take(2).forEach { role ->
            Card(
                onClick = onBrowseCareers,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_role_${role.id}"),
                colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
                border = BorderStroke(1.dp, MilaGoldSubtle),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusPill(label = role.category.displayLabel, tone = StatusTone.GOLD)
                        Text(role.title, style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory))
                        Text(
                            text = "${role.location} • ${role.experienceRequired}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Inspect ${role.title}",
                        tint = MilaChampagneGold
                    )
                }
            }
        }

        GoldFilledButton(
            text = "I AM LOOKING FOR A JOB",
            onClick = onBrowseCareers,
            testTag = "iam_looking_for_job_button",
            modifier = Modifier.fillMaxWidth()
        )

        GoldOutlineButton(
            text = "View All Careers",
            onClick = onBrowseCareers,
            testTag = "home_view_careers_button",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TestimonialCard(
    testimonial: Testimonial,
    modifier: Modifier = Modifier
) {
    val safeRating = testimonial.rating.coerceIn(1, 5)
    Card(
        modifier = modifier,
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
                Icon(
                    imageVector = Icons.Rounded.FormatQuote,
                    contentDescription = null,
                    tint = MilaChampagneGold,
                    modifier = Modifier.size(26.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= safeRating) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = if (star == 1) "$safeRating out of 5 stars" else null,
                            tint = if (star <= safeRating) MilaChampagneGold else MilaWarmStone,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Text(
                text = "\"${testimonial.quote}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MilaIvory,
                    fontStyle = FontStyle.Italic
                )
            )
            HorizontalDivider(color = MilaGoldSubtle)
            Text(
                text = testimonial.attributionRole,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MilaChampagneGold,
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (testimonial.engagementContext.isNotBlank()) {
                Text(
                    text = testimonial.engagementContext,
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                )
            }
            if (testimonial.adminReply.isNotBlank()) {
                Surface(
                    color = MilaCharcoalDeep,
                    border = BorderStroke(1.dp, MilaGoldSubtle),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Reply,
                                contentDescription = null,
                                tint = MilaChampagneGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "MILA HOSPITALITY REPLY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MilaChampagneGold,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Text(
                            text = testimonial.adminReply,
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CaseStudyCard(
    study: CaseStudy,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag("case_study_${study.id}"),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalSurface),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                Image(
                    painter = painterResource(id = study.imageRes),
                    contentDescription = study.propertyName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color(0xCC111412),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = study.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.0.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = study.propertyName,
                    style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory),
                    maxLines = 2
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MilaChampagneGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${study.location} • ${study.scale}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                    )
                }
                Text(
                    text = study.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvoryMuted),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun HomeContactInfoCard(
    onContactUs: () -> Unit,
    onIndustries: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MilaCharcoalDeep),
        border = BorderStroke(1.dp, MilaGoldSubtle),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CONTACT & REGIONAL COVERAGE",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MilaChampagneGold,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "Partner With MILA Across India",
                style = MaterialTheme.typography.headlineSmall.copy(color = MilaIvory)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Public, contentDescription = null, tint = MilaChampagneGold, modifier = Modifier.size(18.dp))
                Text(
                    text = "Mumbai & West • New Delhi NCR • Bengaluru & South • Rajasthan & Goa",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaIvory)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MilaChampagneGold, modifier = Modifier.size(18.dp))
                Text(
                    text = "Advisory & recruitment enquiries answered within 1 business day (IST).",
                    style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoldFilledButton("Contact Us", onContactUs, "home_contact_us_btn", Modifier.weight(1f))
                GoldOutlineButton("Industries", onIndustries, "home_industries_btn", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CaseStudyDetailSheet(
    study: CaseStudy,
    onCommissionSimilar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = study.imageRes),
            contentDescription = study.propertyName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        StatusPill(label = study.category, tone = StatusTone.GOLD)
        Text(study.propertyName, style = MaterialTheme.typography.headlineMedium.copy(color = MilaIvory))
        Text("${study.location} • ${study.scale}", style = MaterialTheme.typography.titleSmall.copy(color = MilaChampagneGold))
        Text(study.summary, style = MaterialTheme.typography.bodyLarge.copy(color = MilaIvoryMuted))
        HorizontalDivider(color = MilaGoldSubtle)
        Text(
            text = "KEY OUTCOMES",
            style = MaterialTheme.typography.labelMedium.copy(color = MilaChampagneGold, letterSpacing = 1.5.sp)
        )
        study.outcomes.forEach { outcome ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MilaChampagneGold, modifier = Modifier.size(18.dp))
                Text(outcome, style = MaterialTheme.typography.bodyMedium.copy(color = MilaIvory))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        GoldFilledButton(
            text = "Enquire About Similar Mandate",
            onClick = onCommissionSimilar,
            testTag = "case_study_commission_button",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
