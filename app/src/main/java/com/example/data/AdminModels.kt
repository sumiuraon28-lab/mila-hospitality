package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

const val OWNER_BOOTSTRAP_EMAIL = "sumiuraon28@gmail.com"

enum class AdminRole(
    val displayLabel: String,
    val scopeDescription: String,
    val isReadOnly: Boolean = false
) {
    SUPER_ADMIN(
        displayLabel = "Super Admin",
        scopeDescription = "Complete access across clients, recruitment, CMS content, reports, admins, and platform settings."
    ),
    RECRUITMENT_ADMIN(
        displayLabel = "Recruitment Admin",
        scopeDescription = "Full access to job vacancies, candidates, private CV vault, applications, recruitment status, and notifications."
    ),
    CONTENT_ADMIN(
        displayLabel = "Content Admin",
        scopeDescription = "Full access to website pages, services, categories, portfolio items, testimonials, and leadership team."
    ),
    CONSULTANT(
        displayLabel = "Consultant",
        scopeDescription = "Access to assigned client enquiries, advisory notes, and status progression."
    ),
    REPORTING_ADMIN(
        displayLabel = "Reporting Admin",
        scopeDescription = "Strict read-only executive oversight across real-time reports, KPIs, and activity audit logs.",
        isReadOnly = true
    );

    fun allowedModules(): Set<AdminModule> = when (this) {
        SUPER_ADMIN -> AdminModule.entries.toSet()
        RECRUITMENT_ADMIN -> setOf(
            AdminModule.DASHBOARD_OVERVIEW,
            AdminModule.JOB_VACANCY_MANAGEMENT,
            AdminModule.CANDIDATE_MANAGEMENT,
            AdminModule.CV_DATABASE,
            AdminModule.JOB_APPLICATIONS,
            AdminModule.RECRUITMENT_STATUS_MANAGEMENT,
            AdminModule.NOTIFICATION_MANAGEMENT,
            AdminModule.REPORTS_AND_ANALYTICS,
            AdminModule.ACTIVITY_LOGS
        )
        CONTENT_ADMIN -> setOf(
            AdminModule.DASHBOARD_OVERVIEW,
            AdminModule.SERVICE_MANAGEMENT,
            AdminModule.SERVICE_CATEGORIES,
            AdminModule.WEBSITE_CONTENT_MANAGEMENT,
            AdminModule.PORTFOLIO_MANAGEMENT,
            AdminModule.TESTIMONIALS,
            AdminModule.TEAM_MANAGEMENT,
            AdminModule.REPORTS_AND_ANALYTICS,
            AdminModule.ACTIVITY_LOGS
        )
        CONSULTANT -> setOf(
            AdminModule.DASHBOARD_OVERVIEW,
            AdminModule.CLIENT_ENQUIRIES,
            AdminModule.ACTIVITY_LOGS
        )
        REPORTING_ADMIN -> setOf(
            AdminModule.DASHBOARD_OVERVIEW,
            AdminModule.REPORTS_AND_ANALYTICS,
            AdminModule.ACTIVITY_LOGS
        )
    }

    fun canMutate(module: AdminModule): Boolean {
        if (isReadOnly) return false
        return module in allowedModules() && module != AdminModule.DASHBOARD_OVERVIEW &&
            module != AdminModule.REPORTS_AND_ANALYTICS && module != AdminModule.ACTIVITY_LOGS
    }

    companion object {
        fun fromRaw(raw: String): AdminRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: REPORTING_ADMIN
    }
}

enum class AdminModule(
    val number: Int,
    val title: String,
    val group: String
) {
    DASHBOARD_OVERVIEW(1, "Dashboard Overview", "Executive Command"),
    CLIENT_MANAGEMENT(2, "Client Management", "Client & Advisory CRM"),
    CLIENT_ENQUIRIES(3, "Client Enquiries", "Client & Advisory CRM"),
    SERVICE_MANAGEMENT(4, "Service Management", "Consultancy CMS"),
    SERVICE_CATEGORIES(5, "Service Categories", "Consultancy CMS"),
    JOB_VACANCY_MANAGEMENT(6, "Job Vacancy Management", "MILA Talent & CVs"),
    CANDIDATE_MANAGEMENT(7, "Candidate Management", "MILA Talent & CVs"),
    CV_DATABASE(8, "CV Database", "MILA Talent & CVs"),
    JOB_APPLICATIONS(9, "Job Applications", "MILA Talent & CVs"),
    RECRUITMENT_STATUS_MANAGEMENT(10, "Recruitment Status", "MILA Talent & CVs"),
    NOTIFICATION_MANAGEMENT(11, "Notifications", "MILA Talent & CVs"),
    WEBSITE_CONTENT_MANAGEMENT(12, "Website Content", "Editorial & Brand CMS"),
    PORTFOLIO_MANAGEMENT(13, "Portfolio Management", "Editorial & Brand CMS"),
    TESTIMONIALS(14, "Testimonials", "Editorial & Brand CMS"),
    TEAM_MANAGEMENT(15, "Team Management", "Editorial & Brand CMS"),
    REPORTS_AND_ANALYTICS(16, "Reports & Analytics", "Governance & Audit"),
    ADMIN_USER_MANAGEMENT(17, "Admin Users", "Governance & Audit"),
    SETTINGS(18, "Settings", "Governance & Audit"),
    ACTIVITY_LOGS(19, "Activity Logs", "Governance & Audit")
}

sealed interface AdminSessionState {
    data object Loading : AdminSessionState
    data object NotAdmin : AdminSessionState
    data class Authorized(val adminUser: AdminUser) : AdminSessionState
}

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.REPORTING_ADMIN.name,
    val active: Boolean = true,
    val assignedDepartment: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val resolvedRole: AdminRole
        get() = AdminRole.fromRaw(role)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "uid" to uid.trim(),
            "email" to email.trim().lowercase(),
            "displayName" to displayName.trim().ifBlank { "MILA Admin" },
            "role" to resolvedRole.name,
            "active" to active,
            "assignedDepartment" to assignedDepartment?.trim()?.takeIf { it.isNotEmpty() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(existingCreatedAt: Timestamp?): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "uid" to uid.trim(),
            "email" to email.trim().lowercase(),
            "displayName" to displayName.trim().ifBlank { "MILA Admin" },
            "role" to resolvedRole.name,
            "active" to active,
            "assignedDepartment" to assignedDepartment?.trim()?.takeIf { it.isNotEmpty() },
            "createdAt" to (existingCreatedAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class AdminInvite(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.CONSULTANT.name,
    val active: Boolean = true,
    val createdByUid: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val resolvedRole: AdminRole
        get() = AdminRole.fromRaw(role)

    fun toCreateMap(creatorUid: String): Map<String, Any> = mapOf(
        "email" to normalizeEmailKey(email),
        "displayName" to displayName.trim().ifBlank { "MILA Team Member" },
        "role" to resolvedRole.name,
        "active" to active,
        "createdByUid" to creatorUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun normalizeEmailKey(email: String): String = email.trim().lowercase()
    }
}

@Keep
@IgnoreExtraProperties
data class ServiceCategoryRecord(
    @DocumentId val id: String = "",
    val code: String = "",
    val sortOrder: Long = 1L,
    val title: String = "",
    val divisionName: String = "MILA Consultancy",
    val summary: String = "",
    val published: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toAdminMap(): Map<String, Any> = mapOf(
        "code" to code.trim(),
        "sortOrder" to sortOrder,
        "title" to title.trim(),
        "divisionName" to divisionName.trim(),
        "summary" to summary.trim(),
        "published" to published,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class JobVacancyRecord(
    @DocumentId val id: String = "",
    val title: String = "",
    val propertyTier: String = "",
    val location: String = "",
    val cityRegion: String = "",
    val compensation: String = "",
    val experienceRequired: String = "",
    val minExperienceYears: Long = 0L,
    val category: String = RoleCategory.PRE_OPENING.name,
    val summary: String = "",
    val keyMandates: List<String> = emptyList(),
    val desiredSkills: List<String> = emptyList(),
    val urgent: Boolean = false,
    val published: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    val resolvedCategory: RoleCategory
        get() = RoleCategory.entries.firstOrNull { it.name == category } ?: RoleCategory.PRE_OPENING

    fun toHospitalityRole(): HospitalityRole = HospitalityRole(
        id = id,
        title = title,
        propertyTier = propertyTier,
        location = location,
        cityRegion = cityRegion,
        compensation = compensation,
        experienceRequired = experienceRequired,
        minExperienceYears = minExperienceYears.toInt(),
        category = resolvedCategory,
        summary = summary,
        keyMandates = keyMandates,
        desiredSkills = desiredSkills,
        isUrgent = urgent
    )

    fun toAdminMap(): Map<String, Any> = mapOf(
        "title" to title.trim(),
        "propertyTier" to propertyTier.trim(),
        "location" to location.trim(),
        "cityRegion" to cityRegion.trim(),
        "compensation" to compensation.trim(),
        "experienceRequired" to experienceRequired.trim(),
        "minExperienceYears" to minExperienceYears,
        "category" to resolvedCategory.name,
        "summary" to summary.trim(),
        "keyMandates" to keyMandates.filter { it.isNotBlank() },
        "desiredSkills" to desiredSkills.filter { it.isNotBlank() },
        "urgent" to urgent,
        "published" to published,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromHospitalityRole(role: HospitalityRole, published: Boolean = true): JobVacancyRecord = JobVacancyRecord(
            id = role.id,
            title = role.title,
            propertyTier = role.propertyTier,
            location = role.location,
            cityRegion = role.cityRegion,
            compensation = role.compensation,
            experienceRequired = role.experienceRequired,
            minExperienceYears = role.minExperienceYears.toLong(),
            category = role.category.name,
            summary = role.summary,
            keyMandates = role.keyMandates,
            desiredSkills = role.desiredSkills,
            urgent = role.isUrgent,
            published = published
        )
    }
}

@Keep
@IgnoreExtraProperties
data class SitePageContent(
    @DocumentId val id: String = "",
    val pageKey: String = "",
    val pageTitle: String = "",
    val eyebrow: String = "",
    val headline: String = "",
    val subheadline: String = "",
    val bodyCopy: String = "",
    val heroImageKey: String = "hero_hotel",
    val heroImageUrl: String? = null,
    val ctaLabel: String = "Explore Services",
    val published: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toAdminMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "pageKey" to pageKey.trim(),
            "pageTitle" to pageTitle.trim(),
            "eyebrow" to eyebrow.trim(),
            "headline" to headline.trim(),
            "subheadline" to subheadline.trim(),
            "bodyCopy" to bodyCopy.trim(),
            "heroImageKey" to heroImageKey.trim(),
            "heroImageUrl" to heroImageUrl?.trim()?.takeIf { it.isNotEmpty() },
            "ctaLabel" to ctaLabel.trim(),
            "published" to published,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class PortfolioItemRecord(
    @DocumentId val id: String = "",
    val propertyName: String = "",
    val location: String = "",
    val category: String = "",
    val filterGroup: String = "Pre-Opening",
    val scale: String = "",
    val summary: String = "",
    val outcomes: List<String> = emptyList(),
    val imageKey: String = "palace_resort",
    val imageUrl: String? = null,
    val sortOrder: Long = 1L,
    val published: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toCaseStudy(): CaseStudy = CaseStudy(
        id = id,
        propertyName = propertyName,
        location = location,
        category = category,
        filterGroup = filterGroup,
        scale = scale,
        summary = summary,
        outcomes = outcomes,
        imageRes = ServiceCatalogRepository.resolveDrawableForKey(imageKey)
    )

    fun toAdminMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "propertyName" to propertyName.trim(),
            "location" to location.trim(),
            "category" to category.trim(),
            "filterGroup" to filterGroup.trim(),
            "scale" to scale.trim(),
            "summary" to summary.trim(),
            "outcomes" to outcomes.filter { it.isNotBlank() },
            "imageKey" to imageKey.trim(),
            "imageUrl" to imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            "sortOrder" to sortOrder,
            "published" to published,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class TestimonialRecord(
    @DocumentId val id: String = "",
    val quote: String = "",
    val attributionRole: String = "",
    val engagementContext: String = "",
    val sortOrder: Long = 1L,
    val published: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toTestimonial(): Testimonial = Testimonial(
        id = id,
        quote = quote,
        attributionRole = attributionRole,
        engagementContext = engagementContext
    )

    fun toAdminMap(): Map<String, Any> = mapOf(
        "quote" to quote.trim(),
        "attributionRole" to attributionRole.trim(),
        "engagementContext" to engagementContext.trim(),
        "sortOrder" to sortOrder,
        "published" to published,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class TeamMemberRecord(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val designation: String = "",
    val divisionName: String = "MILA Consultancy",
    val specialty: String = "",
    val yearsExperience: Long = 10L,
    val credentials: String = "",
    val imageKey: String = "hero_hotel",
    val imageUrl: String? = null,
    val sortOrder: Long = 1L,
    val published: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    fun toAdminMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "fullName" to fullName.trim(),
            "designation" to designation.trim(),
            "divisionName" to divisionName.trim(),
            "specialty" to specialty.trim(),
            "yearsExperience" to yearsExperience,
            "credentials" to credentials.trim(),
            "imageKey" to imageKey.trim(),
            "imageUrl" to imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            "sortOrder" to sortOrder,
            "published" to published,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class AdminSettings(
    @DocumentId val id: String = "platform",
    val maintenanceMode: Boolean = false,
    val allowClientRegistrations: Boolean = true,
    val allowCandidateRegistrations: Boolean = true,
    val requireCvForApplications: Boolean = false,
    val defaultEnquirySlaHours: Long = 24L,
    val supportEmail: String = "advisory@milahospitality.in",
    val officeRegions: String = "Mumbai • New Delhi NCR • Bengaluru • Rajasthan & Goa",
    val updatedAt: Timestamp? = null
) {
    fun toAdminMap(): Map<String, Any> = mapOf(
        "maintenanceMode" to maintenanceMode,
        "allowClientRegistrations" to allowClientRegistrations,
        "allowCandidateRegistrations" to allowCandidateRegistrations,
        "requireCvForApplications" to requireCvForApplications,
        "defaultEnquirySlaHours" to defaultEnquirySlaHours,
        "supportEmail" to supportEmail.trim(),
        "officeRegions" to officeRegions.trim(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val actorRole: String = "",
    val module: String = "",
    val action: String = "",
    val targetId: String = "",
    val summary: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "actorUid" to actorUid,
        "actorEmail" to actorEmail.ifBlank { "staff@milahospitality.in" },
        "actorRole" to actorRole,
        "module" to module,
        "action" to action,
        "targetId" to targetId.ifBlank { "record" },
        "summary" to summary.take(400),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

data class RealAnalyticsSnapshot(
    val totalClients: Int = 0,
    val totalEnquiries: Int = 0,
    val activeEnquiries: Int = 0,
    val enquiriesByStatus: Map<String, Int> = emptyMap(),
    val enquiriesByService: Map<String, Int> = emptyMap(),
    val publishedServicesCount: Int = 0,
    val totalServicesCount: Int = 0,
    val totalVacanciesCount: Int = 0,
    val publishedVacanciesCount: Int = 0,
    val totalCandidates: Int = 0,
    val candidatesWithCv: Int = 0,
    val jobAlertsEnabledCount: Int = 0,
    val totalApplications: Int = 0,
    val applicationsByStatus: Map<String, Int> = emptyMap(),
    val publishedPortfolioCount: Int = 0,
    val publishedTestimonialsCount: Int = 0,
    val publishedTeamCount: Int = 0,
    val activeAdminsCount: Int = 0,
    val activityLogsCount: Int = 0
)
