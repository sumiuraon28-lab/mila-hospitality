package com.example.data

import com.example.BuildConfig
import com.example.notifications.CandidateDeviceToken
import com.example.notifications.NotificationControlSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SupabaseAdminSession(
    val admin: AdminUser,
    val accessToken: String
)

/**
 * Authenticated Supabase PostgREST repository for the MILA Admin Portal.
 * Every query and mutation sends the verified administrator's Supabase JWT
 * so PostgreSQL RLS policies enforce 5-role RBAC server-side on every request.
 */
class SupabaseAdminRepository(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    fun isConfigured(): Boolean = SupabaseConfig.isConfigured(supabaseUrl, supabaseAnonKey)

    private fun baseUrl(): String = SupabaseConfig.normalizedBaseUrl(supabaseUrl)
    private fun anonKey(): String = supabaseAnonKey.trim()

    // =========================================================================
    // READS (RBAC-SCOPED BY POSTGRESQL RLS)
    // =========================================================================

    suspend fun fetchClients(token: String): Result<List<ClientProfile>> =
        getList("/rest/v1/client_profiles?order=created_at.desc", token) { o ->
            ClientProfile(
                id = o.optString("id"),
                userId = o.optString("user_id"),
                fullName = o.optString("full_name"),
                email = o.optString("email"),
                mobileNumber = o.optString("mobile_number"),
                companyName = o.optString("company_name"),
                designation = o.optString("designation"),
                city = o.optString("city"),
                businessCategory = o.optString("business_category", BusinessCategory.LUXURY_HOTEL_RESORT.name)
            )
        }

    suspend fun fetchInquiries(token: String): Result<List<ConsultationInquiry>> =
        getList("/rest/v1/service_enquiries?order=created_at.desc", token) { o ->
            ConsultationInquiry(
                id = o.optString("id"),
                userId = o.optString("user_id"),
                propertyName = o.optString("property_name"),
                city = o.optString("city"),
                serviceType = o.optString("service_type", ConsultationServiceType.HOSPITALITY_PRE_OPENING.name),
                scaleSummary = o.optString("scale_summary"),
                contactName = o.optString("contact_name"),
                contactEmail = o.optString("contact_email"),
                contactPhone = o.optString("contact_phone"),
                briefNotes = o.optNullable("brief_notes"),
                assignedConsultantUid = o.optNullable("assigned_consultant_id"),
                assignedConsultantEmail = o.optNullable("assigned_consultant_email"),
                consultantNotes = o.optNullable("consultant_notes"),
                status = o.optString("status", InquiryStatus.SUBMITTED.name)
            )
        }

    suspend fun fetchServices(token: String): Result<List<ServiceOffering>> =
        getList("/rest/v1/services?order=sort_order.asc", token) { o ->
            ServiceOffering(
                id = o.optString("id"),
                serviceType = o.optString("service_type"),
                sortOrder = o.optLong("sort_order", 1L),
                slug = o.optString("slug"),
                divisionName = o.optString("division_name", "MILA Consultancy"),
                title = o.optString("title"),
                subtitle = o.optString("subtitle"),
                summary = o.optString("summary"),
                detailedDescription = o.optString("detailed_description"),
                servicesIncluded = o.optStringArray("services_included"),
                ownerBenefits = o.optStringArray("owner_benefits"),
                deliverables = o.optStringArray("deliverables"),
                typicalTimeline = o.optString("typical_timeline"),
                imageKey = o.optString("image_key", "palace_resort"),
                imageUrl = o.optNullable("image_url"),
                published = o.optBoolean("published", true)
            )
        }

    suspend fun fetchCategories(token: String): Result<List<ServiceCategoryRecord>> =
        getList("/rest/v1/service_categories?order=sort_order.asc", token) { o ->
            ServiceCategoryRecord(
                id = o.optString("id"),
                code = o.optString("code"),
                sortOrder = o.optLong("sort_order", 1L),
                title = o.optString("title"),
                divisionName = o.optString("division_name", "MILA Consultancy"),
                summary = o.optString("summary"),
                published = o.optBoolean("published", true)
            )
        }

    suspend fun fetchVacancies(token: String): Result<List<JobVacancyRecord>> =
        getList("/rest/v1/job_vacancies?order=created_at.desc", token) { o ->
            JobVacancyRecord(
                id = o.optString("id"),
                title = o.optString("title"),
                propertyTier = o.optString("property_tier"),
                location = o.optString("location"),
                cityRegion = o.optString("city_region"),
                compensation = o.optString("compensation"),
                experienceRequired = o.optString("experience_required"),
                minExperienceYears = o.optLong("min_experience_years", 0L),
                category = o.optString("category", RoleCategory.PRE_OPENING.name),
                summary = o.optString("summary"),
                keyMandates = o.optStringArray("key_mandates"),
                desiredSkills = o.optStringArray("desired_skills"),
                urgent = o.optBoolean("is_urgent", false),
                published = o.optBoolean("published", true)
            )
        }

    suspend fun fetchCandidates(token: String): Result<List<CandidateProfile>> =
        getList("/rest/v1/candidate_profiles?order=created_at.desc", token) { o ->
            CandidateProfile(
                id = o.optString("id"),
                userId = o.optString("user_id"),
                fullName = o.optString("full_name"),
                email = o.optString("email"),
                mobileNumber = o.optString("mobile_number"),
                location = o.optString("location"),
                currentDesignation = o.optString("current_designation"),
                department = o.optString("department", HospitalityDepartment.EXECUTIVE.name),
                totalExperienceYears = o.optLong("total_experience_years", 0L),
                currentEmployer = o.optString("current_employer"),
                previousEmployers = o.optString("previous_employers"),
                education = o.optString("education"),
                skills = o.optString("skills"),
                preferredJobLocations = o.optString("preferred_job_locations"),
                expectedSalary = o.optString("expected_salary"),
                availability = o.optString("availability", CandidateAvailability.WITHIN_30_DAYS.name),
                noticePeriod = o.optString("notice_period", "30 Days"),
                jobAlertsEnabled = o.optBoolean("job_alerts_enabled", true),
                cvFileName = o.optNullable("cv_file_name"),
                cvMimeType = o.optNullable("cv_mime_type"),
                cvFileSizeBytes = if (o.isNull("cv_file_size_bytes")) null else o.optLong("cv_file_size_bytes"),
                cvStoragePath = o.optNullable("cv_storage_path")
            )
        }

    suspend fun fetchApplications(token: String): Result<List<JobApplication>> =
        getList("/rest/v1/job_applications?order=created_at.desc", token) { o ->
            JobApplication(
                id = o.optString("id"),
                userId = o.optString("user_id"),
                roleId = o.optString("role_id"),
                roleTitle = o.optString("role_title"),
                location = o.optString("location"),
                candidateName = o.optString("candidate_name"),
                candidateEmail = o.optString("candidate_email"),
                candidatePhone = o.optString("candidate_phone"),
                yearsExperience = o.optLong("years_experience", 0L),
                currentProperty = o.optString("current_property"),
                coverNote = o.optNullable("cover_note"),
                cvFileName = o.optNullable("cv_file_name"),
                status = o.optString("status", ApplicationStatus.APPLIED.name)
            )
        }

    suspend fun fetchNotifications(token: String): Result<List<JobNotification>> =
        getList("/rest/v1/notifications?order=created_at.desc&limit=100", token) { o ->
            JobNotification(
                id = o.optString("id"),
                userId = o.optString("user_id"),
                roleId = o.optString("role_id"),
                title = o.optString("title"),
                message = o.optString("message"),
                department = o.optString("department"),
                location = o.optString("location"),
                isRead = o.optBoolean("is_read", false)
            )
        }

    suspend fun fetchSitePages(token: String): Result<List<SitePageContent>> =
        getList("/rest/v1/website_content?order=page_key.asc", token) { o ->
            SitePageContent(
                id = o.optString("id"),
                pageKey = o.optString("page_key"),
                pageTitle = o.optString("page_title"),
                eyebrow = o.optString("eyebrow"),
                headline = o.optString("headline"),
                subheadline = o.optString("subheadline"),
                bodyCopy = o.optString("body_copy"),
                heroImageKey = o.optString("hero_image_key", "hero_hotel"),
                heroImageUrl = o.optNullable("hero_image_url"),
                ctaLabel = o.optString("cta_label", "Explore Services"),
                published = o.optBoolean("published", true)
            )
        }

    suspend fun fetchPortfolioItems(token: String): Result<List<PortfolioItemRecord>> =
        getList("/rest/v1/portfolio_projects?order=sort_order.asc", token) { o ->
            PortfolioItemRecord(
                id = o.optString("id"),
                propertyName = o.optString("property_name"),
                location = o.optString("location"),
                category = o.optString("category"),
                filterGroup = o.optString("filter_group", "Pre-Opening"),
                scale = o.optString("scale"),
                summary = o.optString("summary"),
                outcomes = o.optStringArray("outcomes"),
                imageKey = o.optString("image_key", "palace_resort"),
                imageUrl = o.optNullable("image_url"),
                sortOrder = o.optLong("sort_order", 1L),
                published = o.optBoolean("published", true)
            )
        }

    suspend fun fetchTestimonials(token: String): Result<List<TestimonialRecord>> =
        getList("/rest/v1/testimonials?order=sort_order.asc", token) { o ->
            TestimonialRecord(
                id = o.optString("id"),
                quote = o.optString("quote"),
                attributionRole = o.optString("attribution_role"),
                engagementContext = o.optString("engagement_context"),
                sortOrder = o.optLong("sort_order", 1L),
                published = o.optBoolean("published", true)
            )
        }

    suspend fun fetchTeamMembers(token: String): Result<List<TeamMemberRecord>> =
        getList("/rest/v1/team_members?order=sort_order.asc", token) { o ->
            TeamMemberRecord(
                id = o.optString("id"),
                fullName = o.optString("full_name"),
                designation = o.optString("designation"),
                divisionName = o.optString("division_name", "MILA Consultancy"),
                specialty = o.optString("specialty"),
                yearsExperience = o.optLong("years_experience", 10L),
                credentials = o.optString("credentials"),
                imageKey = o.optString("image_key", "hero_hotel"),
                imageUrl = o.optNullable("image_url"),
                sortOrder = o.optLong("sort_order", 1L),
                published = o.optBoolean("published", true)
            )
        }

    suspend fun fetchAdminUsers(token: String): Result<List<AdminUser>> =
        getList("/rest/v1/admin_users?order=created_at.desc", token) { o ->
            AdminUser(
                id = o.optString("id"),
                uid = o.optString("user_id"),
                email = o.optString("email"),
                displayName = o.optString("display_name"),
                role = o.optString("role", AdminRole.REPORTING_ADMIN.name),
                active = o.optBoolean("active", true),
                assignedDepartment = o.optNullable("assigned_department")
            )
        }

    suspend fun fetchAdminInvites(token: String): Result<List<AdminInvite>> =
        getList("/rest/v1/admin_invites?order=created_at.desc", token) { o ->
            AdminInvite(
                id = o.optString("id"),
                email = o.optString("email"),
                displayName = o.optString("display_name"),
                role = o.optString("role", AdminRole.CONSULTANT.name),
                active = o.optBoolean("active", true),
                createdByUid = o.optString("created_by")
            )
        }

    suspend fun fetchPlatformSettings(token: String): Result<AdminSettings?> =
        getList("/rest/v1/platform_settings?id=eq.platform&limit=1", token) { o ->
            AdminSettings(
                id = o.optString("id", "platform"),
                maintenanceMode = o.optBoolean("maintenance_mode", false),
                allowClientRegistrations = o.optBoolean("allow_client_registrations", true),
                allowCandidateRegistrations = o.optBoolean("allow_candidate_registrations", true),
                requireCvForApplications = o.optBoolean("require_cv_for_applications", false),
                defaultEnquirySlaHours = o.optLong("default_enquiry_sla_hours", 24L),
                supportEmail = o.optString("support_email", "advisory@milahospitality.in"),
                officeRegions = o.optString("office_regions", "Mumbai • New Delhi NCR • Bengaluru • Rajasthan & Goa")
            )
        }.map { it.firstOrNull() }

    suspend fun fetchActivityLogs(token: String): Result<List<ActivityLog>> =
        getList("/rest/v1/activity_logs?order=created_at.desc&limit=100", token) { o ->
            ActivityLog(
                id = o.optString("id"),
                actorUid = o.optString("actor_uid"),
                actorEmail = o.optString("actor_email"),
                actorRole = o.optString("actor_role"),
                module = o.optString("module"),
                action = o.optString("action"),
                targetId = o.optString("target_id"),
                summary = o.optString("summary")
            )
        }

    suspend fun fetchNotificationControls(token: String): Result<NotificationControlSettings?> =
        getList("/rest/v1/notification_controls?id=eq.global&limit=1", token) { o ->
            NotificationControlSettings(
                id = o.optString("id", "global"),
                autoDispatchOnPublish = o.optBoolean("auto_dispatch_on_vacancy_publish", true),
                enableInApp = o.optBoolean("enable_in_app", true),
                enableAndroidPush = o.optBoolean("enable_android_push", true),
                enableIosPush = o.optBoolean("enable_ios_push", true),
                enableEmailNotifications = o.optBoolean("enable_email_notifications", true),
                enableApplicationStatusAlerts = o.optBoolean("enable_application_status_notifications", true),
                requireDepartmentMatch = o.optBoolean("require_department_match", true),
                requireLocationMatch = o.optBoolean("require_location_match", true),
                experienceToleranceYears = o.optLong("experience_tolerance_years", 1L),
                dailyCapPerCandidate = o.optLong("max_notifications_per_candidate_per_day", 5L)
            )
        }.map { it.firstOrNull() }

    suspend fun fetchDeviceTokens(token: String): Result<List<CandidateDeviceToken>> =
        getList("/rest/v1/device_push_tokens?is_active=eq.true&order=last_seen_at.desc", token) { o ->
            CandidateDeviceToken(
                id = o.optString("id"),
                userId = o.optString("user_id"),
                fcmToken = o.optString("fcm_token"),
                platform = o.optString("platform", "ANDROID"),
                deviceLabel = o.optString("device_label", "Mobile Device"),
                active = o.optBoolean("is_active", true)
            )
        }

    // =========================================================================
    // MUTATIONS + AUDIT LOGGING
    // =========================================================================

    suspend fun updateClient(session: SupabaseAdminSession, client: ClientProfile): Result<Unit> {
        val payload = JSONObject().apply {
            put("full_name", client.fullName.trim())
            put("email", client.email.trim().lowercase())
            put("mobile_number", client.mobileNumber.trim())
            put("company_name", client.companyName.trim())
            put("designation", client.designation.trim())
            put("city", client.city.trim())
            put("business_category", client.resolvedBusinessCategory.name)
        }
        return mutateAndLog(
            session = session,
            method = "PATCH",
            path = "/rest/v1/client_profiles?id=eq.${enc(client.id)}",
            body = payload.toString(),
            module = "CLIENT_MANAGEMENT",
            action = "UPDATE",
            targetId = client.id,
            summary = "Updated client ${client.companyName}"
        )
    }

    suspend fun deleteClient(session: SupabaseAdminSession, client: ClientProfile): Result<Unit> =
        mutateAndLog(
            session = session,
            method = "DELETE",
            path = "/rest/v1/client_profiles?id=eq.${enc(client.id)}",
            body = null,
            module = "CLIENT_MANAGEMENT",
            action = "DELETE",
            targetId = client.id,
            summary = "Removed client ${client.companyName}"
        )

    suspend fun updateInquiry(
        session: SupabaseAdminSession,
        inquiry: ConsultationInquiry,
        status: InquiryStatus,
        notes: String,
        consultant: AdminUser?
    ): Result<Unit> {
        val payload = JSONObject().apply {
            put("status", status.name)
            put("consultant_notes", notes.trim().takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
            if (consultant != null && consultant.uid.isNotBlank()) {
                put("assigned_consultant_id", consultant.uid)
                put("assigned_consultant_email", consultant.email)
            }
        }
        return mutateAndLog(
            session = session,
            method = "PATCH",
            path = "/rest/v1/service_enquiries?id=eq.${enc(inquiry.id)}",
            body = payload.toString(),
            module = "CLIENT_ENQUIRIES",
            action = "UPDATE",
            targetId = inquiry.id,
            summary = "Updated enquiry ${inquiry.propertyName} to ${status.label}"
        )
    }

    suspend fun deleteInquiry(session: SupabaseAdminSession, inquiry: ConsultationInquiry): Result<Unit> =
        mutateAndLog(
            session = session,
            method = "DELETE",
            path = "/rest/v1/service_enquiries?id=eq.${enc(inquiry.id)}",
            body = null,
            module = "CLIENT_ENQUIRIES",
            action = "DELETE",
            targetId = inquiry.id,
            summary = "Deleted enquiry ${inquiry.propertyName}"
        )

    suspend fun saveService(session: SupabaseAdminSession, service: ServiceOffering): Result<Unit> {
        val payload = JSONObject().apply {
            put("service_type", service.resolvedServiceType.name)
            put("slug", service.slug.trim().ifBlank { service.resolvedServiceType.slug })
            put("sort_order", service.sortOrder)
            put("division_name", service.divisionName.trim())
            put("title", service.title.trim())
            put("subtitle", service.subtitle.trim())
            put("summary", service.summary.trim())
            put("detailed_description", service.detailedDescription.trim())
            put("services_included", JSONArray(service.servicesIncluded))
            put("owner_benefits", JSONArray(service.ownerBenefits))
            put("deliverables", JSONArray(service.deliverables))
            put("typical_timeline", service.typicalTimeline.trim())
            put("image_key", service.imageKey.trim())
            put("image_url", service.imageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
            put("published", service.published)
        }
        val path = if (service.id.isNotBlank()) "/rest/v1/services?id=eq.${enc(service.id)}" else "/rest/v1/services?on_conflict=service_type"
        val method = if (service.id.isNotBlank()) "PATCH" else "POST"
        return mutateAndLog(
            session = session,
            method = method,
            path = path,
            body = payload.toString(),
            prefer = if (method == "POST") "resolution=merge-duplicates,return=minimal" else "return=minimal",
            module = "SERVICE_MANAGEMENT",
            action = if (service.id.isNotBlank()) "UPDATE" else "CREATE",
            targetId = service.id.ifBlank { service.slug },
            summary = "Saved service ${service.title}"
        )
    }

    suspend fun deleteService(session: SupabaseAdminSession, service: ServiceOffering): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/services?id=eq.${enc(service.id)}", null,
            module = "SERVICE_MANAGEMENT", action = "DELETE", targetId = service.id,
            summary = "Deleted service ${service.title}"
        )

    suspend fun saveCategory(session: SupabaseAdminSession, cat: ServiceCategoryRecord): Result<Unit> {
        val payload = JSONObject().apply {
            put("code", cat.code.trim().ifBlank { cat.sortOrder.toString().padStart(2, '0') })
            put("sort_order", cat.sortOrder)
            put("title", cat.title.trim())
            put("division_name", cat.divisionName.trim())
            put("summary", cat.summary.trim())
            put("published", cat.published)
        }
        val isUpdate = cat.id.isNotBlank()
        return mutateAndLog(
            session = session,
            method = if (isUpdate) "PATCH" else "POST",
            path = if (isUpdate) "/rest/v1/service_categories?id=eq.${enc(cat.id)}" else "/rest/v1/service_categories?on_conflict=code",
            body = payload.toString(),
            prefer = if (isUpdate) "return=minimal" else "resolution=merge-duplicates,return=minimal",
            module = "SERVICE_CATEGORIES",
            action = if (isUpdate) "UPDATE" else "CREATE",
            targetId = cat.id.ifBlank { cat.code },
            summary = "Saved service category ${cat.title}"
        )
    }

    suspend fun deleteCategory(session: SupabaseAdminSession, cat: ServiceCategoryRecord): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/service_categories?id=eq.${enc(cat.id)}", null,
            module = "SERVICE_CATEGORIES", action = "DELETE", targetId = cat.id,
            summary = "Deleted category ${cat.title}"
        )

    suspend fun saveVacancy(session: SupabaseAdminSession, vacancy: JobVacancyRecord): Result<Unit> {
        val slug = vacancy.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            .ifBlank { "role-${System.currentTimeMillis()}" }
        val payload = JSONObject().apply {
            if (vacancy.id.isBlank()) put("slug", "$slug-${System.currentTimeMillis() % 10000}")
            put("title", vacancy.title.trim())
            put("property_tier", vacancy.propertyTier.trim())
            put("location", vacancy.location.trim())
            put("city_region", vacancy.cityRegion.trim())
            put("compensation", vacancy.compensation.trim())
            put("experience_required", vacancy.experienceRequired.trim())
            put("min_experience_years", vacancy.minExperienceYears.coerceIn(0L, 50L))
            put("category", vacancy.resolvedCategory.name)
            put("summary", vacancy.summary.trim())
            put("key_mandates", JSONArray(vacancy.keyMandates.filter { it.isNotBlank() }))
            put("desired_skills", JSONArray(vacancy.desiredSkills.filter { it.isNotBlank() }))
            put("is_urgent", vacancy.urgent)
            put("published", vacancy.published)
        }
        val isUpdate = vacancy.id.isNotBlank()
        return mutateAndLog(
            session = session,
            method = if (isUpdate) "PATCH" else "POST",
            path = if (isUpdate) "/rest/v1/job_vacancies?id=eq.${enc(vacancy.id)}" else "/rest/v1/job_vacancies",
            body = payload.toString(),
            module = "JOB_VACANCY_MANAGEMENT",
            action = if (isUpdate) "UPDATE" else "CREATE",
            targetId = vacancy.id.ifBlank { slug },
            summary = "Saved job vacancy ${vacancy.title}"
        )
    }

    suspend fun deleteVacancy(session: SupabaseAdminSession, vacancy: JobVacancyRecord): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/job_vacancies?id=eq.${enc(vacancy.id)}", null,
            module = "JOB_VACANCY_MANAGEMENT", action = "DELETE", targetId = vacancy.id,
            summary = "Deleted job vacancy ${vacancy.title}"
        )

    suspend fun updateCandidate(session: SupabaseAdminSession, candidate: CandidateProfile): Result<Unit> {
        val payload = JSONObject().apply {
            put("full_name", candidate.fullName.trim())
            put("email", candidate.email.trim().lowercase())
            put("mobile_number", candidate.mobileNumber.trim())
            put("location", candidate.location.trim())
            put("current_designation", candidate.currentDesignation.trim())
            put("department", candidate.resolvedDepartment.name)
            put("total_experience_years", candidate.totalExperienceYears.coerceIn(0L, 60L))
            put("current_employer", candidate.currentEmployer.trim())
            put("previous_employers", candidate.previousEmployers.trim())
            put("education", candidate.education.trim())
            put("skills", candidate.skills.trim())
            put("preferred_job_locations", candidate.preferredJobLocations.trim())
            put("expected_salary", candidate.expectedSalary.trim())
            put("availability", candidate.resolvedAvailability.name)
            put("notice_period", candidate.noticePeriod.trim())
            put("job_alerts_enabled", candidate.jobAlertsEnabled)
        }
        return mutateAndLog(
            session = session,
            method = "PATCH",
            path = "/rest/v1/candidate_profiles?id=eq.${enc(candidate.id)}",
            body = payload.toString(),
            module = "CANDIDATE_MANAGEMENT",
            action = "UPDATE",
            targetId = candidate.id,
            summary = "Updated candidate profile for ${candidate.fullName}"
        )
    }

    suspend fun deleteCandidate(session: SupabaseAdminSession, candidate: CandidateProfile): Result<Unit> =
        mutateAndLog(
            session = session,
            method = "DELETE",
            path = "/rest/v1/candidate_profiles?id=eq.${enc(candidate.id)}",
            body = null,
            module = "CANDIDATE_MANAGEMENT",
            action = "DELETE",
            targetId = candidate.id,
            summary = "Deleted candidate profile for ${candidate.fullName}"
        )

    suspend fun toggleCandidateAlerts(session: SupabaseAdminSession, candidate: CandidateProfile): Result<Unit> {
        val next = !candidate.jobAlertsEnabled
        return mutateAndLog(
            session, "PATCH", "/rest/v1/candidate_profiles?id=eq.${enc(candidate.id)}",
            JSONObject().put("job_alerts_enabled", next).toString(),
            module = "CANDIDATE_MANAGEMENT", action = "UPDATE", targetId = candidate.id,
            summary = "Set job alerts=${next} for ${candidate.fullName}"
        )
    }

    suspend fun clearCandidateCv(session: SupabaseAdminSession, candidate: CandidateProfile): Result<Unit> {
        val payload = JSONObject().apply {
            put("cv_file_name", JSONObject.NULL)
            put("cv_mime_type", JSONObject.NULL)
            put("cv_file_size_bytes", JSONObject.NULL)
            put("cv_storage_path", JSONObject.NULL)
        }
        return mutateAndLog(
            session, "PATCH", "/rest/v1/candidate_profiles?id=eq.${enc(candidate.id)}",
            payload.toString(),
            module = "CV_DATABASE", action = "PURGE_CV", targetId = candidate.id,
            summary = "Cleared CV record for ${candidate.fullName}"
        )
    }

    suspend fun downloadPrivateCvBytes(
        session: SupabaseAdminSession,
        candidate: CandidateProfile,
        storagePath: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Supabase storage is not configured."))
        }
        val cleanPath = storagePath.trim().trimStart('/').removePrefix("candidate-cvs/")
        if (cleanPath.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No CV storage path available for ${candidate.fullName}."))
        }
        val encodedPath = cleanPath.split('/').joinToString("/") { segment ->
            SupabaseConfig.encodeQueryValue(segment).replace("+", "%20")
        }
        try {
            val req = Request.Builder()
                .url("${baseUrl()}/storage/v1/object/authenticated/candidate-cvs/$encodedPath")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .get()
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    val raw = res.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException(extractError(raw, "Could not download CV from private storage (${res.code})."))
                    )
                }
                val bytes = res.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("Downloaded CV file was empty."))
                }
                if (!session.admin.resolvedRole.isReadOnly) {
                    runCatching {
                        val logPayload = JSONObject().apply {
                            if (session.admin.uid.isNotBlank()) put("actor_uid", session.admin.uid)
                            put("actor_email", session.admin.email.ifBlank { "admin@milahospitality.in" })
                            put("actor_role", session.admin.resolvedRole.name)
                            put("module", "CV_DATABASE")
                            put("action", "DOWNLOAD_CV")
                            put("target_id", candidate.id.ifBlank { candidate.userId })
                            put("summary", "Downloaded CV ${candidate.cvFileName ?: cleanPath} for ${candidate.fullName}".take(400))
                        }
                        executeMutation("POST", "/rest/v1/activity_logs", session.accessToken, logPayload.toString(), "return=minimal")
                    }
                }
                Result.success(bytes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplicationStatus(
        session: SupabaseAdminSession,
        app: JobApplication,
        status: ApplicationStatus,
        notifyCandidate: Boolean
    ): Result<Unit> {
        val res = mutateAndLog(
            session, "PATCH", "/rest/v1/job_applications?id=eq.${enc(app.id)}",
            JSONObject().put("status", status.name).toString(),
            module = "JOB_APPLICATIONS", action = "STATUS_UPDATE", targetId = app.id,
            summary = "Updated ${app.candidateName} (${app.roleTitle}) to ${status.label}"
        )
        if (res.isSuccess && notifyCandidate && app.userId.isNotBlank()) {
            sendNotification(
                session = session,
                targetUserId = app.userId,
                roleId = app.roleId,
                title = "Application Update: ${app.roleTitle}",
                message = "Your application status with MILA Talent is now: ${status.label}.",
                department = "Recruitment",
                location = app.location
            )
        }
        return res
    }

    suspend fun deleteApplication(session: SupabaseAdminSession, app: JobApplication): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/job_applications?id=eq.${enc(app.id)}", null,
            module = "JOB_APPLICATIONS", action = "DELETE", targetId = app.id,
            summary = "Deleted application from ${app.candidateName}"
        )

    suspend fun sendNotification(
        session: SupabaseAdminSession,
        targetUserId: String,
        roleId: String,
        title: String,
        message: String,
        department: String,
        location: String
    ): Result<Unit> {
        val payload = JSONObject().apply {
            put("user_id", targetUserId.trim())
            put("role_id", roleId.trim())
            put("title", title.trim())
            put("message", message.trim())
            put("department", department.trim())
            put("location", location.trim())
            put("is_read", false)
        }
        return mutateAndLog(
            session, "POST", "/rest/v1/notifications", payload.toString(),
            module = "NOTIFICATION_MANAGEMENT", action = "SEND", targetId = targetUserId,
            summary = "Dispatched notification: $title"
        )
    }

    suspend fun deleteNotification(session: SupabaseAdminSession, notification: JobNotification): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/notifications?id=eq.${enc(notification.id)}", null,
            module = "NOTIFICATION_MANAGEMENT", action = "DELETE", targetId = notification.id,
            summary = "Deleted notification ${notification.title}"
        )

    suspend fun saveNotificationControls(session: SupabaseAdminSession, controls: NotificationControlSettings): Result<Unit> {
        val payload = JSONObject().apply {
            put("auto_dispatch_on_vacancy_publish", controls.autoDispatchOnPublish)
            put("enable_in_app", controls.enableInApp)
            put("enable_android_push", controls.enableAndroidPush)
            put("enable_ios_push", controls.enableIosPush)
            put("enable_email_notifications", controls.enableEmailNotifications)
            put("enable_application_status_notifications", controls.enableApplicationStatusAlerts)
            put("require_department_match", controls.requireDepartmentMatch)
            put("require_location_match", controls.requireLocationMatch)
            put("experience_tolerance_years", controls.experienceToleranceYears.coerceIn(0L, 5L))
            put("max_notifications_per_candidate_per_day", controls.dailyCapPerCandidate.coerceIn(1L, 50L))
        }
        return mutateAndLog(
            session, "PATCH", "/rest/v1/notification_controls?id=eq.global", payload.toString(),
            module = "NOTIFICATION_MANAGEMENT", action = "UPDATE_CONTROLS", targetId = "global",
            summary = "Updated notification channel controls"
        )
    }

    suspend fun saveSitePage(session: SupabaseAdminSession, page: SitePageContent): Result<Unit> {
        val payload = JSONObject().apply {
            put("page_key", page.pageKey.trim().ifBlank { "page_${System.currentTimeMillis()}" })
            put("page_title", page.pageTitle.trim())
            put("eyebrow", page.eyebrow.trim())
            put("headline", page.headline.trim())
            put("subheadline", page.subheadline.trim())
            put("body_copy", page.bodyCopy.trim())
            put("hero_image_key", page.heroImageKey.trim())
            put("hero_image_url", page.heroImageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
            put("cta_label", page.ctaLabel.trim())
            put("published", page.published)
        }
        val isUpdate = page.id.isNotBlank()
        return mutateAndLog(
            session,
            method = if (isUpdate) "PATCH" else "POST",
            path = if (isUpdate) "/rest/v1/website_content?id=eq.${enc(page.id)}" else "/rest/v1/website_content?on_conflict=page_key",
            body = payload.toString(),
            prefer = if (isUpdate) "return=minimal" else "resolution=merge-duplicates,return=minimal",
            module = "WEBSITE_CONTENT_MANAGEMENT", action = if (isUpdate) "UPDATE" else "CREATE",
            targetId = page.pageKey, summary = "Updated page ${page.pageTitle}"
        )
    }

    suspend fun deleteSitePage(session: SupabaseAdminSession, page: SitePageContent): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/website_content?id=eq.${enc(page.id)}", null,
            module = "WEBSITE_CONTENT_MANAGEMENT", action = "DELETE", targetId = page.id,
            summary = "Deleted website page ${page.pageTitle}"
        )

    suspend fun savePortfolioItem(session: SupabaseAdminSession, item: PortfolioItemRecord): Result<Unit> {
        val slug = item.propertyName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            .ifBlank { "portfolio-${System.currentTimeMillis()}" }
        val payload = JSONObject().apply {
            if (item.id.isBlank()) put("slug", "$slug-${System.currentTimeMillis() % 10000}")
            put("property_name", item.propertyName.trim())
            put("location", item.location.trim())
            put("category", item.category.trim())
            put("filter_group", item.filterGroup.trim())
            put("scale", item.scale.trim())
            put("summary", item.summary.trim())
            put("outcomes", JSONArray(item.outcomes.filter { it.isNotBlank() }))
            put("image_key", item.imageKey.trim())
            put("image_url", item.imageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
            put("sort_order", item.sortOrder)
            put("published", item.published)
        }
        val isUpdate = item.id.isNotBlank()
        return mutateAndLog(
            session, if (isUpdate) "PATCH" else "POST",
            if (isUpdate) "/rest/v1/portfolio_projects?id=eq.${enc(item.id)}" else "/rest/v1/portfolio_projects",
            payload.toString(),
            module = "PORTFOLIO_MANAGEMENT", action = if (isUpdate) "UPDATE" else "CREATE",
            targetId = item.id.ifBlank { slug }, summary = "Saved portfolio item ${item.propertyName}"
        )
    }

    suspend fun deletePortfolioItem(session: SupabaseAdminSession, item: PortfolioItemRecord): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/portfolio_projects?id=eq.${enc(item.id)}", null,
            module = "PORTFOLIO_MANAGEMENT", action = "DELETE", targetId = item.id,
            summary = "Deleted portfolio item ${item.propertyName}"
        )

    suspend fun saveTestimonial(session: SupabaseAdminSession, item: TestimonialRecord): Result<Unit> {
        val payload = JSONObject().apply {
            put("quote", item.quote.trim())
            put("attribution_role", item.attributionRole.trim())
            put("engagement_context", item.engagementContext.trim())
            put("sort_order", item.sortOrder)
            put("published", item.published)
        }
        val isUpdate = item.id.isNotBlank()
        return mutateAndLog(
            session, if (isUpdate) "PATCH" else "POST",
            if (isUpdate) "/rest/v1/testimonials?id=eq.${enc(item.id)}" else "/rest/v1/testimonials",
            payload.toString(),
            module = "TESTIMONIALS", action = if (isUpdate) "UPDATE" else "CREATE",
            targetId = item.id.ifBlank { "testimonial" }, summary = "Saved testimonial (${item.attributionRole})"
        )
    }

    suspend fun deleteTestimonial(session: SupabaseAdminSession, item: TestimonialRecord): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/testimonials?id=eq.${enc(item.id)}", null,
            module = "TESTIMONIALS", action = "DELETE", targetId = item.id,
            summary = "Deleted testimonial"
        )

    suspend fun saveTeamMember(session: SupabaseAdminSession, member: TeamMemberRecord): Result<Unit> {
        val payload = JSONObject().apply {
            put("full_name", member.fullName.trim())
            put("designation", member.designation.trim())
            put("division_name", member.divisionName.trim())
            put("specialty", member.specialty.trim())
            put("years_experience", member.yearsExperience.coerceIn(0L, 60L))
            put("credentials", member.credentials.trim())
            put("image_key", member.imageKey.trim())
            put("image_url", member.imageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
            put("sort_order", member.sortOrder)
            put("published", member.published)
        }
        val isUpdate = member.id.isNotBlank()
        return mutateAndLog(
            session, if (isUpdate) "PATCH" else "POST",
            if (isUpdate) "/rest/v1/team_members?id=eq.${enc(member.id)}" else "/rest/v1/team_members",
            payload.toString(),
            module = "TEAM_MANAGEMENT", action = if (isUpdate) "UPDATE" else "CREATE",
            targetId = member.id.ifBlank { member.fullName }, summary = "Saved team member ${member.fullName}"
        )
    }

    suspend fun deleteTeamMember(session: SupabaseAdminSession, member: TeamMemberRecord): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/team_members?id=eq.${enc(member.id)}", null,
            module = "TEAM_MANAGEMENT", action = "DELETE", targetId = member.id,
            summary = "Deleted team member ${member.fullName}"
        )

    suspend fun saveAdminInvite(session: SupabaseAdminSession, invite: AdminInvite): Result<Unit> {
        val email = AdminInvite.normalizeEmailKey(invite.email)
        val payload = JSONObject().apply {
            put("email", email)
            put("display_name", invite.displayName.trim().ifBlank { "MILA Staff" })
            put("role", invite.resolvedRole.name)
            put("active", true)
            if (session.admin.uid.isNotBlank()) put("created_by", session.admin.uid)
        }
        return mutateAndLog(
            session, "POST", "/rest/v1/admin_invites?on_conflict=email", payload.toString(),
            prefer = "resolution=merge-duplicates,return=minimal",
            module = "ADMIN_USER_MANAGEMENT", action = "INVITE", targetId = email,
            summary = "Invited $email as ${invite.resolvedRole.displayLabel}"
        )
    }

    suspend fun deleteAdminInvite(session: SupabaseAdminSession, invite: AdminInvite): Result<Unit> =
        mutateAndLog(
            session, "DELETE", "/rest/v1/admin_invites?id=eq.${enc(invite.id)}", null,
            module = "ADMIN_USER_MANAGEMENT", action = "REVOKE_INVITE", targetId = invite.email,
            summary = "Revoked staff invite for ${invite.email}"
        )

    suspend fun updateAdminUser(session: SupabaseAdminSession, target: AdminUser): Result<Unit> {
        val payload = JSONObject().apply {
            put("role", target.resolvedRole.name)
            put("active", target.active)
        }
        return mutateAndLog(
            session, "PATCH", "/rest/v1/admin_users?id=eq.${enc(target.id)}", payload.toString(),
            module = "ADMIN_USER_MANAGEMENT", action = "UPDATE_ROLE", targetId = target.email,
            summary = "Updated ${target.email} -> ${target.resolvedRole.displayLabel} (active=${target.active})"
        )
    }

    suspend fun savePlatformSettings(session: SupabaseAdminSession, settings: AdminSettings): Result<Unit> {
        val payload = JSONObject().apply {
            put("maintenance_mode", settings.maintenanceMode)
            put("allow_client_registrations", settings.allowClientRegistrations)
            put("allow_candidate_registrations", settings.allowCandidateRegistrations)
            put("require_cv_for_applications", settings.requireCvForApplications)
            put("default_enquiry_sla_hours", settings.defaultEnquirySlaHours.coerceIn(1L, 168L))
            put("support_email", settings.supportEmail.trim())
            put("office_regions", settings.officeRegions.trim())
        }
        return mutateAndLog(
            session, "PATCH", "/rest/v1/platform_settings?id=eq.platform", payload.toString(),
            module = "SETTINGS", action = "UPDATE", targetId = "platform",
            summary = "Updated platform settings"
        )
    }

    // =========================================================================
    // INTERNAL HTTP HELPERS
    // =========================================================================

    private suspend fun <T> getList(
        path: String,
        token: String,
        mapper: (JSONObject) -> T
    ): Result<List<T>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        try {
            val req = Request.Builder()
                .url("${baseUrl()}$path")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .get()
                .build()
            httpClient.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    Result.failure(IllegalStateException(extractError(raw, "Query failed (${res.code}).")))
                } else {
                    val arr = if (raw.isBlank()) JSONArray() else JSONArray(raw)
                    Result.success(List(arr.length()) { idx -> mapper(arr.getJSONObject(idx)) })
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun mutateAndLog(
        session: SupabaseAdminSession,
        method: String,
        path: String,
        body: String?,
        prefer: String = "return=minimal",
        module: String,
        action: String,
        targetId: String,
        summary: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Admin backend is not configured."))
        }
        val result = executeMutation(method, path, session.accessToken, body, prefer)
        if (result.isSuccess && !session.admin.resolvedRole.isReadOnly) {
            runCatching {
                val logPayload = JSONObject().apply {
                    if (session.admin.uid.isNotBlank()) put("actor_uid", session.admin.uid)
                    put("actor_email", session.admin.email.ifBlank { "admin@milahospitality.in" })
                    put("actor_role", session.admin.resolvedRole.name)
                    put("module", module)
                    put("action", action)
                    put("target_id", targetId.ifBlank { "record" })
                    put("summary", summary.take(400))
                }
                executeMutation("POST", "/rest/v1/activity_logs", session.accessToken, logPayload.toString(), "return=minimal")
            }
        }
        result
    }

    private fun executeMutation(
        method: String,
        path: String,
        accessToken: String,
        body: String?,
        prefer: String
    ): Result<Unit> {
        return try {
            val builder = Request.Builder()
                .url("${baseUrl()}$path")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", prefer)
            when (method) {
                "POST" -> builder.post((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
                "PATCH" -> builder.patch((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
                "DELETE" -> if (body != null) builder.delete(body.toRequestBody(JSON_MEDIA_TYPE)) else builder.delete()
                else -> builder.method(method, body?.toRequestBody(JSON_MEDIA_TYPE))
            }
            httpClient.newCall(builder.build()).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException(extractError(raw, "Admin operation failed (${res.code}).")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun enc(v: String): String = SupabaseConfig.encodeQueryValue(v)

    private fun JSONObject.optNullable(key: String): String? =
        optString(key).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.optStringArray(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i).trim()
                if (v.isNotEmpty()) add(v)
            }
        }
    }

    private fun extractError(raw: String, fallback: String): String = try {
        val json = JSONObject(raw)
        json.optString("message").takeIf { it.isNotBlank() }
            ?: json.optString("hint").takeIf { it.isNotBlank() }
            ?: json.optString("error_description").takeIf { it.isNotBlank() }
            ?: fallback
    } catch (_: Exception) {
        fallback
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
