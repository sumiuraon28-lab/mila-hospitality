package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Strongly-typed Supabase PostgREST & Server-Side RPC client for MILA Hospitality Consultant.
 *
 * All authenticated queries send the caller's Supabase user JWT (`Authorization: Bearer <token>`)
 * alongside the publishable anon key so PostgreSQL Row Level Security enforces per-user isolation
 * and server-side RBAC on every request.
 */
class SupabaseDatabaseClient(
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
    // 1. USER PROFILES & CLIENT PROFILES
    // =========================================================================

    suspend fun upsertUserProfile(
        accessToken: String,
        userId: String,
        email: String,
        fullName: String,
        mobileNumber: String,
        accountType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        val safeType = if (accountType == "CANDIDATE") "CANDIDATE" else "CLIENT"
        val payload = JSONObject().apply {
            put("id", userId)
            put("email", email.trim().lowercase())
            put("full_name", fullName.trim())
            put("mobile_number", mobileNumber.trim())
            put("account_type", safeType)
        }
        executeMutation(
            method = "POST",
            path = "/rest/v1/user_profiles?on_conflict=id",
            accessToken = accessToken,
            body = payload.toString(),
            prefer = "resolution=merge-duplicates,return=minimal"
        )
    }

    suspend fun upsertClientProfile(
        accessToken: String,
        userId: String,
        profile: ClientProfile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("full_name", profile.fullName.trim())
            put("email", profile.email.trim().lowercase())
            put("mobile_number", profile.mobileNumber.trim())
            put("company_name", profile.companyName.trim())
            put("designation", profile.designation.trim())
            put("city", profile.city.trim())
            put("business_category", profile.resolvedBusinessCategory.name)
        }
        executeMutation(
            method = "POST",
            path = "/rest/v1/client_profiles?on_conflict=user_id",
            accessToken = accessToken,
            body = payload.toString(),
            prefer = "resolution=merge-duplicates,return=minimal"
        )
    }

    suspend fun fetchClientProfile(
        accessToken: String,
        userId: String
    ): Result<ClientProfile?> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(null)
        val encoded = SupabaseConfig.encodeQueryValue(userId)
        executeGetArray("/rest/v1/client_profiles?user_id=eq.$encoded&limit=1", accessToken)
            .map { array ->
                if (array.length() == 0) null else parseClientProfile(array.getJSONObject(0))
            }
    }

    // =========================================================================
    // 2. SERVICE ENQUIRIES (CLIENT SELF-SCOPED VIA RLS)
    // =========================================================================

    suspend fun createServiceEnquiry(
        accessToken: String,
        userId: String,
        inquiry: ConsultationInquiry
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.failure(IllegalStateException("Supabase not configured."))
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("service_type", inquiry.resolvedServiceType.name)
            put("property_name", inquiry.propertyName.trim())
            put("city", inquiry.city.trim())
            put("scale_summary", inquiry.scaleSummary.trim())
            put("contact_name", inquiry.contactName.trim())
            put("contact_email", inquiry.contactEmail.trim())
            put("contact_phone", inquiry.contactPhone.trim())
            put("brief_notes", inquiry.briefNotes?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
            put("status", InquiryStatus.SUBMITTED.name)
        }
        try {
            val req = Request.Builder()
                .url("${baseUrl()}/rest/v1/service_enquiries")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(extractError(raw, "Enquiry submission failed (${res.code}).")))
                }
                val arr = JSONArray(raw)
                val refCode = if (arr.length() > 0) {
                    arr.getJSONObject(0).optString("reference_code").ifBlank { arr.getJSONObject(0).optString("id") }
                } else {
                    "MILA-CONFIRMED"
                }
                Result.success(refCode)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchClientEnquiries(
        accessToken: String,
        userId: String
    ): Result<List<ConsultationInquiry>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        val encoded = SupabaseConfig.encodeQueryValue(userId)
        executeGetArray(
            "/rest/v1/service_enquiries?user_id=eq.$encoded&order=created_at.desc",
            accessToken
        ).map { arr ->
            List(arr.length()) { idx -> parseConsultationInquiry(arr.getJSONObject(idx)) }
        }
    }

    suspend fun updateClientEnquiry(
        accessToken: String,
        inquiryId: String,
        briefNotes: String? = null,
        status: InquiryStatus? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        val payload = JSONObject().apply {
            if (briefNotes != null) put("brief_notes", briefNotes.trim())
            if (status != null) put("status", status.name)
        }
        val encoded = SupabaseConfig.encodeQueryValue(inquiryId)
        executeMutation(
            method = "PATCH",
            path = "/rest/v1/service_enquiries?id=eq.$encoded",
            accessToken = accessToken,
            body = payload.toString()
        )
    }

    suspend fun deleteClientEnquiry(
        accessToken: String,
        inquiryId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        val encoded = SupabaseConfig.encodeQueryValue(inquiryId)
        executeMutation(
            method = "DELETE",
            path = "/rest/v1/service_enquiries?id=eq.$encoded",
            accessToken = accessToken,
            body = null
        )
    }

    // =========================================================================
    // 3. CANDIDATE PROFILES, PREFERENCES, APPLICATIONS, SAVED JOBS & ALERTS
    // =========================================================================

    suspend fun upsertCandidateProfileAndPreferences(
        accessToken: String,
        userId: String,
        profile: CandidateProfile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        val safeCvPath = profile.cvStoragePath?.takeIf { SupabaseConfig.isSafePrivateCvPath(it) }
        val profileJson = JSONObject().apply {
            put("user_id", userId)
            put("full_name", profile.fullName.trim())
            put("email", profile.email.trim().lowercase())
            put("mobile_number", profile.mobileNumber.trim())
            put("location", profile.location.trim())
            put("current_designation", profile.currentDesignation.trim())
            put("department", profile.resolvedDepartment.name)
            put("total_experience_years", profile.totalExperienceYears.coerceIn(0L, 60L))
            put("current_employer", profile.currentEmployer.trim())
            put("previous_employers", profile.previousEmployers.trim())
            put("education", profile.education.trim())
            put("skills", profile.skills.trim())
            put("preferred_job_locations", profile.preferredJobLocations.trim())
            put("expected_salary", profile.expectedSalary.trim())
            put("availability", profile.resolvedAvailability.name)
            put("notice_period", profile.noticePeriod.trim())
            put("job_alerts_enabled", profile.jobAlertsEnabled)
            put("cv_file_name", profile.cvFileName ?: JSONObject.NULL)
            put("cv_mime_type", profile.cvMimeType ?: JSONObject.NULL)
            put("cv_file_size_bytes", profile.cvFileSizeBytes ?: JSONObject.NULL)
            put("cv_storage_path", safeCvPath ?: JSONObject.NULL)
        }
        val profileResult = executeMutation(
            method = "POST",
            path = "/rest/v1/candidate_profiles?on_conflict=user_id",
            accessToken = accessToken,
            body = profileJson.toString(),
            prefer = "resolution=merge-duplicates,return=minimal"
        )
        if (profileResult.isFailure) return@withContext profileResult

        val prefLocations = profile.preferredJobLocations
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val prefJson = JSONObject().apply {
            put("user_id", userId)
            put("job_alerts_enabled", profile.jobAlertsEnabled)
            put("preferred_locations", JSONArray(prefLocations))
            put("preferred_departments", JSONArray(listOf(profile.resolvedDepartment.name)))
            put("min_expected_salary", profile.expectedSalary.trim())
            put("availability", profile.resolvedAvailability.name)
            put("notice_period", profile.noticePeriod.trim())
            put("email_alerts", profile.jobAlertsEnabled)
            put("push_alerts", profile.jobAlertsEnabled)
        }
        executeMutation(
            method = "POST",
            path = "/rest/v1/candidate_preferences?on_conflict=user_id",
            accessToken = accessToken,
            body = prefJson.toString(),
            prefer = "resolution=merge-duplicates,return=minimal"
        )
    }

    suspend fun fetchCandidateProfile(
        accessToken: String,
        userId: String
    ): Result<CandidateProfile?> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(null)
        val encoded = SupabaseConfig.encodeQueryValue(userId)
        executeGetArray("/rest/v1/candidate_profiles?user_id=eq.$encoded&limit=1", accessToken)
            .map { arr -> if (arr.length() == 0) null else parseCandidateProfile(arr.getJSONObject(0)) }
    }

    suspend fun fetchCandidateApplications(
        accessToken: String,
        userId: String
    ): Result<List<JobApplication>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        val encoded = SupabaseConfig.encodeQueryValue(userId)
        executeGetArray("/rest/v1/job_applications?user_id=eq.$encoded&order=created_at.desc", accessToken)
            .map { arr -> List(arr.length()) { parseJobApplication(arr.getJSONObject(it)) } }
    }

    suspend fun withdrawCandidateApplication(
        accessToken: String,
        applicationId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        val encoded = SupabaseConfig.encodeQueryValue(applicationId)
        val payload = JSONObject().apply { put("status", ApplicationStatus.WITHDRAWN.name) }
        executeMutation(
            method = "PATCH",
            path = "/rest/v1/job_applications?id=eq.$encoded",
            accessToken = accessToken,
            body = payload.toString()
        )
    }

    suspend fun fetchSavedJobs(
        accessToken: String,
        userId: String
    ): Result<List<SavedRole>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        val encoded = SupabaseConfig.encodeQueryValue(userId)
        executeGetArray("/rest/v1/saved_jobs?user_id=eq.$encoded&order=saved_at.desc", accessToken)
            .map { arr ->
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    SavedRole(
                        id = o.optString("id"),
                        userId = o.optString("user_id"),
                        roleId = o.optString("role_id"),
                        roleTitle = o.optString("role_title"),
                        location = o.optString("location"),
                        compensation = o.optString("compensation")
                    )
                }
            }
    }

    suspend fun toggleSavedJob(
        accessToken: String,
        userId: String,
        role: HospitalityRole,
        currentlySaved: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        if (currentlySaved) {
            val u = SupabaseConfig.encodeQueryValue(userId)
            val r = SupabaseConfig.encodeQueryValue(role.id)
            executeMutation(
                method = "DELETE",
                path = "/rest/v1/saved_jobs?user_id=eq.$u&role_id=eq.$r",
                accessToken = accessToken,
                body = null
            )
        } else {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("role_id", role.id)
                put("role_title", role.title)
                put("location", role.location)
                put("compensation", role.compensation)
            }
            executeMutation(
                method = "POST",
                path = "/rest/v1/saved_jobs?on_conflict=user_id,role_id",
                accessToken = accessToken,
                body = payload.toString(),
                prefer = "resolution=merge-duplicates,return=minimal"
            )
        }
    }

    suspend fun fetchNotifications(
        accessToken: String,
        userId: String
    ): Result<List<JobNotification>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        val encoded = SupabaseConfig.encodeQueryValue(userId)
        executeGetArray("/rest/v1/notifications?user_id=eq.$encoded&order=created_at.desc", accessToken)
            .map { arr ->
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
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
            }
    }

    suspend fun markNotificationRead(
        accessToken: String,
        notificationId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(Unit)
        val encoded = SupabaseConfig.encodeQueryValue(notificationId)
        executeMutation(
            method = "PATCH",
            path = "/rest/v1/notifications?id=eq.$encoded",
            accessToken = accessToken,
            body = JSONObject().put("is_read", true).toString()
        )
    }

    // =========================================================================
    // 4. PUBLIC PUBLISHED CMS CATALOG (SERVICES, VACANCIES, PORTFOLIO, CONTENT)
    // =========================================================================

    suspend fun fetchPublishedServices(): Result<List<ConsultancyService>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        executeGetArray("/rest/v1/services?published=eq.true&order=sort_order.asc", null)
            .map { arr ->
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    val offering = ServiceOffering(
                        id = o.optString("id"),
                        serviceType = o.optString("service_type"),
                        sortOrder = o.optLong("sort_order", (idx + 1).toLong()),
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
                        imageUrl = o.optString("image_url").takeIf { it.isNotBlank() && it != "null" },
                        published = o.optBoolean("published", true)
                    )
                    val resolvedType = offering.resolvedServiceType
                    val fallback = MilaCatalog.services.firstOrNull { it.type == resolvedType }
                        ?: MilaCatalog.services.first()
                    val resolvedImageKey = offering.imageKey.ifBlank { fallback.imageKey }
                    ConsultancyService(
                        id = offering.id.ifBlank { fallback.id },
                        sortOrder = offering.sortOrder.toInt().coerceIn(1, 100),
                        type = resolvedType,
                        slug = offering.slug.ifBlank { fallback.slug },
                        divisionName = offering.divisionName.ifBlank { fallback.divisionName },
                        title = offering.title.ifBlank { fallback.title },
                        subtitle = offering.subtitle.ifBlank { fallback.subtitle },
                        summary = offering.summary.ifBlank { fallback.summary },
                        detailedDescription = offering.detailedDescription.ifBlank { fallback.detailedDescription },
                        servicesIncluded = offering.servicesIncluded.ifEmpty { fallback.servicesIncluded },
                        ownerBenefits = offering.ownerBenefits.ifEmpty { fallback.ownerBenefits },
                        deliverables = offering.deliverables.ifEmpty { fallback.deliverables },
                        typicalTimeline = offering.typicalTimeline.ifBlank { fallback.typicalTimeline },
                        imageKey = resolvedImageKey,
                        imageRes = ServiceCatalogRepository.resolveDrawableForKey(resolvedImageKey),
                        imageUrl = offering.imageUrl
                    )
                }
            }
    }

    suspend fun fetchPublishedVacancies(): Result<List<HospitalityRole>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        executeGetArray("/rest/v1/job_vacancies?published=eq.true&order=created_at.desc", null)
            .map { arr ->
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    JobVacancyRecord(
                        id = o.optString("slug").ifBlank { o.optString("id") },
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
                    ).toHospitalityRole()
                }
            }
    }

    suspend fun fetchPublishedPortfolio(): Result<List<CaseStudy>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        executeGetArray("/rest/v1/portfolio_projects?published=eq.true&order=sort_order.asc", null)
            .map { arr ->
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    PortfolioItemRecord(
                        id = o.optString("slug").ifBlank { o.optString("id") },
                        propertyName = o.optString("property_name"),
                        location = o.optString("location"),
                        category = o.optString("category"),
                        filterGroup = o.optString("filter_group", "Pre-Opening"),
                        scale = o.optString("scale"),
                        summary = o.optString("summary"),
                        outcomes = o.optStringArray("outcomes"),
                        imageKey = o.optString("image_key", "palace_resort"),
                        imageUrl = o.optString("image_url").takeIf { it.isNotBlank() && it != "null" },
                        sortOrder = o.optLong("sort_order", (idx + 1).toLong()),
                        published = o.optBoolean("published", true)
                    ).toCaseStudy()
                }
            }
    }

    suspend fun fetchPublishedTestimonials(): Result<List<Testimonial>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        executeGetArray("/rest/v1/testimonials?published=eq.true&order=sort_order.asc", null)
            .map { arr ->
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    Testimonial(
                        id = o.optString("id"),
                        quote = o.optString("quote"),
                        attributionRole = o.optString("attribution_role"),
                        engagementContext = o.optString("engagement_context")
                    )
                }
            }
    }

    suspend fun fetchPublishedWebsiteContent(): Result<List<SitePageContent>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(emptyList())
        executeGetArray("/rest/v1/website_content?published=eq.true&order=page_key.asc", null)
            .map { arr ->
                List(arr.length()) { idx ->
                    val o = arr.getJSONObject(idx)
                    SitePageContent(
                        id = o.optString("id"),
                        pageKey = o.optString("page_key"),
                        pageTitle = o.optString("page_title"),
                        eyebrow = o.optString("eyebrow"),
                        headline = o.optString("headline"),
                        subheadline = o.optString("subheadline"),
                        bodyCopy = o.optString("body_copy"),
                        heroImageKey = o.optString("hero_image_key", "hero_hotel"),
                        heroImageUrl = o.optString("hero_image_url").takeIf { it.isNotBlank() && it != "null" },
                        ctaLabel = o.optString("cta_label", "Explore Services"),
                        published = o.optBoolean("published", true)
                    )
                }
            }
    }

    // =========================================================================
    // 5. SERVER-SIDE ADMIN ROLE VERIFICATION (POST /rest/v1/rpc/verify_admin_session)
    // =========================================================================

    suspend fun verifyAdminSessionServerSide(accessToken: String): Result<AdminUser?> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Result.success(null)
        try {
            val req = Request.Builder()
                .url("${baseUrl()}/rest/v1/rpc/verify_admin_session")
                .addHeader("apikey", anonKey())
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(extractError(raw, "Admin verification failed (${res.code}).")))
                }
                val json = JSONObject(raw)
                if (!json.optBoolean("authorized", false)) {
                    return@withContext Result.success(null)
                }
                val resolved = AdminUser(
                    id = json.optString("id"),
                    uid = json.optString("uid"),
                    email = json.optString("email"),
                    displayName = json.optString("display_name", "MILA Admin"),
                    role = json.optString("role", AdminRole.REPORTING_ADMIN.name),
                    active = json.optBoolean("active", true),
                    assignedDepartment = json.optString("assigned_department").takeIf { it.isNotBlank() && it != "null" }
                )
                Result.success(if (resolved.active) resolved else null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // INTERNAL HTTP HELPERS
    // =========================================================================

    private fun executeGetArray(path: String, accessToken: String?): Result<JSONArray> {
        return try {
            val builder = Request.Builder()
                .url("${baseUrl()}$path")
                .addHeader("apikey", anonKey())
                .addHeader("Accept", "application/json")
                .get()
            if (!accessToken.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $accessToken")
            }
            httpClient.newCall(builder.build()).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    Result.failure(IllegalStateException(extractError(raw, "Supabase query failed (${res.code}).")))
                } else {
                    Result.success(if (raw.isBlank()) JSONArray() else JSONArray(raw))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeMutation(
        method: String,
        path: String,
        accessToken: String,
        body: String?,
        prefer: String = "return=minimal"
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
                else Result.failure(IllegalStateException(extractError(raw, "Supabase mutation failed (${res.code}).")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractError(raw: String, fallback: String): String {
        return try {
            val json = JSONObject(raw)
            json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("hint").takeIf { it.isNotBlank() }
                ?: json.optString("error_description").takeIf { it.isNotBlank() }
                ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun JSONObject.optStringArray(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i).trim()
                if (v.isNotEmpty()) add(v)
            }
        }
    }

    private fun parseClientProfile(o: JSONObject): ClientProfile = ClientProfile(
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

    private fun parseConsultationInquiry(o: JSONObject): ConsultationInquiry = ConsultationInquiry(
        id = o.optString("id"),
        userId = o.optString("user_id"),
        propertyName = o.optString("property_name"),
        city = o.optString("city"),
        serviceType = o.optString("service_type", ConsultationServiceType.HOSPITALITY_PRE_OPENING.name),
        scaleSummary = o.optString("scale_summary"),
        contactName = o.optString("contact_name"),
        contactEmail = o.optString("contact_email"),
        contactPhone = o.optString("contact_phone"),
        briefNotes = o.optString("brief_notes").takeIf { it.isNotBlank() && it != "null" },
        status = o.optString("status", InquiryStatus.SUBMITTED.name),
        assignedConsultantUid = o.optString("assigned_consultant_id").takeIf { it.isNotBlank() && it != "null" },
        assignedConsultantEmail = o.optString("assigned_consultant_email").takeIf { it.isNotBlank() && it != "null" },
        consultantNotes = o.optString("consultant_notes").takeIf { it.isNotBlank() && it != "null" }
    )

    private fun parseCandidateProfile(o: JSONObject): CandidateProfile = CandidateProfile(
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
        cvFileName = o.optString("cv_file_name").takeIf { it.isNotBlank() && it != "null" },
        cvMimeType = o.optString("cv_mime_type").takeIf { it.isNotBlank() && it != "null" },
        cvFileSizeBytes = if (o.isNull("cv_file_size_bytes")) null else o.optLong("cv_file_size_bytes"),
        cvStoragePath = o.optString("cv_storage_path").takeIf { SupabaseConfig.isSafePrivateCvPath(it) }
    )

    private fun parseJobApplication(o: JSONObject): JobApplication = JobApplication(
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
        coverNote = o.optString("cover_note").takeIf { it.isNotBlank() && it != "null" },
        cvFileName = o.optString("cv_file_name").takeIf { it.isNotBlank() && it != "null" },
        status = o.optString("status", ApplicationStatus.APPLIED.name)
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
