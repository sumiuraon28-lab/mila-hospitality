package com.example.data

import com.example.R

object ServiceCatalogDefaults {
    val services: List<ConsultancyService> = listOf(
        ConsultancyService(
            id = "hospitality-pre-opening-consultancy",
            sortOrder = 1,
            type = ConsultationServiceType.HOSPITALITY_PRE_OPENING,
            slug = ConsultationServiceType.HOSPITALITY_PRE_OPENING.slug,
            divisionName = "MILA Consultancy",
            title = "Hospitality Pre-Opening Consultancy",
            subtitle = "Turnkey Critical Path Governance for Hotels & Resorts",
            summary = "End-to-end pre-opening stewardship backed by 30+ hospitality pre-opening launches across urban hotels, heritage properties, and destination resorts.",
            detailedDescription = "Launching a hotel or resort demands disciplined alignment across architecture, engineering, procurement, statutory readiness, and operational training. Drawing on 15+ years of hospitality industry leadership and 30+ pre-opening projects across India, MILA Consultancy partners with owners and investors from site transition through soft opening to deliver seamless operational readiness without costly delays.",
            servicesIncluded = listOf(
                "Master pre-opening critical path planning and weekly readiness audits",
                "Front-of-house, back-of-house, kitchen, and service area flow review",
                "OS&E and FF&E specification, vendor evaluation, and procurement advisory",
                "Pre-opening budget modeling, department manning guides, and SOP architecture",
                "Mock guest stays, dry runs, and opening-night service choreography"
            ),
            ownerBenefits = listOf(
                "Prevents costly post-handover design rework and launch delays",
                "Protects capital expenditure through disciplined OS&E specification",
                "Establishes cohesive luxury service standards from day one",
                "Gives owners transparent weekly visibility across every pre-opening workstream"
            ),
            deliverables = listOf(
                "180-Day Pre-Opening Critical Path & Milestone Tracker",
                "Departmental Operating Manuals (Rooms, F&B, Kitchen, Engineering)",
                "OS&E Procurement Matrix & Pre-Opening Capital Budget",
                "Soft-Opening Readiness & Mock-Stay Audit Report"
            ),
            typicalTimeline = "4 – 12 Months",
            imageKey = "palace_resort",
            imageRes = R.drawable.img_palace_resort_1795637233184
        ),
        ConsultancyService(
            id = "restaurant-cafe-development",
            sortOrder = 2,
            type = ConsultationServiceType.RESTAURANT_CAFE_DEVELOPMENT,
            slug = ConsultationServiceType.RESTAURANT_CAFE_DEVELOPMENT.slug,
            divisionName = "MILA Consultancy",
            title = "Restaurant and Café Development",
            subtitle = "Commercial Concept-to-Launch Dining & Roastery Setups",
            summary = "Turnkey development for independent restaurants, specialty cafés, bakeries, and multi-outlet dining ventures—grounded in 20+ successful setups.",
            detailedDescription = "Whether launching a flagship fine-dining room, an artisanal neighborhood café, or a multi-unit hospitality format, MILA transforms creative ambition into profitable unit economics. Having delivered 20+ café and restaurant setups across India, our team bridges culinary direction, commercial kitchen ergonomics, menu engineering, and opening brigade readiness.",
            servicesIncluded = listOf(
                "Dining format positioning, seating capacity optimization, and service flow",
                "Commercial hot/cold kitchen and specialty espresso bar ergonomic planning",
                "Culinary direction, recipe standardization, and menu engineering matrix",
                "Vendor sourcing, food-cost modeling, and portion/waste governance",
                "Opening team calibration, tasting trials, and soft-launch stress testing"
            ),
            ownerBenefits = listOf(
                "Balances culinary craft with disciplined food-cost and margin targets",
                "Eliminates kitchen and barista station bottlenecks during peak hours",
                "Reduces wastage through standardized prep and par-level systems",
                "Accelerates path to stable unit-level profitability post launch"
            ),
            deliverables = listOf(
                "Kitchen & Espresso Bar Workflow Schematic",
                "Standardized Recipe Bible & Costed Menu Engineering Matrix",
                "Vendor & Equipment Specification Dossier",
                "Launch Playbook & Soft-Opening Calibration Sign-Off"
            ),
            typicalTimeline = "8 – 16 Weeks",
            imageKey = "fine_dining",
            imageRes = R.drawable.img_fine_dining_1795637233362
        ),
        ConsultancyService(
            id = "bar-beverage-consultancy",
            sortOrder = 3,
            type = ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY,
            slug = ConsultationServiceType.BAR_BEVERAGE_CONSULTANCY.slug,
            divisionName = "MILA Academy & Expertise",
            title = "Bar and Beverage Consultancy",
            subtitle = "Led by an Award-Winning Three-Time Champion Mixologist",
            summary = "Signature cocktail creation, ergonomic speed-bar architecture, cellar curation, and beverage margin governance for bars, lounges, and hotels.",
            detailedDescription = "A memorable beverage program drives both guest loyalty and high-margin F&B revenue. Led directly by our award-winning three-time champion mixologist, MILA crafts beverage programs that pair sensory storytelling and Indian botanical techniques with ergonomic speed-rail design, zero-waste prep labs, and tight pour-cost discipline.",
            servicesIncluded = listOf(
                "Signature cocktail, zero-proof, and seasonal beverage menu curation",
                "Ergonomic bar station build, under-counter layout, and clear-ice program",
                "Zero-waste botanical prep lab design and batching workflows",
                "Wine, spirit, and craft coffee/tea portfolio curation",
                "Pour-cost controls, glassware selection, and bartender technical mastery"
            ),
            ownerBenefits = listOf(
                "Elevates venue prestige through championship-grade beverage design",
                "Increases bartender speed of service during high-volume evening trade",
                "Protects beverage margins with standardized yields and pour controls",
                "Builds repeatable drink consistency across every shift"
            ),
            deliverables = listOf(
                "Bar Ergonomics & Equipment Layout Blueprint",
                "Costed Signature Cocktail & Zero-Proof Recipe Manual",
                "Spirit & Wine Portfolio Architecture",
                "Prep-Lab SOPs & Bar Team Certification Sessions"
            ),
            typicalTimeline = "4 – 10 Weeks",
            imageKey = "mixology_bar",
            imageRes = R.drawable.img_mixology_bar_1795637233060
        ),
        ConsultancyService(
            id = "hospitality-training",
            sortOrder = 4,
            type = ConsultationServiceType.HOSPITALITY_TRAINING,
            slug = ConsultationServiceType.HOSPITALITY_TRAINING.slug,
            divisionName = "MILA Academy & Expertise",
            title = "Hospitality Training",
            subtitle = "Service Choreography, Barista & Beverage Academies",
            summary = "Structured classroom and floor-coaching academies elevating front-of-house etiquette, culinary standards, bar mastery, and supervisory leadership.",
            detailedDescription = "Consistent guest delight depends on confident, well-coached teams. MILA Academy & Expertise conducts hands-on, on-property and pre-opening training workshops tailored to Indian luxury hospitality—covering guest arrival rituals, fine-dining sequence of service, specialty coffee and mixology precision, upselling etiquette, and supervisor coaching.",
            servicesIncluded = listOf(
                "Luxury front-office, concierge, and guest recovery etiquette workshops",
                "Fine-dining table service, wine presentation, and banquet choreography",
                "Specialty barista calibration and practical bartender technique labs",
                "Department Head and floor supervisor leadership coaching",
                "Mystery audit preparation and recurring service refresher modules"
            ),
            ownerBenefits = listOf(
                "Delivers uniform guest experiences across shifts and outlets",
                "Improves average check values through natural, informed service",
                "Reduces early-stage staff attrition with structured skill progression",
                "Equips supervisors to coach and maintain standards independently"
            ),
            deliverables = listOf(
                "Role-Specific Service & Etiquette Training Curriculum",
                "On-Property Practical Workshop & Assessment Schedule",
                "Individual Competency Scorecards & Coaching Plans",
                "Supervisor Quality Observation Checklists"
            ),
            typicalTimeline = "2 – 8 Weeks",
            imageKey = "hospitality_training",
            imageRes = R.drawable.img_hospitality_training_1795638556708
        ),
        ConsultancyService(
            id = "operations-management",
            sortOrder = 5,
            type = ConsultationServiceType.OPERATIONS_MANAGEMENT,
            slug = ConsultationServiceType.OPERATIONS_MANAGEMENT.slug,
            divisionName = "MILA Consultancy",
            title = "Operations Management",
            subtitle = "P&L Turnaround, Margin Governance & Daily Discipline",
            summary = "Hands-on operational diagnostics, cost control, shift productivity, and service recalibration for operating hotels, resorts, and dining groups.",
            detailedDescription = "Even well-positioned properties can experience margin leakage, inconsistent service delivery, or rising labor costs over time. MILA conducts forensic operational audits and provides structured operations management support—realigning procurement, shift rosters, menu profitability, and department workflows into a measurable 90-day turnaround plan.",
            servicesIncluded = listOf(
                "Comprehensive P&L diagnostics across food, beverage, and labor ratios",
                "Mystery guest evaluations and end-to-end service sequence audits",
                "Inventory control, par-level, and supplier negotiation restructuring",
                "Departmental shift roster optimization and productivity benchmarks",
                "Weekly KPI review governance with property leadership"
            ),
            ownerBenefits = listOf(
                "Identifies and plugs hidden food, beverage, and payroll leakage",
                "Restores guest satisfaction scores without compromising brand feel",
                "Provides owners with clear weekly financial and operational KPIs",
                "Builds lasting internal accountability across department heads"
            ),
            deliverables = listOf(
                "360° Operational & P&L Diagnostic Report",
                "90-Day Margin Recovery & Turnaround Roadmap",
                "Revised Shift Rostering & Productivity Templates",
                "Monthly Owner Performance Governance Pack"
            ),
            typicalTimeline = "3 – 12 Weeks",
            imageKey = "hero_hotel",
            imageRes = R.drawable.img_hero_hotel_1795637232836
        ),
        ConsultancyService(
            id = "recruitment-talent-acquisition",
            sortOrder = 6,
            type = ConsultationServiceType.RECRUITMENT_TALENT_ACQUISITION,
            slug = ConsultationServiceType.RECRUITMENT_TALENT_ACQUISITION.slug,
            divisionName = "MILA Talent",
            title = "Recruitment and Talent Acquisition",
            subtitle = "Executive Search & Pre-Opening Brigade Mobilization",
            summary = "Discreet, competency-vetted hospitality recruitment for General Managers, Executive Chefs, Head Mixologists, and complete opening teams across India.",
            detailedDescription = "Delivered through MILA Talent, our recruitment practice combines deep Indian hospitality networks with practical, operator-led vetting. Rather than forwarding unscreened resumes, we evaluate candidates through structured leadership interviews, culinary tastings, and live bar assessments—ensuring cultural, commercial, and technical fit for every property.",
            servicesIncluded = listOf(
                "Confidential executive search for General Managers and F&B Directors",
                "Practical vetting for Executive Chefs, Pastry Chefs, and Head Mixologists",
                "Full pre-opening department head and core brigade mobilization",
                "Compensation benchmarking across Indian metro and leisure markets",
                "Offer-to-onboarding transition support and 90-day check-ins"
            ),
            ownerBenefits = listOf(
                "Saves leadership hours with thoroughly vetted, hospitality-tested shortlists",
                "Lowers hiring risk through live culinary and beverage practical trials",
                "Attracts passive senior leaders via confidential market outreach",
                "Aligns compensation packages with current Indian market benchmarks"
            ),
            deliverables = listOf(
                "Role Mandate Brief & Compensation Benchmark Guide",
                "Competency-Assessed Candidate Shortlist & Dossiers",
                "Practical Trial & Tasting Evaluation Reports",
                "Onboarding & 90-Day Integration Checklist"
            ),
            typicalTimeline = "3 – 8 Weeks",
            imageKey = "executive_recruitment",
            imageRes = R.drawable.img_executive_recruitment_1795638556883
        ),
        ConsultancyService(
            id = "business-concept-development",
            sortOrder = 7,
            type = ConsultationServiceType.BUSINESS_CONCEPT_DEVELOPMENT,
            slug = ConsultationServiceType.BUSINESS_CONCEPT_DEVELOPMENT.slug,
            divisionName = "MILA Consultancy",
            title = "Business and Concept Development",
            subtitle = "Feasibility Studies, Concept Creation & Brand Positioning",
            summary = "Market-backed feasibility modeling, culinary and guest concept creation, and commercial expansion blueprints for new hospitality ventures.",
            detailedDescription = "Before committing capital to fit-outs or leases, hospitality investors need rigorous commercial clarity. MILA partners with promoters, real estate developers, and family offices to evaluate catchment demand, define distinctive dining or hotel concepts, build multi-year financial projections, and structure compelling brand narratives ready for execution.",
            servicesIncluded = listOf(
                "Catchment feasibility, competitive landscape, and pricing power study",
                "Original hospitality, restaurant, café, or lifestyle concept creation",
                "Space allocation, revenue-per-square-foot, and daypart optimization",
                "CAPEX, OPEX, breakeven, and 3-to-5 year financial feasibility modeling",
                "Franchise or multi-city expansion readiness frameworks"
            ),
            ownerBenefits = listOf(
                "Validates investment returns and payback horizons prior to capital deployment",
                "Differentiates the venue in crowded urban and resort hospitality markets",
                "Aligns interior designers, chefs, and operators around one unified brief",
                "Equips promoters with investor-grade feasibility documentation"
            ),
            deliverables = listOf(
                "Market Feasibility & Competitive Positioning Dossier",
                "Complete Concept Narrative & Guest Experience Blueprint",
                "CAPEX / OPEX & 5-Year Financial Projection Model",
                "Design & Architect Functional Space Brief"
            ),
            typicalTimeline = "4 – 10 Weeks",
            imageKey = "concept_design",
            imageRes = R.drawable.img_concept_design_1795638557169
        ),
        ConsultancyService(
            id = "hospitality-management-solutions",
            sortOrder = 8,
            type = ConsultationServiceType.HOSPITALITY_MANAGEMENT_SOLUTIONS,
            slug = ConsultationServiceType.HOSPITALITY_MANAGEMENT_SOLUTIONS.slug,
            divisionName = "MILA Consultancy",
            title = "Hospitality Management Solutions",
            subtitle = "Interim Leadership, Asset Stewardship & Retainer Advisory",
            summary = "Ongoing management governance, interim executive stewardship, asset management, and quality assurance for independent hotels and hospitality portfolios.",
            detailedDescription = "Independent hotel owners, boutique resorts, and multi-outlet F&B groups often require senior operational oversight without surrendering brand autonomy. MILA provides flexible Hospitality Management Solutions—combining interim executive leadership, asset representation, vendor and SOP governance, and ongoing quarterly performance stewardship.",
            servicesIncluded = listOf(
                "Interim General Manager or Operations Director deployment during transitions",
                "Ongoing owner-representative asset management and operator reviews",
                "Multi-outlet standard operating procedure harmonization and compliance",
                "Quarterly quality audits, guest sentiment analysis, and revenue strategy",
                "Annual budgeting, capex planning, and seasonal business readiness"
            ),
            ownerBenefits = listOf(
                "Gives independent owners institutional hospitality expertise on retainer",
                "Maintains operational continuity during leadership transitions",
                "Drives sustained RevPAR, cover growth, and asset value preservation",
                "Protects owner interests with objective, data-backed performance reviews"
            ),
            deliverables = listOf(
                "Management Governance & KPI Scorecard Framework",
                "Annual Operating Budget & Revenue Action Plan",
                "Quarterly On-Site Property Audit & Asset Report",
                "Executive Leadership Transition & Handover Plan"
            ),
            typicalTimeline = "Quarterly / Annual Retainer",
            imageKey = "management_solutions",
            imageRes = R.drawable.img_management_solutions_1795638555350
        )
    )
}
