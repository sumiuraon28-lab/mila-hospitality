package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class ConsultationServiceType(
    val label: String,
    val slug: String
) {
    HOSPITALITY_PRE_OPENING(
        label = "Hospitality Pre-Opening Consultancy",
        slug = "hospitality-pre-opening-consultancy"
    ),
    RESTAURANT_CAFE_DEVELOPMENT(
        label = "Restaurant and Café Development",
        slug = "restaurant-cafe-development"
    ),
    BAR_BEVERAGE_CONSULTANCY(
        label = "Bar and Beverage Consultancy",
        slug = "bar-beverage-consultancy"
    ),
    HOSPITALITY_TRAINING(
        label = "Hospitality Training",
        slug = "hospitality-training"
    ),
    OPERATIONS_MANAGEMENT(
        label = "Operations Management",
        slug = "operations-management"
    ),
    RECRUITMENT_TALENT_ACQUISITION(
        label = "Recruitment and Talent Acquisition",
        slug = "recruitment-talent-acquisition"
    ),
    BUSINESS_CONCEPT_DEVELOPMENT(
        label = "Business and Concept Development",
        slug = "business-concept-development"
    ),
    HOSPITALITY_MANAGEMENT_SOLUTIONS(
        label = "Hospitality Management Solutions",
        slug = "hospitality-management-solutions"
    );

    companion object {
        fun fromRaw(raw: String): ConsultationServiceType = when (raw) {
            "PRE_OPENING" -> HOSPITALITY_PRE_OPENING
            "CAFE_RESTAURANT_SETUP" -> RESTAURANT_CAFE_DEVELOPMENT
            "BEVERAGE_MIXOLOGY" -> BAR_BEVERAGE_CONSULTANCY
            "EXECUTIVE_SEARCH" -> RECRUITMENT_TALENT_ACQUISITION
            "OPERATIONAL_AUDIT" -> OPERATIONS_MANAGEMENT
            else -> entries.firstOrNull { it.name == raw || it.slug == raw } ?: HOSPITALITY_PRE_OPENING
        }
    }
}

@Keep
@IgnoreExtraProperties
data class ServiceOffering(
    @DocumentId val id: String = "",
    val serviceType: String = ConsultationServiceType.HOSPITALITY_PRE_OPENING.name,
    val sortOrder: Long = 1L,
    val slug: String = "",
    val divisionName: String = "MILA Consultancy",
    val title: String = "",
    val subtitle: String = "",
    val summary: String = "",
    val detailedDescription: String = "",
    val servicesIncluded: List<String> = emptyList(),
    val ownerBenefits: List<String> = emptyList(),
    val deliverables: List<String> = emptyList(),
    val typicalTimeline: String = "",
    val imageKey: String = "palace_resort",
    val imageUrl: String? = null,
    val published: Boolean = true,
    val updatedAt: Timestamp? = null
) {
    val resolvedServiceType: ConsultationServiceType
        get() = ConsultationServiceType.fromRaw(serviceType)

    fun toAdminMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "serviceType" to serviceType,
            "sortOrder" to sortOrder,
            "slug" to slug.trim(),
            "divisionName" to divisionName.trim(),
            "title" to title.trim(),
            "subtitle" to subtitle.trim(),
            "summary" to summary.trim(),
            "detailedDescription" to detailedDescription.trim(),
            "servicesIncluded" to servicesIncluded,
            "ownerBenefits" to ownerBenefits,
            "deliverables" to deliverables,
            "typicalTimeline" to typicalTimeline.trim(),
            "imageKey" to imageKey.trim(),
            "imageUrl" to imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            "published" to published,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

enum class InquiryStatus(val label: String) {
    SUBMITTED("Enquiry Submitted"),
    UNDER_REVIEW("Partner Review"),
    PROPOSAL_SHARED("Proposal Shared"),
    CONVERTED("Mandate Signed"),
    WITHDRAWN("Withdrawn");

    companion object {
        fun fromRaw(raw: String): InquiryStatus =
            entries.firstOrNull { it.name == raw } ?: SUBMITTED
    }
}

enum class ApplicationStatus(val label: String) {
    APPLIED("Application Submitted"),
    UNDER_REVIEW("Under Partner Review"),
    SHORTLISTED("Shortlisted for Client"),
    INTERVIEW_SCHEDULED("Interview Scheduled"),
    OFFER_EXTENDED("Offer Extended"),
    HIRED("Placed / Hired"),
    DECLINED("Not Progressed"),
    WITHDRAWN("Withdrawn");

    companion object {
        fun fromRaw(raw: String): ApplicationStatus =
            entries.firstOrNull { it.name == raw } ?: APPLIED
    }
}

@Keep
@IgnoreExtraProperties
data class ConsultationInquiry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val propertyName: String = "",
    val city: String = "",
    val serviceType: String = ConsultationServiceType.HOSPITALITY_PRE_OPENING.name,
    val scaleSummary: String = "",
    val contactName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val briefNotes: String? = null,
    val assignedConsultantUid: String? = null,
    val assignedConsultantEmail: String? = null,
    val consultantNotes: String? = null,
    val status: String = InquiryStatus.SUBMITTED.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val resolvedServiceType: ConsultationServiceType
        get() = ConsultationServiceType.fromRaw(serviceType)

    val resolvedStatus: InquiryStatus
        get() = InquiryStatus.fromRaw(status)

    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "propertyName" to propertyName.trim(),
            "city" to city.trim(),
            "serviceType" to serviceType,
            "scaleSummary" to scaleSummary.trim(),
            "contactName" to contactName.trim(),
            "contactEmail" to contactEmail.trim(),
            "contactPhone" to contactPhone.trim(),
            "briefNotes" to briefNotes?.trim()?.takeIf { it.isNotEmpty() },
            "status" to InquiryStatus.SUBMITTED.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class JobApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roleId: String = "",
    val roleTitle: String = "",
    val location: String = "",
    val candidateName: String = "",
    val candidateEmail: String = "",
    val candidatePhone: String = "",
    val yearsExperience: Long = 0L,
    val currentProperty: String = "",
    val coverNote: String? = null,
    val cvFileName: String? = null,
    val status: String = ApplicationStatus.APPLIED.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val resolvedStatus: ApplicationStatus
        get() = ApplicationStatus.fromRaw(status)

    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "roleId" to roleId.trim(),
            "roleTitle" to roleTitle.trim(),
            "location" to location.trim(),
            "candidateName" to candidateName.trim(),
            "candidateEmail" to candidateEmail.trim(),
            "candidatePhone" to candidatePhone.trim(),
            "yearsExperience" to yearsExperience,
            "currentProperty" to currentProperty.trim(),
            "coverNote" to coverNote?.trim()?.takeIf { it.isNotEmpty() },
            "cvFileName" to cvFileName?.trim()?.takeIf { it.isNotEmpty() },
            "status" to ApplicationStatus.APPLIED.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class SavedRole(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roleId: String = "",
    val roleTitle: String = "",
    val location: String = "",
    val compensation: String = "",
    val category: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "roleId" to roleId.trim(),
            "roleTitle" to roleTitle.trim(),
            "location" to location.trim(),
            "compensation" to compensation.trim(),
            "category" to category.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

enum class BusinessCategory(val label: String) {
    LUXURY_HOTEL_RESORT("Luxury Hotel / Resort"),
    BOUTIQUE_HERITAGE_PROPERTY("Boutique / Heritage Property"),
    RESTAURANT_FINE_DINING("Restaurant / Fine Dining"),
    CAFE_ROASTERY_BAKERY("Café / Roastery / Bakery"),
    BAR_COCKTAIL_LOUNGE("Bar / Cocktail Lounge"),
    HOSPITALITY_GROUP_INVESTOR("Hospitality Group / Investor"),
    OTHER_HOSPITALITY_VENTURE("Other Hospitality Venture");

    companion object {
        fun fromRaw(raw: String): BusinessCategory =
            entries.firstOrNull { it.name == raw || it.label.equals(raw, ignoreCase = true) }
                ?: LUXURY_HOTEL_RESORT
    }
}

@Keep
@IgnoreExtraProperties
data class ClientProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val companyName: String = "",
    val designation: String = "",
    val city: String = "",
    val businessCategory: String = BusinessCategory.LUXURY_HOTEL_RESORT.name,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val resolvedBusinessCategory: BusinessCategory
        get() = BusinessCategory.fromRaw(businessCategory)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName.trim(),
        "email" to email.trim(),
        "mobileNumber" to mobileNumber.trim(),
        "companyName" to companyName.trim(),
        "designation" to designation.trim(),
        "city" to city.trim(),
        "businessCategory" to businessCategory,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "fullName" to fullName.trim(),
        "email" to email.trim(),
        "mobileNumber" to mobileNumber.trim(),
        "companyName" to companyName.trim(),
        "designation" to designation.trim(),
        "city" to city.trim(),
        "businessCategory" to businessCategory,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class EnquiryConfirmation(
    val referenceCode: String,
    val serviceType: ConsultationServiceType,
    val propertyName: String,
    val city: String,
    val scaleSummary: String,
    val contactName: String,
    val contactEmail: String
)

enum class HospitalityDepartment(val label: String) {
    HOSTESS("Hostess"),
    GRE("GRE"),
    BARTENDER("Bartender"),
    HOUSEKEEPING_WELLNESS("Housekeeping"),
    DRIVER("Driver"),
    STORE_KEEPER("Store Keeper"),
    STORE_MANAGER("Store Manager"),
    MAINTENANCE("Maintenance"),
    FNB_SERVICE("Food & Beverage Service"),
    CULINARY("Culinary & Pastry"),
    MIXOLOGY("Bar & Mixology"),
    ROOMS("Front Office & Rooms"),
    EXECUTIVE("Executive & General Management"),
    PRE_OPENING("Pre-Opening & Projects"),
    SALES_MARKETING("Sales, Marketing & Events");

    companion object {
        fun fromRaw(raw: String): HospitalityDepartment {
            val clean = raw.trim()
            if (clean.equals("HOUSEKEEPING", ignoreCase = true)) return HOUSEKEEPING_WELLNESS
            if (clean.equals("MAINTAINANCE", ignoreCase = true)) return MAINTENANCE
            return entries.firstOrNull { it.name.equals(clean, ignoreCase = true) || it.label.equals(clean, ignoreCase = true) }
                ?: EXECUTIVE
        }
    }
}

enum class CandidateAvailability(val label: String) {
    IMMEDIATE("Immediate Joiner"),
    WITHIN_15_DAYS("Within 15 Days"),
    WITHIN_30_DAYS("Within 30 Days"),
    WITHIN_60_DAYS("Within 60 Days"),
    OPEN_TO_OPPORTUNITIES("Open to Select Mandates");

    companion object {
        fun fromRaw(raw: String): CandidateAvailability =
            entries.firstOrNull { it.name == raw || it.label.equals(raw, ignoreCase = true) }
                ?: WITHIN_30_DAYS
    }
}

@Keep
@IgnoreExtraProperties
data class CandidateProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val location: String = "",
    val currentDesignation: String = "",
    val department: String = HospitalityDepartment.EXECUTIVE.name,
    val totalExperienceYears: Long = 0L,
    val currentEmployer: String = "",
    val previousEmployers: String = "",
    val education: String = "",
    val skills: String = "",
    val preferredJobLocations: String = "",
    val expectedSalary: String = "",
    val availability: String = CandidateAvailability.WITHIN_30_DAYS.name,
    val noticePeriod: String = "30 Days",
    val jobAlertsEnabled: Boolean = true,
    val cvFileName: String? = null,
    val cvMimeType: String? = null,
    val cvFileSizeBytes: Long? = null,
    val cvStoragePath: String? = null,
    val cvUploadedAtMillis: Long? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val resolvedDepartment: HospitalityDepartment
        get() = HospitalityDepartment.fromRaw(department)

    val resolvedAvailability: CandidateAvailability
        get() = CandidateAvailability.fromRaw(availability)

    val hasCvUploaded: Boolean
        get() = !cvFileName.isNullOrBlank() && !cvStoragePath.isNullOrBlank()

    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "fullName" to fullName.trim(),
            "email" to email.trim(),
            "mobileNumber" to mobileNumber.trim(),
            "location" to location.trim(),
            "currentDesignation" to currentDesignation.trim(),
            "department" to department,
            "totalExperienceYears" to totalExperienceYears,
            "currentEmployer" to currentEmployer.trim(),
            "previousEmployers" to previousEmployers.trim(),
            "education" to education.trim(),
            "skills" to skills.trim(),
            "preferredJobLocations" to preferredJobLocations.trim(),
            "expectedSalary" to expectedSalary.trim(),
            "availability" to availability,
            "noticePeriod" to noticePeriod.trim(),
            "jobAlertsEnabled" to jobAlertsEnabled,
            "cvFileName" to cvFileName?.trim()?.takeIf { it.isNotEmpty() },
            "cvMimeType" to cvMimeType?.trim()?.takeIf { it.isNotEmpty() },
            "cvFileSizeBytes" to cvFileSizeBytes,
            "cvStoragePath" to cvStoragePath?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("http") },
            "cvUploadedAtMillis" to cvUploadedAtMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "fullName" to fullName.trim(),
            "email" to email.trim(),
            "mobileNumber" to mobileNumber.trim(),
            "location" to location.trim(),
            "currentDesignation" to currentDesignation.trim(),
            "department" to department,
            "totalExperienceYears" to totalExperienceYears,
            "currentEmployer" to currentEmployer.trim(),
            "previousEmployers" to previousEmployers.trim(),
            "education" to education.trim(),
            "skills" to skills.trim(),
            "preferredJobLocations" to preferredJobLocations.trim(),
            "expectedSalary" to expectedSalary.trim(),
            "availability" to availability,
            "noticePeriod" to noticePeriod.trim(),
            "jobAlertsEnabled" to jobAlertsEnabled,
            "cvFileName" to (cvFileName?.trim()?.takeIf { it.isNotEmpty() } ?: FieldValue.delete()),
            "cvMimeType" to (cvMimeType?.trim()?.takeIf { it.isNotEmpty() } ?: FieldValue.delete()),
            "cvFileSizeBytes" to (cvFileSizeBytes ?: FieldValue.delete()),
            "cvStoragePath" to (cvStoragePath?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("http") } ?: FieldValue.delete()),
            "cvUploadedAtMillis" to (cvUploadedAtMillis ?: FieldValue.delete()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class JobNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roleId: String = "",
    val title: String = "",
    val message: String = "",
    val department: String = "",
    val location: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "roleId" to roleId.trim(),
        "title" to title.trim(),
        "message" to message.trim(),
        "department" to department.trim(),
        "location" to location.trim(),
        "isRead" to isRead,
        "createdAt" to FieldValue.serverTimestamp()
    )
}


