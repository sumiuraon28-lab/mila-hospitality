package com.example.data

object AdminDefaults {
    val defaultServiceCategories: List<ServiceCategoryRecord> = listOf(
        ServiceCategoryRecord(
            id = "cat_pre_opening",
            code = "HOSPITALITY_PRE_OPENING",
            sortOrder = 1L,
            title = "Hospitality Pre-Opening Consultancy",
            divisionName = "MILA Consultancy",
            summary = "End-to-end 180-day critical path governance, OS&E readiness, SOP manuals, and soft-launch stewardship.",
            published = true
        ),
        ServiceCategoryRecord(
            id = "cat_restaurant_cafe",
            code = "RESTAURANT_CAFE_DEVELOPMENT",
            sortOrder = 2L,
            title = "Restaurant and Café Development",
            divisionName = "MILA Consultancy",
            summary = "Kitchen ergonomics, espresso workflow, food-cost matrix, and commercial F&B launch management.",
            published = true
        ),
        ServiceCategoryRecord(
            id = "cat_bar_beverage",
            code = "BAR_BEVERAGE_CONSULTANCY",
            sortOrder = 3L,
            title = "Bar and Beverage Consultancy",
            divisionName = "MILA Academy & Expertise",
            summary = "Led by an award-winning three-time champion mixologist—signature craft menus and speed-bar design.",
            published = true
        ),
        ServiceCategoryRecord(
            id = "cat_training",
            code = "HOSPITALITY_TRAINING",
            sortOrder = 4L,
            title = "Hospitality Training",
            divisionName = "MILA Academy & Expertise",
            summary = "Service choreography, barista & bartender upskilling, culinary calibration, and HOD leadership.",
            published = true
        ),
        ServiceCategoryRecord(
            id = "cat_operations",
            code = "OPERATIONS_MANAGEMENT",
            sortOrder = 5L,
            title = "Operations Management",
            divisionName = "MILA Consultancy",
            summary = "Multi-outlet P&L governance, quality mystery audits, yield optimization, and cost control.",
            published = true
        ),
        ServiceCategoryRecord(
            id = "cat_recruitment",
            code = "RECRUITMENT_TALENT_ACQUISITION",
            sortOrder = 6L,
            title = "Recruitment and Talent Acquisition",
            divisionName = "MILA Talent",
            summary = "Confidential executive search for GMs, Executive Chefs, Head Mixologists, and opening brigades.",
            published = true
        ),
        ServiceCategoryRecord(
            id = "cat_concept_dev",
            code = "BUSINESS_CONCEPT_DEVELOPMENT",
            sortOrder = 7L,
            title = "Business and Concept Development",
            divisionName = "MILA Consultancy",
            summary = "Feasibility intelligence, capex/opex validation, guest positioning, and F&B concept architecture.",
            published = true
        ),
        ServiceCategoryRecord(
            id = "cat_management_solutions",
            code = "HOSPITALITY_MANAGEMENT_SOLUTIONS",
            sortOrder = 8L,
            title = "Hospitality Management Solutions",
            divisionName = "MILA Consultancy",
            summary = "Interim executive stewardship, owner representation, and structured performance turnarounds.",
            published = true
        )
    )

    val defaultSitePages: List<SitePageContent> = listOf(
        SitePageContent(
            id = "page_home",
            pageKey = "home",
            pageTitle = "Home",
            eyebrow = "Indian Hospitality Consultancy & Recruitment",
            headline = "Creating Excellence in Hospitality",
            subheadline = "Turnkey hotel pre-openings, café and restaurant setups, champion mixology curation, and executive hospitality recruitment.",
            bodyCopy = "Built on 15+ years of operational leadership, 30+ pre-opening projects, and 20+ café and restaurant setups across India.",
            heroImageKey = "hero_hotel",
            ctaLabel = "Our Services",
            published = true
        ),
        SitePageContent(
            id = "page_about",
            pageKey = "about_us",
            pageTitle = "About Us",
            eyebrow = "MILA Hospitality Consultant",
            headline = "Three Integrated Divisions Under One Advisory Standard",
            subheadline = "Serving hotel owners, heritage palace trusts, restaurateurs, and hospitality leaders across India.",
            bodyCopy = "MILA Consultancy, MILA Talent, and MILA Academy & Expertise unite commercial rigor with champion mixology and executive placement.",
            heroImageKey = "palace_resort",
            ctaLabel = "Contact Us",
            published = true
        ),
        SitePageContent(
            id = "page_careers",
            pageKey = "careers",
            pageTitle = "Careers (MILA Talent)",
            eyebrow = "MILA Talent • Hospitality Recruitment Portal",
            headline = "Executive Search & Open Hospitality Roles",
            subheadline = "Confidential placements for luxury hotels, heritage resorts, craft cocktail bars, and multi-unit café groups.",
            bodyCopy = "Candidates access our private CV vault, job alerts, and 5-stage application tracker across pan-India hospitality properties.",
            heroImageKey = "executive_recruitment",
            ctaLabel = "I AM LOOKING FOR A JOB",
            published = true
        ),
        SitePageContent(
            id = "page_expertise",
            pageKey = "our_expertise",
            pageTitle = "Our Expertise (MILA Academy)",
            eyebrow = "MILA Academy & Expertise",
            headline = "Specialist Mixology & Hospitality Mastery",
            subheadline = "Guided by an award-winning three-time champion mixologist alongside veteran hotel pre-opening directors.",
            bodyCopy = "Delivering signature botanical beverage programs, ergonomic bar design, and service academy training.",
            heroImageKey = "mixology_bar",
            ctaLabel = "Commission Advisory",
            published = true
        )
    )

    val defaultTeamMembers: List<TeamMemberRecord> = listOf(
        TeamMemberRecord(
            id = "team_lead_consultancy",
            fullName = "Principal Hospitality Practice Lead",
            designation = "Managing Principal — Pre-Openings & Operations",
            divisionName = "MILA Consultancy",
            specialty = "Luxury Hotel Pre-Openings & Asset Stewardship",
            yearsExperience = 15L,
            credentials = "Directed 30+ hotel and resort pre-openings and operational turnaround mandates across India.",
            imageKey = "palace_resort",
            sortOrder = 1L,
            published = true
        ),
        TeamMemberRecord(
            id = "team_lead_mixology",
            fullName = "Master Beverage & Mixology Director",
            designation = "Award-Winning Three-Time Champion Mixologist",
            divisionName = "MILA Academy & Expertise",
            specialty = "Craft Beverage Labs, Bar Ergonomics & Pour-Cost Engineering",
            yearsExperience = 14L,
            credentials = "Three-time champion mixologist architecting 20+ signature bar and specialty café beverage programs.",
            imageKey = "mixology_bar",
            sortOrder = 2L,
            published = true
        ),
        TeamMemberRecord(
            id = "team_lead_talent",
            fullName = "Executive Search & Talent Principal",
            designation = "Head of Hospitality Recruitment",
            divisionName = "MILA Talent",
            specialty = "GM, Executive Chef & Pre-Opening Brigade Mobilization",
            yearsExperience = 13L,
            credentials = "Leads competency-assessed executive placements for luxury urban hotels, resorts, and fine-dining groups.",
            imageKey = "executive_recruitment",
            sortOrder = 3L,
            published = true
        )
    )

    val defaultPortfolioItems: List<PortfolioItemRecord> = MilaCatalog.caseStudies.mapIndexed { idx, cs ->
        val imgKey = when (cs.filterGroup) {
            "Mixology" -> "mixology_bar"
            "Café & Restaurant" -> "fine_dining"
            "Executive Search" -> "hero_hotel"
            else -> "palace_resort"
        }
        PortfolioItemRecord(
            id = cs.id,
            propertyName = cs.propertyName,
            location = cs.location,
            category = cs.category,
            filterGroup = cs.filterGroup,
            scale = cs.scale,
            summary = cs.summary,
            outcomes = cs.outcomes,
            imageKey = imgKey,
            sortOrder = (idx + 1).toLong(),
            published = true
        )
    }

    val defaultTestimonials: List<TestimonialRecord> = MilaCatalog.testimonials.mapIndexed { idx, t ->
        TestimonialRecord(
            id = t.id,
            quote = t.quote,
            attributionRole = t.attributionRole,
            engagementContext = t.engagementContext,
            sortOrder = (idx + 1).toLong(),
            published = true
        )
    }

    val defaultVacancies: List<JobVacancyRecord> = MilaCatalog.openRoles.map { role ->
        JobVacancyRecord.fromHospitalityRole(role, published = true)
    }
}
