package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaseStudy
import com.example.data.ConsultancyService
import com.example.data.ConsultationServiceType
import com.example.data.HospitalityRole
import com.example.data.MilaCatalog
import com.example.data.PromoBanner
import com.example.data.Testimonial
import com.example.ui.components.GoldOutlineButton
import com.example.ui.components.LuxuryMetricTile
import com.example.ui.components.SectionHeading
import com.example.ui.theme.MilaChampagneGold
import com.example.ui.theme.MilaCharcoalSurface
import com.example.ui.theme.MilaGoldSubtle
import com.example.ui.theme.MilaIvory
import com.example.ui.theme.MilaWarmStone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtelierScreen(
    services: List<ConsultancyService>,
    onOpenServiceDetail: (ConsultancyService) -> Unit,
    onOpenConsultation: (ConsultationServiceType) -> Unit,
    onNavigateRoute: (String) -> Unit,
    openRoles: List<HospitalityRole> = MilaCatalog.openRoles,
    portfolioItems: List<CaseStudy> = MilaCatalog.caseStudies,
    testimonials: List<Testimonial> = MilaCatalog.testimonials,
    promoBanners: List<PromoBanner> = emptyList(),
    shouldShowAutoPopup: (String) -> Boolean = { false },
    onAutoPopupShown: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCaseStudy by remember { mutableStateOf<CaseStudy?>(null) }
    var activePopupBanner by remember { mutableStateOf<PromoBanner?>(null) }

    LaunchedEffect(promoBanners) {
        val autoBanner = promoBanners.firstOrNull { it.showAutoPopup && shouldShowAutoPopup(it.id) }
        if (autoBanner != null && activePopupBanner == null) {
            onAutoPopupShown(autoBanner.id)
            activePopupBanner = autoBanner
        }
    }

    val quickDirectory = remember {
        listOf(
            "About Us" to "about_us",
            "Our Services" to "our_services",
            "Industries We Serve" to "industries_we_serve",
            "Our Expertise" to "our_expertise",
            "Our Portfolio" to "our_portfolio",
            "Careers" to "careers",
            "Contact Us" to "contact_us",
            "My Enquiries" to "client_login",
            "Job Seeker Profile & CV" to "job_seeker_login"
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                HeroAtelierBanner(
                    onExploreServicesClick = { onNavigateRoute("our_services") },
                    onExploreCareersClick = { onNavigateRoute("careers") }
                )
            }

            if (promoBanners.isNotEmpty()) {
                item(key = "home_top_promo_banner") {
                    TopPromoBannerSection(
                        banners = promoBanners,
                        onOpenBannerPopup = { banner -> activePopupBanner = banner },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ALL PAGES & PORTALS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MilaChampagneGold,
                            letterSpacing = 1.6.sp
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickDirectory) { (label, route) ->
                            FilterChip(
                                selected = false,
                                onClick = { onNavigateRoute(route) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MilaCharcoalSurface,
                                    labelColor = MilaIvory
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = MilaGoldSubtle
                                ),
                                modifier = Modifier.testTag("home_quick_$route")
                            )
                        }
                    }
                }
            }

            item {
                CompanyIntroductionCard(
                    onReadAboutUs = { onNavigateRoute("about_us") },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeading(
                        eyebrow = "Experience & Track Record",
                        title = "15+ Years in Indian Hospitality",
                        subtitle = "Proven pre-opening governance, commercial restaurant launches, and specialist beverage leadership."
                    )

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
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeading(
                        eyebrow = "Three Core Business Divisions",
                        title = "Consultancy, Talent & Academy",
                        subtitle = "Select a division to explore specialized advisory services, hospitality careers, or training programs."
                    )
                    MilaCatalog.divisions.forEach { division ->
                        BusinessDivisionCard(
                            division = division,
                            onClick = { onNavigateRoute(division.targetRoute) }
                        )
                    }
                }
            }

            item {
                PracticeSnapshotSection(
                    services = services,
                    onSelectServicePage = onOpenServiceDetail,
                    onViewAllServices = { onNavigateRoute("our_services") },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                MixologySpotlightCard(
                    onExploreExpertise = { onNavigateRoute("our_expertise") },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                HomeRecruitmentSection(
                    openRoles = openRoles,
                    onBrowseCareers = { onNavigateRoute("careers") },
                    onJobSeekerPortal = { onNavigateRoute("job_seeker_login") },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeading(
                        eyebrow = "Our Portfolio",
                        title = "Pre-Opening & Culinary Highlights",
                        subtitle = "Tap any highlight to inspect operational scope or browse our full portfolio.",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(portfolioItems.take(4), key = { it.id }) { study ->
                            CaseStudyCard(
                                study = study,
                                onClick = { selectedCaseStudy = study },
                                modifier = Modifier.width(300.dp)
                            )
                        }
                    }

                    GoldOutlineButton(
                        text = "View Full Portfolio",
                        onClick = { onNavigateRoute("our_portfolio") },
                        testTag = "home_full_portfolio_button",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeading(
                        eyebrow = "Partner Perspectives",
                        title = "Client Testimonials",
                        subtitle = "Feedback from hospitality owners and operations leaders across our India engagements.",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(testimonials, key = { it.id }) { testimonial ->
                            TestimonialCard(
                                testimonial = testimonial,
                                modifier = Modifier.width(310.dp)
                            )
                        }
                    }
                }
            }

            item {
                HomeContactInfoCard(
                    onContactUs = { onNavigateRoute("contact_us") },
                    onIndustries = { onNavigateRoute("industries_we_serve") },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }

    selectedCaseStudy?.let { study ->
        ModalBottomSheet(
            onDismissRequest = { selectedCaseStudy = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MilaCharcoalSurface,
            contentColor = MilaIvory
        ) {
            CaseStudyDetailSheet(
                study = study,
                onCommissionSimilar = {
                    selectedCaseStudy = null
                    onOpenConsultation(ConsultationServiceType.HOSPITALITY_PRE_OPENING)
                }
            )
        }
    }

    activePopupBanner?.let { banner ->
        PromoBannerPopupDialog(
            banner = banner,
            onDismiss = { activePopupBanner = null },
            onActionClick = { selected ->
                activePopupBanner = null
                when (selected.actionRoute) {
                    "enquire", "contact_us" -> onOpenConsultation(ConsultationServiceType.HOSPITALITY_PRE_OPENING)
                    "our_services" -> onNavigateRoute("our_services")
                    "careers" -> onNavigateRoute("careers")
                    "our_portfolio" -> onNavigateRoute("our_portfolio")
                    "our_expertise" -> onNavigateRoute("our_expertise")
                    else -> Unit
                }
            }
        )
    }
}

@Composable
private fun PracticeSnapshotSection(
    services: List<ConsultancyService>,
    onSelectServicePage: (ConsultancyService) -> Unit,
    onViewAllServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeading(
            eyebrow = "Service Categories (01 – 08)",
            title = "Complete Consultancy Catalogue",
            subtitle = "Tap any service category to view its detailed scope, owner benefits, deliverables, and photography."
        )

        services.forEach { service ->
            Card(
                onClick = { onSelectServicePage(service) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_service_${service.type.name}"),
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
                        Text(
                            text = "${service.numberBadge}. ${service.title}",
                            style = MaterialTheme.typography.titleMedium.copy(color = MilaIvory)
                        )
                        Text(service.subtitle, style = MaterialTheme.typography.bodySmall.copy(color = MilaChampagneGold))
                        Text(
                            text = "${service.divisionName} • Timeline: ${service.typicalTimeline}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MilaWarmStone)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Open ${service.title}",
                        tint = MilaChampagneGold
                    )
                }
            }
        }

        GoldOutlineButton(
            text = "Explore All 8 Services",
            onClick = onViewAllServices,
            testTag = "view_all_advisory_button",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
