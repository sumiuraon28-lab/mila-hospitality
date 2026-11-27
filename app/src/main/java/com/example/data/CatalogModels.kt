package com.example.data

import androidx.annotation.DrawableRes

data class ImpactMetric(
    val figure: String,
    val label: String,
    val detail: String
)

data class BusinessDivision(
    val id: String,
    val code: String,
    val name: String,
    val tagline: String,
    val description: String,
    val highlights: List<String>,
    val targetRoute: String
)

data class Testimonial(
    val id: String,
    val quote: String,
    val attributionRole: String,
    val engagementContext: String,
    val rating: Int = 5,
    val adminReply: String = "",
    val adminRepliedAt: String? = null,
    val source: String = "ADMIN"
)

data class ConsultancyService(
    val id: String,
    val sortOrder: Int,
    val type: ConsultationServiceType,
    val slug: String,
    val divisionName: String,
    val title: String,
    val subtitle: String,
    val summary: String,
    val detailedDescription: String,
    val servicesIncluded: List<String>,
    val ownerBenefits: List<String>,
    val deliverables: List<String>,
    val typicalTimeline: String,
    val imageKey: String,
    @DrawableRes val imageRes: Int,
    val imageUrl: String? = null
) {
    val numberBadge: String
        get() = sortOrder.toString().padStart(2, '0')
}

data class CaseStudy(
    val id: String,
    val propertyName: String,
    val location: String,
    val category: String,
    val filterGroup: String,
    val scale: String,
    val summary: String,
    val outcomes: List<String>,
    @DrawableRes val imageRes: Int
)

data class IndustrySector(
    val id: String,
    val name: String,
    val regions: String,
    val overview: String,
    val advisoryFocus: List<String>,
    @DrawableRes val imageRes: Int
)

data class ExpertisePillar(
    val id: String,
    val code: String,
    val title: String,
    val headlineMetric: String,
    val summary: String,
    val capabilities: List<String>
)

data class MixologySignature(
    val id: String,
    val name: String,
    val inspiration: String,
    val tastingNotes: String,
    val glassAndGarnish: String
)

enum class RoleCategory(val displayLabel: String) {
    ALL("All Departments"),
    EXECUTIVE("Executive & GM"),
    CULINARY("Culinary & Pastry"),
    MIXOLOGY("Bar & Mixology"),
    PRE_OPENING("Pre-Opening"),
    FNB_SERVICE("F&B Service"),
    ROOMS("Front Office & Rooms")
}

enum class ExperienceFilter(val label: String) {
    ALL("All Experience"),
    MID("0–5 Years"),
    SENIOR("6–10 Years"),
    EXECUTIVE("10+ Years");

    fun matches(minYears: Int): Boolean = when (this) {
        ALL -> true
        MID -> minYears in 0..5
        SENIOR -> minYears in 6..10
        EXECUTIVE -> minYears >= 10
    }
}

data class HospitalityRole(
    val id: String,
    val title: String,
    val propertyTier: String,
    val location: String,
    val cityRegion: String,
    val compensation: String,
    val experienceRequired: String,
    val minExperienceYears: Int,
    val category: RoleCategory,
    val summary: String,
    val keyMandates: List<String>,
    val desiredSkills: List<String> = emptyList(),
    val isUrgent: Boolean = false
)

