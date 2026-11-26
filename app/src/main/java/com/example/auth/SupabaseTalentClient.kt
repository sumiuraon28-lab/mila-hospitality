package com.example.auth

import com.example.BuildConfig
import com.example.data.CandidateAvailability
import com.example.data.CandidateProfile
import com.example.data.HospitalityDepartment
import com.example.data.HospitalityRole
import com.example.data.JobApplication
import com.example.data.JobNotification
import com.example.data.SavedRole
import com.example.data.SupabaseConfig
import com.example.data.SupabaseDatabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CandidateRegistrationRequest(
    val fullName: String,
    val email: String,
    val mobileNumber: String,
    val location: String,
    val currentDesignation: String,
    val department: HospitalityDepartment,
    val totalExperienceYears: Long,
    val currentEmployer: String,
    val previousEmployers: String,
    val education: String,
    val skills: String,
    val preferredJobLocations: String,
    val expectedSalary: String,
    val availability: CandidateAvailability,
    val noticePeriod: String,
    val password: String
)

data class SupabaseCandidateSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val profile: CandidateProfile
)

sealed interface CandidateAuthOutcome {
    data class Authenticated(val session: SupabaseCandidateSession, val message: String) : CandidateAuthOutcome
    data class PendingEmailVerification(val email: String, val message: String, val draftProfile: CandidateProfile) : CandidateAuthOutcome
    data class RequiresGoogleSyncOrSecrets(val message: String, val draftProfile: CandidateProfile? = null) : CandidateAuthOutcome
    data class Failure(val message: String) : CandidateAuthOutcome
}

data class PrivateCvMetadata(
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val storagePath: String,
    val uploadedAtMillis: Long
)

object CandidateValidator {
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    const val MAX_CV_SIZE_BYTES = 10L * 1024L * 1024L
    const val PRIVATE_CV_BUCKET = "candidate-cvs"

    val ALLOWED_CV_MIME_TYPES = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )

    private val allowedExtensions = setOf("pdf", "doc", "docx")

    fun validateRegistration(request: CandidateRegistrationRequest): String? {
        validateProfileFields(request.toCandidateProfile())?.let { return it }
        return when {
            request.password.length < 8 ->
                "Password must be at least 8 characters."
            !request.password.any { it.isDigit() } || !request.password.any { it.isLetter() } ->
                "Password must include both letters and numbers."
            else -> null
        }
    }

    fun validateProfileFields(profile: CandidateProfile): String? {
        return when {
            profile.fullName.trim().length < 2 -> "Please enter your full name."
            !emailRegex.matches(profile.email.trim()) -> "Please enter a valid email address."
            profile.mobileNumber.trim().length < 7 -> "Please enter a valid mobile number."
            profile.location.trim().length < 2 -> "Please enter your current location."
            profile.currentDesignation.trim().length < 2 -> "Please enter your current designation."
            profile.totalExperienceYears !in 0L..60L -> "Total experience must be between 0 and 60 years."
            profile.currentEmployer.trim().isEmpty() -> "Please enter your current or most recent employer."
            profile.education.trim().length < 2 -> "Please enter your education background."
            profile.skills.trim().length < 2 -> "Please list your key hospitality skills."
            profile.preferredJobLocations.trim().length < 2 -> "Please enter preferred job locations."
            profile.expectedSalary.trim().length < 2 -> "Please specify your expected salary range."
            profile.noticePeriod.trim().length < 2 -> "Please select or enter your notice period."
            else -> null
        }
    }

    fun validateCvFile(fileName: String, mimeType: String?, sizeBytes: Long): String? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mimeValid = mimeType != null && mimeType in ALLOWED_CV_MIME_TYPES
        if (!mimeValid && ext !in allowedExtensions) {
            return "Only PDF, DOC, or DOCX files are accepted for CVs."
        }
        if (sizeBytes <= 0L) {
            return "The selected CV file appears empty."
        }
        if (sizeBytes > MAX_CV_SIZE_BYTES) {
            return "CV file size must not exceed 10 MB."
        }
        return null
    }

    fun sanitizeCvFileName(rawName: String): String {
        val cleaned = rawName.trim().replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
        return cleaned.ifBlank { "candidate_cv.pdf" }.take(120)
    }

    fun buildPrivateStoragePath(userId: String, fileName: String, timestampMillis: Long): String {
        val safeUid = userId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val safeName = sanitizeCvFileName(fileName)
        return "$safeUid/cv_${timestampMillis}_$safeName"
    }

    fun inferMimeType(fileName: String, providedMimeType: String?): String {
        if (providedMimeType in ALLOWED_CV_MIME_TYPES) return providedMimeType!!
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/pdf"
        }
    }

    fun CandidateRegistrationRequest.toCandidateProfile(): CandidateProfile = CandidateProfile(
        fullName = fullName.trim(),
        email = email.trim(),
        mobileNumber = mobileNumber.trim(),
        location = location.trim(),
        currentDesignation = currentDesignation.trim(),
        department = department.name,
        totalExperienceYears = totalExperienceYears,
        currentEmployer = currentEmployer.trim(),
        previousEmployers = previousEmployers.trim(),
        education = education.trim(),
        skills = skills.trim(),
        preferredJobLocations = preferredJobLocations.trim(),
        expectedSalary = expectedSalary.trim(),
        availability = availability.name,
        noticePeriod = noticePeriod.trim(),
        jobAlertsEnabled = true
    )
}

class SupabaseTalentClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    private val dbClient: SupabaseDatabaseClient = SupabaseDatabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseAnonKey = supabaseAnonKey,
        httpClient = httpClient
    )

    private val _session = MutableStateFlow<SupabaseCandidateSession?>(null)
    val session: StateFlow<SupabaseCandidateSession?> = _session.asStateFlow()

    private val _pendingDraftCandidate = MutableStateFlow<CandidateProfile?>(null)
    val pendingDraftCandidate: StateFlow<CandidateProfile?> = _pendingDraftCandidate.asStateFlow()

    private val _myApplications = MutableStateFlow<List<JobApplication>>(emptyList())
    val myApplications: StateFlow<List<JobApplication>> = _myApplications.asStateFlow()

    private val _mySavedRoles = MutableStateFlow<List<SavedRole>>(emptyList())
    val mySavedRoles: StateFlow<List<SavedRole>> = _mySavedRoles.asStateFlow()

    private val _myNotifications = MutableStateFlow<List<JobNotification>>(emptyList())
    val myNotifications: StateFlow<List<JobNotification>> = _myNotifications.asStateFlow()

    fun isConfigured(): Boolean = SupabaseConfig.isConfigured(supabaseUrl, supabaseAnonKey)

    fun clearPendingDraftCandidate() {
        _pendingDraftCandidate.value = null
    }

    suspend fun signUpCandidate(request: CandidateRegistrationRequest): CandidateAuthOutcome {
        CandidateValidator.validateRegistration(request)?.let { err ->
            return CandidateAuthOutcome.Failure(err)
        }
        val profile = with(CandidateValidator) { request.toCandidateProfile() }

        if (!isConfigured()) {
            _pendingDraftCandidate.value = profile
            return CandidateAuthOutcome.RequiresGoogleSyncOrSecrets(
                message = "Candidate profile validated and staged. Continue with Google below to activate your MILA Talent account immediately, or add SUPABASE_URL and SUPABASE_ANON_KEY in Secrets.",
                draftProfile = profile
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("email", profile.email)
                    put("password", request.password)
                    put("data", profileToMetadataJson(profile))
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/signup")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful) {
                        return@withContext CandidateAuthOutcome.Failure(
                            extractErrorMessage(json, "Candidate registration failed (HTTP ${response.code}).")
                        )
                    }
                    val accessToken = json?.optString("access_token").orEmpty()
                    if (accessToken.isNotBlank()) {
                        val parsed = parseCandidateSession(json!!, profile)
                        _session.value = parsed
                        _pendingDraftCandidate.value = parsed.profile
                        dbClient.upsertUserProfile(
                            accessToken = parsed.accessToken,
                            userId = parsed.userId,
                            email = parsed.email,
                            fullName = parsed.profile.fullName,
                            mobileNumber = parsed.profile.mobileNumber,
                            accountType = "CANDIDATE"
                        )
                        dbClient.upsertCandidateProfileAndPreferences(parsed.accessToken, parsed.userId, parsed.profile)
                        refreshCandidatePortalData()
                        CandidateAuthOutcome.Authenticated(
                            session = parsed,
                            message = "Welcome to MILA Talent, ${profile.fullName}."
                        )
                    } else {
                        _pendingDraftCandidate.value = profile
                        CandidateAuthOutcome.PendingEmailVerification(
                            email = profile.email,
                            message = "Verification link sent to ${profile.email}. Please verify your email before logging in.",
                            draftProfile = profile
                        )
                    }
                }
            } catch (e: Exception) {
                CandidateAuthOutcome.Failure(e.localizedMessage ?: "Network error while creating candidate account.")
            }
        }
    }

    suspend fun signInCandidate(email: String, password: String): CandidateAuthOutcome {
        SupabaseAuthValidator.validateLogin(email, password)?.let { err ->
            return CandidateAuthOutcome.Failure(err)
        }
        if (!isConfigured()) {
            return CandidateAuthOutcome.RequiresGoogleSyncOrSecrets(
                message = "Supabase credentials are not configured yet. Continue with Google below for instant access to your MILA Talent profile."
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("email", email.trim())
                    put("password", password)
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/token?grant_type=password")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful || json == null) {
                        return@withContext CandidateAuthOutcome.Failure(
                            extractErrorMessage(json, "Invalid email or password.")
                        )
                    }
                    var parsed = parseCandidateSession(json, null)
                    val remoteProfile = dbClient.fetchCandidateProfile(parsed.accessToken, parsed.userId).getOrNull()
                    if (remoteProfile != null) {
                        parsed = parsed.copy(profile = remoteProfile)
                    }
                    _session.value = parsed
                    _pendingDraftCandidate.value = parsed.profile
                    refreshCandidatePortalData()
                    CandidateAuthOutcome.Authenticated(
                        session = parsed,
                        message = "Signed in to MILA Talent as ${parsed.profile.fullName.ifBlank { parsed.email }}."
                    )
                }
            } catch (e: Exception) {
                CandidateAuthOutcome.Failure(e.localizedMessage ?: "Unable to connect to MILA Talent authentication.")
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Result<String> {
        SupabaseAuthValidator.validateResetEmail(email)?.let { err ->
            return Result.failure(IllegalArgumentException(err))
        }
        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException("Password reset emails activate once SUPABASE_URL and SUPABASE_ANON_KEY are added in Secrets.")
            )
        }
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply { put("email", email.trim()) }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/recover")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(httpRequest).execute().use { response ->
                    val json = parseJsonOrNull(response.body?.string().orEmpty())
                    if (!response.isSuccessful) {
                        Result.failure(IllegalStateException(extractErrorMessage(json, "Reset email failed.")))
                    } else {
                        Result.success("Password reset link dispatched to ${email.trim()}.")
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun syncCandidateProfile(profile: CandidateProfile): Result<Unit> {
        val current = _session.value ?: return Result.success(Unit)
        if (!isConfigured()) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            dbClient.upsertUserProfile(
                accessToken = current.accessToken,
                userId = current.userId,
                email = profile.email,
                fullName = profile.fullName,
                mobileNumber = profile.mobileNumber,
                accountType = "CANDIDATE"
            )
            dbClient.upsertCandidateProfileAndPreferences(current.accessToken, current.userId, profile).also { result ->
                if (result.isSuccess) {
                    _session.value = current.copy(email = profile.email, profile = profile)
                }
            }
        }
    }

    suspend fun refreshCandidatePortalData() {
        val current = _session.value ?: return
        if (!isConfigured()) return
        dbClient.fetchCandidateApplications(current.accessToken, current.userId).onSuccess {
            _myApplications.value = it
        }
        dbClient.fetchSavedJobs(current.accessToken, current.userId).onSuccess {
            _mySavedRoles.value = it
        }
        dbClient.fetchNotifications(current.accessToken, current.userId).onSuccess {
            _myNotifications.value = it
        }
    }

    suspend fun toggleSavedJob(role: HospitalityRole, currentlySaved: Boolean): Result<Unit> {
        val current = _session.value ?: return Result.failure(IllegalStateException("Sign in required."))
        return dbClient.toggleSavedJob(current.accessToken, current.userId, role, currentlySaved).also {
            if (it.isSuccess) refreshCandidatePortalData()
        }
    }

    suspend fun withdrawApplication(applicationId: String): Result<Unit> {
        val current = _session.value ?: return Result.failure(IllegalStateException("Sign in required."))
        return dbClient.withdrawCandidateApplication(current.accessToken, applicationId).also {
            if (it.isSuccess) refreshCandidatePortalData()
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val current = _session.value ?: return Result.failure(IllegalStateException("Sign in required."))
        return dbClient.markNotificationRead(current.accessToken, notificationId).also {
            if (it.isSuccess) refreshCandidatePortalData()
        }
    }

    suspend fun syncJobApplication(application: JobApplication): Result<Unit> {
        val current = _session.value ?: return Result.success(Unit)
        if (!isConfigured()) return Result.success(Unit)
        val safeCvPath = current.profile.cvStoragePath?.takeIf { SupabaseConfig.isSafePrivateCvPath(it) }
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("user_id", current.userId)
                    put("role_id", application.roleId)
                    put("role_title", application.roleTitle)
                    put("location", application.location)
                    put("candidate_name", application.candidateName)
                    put("candidate_email", application.candidateEmail)
                    put("candidate_phone", application.candidatePhone)
                    put("years_experience", application.yearsExperience)
                    put("current_property", application.currentProperty)
                    put("cover_note", application.coverNote ?: JSONObject.NULL)
                    put("cv_storage_path", safeCvPath ?: JSONObject.NULL)
                    put("cv_file_name", application.cvFileName ?: JSONObject.NULL)
                    put("status", application.status)
                }
                val req = Request.Builder()
                    .url("${normalizedBaseUrl()}/rest/v1/job_applications?on_conflict=user_id,role_id")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Authorization", "Bearer ${current.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        refreshCandidatePortalData()
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalStateException("Supabase application sync failed (${res.code})"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun syncJobNotification(notification: JobNotification): Result<Unit> {
        val current = _session.value ?: return Result.success(Unit)
        if (!isConfigured()) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("user_id", current.userId)
                    put("role_id", notification.roleId)
                    put("title", notification.title)
                    put("message", notification.message)
                    put("department", notification.department)
                    put("location", notification.location)
                    put("is_read", notification.isRead)
                }
                val req = Request.Builder()
                    .url("${normalizedBaseUrl()}/rest/v1/notifications")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Authorization", "Bearer ${current.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        refreshCandidatePortalData()
                        Result.success(Unit)
                    } else {
                        Result.failure(IllegalStateException("Supabase notification sync failed (${res.code})"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uploadOrReplacePrivateCv(
        userId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        previousStoragePath: String?
    ): Result<PrivateCvMetadata> {
        CandidateValidator.validateCvFile(fileName, mimeType, bytes.size.toLong())?.let { err ->
            return Result.failure(IllegalArgumentException(err))
        }
        val now = System.currentTimeMillis()
        val sanitized = CandidateValidator.sanitizeCvFileName(fileName)
        val storagePath = CandidateValidator.buildPrivateStoragePath(userId, sanitized, now)
        val resolvedMime = CandidateValidator.inferMimeType(sanitized, mimeType)

        val currentSession = _session.value
        if (!isConfigured() || currentSession == null) {
            return Result.success(
                PrivateCvMetadata(
                    fileName = sanitized,
                    mimeType = resolvedMime,
                    fileSizeBytes = bytes.size.toLong(),
                    storagePath = storagePath,
                    uploadedAtMillis = now
                )
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                if (!previousStoragePath.isNullOrBlank()) {
                    deletePrivateCvObjects(currentSession.accessToken, listOf(previousStoragePath))
                }
                val uploadReq = Request.Builder()
                    .url("${normalizedBaseUrl()}/storage/v1/object/${CandidateValidator.PRIVATE_CV_BUCKET}/$storagePath")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Authorization", "Bearer ${currentSession.accessToken}")
                    .addHeader("x-upsert", "true")
                    .post(bytes.toRequestBody(resolvedMime.toMediaType()))
                    .build()
                httpClient.newCall(uploadReq).execute().use { res ->
                    if (!res.isSuccessful) {
                        val json = parseJsonOrNull(res.body?.string().orEmpty())
                        return@withContext Result.failure(
                            IllegalStateException(extractErrorMessage(json, "Private CV upload failed (${res.code})."))
                        )
                    }
                    Result.success(
                        PrivateCvMetadata(
                            fileName = sanitized,
                            mimeType = resolvedMime,
                            fileSizeBytes = bytes.size.toLong(),
                            storagePath = storagePath,
                            uploadedAtMillis = now
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deletePrivateCv(storagePath: String?): Result<Unit> {
        val currentSession = _session.value
        if (storagePath.isNullOrBlank() || !isConfigured() || currentSession == null) {
            return Result.success(Unit)
        }
        return withContext(Dispatchers.IO) {
            deletePrivateCvObjects(currentSession.accessToken, listOf(storagePath))
        }
    }

    suspend fun requestShortLivedOwnerSignedUrl(storagePath: String, expiresInSeconds: Int = 60): Result<String> {
        val currentSession = _session.value
            ?: return Result.failure(IllegalStateException("Authenticated Supabase session required."))
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase storage is not configured yet."))
        }
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply { put("expiresIn", expiresInSeconds.coerceIn(30, 300)) }
                val req = Request.Builder()
                    .url("${normalizedBaseUrl()}/storage/v1/object/sign/${CandidateValidator.PRIVATE_CV_BUCKET}/$storagePath")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Authorization", "Bearer ${currentSession.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(req).execute().use { res ->
                    val json = parseJsonOrNull(res.body?.string().orEmpty())
                    val signedPath = json?.optString("signedURL").orEmpty()
                    if (res.isSuccessful && signedPath.isNotBlank()) {
                        Result.success("${normalizedBaseUrl()}/storage/v1$signedPath")
                    } else {
                        Result.failure(IllegalStateException(extractErrorMessage(json, "Unable to authorize private CV access.")))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signOut() {
        val activeToken = _session.value?.accessToken
        _session.value = null
        _pendingDraftCandidate.value = null
        _myApplications.value = emptyList()
        _mySavedRoles.value = emptyList()
        _myNotifications.value = emptyList()
        if (!activeToken.isNullOrBlank() && isConfigured()) {
            withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder()
                        .url("${normalizedBaseUrl()}/auth/v1/logout")
                        .addHeader("apikey", supabaseAnonKey.trim())
                        .addHeader("Authorization", "Bearer $activeToken")
                        .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                    httpClient.newCall(req).execute().close()
                } catch (_: Exception) {
                    // Best-effort token revocation; in-memory session already cleared.
                }
            }
        }
    }

    private fun deletePrivateCvObjects(accessToken: String, paths: List<String>): Result<Unit> {
        return try {
            val payload = JSONObject().apply {
                put("prefixes", JSONArray(paths))
            }
            val req = Request.Builder()
                .url("${normalizedBaseUrl()}/storage/v1/object/${CandidateValidator.PRIVATE_CV_BUCKET}")
                .addHeader("apikey", supabaseAnonKey.trim())
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .delete(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("Failed to remove private CV (${res.code})."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun profileToMetadataJson(profile: CandidateProfile): JSONObject = JSONObject().apply {
        put("account_role", "job_seeker")
        put("full_name", profile.fullName)
        put("mobile_number", profile.mobileNumber)
        put("location", profile.location)
        put("current_designation", profile.currentDesignation)
        put("department", profile.department)
        put("total_experience_years", profile.totalExperienceYears)
        put("current_employer", profile.currentEmployer)
        put("previous_employers", profile.previousEmployers)
        put("education", profile.education)
        put("skills", profile.skills)
        put("preferred_job_locations", profile.preferredJobLocations)
        put("expected_salary", profile.expectedSalary)
        put("availability", profile.availability)
        put("notice_period", profile.noticePeriod)
        put("job_alerts_enabled", profile.jobAlertsEnabled)
    }

    private fun parseCandidateSession(
        json: JSONObject,
        fallback: CandidateProfile?
    ): SupabaseCandidateSession {
        val userObj = json.optJSONObject("user")
        val metaObj = userObj?.optJSONObject("user_metadata")
        val resolvedUid = userObj?.optString("id").orEmpty()
        val resolvedEmail = userObj?.optString("email")?.takeIf { it.isNotBlank() }
            ?: fallback?.email.orEmpty()

        val profile = CandidateProfile(
            userId = resolvedUid,
            fullName = metaObj?.optString("full_name")?.takeIf { it.isNotBlank() } ?: fallback?.fullName.orEmpty(),
            email = resolvedEmail,
            mobileNumber = metaObj?.optString("mobile_number")?.takeIf { it.isNotBlank() } ?: fallback?.mobileNumber.orEmpty(),
            location = metaObj?.optString("location")?.takeIf { it.isNotBlank() } ?: fallback?.location.orEmpty(),
            currentDesignation = metaObj?.optString("current_designation")?.takeIf { it.isNotBlank() } ?: fallback?.currentDesignation.orEmpty(),
            department = metaObj?.optString("department")?.takeIf { it.isNotBlank() } ?: (fallback?.department ?: HospitalityDepartment.EXECUTIVE.name),
            totalExperienceYears = metaObj?.optLong("total_experience_years", fallback?.totalExperienceYears ?: 0L) ?: (fallback?.totalExperienceYears ?: 0L),
            currentEmployer = metaObj?.optString("current_employer")?.takeIf { it.isNotBlank() } ?: fallback?.currentEmployer.orEmpty(),
            previousEmployers = metaObj?.optString("previous_employers")?.takeIf { it.isNotBlank() } ?: fallback?.previousEmployers.orEmpty(),
            education = metaObj?.optString("education")?.takeIf { it.isNotBlank() } ?: fallback?.education.orEmpty(),
            skills = metaObj?.optString("skills")?.takeIf { it.isNotBlank() } ?: fallback?.skills.orEmpty(),
            preferredJobLocations = metaObj?.optString("preferred_job_locations")?.takeIf { it.isNotBlank() } ?: fallback?.preferredJobLocations.orEmpty(),
            expectedSalary = metaObj?.optString("expected_salary")?.takeIf { it.isNotBlank() } ?: fallback?.expectedSalary.orEmpty(),
            availability = metaObj?.optString("availability")?.takeIf { it.isNotBlank() } ?: (fallback?.availability ?: CandidateAvailability.WITHIN_30_DAYS.name),
            noticePeriod = metaObj?.optString("notice_period")?.takeIf { it.isNotBlank() } ?: (fallback?.noticePeriod ?: "30 Days"),
            jobAlertsEnabled = metaObj?.optBoolean("job_alerts_enabled", fallback?.jobAlertsEnabled ?: true) ?: true
        )

        return SupabaseCandidateSession(
            accessToken = json.optString("access_token"),
            refreshToken = json.optString("refresh_token"),
            userId = resolvedUid,
            email = resolvedEmail,
            profile = profile
        )
    }

    private fun normalizedBaseUrl(): String = supabaseUrl.trim().trimEnd('/')

    private fun parseJsonOrNull(raw: String): JSONObject? = try {
        if (raw.isBlank()) null else JSONObject(raw)
    } catch (_: Exception) {
        null
    }

    private fun extractErrorMessage(json: JSONObject?, fallback: String): String {
        if (json == null) return fallback
        return json.optString("msg").takeIf { it.isNotBlank() }
            ?: json.optString("error_description").takeIf { it.isNotBlank() }
            ?: json.optString("message").takeIf { it.isNotBlank() }
            ?: fallback
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
