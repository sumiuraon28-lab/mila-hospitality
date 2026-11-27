package com.example.data

import com.example.R

object MilaCatalog {
    val impactMetrics = listOf(
        ImpactMetric(
            figure = "15+",
            label = "Years Experience",
            detail = "Indian & International Hospitality"
        ),
        ImpactMetric(
            figure = "30+",
            label = "Pre-Opening Projects",
            detail = "Hotels, Resorts & Heritage Properties"
        ),
        ImpactMetric(
            figure = "20+",
            label = "Café & Restaurant Setups",
            detail = "Fine Dining, Roasteries & Lounges"
        ),
        ImpactMetric(
            figure = "3×",
            label = "Champion Mixologist",
            detail = "Award-Winning Beverage Specialist"
        )
    )

    val divisions = listOf(
        BusinessDivision(
            id = "div_consultancy",
            code = "01",
            name = "MILA Consultancy",
            tagline = "Pre-Openings, Café Setups & Turnkey Advisory",
            description = "End-to-end advisory for hotel and resort pre-openings (30+ projects), café and restaurant launches (20+ setups), SOP architecture, and operational profitability.",
            highlights = listOf(
                "Turnkey hotel & resort pre-opening execution",
                "Café, bakery & restaurant concept setup",
                "P&L audits & operational SOP frameworks"
            ),
            targetRoute = "our_services"
        ),
        BusinessDivision(
            id = "div_talent",
            code = "02",
            name = "MILA Talent",
            tagline = "Executive Search & Hospitality Recruitment",
            description = "Dedicated hospitality recruitment connecting Indian hotels, luxury resorts, and dining groups with vetted General Managers, Chefs, Mixologists, and opening brigades.",
            highlights = listOf(
                "Executive leadership & HOD placements",
                "Pre-opening taskforce & brigade mobilization",
                "Dedicated candidate registration & career portal"
            ),
            targetRoute = "careers"
        ),
        BusinessDivision(
            id = "div_academy",
            code = "03",
            name = "MILA Academy & Expertise",
            tagline = "Specialist Mixology, Training & Service Mastery",
            description = "Led by an award-winning three-time champion mixologist—delivering signature beverage curation, bar engineering, service training academies, and culinary calibration.",
            highlights = listOf(
                "Three-time champion mixologist beverage labs",
                "Front-of-house, barista & bartender upskilling",
                "Pre-opening service choreography & standards"
            ),
            targetRoute = "our_expertise"
        )
    )

    val testimonials = listOf(
        Testimonial(
            id = "test_1",
            quote = "MILA brought calm, structured pre-opening discipline to our resort launch—aligning kitchen design, service SOPs, and department head hiring ahead of schedule.",
            attributionRole = "Managing Partner",
            engagementContext = "Luxury Heritage Resort Pre-Opening • Rajasthan"
        ),
        Testimonial(
            id = "test_2",
            quote = "From espresso bar ergonomics to food-cost calibration and opening staff training, their café setup methodology helped us achieve operational stability from week one.",
            attributionRole = "Founder",
            engagementContext = "Specialty Café & Bakery Concept • Bengaluru"
        ),
        Testimonial(
            id = "test_3",
            quote = "Their beverage program and bar-team training transformed both our guest experience and pour-cost discipline across our evening lounge.",
            attributionRole = "Director of Operations",
            engagementContext = "Fine-Dining & Cocktail Lounge • Mumbai"
        )
    )

    val leadershipPrinciples = listOf(
        "Owner-Centric Commercial Rigor" to "Every concept, staffing structure, and menu decision is aligned with sustainable revenue and margin discipline.",
        "Pre-Opening Discipline" to "Structured critical-path timelines, OS&E advisory, and live service rehearsals before welcoming the first guest.",
        "Specialist Mixology Pedigree" to "Guided by an award-winning three-time champion mixologist uniting beverage theatre with strict pour-cost controls.",
        "Dedicated Hospitality Recruitment" to "Competency-assessed executive search and pre-opening talent mobilization across India."
    )

    val serviceRegions = listOf(
        "Mumbai & Western India" to "Luxury urban hotels, fine dining, craft cocktail lounges, and café launches.",
        "New Delhi & NCR" to "Upper-upscale hospitality advisory, diplomatic banqueting, and executive search.",
        "Bengaluru & South India" to "Specialty coffee concepts, roasteries, business hotels, and multi-unit F&B.",
        "Rajasthan, Goa & Leisure Corridors" to "Heritage resort pre-openings, destination retreats, and seasonal brigades."
    )

    val services: List<ConsultancyService>
        get() = ServiceCatalogDefaults.services

    fun serviceBySlug(slug: String, catalog: List<ConsultancyService> = services): ConsultancyService =
        catalog.firstOrNull { it.slug == slug || it.id == slug }
            ?: catalog.firstOrNull()
            ?: services.first()

    fun serviceByType(
        type: ConsultationServiceType,
        catalog: List<ConsultancyService> = services
    ): ConsultancyService =
        catalog.firstOrNull { it.type == type }
            ?: catalog.firstOrNull()
            ?: services.first()

    val industries = listOf(
        IndustrySector(
            id = "ind_luxury_hotels",
            name = "Luxury Hotels & Heritage Resorts",
            regions = "Rajasthan • Mumbai • New Delhi NCR",
            overview = "Pre-opening governance, fine-dining choreography, and executive leadership search for flagship urban hotels and heritage properties.",
            advisoryFocus = listOf(
                "Turnkey pre-opening critical path & taskforce",
                "Luxury front-office, housekeeping & F&B SOPs",
                "General Manager & HOD executive placement"
            ),
            imageRes = R.drawable.img_palace_resort_1795637233184
        ),
        IndustrySector(
            id = "ind_coastal_resorts",
            name = "Boutique & Destination Wellness Retreats",
            regions = "Goa • Kerala • Hill & Coastal Destinations",
            overview = "Destination positioning, seasonal staffing frameworks, holistic dining concepts, and high-touch villa operations.",
            advisoryFocus = listOf(
                "Seasonal yield & guest journey calibration",
                "Botanical beverage & wellness dining curation",
                "Pre-opening Rooms & Operations recruitment"
            ),
            imageRes = R.drawable.img_hero_hotel_1795637232836
        ),
        IndustrySector(
            id = "ind_fine_dining",
            name = "Fine-Dining Restaurants & Cocktail Lounges",
            regions = "Mumbai • New Delhi NCR • Bengaluru • Hyderabad",
            overview = "High-margin restaurant and bar concepts featuring champion-level mixology, cellar curation, and disciplined kitchen workflows.",
            advisoryFocus = listOf(
                "Bar station ergonomics & signature cocktail programs",
                "Menu engineering & beverage cost controls",
                "Executive Chef & Head Mixologist placement"
            ),
            imageRes = R.drawable.img_mixology_bar_1795637233060
        ),
        IndustrySector(
            id = "ind_cafes_roasteries",
            name = "Specialty Cafés, Roasteries & Bakeries",
            regions = "Mumbai • Pune • Bengaluru • Metro High-Streets",
            overview = "20+ café and all-day dining launches engineered for high daily throughput, espresso consistency, and scalable multi-unit SOPs.",
            advisoryFocus = listOf(
                "Espresso bar layout & beverage workflow design",
                "Central kitchen & bakery par-level systems",
                "Multi-unit Café Operations leadership hiring"
            ),
            imageRes = R.drawable.img_fine_dining_1795637233362
        )
    )

    val expertisePillars = listOf(
        ExpertisePillar(
            id = "exp_preopening",
            code = "01",
            title = "Pre-Opening Architecture & Readiness",
            headlineMetric = "30+ Projects",
            summary = "Structured project stewardship bridging design review, procurement, licensing readiness, staffing, and guest-ready soft launches.",
            capabilities = listOf(
                "Critical path scheduling & pre-opening budget tracking",
                "OS&E, tableware, and kitchen equipment advisory",
                "Departmental training academies & mock-stay audits",
                "Operational readiness & service sequence rehearsals"
            )
        ),
        ExpertisePillar(
            id = "exp_mixology",
            code = "02",
            title = "Specialist Mixology & Beverage Labs",
            headlineMetric = "3× Champion Mixologist",
            summary = "Led by an award-winning three-time champion mixologist—uniting artisanal cocktail creation, bar ergonomics, and profitable beverage operations.",
            capabilities = listOf(
                "Signature cocktail R&D & standardized recipe bibles",
                "High-velocity bar station design & ice programs",
                "Zero-waste prep techniques & garnish architecture",
                "Hands-on bartender mastery & pour-cost training"
            )
        ),
        ExpertisePillar(
            id = "exp_cafe_fnb",
            code = "03",
            title = "Café & Culinary Concept Setup",
            headlineMetric = "20+ Setups",
            summary = "Commercial-first restaurant and café creation balancing guest appeal with tight food-cost and labor governance.",
            capabilities = listOf(
                "Concept positioning, seating mix & menu pricing",
                "Hot/cold kitchen & espresso bar workflow review",
                "Menu contribution matrix & waste tracking SOPs",
                "Pre-launch tasting panels & team calibration"
            )
        ),
        ExpertisePillar(
            id = "exp_academy_talent",
            code = "04",
            title = "MILA Academy & Talent Calibration",
            headlineMetric = "15+ Years",
            summary = "Practical hospitality training workshops and competency-based talent assessments elevating floor, bar, and leadership standards.",
            capabilities = listOf(
                "Service etiquette & luxury guest protocol workshops",
                "Practical culinary tastings & live bar assessments",
                "Pre-opening HOD & supervisor leadership coaching",
                "Retention-focused onboarding frameworks"
            )
        )
    )

    val caseStudies = listOf(
        CaseStudy(
            id = "case_udaipur_palace",
            propertyName = "Heritage Palace Resort Pre-Opening",
            location = "Udaipur, Rajasthan",
            category = "Pre-Opening Advisory",
            filterGroup = "Pre-Opening",
            scale = "Luxury Resort • Multi-Venue F&B",
            summary = "Directed pre-opening taskforce, courtyard dining choreography, SOP manuals, and senior department head recruitment ahead of peak season launch.",
            outcomes = listOf(
                "Completed 180-day pre-opening critical path on schedule",
                "Mobilized full executive and HOD leadership team",
                "Standardized luxury arrival & dining service protocols"
            ),
            imageRes = R.drawable.img_palace_resort_1795637233184
        ),
        CaseStudy(
            id = "case_mumbai_atelier",
            propertyName = "Craft Cocktail Lounge & Bar Program",
            location = "Mumbai, Maharashtra",
            category = "Specialist Mixology",
            filterGroup = "Mixology",
            scale = "Upscale Cocktail Bar • Dual-Well Station",
            summary = "Engineered a signature botanical cocktail menu, ergonomic speed-bar layout, and bartender training led by our three-time champion mixologist.",
            outcomes = listOf(
                "Streamlined high-volume evening service workflow",
                "Established recipe standardization & pour-cost controls",
                "Trained full bar team on prep lab & presentation"
            ),
            imageRes = R.drawable.img_mixology_bar_1795637233060
        ),
        CaseStudy(
            id = "case_bengaluru_cafe",
            propertyName = "Specialty Café & Artisanal Bistro Setup",
            location = "Bengaluru, Karnataka",
            category = "Café & Restaurant Setup",
            filterGroup = "Café & Restaurant",
            scale = "All-Day Café & Bakery Concept",
            summary = "Delivered commercial kitchen layout review, espresso bar workflow, menu engineering, and opening brigade training from concept through launch.",
            outcomes = listOf(
                "Aligned kitchen & barista stations for peak rush speed",
                "Built costed all-day food & beverage menu matrix",
                "Executed soft-launch stress tests prior to public opening"
            ),
            imageRes = R.drawable.img_fine_dining_1795637233362
        ),
        CaseStudy(
            id = "case_delhi_courtyard",
            propertyName = "Urban Luxury Hotel Leadership Search",
            location = "New Delhi, NCR",
            category = "MILA Talent Placement",
            filterGroup = "Executive Search",
            scale = "Full-Service Hotel & Banqueting",
            summary = "Partnered on confidential executive search for General Manager, Executive Chef, and F&B leadership alongside banquet service recalibration.",
            outcomes = listOf(
                "Placed senior operational leadership via competency vetting",
                "Recalibrated banqueting & VIP dining workflows",
                "Supported 90-day leadership onboarding"
            ),
            imageRes = R.drawable.img_hero_hotel_1795637232836
        ),
        CaseStudy(
            id = "case_goa_retreat",
            propertyName = "Coastal Boutique Villa Retreat Opening",
            location = "North Goa",
            category = "Resort Pre-Opening",
            filterGroup = "Pre-Opening",
            scale = "Boutique Villas • Coastal Bar & Dining",
            summary = "Guided villa pre-opening readiness, guest concierge standards, and an evening coastal beverage program tailored to leisure guests.",
            outcomes = listOf(
                "Delivered pre-opening OS&E & staffing roadmap",
                "Launched coastal botanical cocktail menu",
                "Trained front-of-house & butler teams"
            ),
            imageRes = R.drawable.img_palace_resort_1795637233184
        ),
        CaseStudy(
            id = "case_mumbai_bakery",
            propertyName = "High-Street Espresso & Bakehouse Launch",
            location = "South Mumbai",
            category = "Café & Restaurant Setup",
            filterGroup = "Café & Restaurant",
            scale = "Boutique Café & Patisserie",
            summary = "Structured back-of-house prep systems, display counter merchandising, and opening barista calibration for a heritage high-street venue.",
            outcomes = listOf(
                "Designed zero-bottleneck takeaway & dine-in flow",
                "Implemented daily par-level & waste tracking SOPs",
                "Recruited & trained opening café leadership"
            ),
            imageRes = R.drawable.img_fine_dining_1795637233362
        )
    )

    val mixologyHonors = listOf(
        "Award-winning three-time champion mixologist leadership",
        "Botanical infusion, clarification & zero-waste bar lab expertise",
        "20+ café, restaurant & bar beverage programs architected across India"
    )

    val mixologySignatures = listOf(
        MixologySignature(
            id = "sig_saffron_coupe",
            name = "Saffron & Smoked Vetiver Coupe",
            inspiration = "Aromatic Heritage Botanical Study",
            tastingNotes = "Warm spice infusion, delicate saffron cordial, subtle vetiver smoke, balanced dry finish.",
            glassAndGarnish = "Crystal Coupe • Aromatic Mist & Citrus Expression"
        ),
        MixologySignature(
            id = "sig_kokum_negroni",
            name = "Coastal Kokum & Black Peppercorn",
            inspiration = "Western Ghats Terroir Profile",
            tastingNotes = "Botanical spirit base, tangy fermented kokum aperitif, toasted peppercorn warmth.",
            glassAndGarnish = "Rocks Glass • Hand-Cut Clear Ice Block"
        ),
        MixologySignature(
            id = "sig_darjeeling_highball",
            name = "First Flush Tea & Citrus Highball",
            inspiration = "Clarified Botanical Highball",
            tastingNotes = "Clarified single-origin tea distillate, aromatic lime oleo, crisp effervescent lift.",
            glassAndGarnish = "Fluted Highball • Fresh Citrus Coin"
        )
    )

    val openRoles = listOf(
        HospitalityRole(
            id = "role_gm_udaipur",
            title = "Pre-Opening General Manager",
            propertyTier = "Luxury Heritage Resort (Pre-Opening)",
            location = "Udaipur, Rajasthan",
            cityRegion = "Udaipur & Rajasthan",
            compensation = "Competitive Executive Package",
            experienceRequired = "14+ Years (Luxury GM)",
            minExperienceYears = 14,
            category = RoleCategory.PRE_OPENING,
            summary = "Lead pre-opening critical path, owner-operator alignment, luxury positioning, and full department head mobilization.",
            keyMandates = listOf(
                "Own pre-opening readiness timeline and operational budgets",
                "Recruit and mentor Heads of Department",
                "Establish luxury service standards across all guest touchpoints"
            ),
            desiredSkills = listOf("Pre-Opening Governance", "Luxury Resort P&L", "Owner Relations", "HOD Leadership"),
            isUrgent = true
        ),
        HospitalityRole(
            id = "role_exec_chef_mumbai",
            title = "Executive Chef — Modern Indian",
            propertyTier = "5-Star Urban Hotel & Fine Dining",
            location = "Mumbai, Maharashtra",
            cityRegion = "Mumbai",
            compensation = "Senior Culinary Compensation",
            experienceRequired = "12+ Years",
            minExperienceYears = 12,
            category = RoleCategory.CULINARY,
            summary = "Spearhead culinary direction across a contemporary Indian fine-dining restaurant, banqueting, and all-day dining.",
            keyMandates = listOf(
                "Design seasonal regional-Indian tasting menus",
                "Maintain disciplined food-cost and kitchen hygiene controls",
                "Lead and mentor a multi-outlet culinary brigade"
            ),
            desiredSkills = listOf("Modern Indian Gastronomy", "Menu Engineering", "Food Cost Control", "HACCP"),
            isUrgent = true
        ),
        HospitalityRole(
            id = "role_head_mixologist_delhi",
            title = "Head Mixologist & Beverage Manager",
            propertyTier = "Upscale Cocktail Lounge & Speakeasy",
            location = "New Delhi, NCR",
            cityRegion = "New Delhi NCR",
            compensation = "Competitive + Performance Incentives",
            experienceRequired = "7+ Years",
            minExperienceYears = 7,
            category = RoleCategory.MIXOLOGY,
            summary = "Lead progressive cocktail program development, prep-lab execution, and a high-energy bar & floor service team.",
            keyMandates = listOf(
                "Launch seasonal craft cocktail collections",
                "Maintain disciplined pour-cost and inventory accuracy",
                "Conduct ongoing bartender technique & hospitality training"
            ),
            desiredSkills = listOf("Craft Mixology", "Beverage R&D", "Pour-Cost Governance", "Bar Team Training"),
            isUrgent = true
        ),
        HospitalityRole(
            id = "role_fnb_director_blr",
            title = "Director of Food & Beverage",
            propertyTier = "Upper-Upscale Full-Service Hotel",
            location = "Bengaluru, Karnataka",
            cityRegion = "Bengaluru",
            compensation = "Executive Leadership Package",
            experienceRequired = "10+ Years",
            minExperienceYears = 10,
            category = RoleCategory.FNB_SERVICE,
            summary = "Full P&L ownership across multiple dining outlets, banqueting operations, and specialty coffee lounge.",
            keyMandates = listOf(
                "Drive outlet revenue growth and banquet yield",
                "Optimize departmental labor productivity and margins",
                "Champion guest satisfaction and service consistency"
            ),
            desiredSkills = listOf("Multi-Outlet F&B P&L", "Banquet Revenue", "Service Choreography")
        ),
        HospitalityRole(
            id = "role_cafe_ops_director_mumbai",
            title = "Operations Director — Café & Bakery Group",
            propertyTier = "Multi-Outlet Specialty Café Concept",
            location = "Mumbai & Pune",
            cityRegion = "Mumbai",
            compensation = "Senior Operations Package",
            experienceRequired = "9+ Years",
            minExperienceYears = 9,
            category = RoleCategory.EXECUTIVE,
            summary = "Scale multi-unit café operations, central commissary SOPs, and upcoming new store launches.",
            keyMandates = listOf(
                "Standardize coffee calibration and bakery par-level systems",
                "Lead pre-opening readiness for upcoming outlets",
                "Manage unit-level profitability and team retention"
            ),
            desiredSkills = listOf("Multi-Unit Café Scaling", "Commissary SOPs", "Unit Economics")
        ),
        HospitalityRole(
            id = "role_rooms_manager_goa",
            title = "Pre-Opening Rooms Division Manager",
            propertyTier = "Coastal Boutique Resort (Pre-Opening)",
            location = "North Goa",
            cityRegion = "Goa",
            compensation = "Competitive Resort Package",
            experienceRequired = "8+ Years",
            minExperienceYears = 8,
            category = RoleCategory.ROOMS,
            summary = "Establish villa arrival protocols, guest concierge journeys, and seamless front office & housekeeping workflows.",
            keyMandates = listOf(
                "Train front office, concierge, and housekeeping associates",
                "Implement PMS and luxury arrival service standards",
                "Coordinate pre-opening OS&E linen and guest amenity staging"
            ),
            desiredSkills = listOf("Rooms Division Ops", "Guest Concierge", "PMS Setup", "OS&E Staging")
        ),
        HospitalityRole(
            id = "role_sommelier_jaipur",
            title = "Head Sommelier & Beverage Curator",
            propertyTier = "Luxury Heritage Hotel & Dining",
            location = "Jaipur, Rajasthan",
            cityRegion = "Udaipur & Rajasthan",
            compensation = "Specialist Leadership Package",
            experienceRequired = "5+ Years",
            minExperienceYears = 5,
            category = RoleCategory.MIXOLOGY,
            summary = "Curate fine wine and spirit selections, lead private cellar tastings, and elevate beverage pairing service.",
            keyMandates = listOf(
                "Manage cellar inventory and seasonal wine list updates",
                "Train dining room teams on pairing and service rituals",
                "Host curated tasting dinners and private dining events"
            ),
            desiredSkills = listOf("Wine & Spirit Curation", "Cellar Management", "Dining Pairing Service")
        ),
        HospitalityRole(
            id = "role_pastry_chef_hyd",
            title = "Executive Pastry Chef",
            propertyTier = "Luxury Urban Hotel & Patisserie",
            location = "Hyderabad, Telangana",
            cityRegion = "Hyderabad",
            compensation = "Senior Pastry Package",
            experienceRequired = "8+ Years",
            minExperienceYears = 8,
            category = RoleCategory.CULINARY,
            summary = "Lead artisanal viennoiserie, plated fine-dining desserts, banquet pastry production, and patisserie retail.",
            keyMandates = listOf(
                "Develop signature afternoon tea and seasonal pastry lines",
                "Maintain strict cold-chain hygiene and ingredient sourcing",
                "Train and mentor the dedicated pastry & bakery brigade"
            ),
            desiredSkills = listOf("Artisanal Viennoiserie", "Banquet Pastry", "Cold-Chain HACCP")
        ),
        HospitalityRole(
            id = "role_bar_lead_goa",
            title = "Senior Bar Supervisor — Coastal Lounge",
            propertyTier = "Destination Beachside Dining & Bar",
            location = "Anjuna, Goa",
            cityRegion = "Goa",
            compensation = "Competitive + Service Charge",
            experienceRequired = "4+ Years",
            minExperienceYears = 4,
            category = RoleCategory.MIXOLOGY,
            summary = "Lead high-volume botanical cocktail service, bar prep inventory, and shift floor discipline.",
            keyMandates = listOf(
                "Execute botanical prep batches and speed-bar service",
                "Supervise daily bar inventory and glassware par levels",
                "Coach junior bartenders on service cadence"
            ),
            desiredSkills = listOf("High-Volume Bar Service", "Botanical Prep", "Bar Inventory Control")
        )
    )
}
