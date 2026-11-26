package com.example.auth

import com.example.BuildConfig
import com.example.data.AdminUser
import com.example.data.BusinessCategory
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.EnquiryConfirmation
import com.example.data.InquiryStatus
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
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ClientRegistrationRequest(
    val fullName: String,
    val email: String,
    val mobileNumber: String,
    val companyName: String,
    val designation: String,
    val city: String,
    val businessCategory: BusinessCategory,
    val password: String
)

data class SupabaseClientMetadata(
    val fullName: String,
    val email: String,
    val mobileNumber: String,
    val companyName: String,
    val designation: String,
    val city: String,
    val businessCategory: BusinessCategory
)

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val metadata: SupabaseClientMetadata
)

sealed interface ClientAuthOutcome {
    data class Authenticated(
        val session: SupabaseSession,
        val message: String
    ) : ClientAuthOutcome

    data class PendingEmailVerification(
        val email: String,
        val message: String,
        val draftProfile: SupabaseClientMetadata
    ) : ClientAuthOutcome

    data class RequiresGoogleSyncOrSecrets(
        val message: String,
        val draftProfile: SupabaseClientMetadata? = null
    ) : ClientAuthOutcome

    data class Failure(val message: String) : ClientAuthOutcome
}

object SupabaseAuthValidator {
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateRegistration(request: ClientRegistrationRequest): String? {
        return when {
            request.fullName.trim().length < 2 ->
                "Please enter your full name (minimum 2 characters)."
            !emailRegex.matches(request.email.trim()) ->
                "Please enter a valid business email address."
            request.mobileNumber.trim().length < 7 ->
                "Please enter a valid mobile number (minimum 7 digits)."
            request.companyName.trim().length < 2 ->
                "Please enter your company, hotel, or restaurant name."
            request.designation.trim().length < 2 ->
                "Please enter your designation or role."
            request.city.trim().length < 2 ->
                "Please enter your city."
            request.password.length < 8 ->
                "Password must be at least 8 characters."
            !request.password.any { it.isDigit() } || !request.password.any { it.isLetter() } ->
                "Password must include both letters and numbers."
            else -> null
        }
    }

    fun validateLogin(email: String, password: String): String? {
        return when {
            !emailRegex.matches(email.trim()) -> "Please enter a valid email address."
            password.isEmpty() -> "Please enter your password."
            else -> null
        }
    }

    fun validateResetEmail(email: String): String? {
        return if (!emailRegex.matches(email.trim())) {
            "Please enter the email address associated with your account."
        } else {
            null
        }
    }

    fun validateProfileUpdate(profile: SupabaseClientMetadata): String? {
        return when {
            profile.fullName.trim().length < 2 -> "Full name must be at least 2 characters."
            !emailRegex.matches(profile.email.trim()) -> "Please enter a valid email address."
            profile.mobileNumber.trim().length < 7 -> "Please enter a valid mobile number."
            profile.companyName.trim().length < 2 -> "Please enter your company name."
            profile.designation.trim().length < 2 -> "Please enter your designation."
            profile.city.trim().length < 2 -> "Please enter your city."
            else -> null
        }
    }
}

class SupabaseAuthClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val dbClient: SupabaseDatabaseClient = SupabaseDatabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseAnonKey = supabaseAnonKey,
        httpClient = httpClient
    )

    private val _session = MutableStateFlow<SupabaseSession?>(null)
    val session: StateFlow<SupabaseSession?> = _session.asStateFlow()

    private val _pendingDraftProfile = MutableStateFlow<SupabaseClientMetadata?>(null)
    val pendingDraftProfile: StateFlow<SupabaseClientMetadata?> = _pendingDraftProfile.asStateFlow()

    private val _myEnquiries = MutableStateFlow<List<ConsultationInquiry>>(emptyList())
    val myEnquiries: StateFlow<List<ConsultationInquiry>> = _myEnquiries.asStateFlow()

    fun isConfigured(): Boolean = SupabaseConfig.isConfigured(supabaseUrl, supabaseAnonKey)

    fun clearPendingDraftProfile() {
        _pendingDraftProfile.value = null
    }

    suspend fun signUp(request: ClientRegistrationRequest): ClientAuthOutcome {
        SupabaseAuthValidator.validateRegistration(request)?.let { error ->
            return ClientAuthOutcome.Failure(error)
        }

        val metadata = SupabaseClientMetadata(
            fullName = request.fullName.trim(),
            email = request.email.trim(),
            mobileNumber = request.mobileNumber.trim(),
            companyName = request.companyName.trim(),
            designation = request.designation.trim(),
            city = request.city.trim(),
            businessCategory = request.businessCategory
        )

        if (!isConfigured()) {
            _pendingDraftProfile.value = metadata
            return ClientAuthOutcome.RequiresGoogleSyncOrSecrets(
                message = "Your registration profile is ready. Continue with Google below to activate your MILA client account immediately, or add SUPABASE_URL and SUPABASE_ANON_KEY in Secrets for email/password authentication.",
                draftProfile = metadata
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("email", metadata.email)
                    put("password", request.password)
                    put("data", metadata.toJson())
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
                        val msg = extractErrorMessage(json, "Registration failed (HTTP ${response.code}).")
                        return@withContext ClientAuthOutcome.Failure(msg)
                    }

                    val accessToken = json?.optString("access_token").orEmpty()
                    if (accessToken.isNotBlank()) {
                        val parsedSession = parseSession(json!!, fallbackMetadata = metadata)
                        _session.value = parsedSession
                        _pendingDraftProfile.value = metadata
                        syncClientTables(parsedSession, metadata)
                        refreshClientEnquiries()
                        ClientAuthOutcome.Authenticated(
                            session = parsedSession,
                            message = "Welcome to MILA Hospitality, ${metadata.fullName}."
                        )
                    } else {
                        _pendingDraftProfile.value = metadata
                        ClientAuthOutcome.PendingEmailVerification(
                            email = metadata.email,
                            message = "Account created for ${metadata.email}. Please confirm the verification link sent to your inbox, then log in.",
                            draftProfile = metadata
                        )
                    }
                }
            } catch (e: Exception) {
                ClientAuthOutcome.Failure(e.localizedMessage ?: "Network error while creating your account.")
            }
        }
    }

    suspend fun signInWithPassword(email: String, password: String): ClientAuthOutcome {
        SupabaseAuthValidator.validateLogin(email, password)?.let { error ->
            return ClientAuthOutcome.Failure(error)
        }

        if (!isConfigured()) {
            return ClientAuthOutcome.RequiresGoogleSyncOrSecrets(
                message = "Supabase credentials are not yet configured in Secrets. Use Continue with Google below for instant client access, or configure SUPABASE_URL and SUPABASE_ANON_KEY."
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
                        val msg = extractErrorMessage(json, "Invalid email or password.")
                        return@withContext ClientAuthOutcome.Failure(msg)
                    }
                    var parsedSession = parseSession(json, fallbackMetadata = null)
                    val remoteProfile = dbClient.fetchClientProfile(parsedSession.accessToken, parsedSession.userId).getOrNull()
                    if (remoteProfile != null) {
                        parsedSession = parsedSession.copy(
                            metadata = SupabaseClientMetadata(
                                fullName = remoteProfile.fullName,
                                email = remoteProfile.email,
                                mobileNumber = remoteProfile.mobileNumber,
                                companyName = remoteProfile.companyName,
                                designation = remoteProfile.designation,
                                city = remoteProfile.city,
                                businessCategory = remoteProfile.resolvedBusinessCategory
                            )
                        )
                    }
                    _session.value = parsedSession
                    _pendingDraftProfile.value = parsedSession.metadata
                    refreshClientEnquiries()
                    ClientAuthOutcome.Authenticated(
                        session = parsedSession,
                        message = "Signed in as ${parsedSession.metadata.fullName.ifBlank { parsedSession.email }}."
                    )
                }
            } catch (e: Exception) {
                ClientAuthOutcome.Failure(e.localizedMessage ?: "Unable to connect to authentication service.")
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Result<String> {
        SupabaseAuthValidator.validateResetEmail(email)?.let { error ->
            return Result.failure(IllegalArgumentException(error))
        }

        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException(
                    "Password reset emails activate once SUPABASE_URL and SUPABASE_ANON_KEY are added in AI Studio Secrets."
                )
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("email", email.trim())
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/recover")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful) {
                        val msg = extractErrorMessage(json, "Could not send password reset instructions.")
                        Result.failure(IllegalStateException(msg))
                    } else {
                        Result.success("Password reset link dispatched to ${email.trim()}. Check your inbox.")
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateUserProfile(metadata: SupabaseClientMetadata): Result<SupabaseSession> {
        val currentSession = _session.value
            ?: return Result.failure(IllegalStateException("No active Supabase session."))
        SupabaseAuthValidator.validateProfileUpdate(metadata)?.let { error ->
            return Result.failure(IllegalArgumentException(error))
        }
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase credentials are not configured."))
        }

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    if (metadata.email.trim() != currentSession.email) {
                        put("email", metadata.email.trim())
                    }
                    put("data", metadata.toJson())
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/user")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Authorization", "Bearer ${currentSession.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful) {
                        val msg = extractErrorMessage(json, "Failed to update client profile.")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }
                    val updatedSession = currentSession.copy(
                        email = metadata.email.trim(),
                        metadata = metadata
                    )
                    _session.value = updatedSession
                    syncClientTables(updatedSession, metadata)
                    Result.success(updatedSession)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun refreshClientEnquiries() {
        val current = _session.value ?: return
        if (!isConfigured()) return
        dbClient.fetchClientEnquiries(current.accessToken, current.userId).onSuccess { list ->
            _myEnquiries.value = list
        }
    }

    suspend fun submitServiceEnquiry(inquiry: ConsultationInquiry): Result<EnquiryConfirmation> {
        val current = _session.value
            ?: return Result.failure(IllegalStateException("Please sign in to submit a service enquiry."))
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Supabase credentials are not configured in Secrets."))
        }
        return dbClient.createServiceEnquiry(current.accessToken, current.userId, inquiry).map { refCode ->
            refreshClientEnquiries()
            EnquiryConfirmation(
                referenceCode = refCode,
                serviceType = inquiry.resolvedServiceType,
                propertyName = inquiry.propertyName,
                city = inquiry.city,
                scaleSummary = inquiry.scaleSummary,
                contactName = inquiry.contactName,
                contactEmail = inquiry.contactEmail
            )
        }
    }

    suspend fun updateEnquiryNotes(inquiryId: String, notes: String): Result<Unit> {
        val current = _session.value ?: return Result.failure(IllegalStateException("Sign in required."))
        return dbClient.updateClientEnquiry(current.accessToken, inquiryId, briefNotes = notes).also {
            if (it.isSuccess) refreshClientEnquiries()
        }
    }

    suspend fun withdrawEnquiry(inquiryId: String): Result<Unit> {
        val current = _session.value ?: return Result.failure(IllegalStateException("Sign in required."))
        return dbClient.updateClientEnquiry(current.accessToken, inquiryId, status = InquiryStatus.WITHDRAWN).also {
            if (it.isSuccess) refreshClientEnquiries()
        }
    }

    suspend fun deleteEnquiry(inquiryId: String): Result<Unit> {
        val current = _session.value ?: return Result.failure(IllegalStateException("Sign in required."))
        return dbClient.deleteClientEnquiry(current.accessToken, inquiryId).also {
            if (it.isSuccess) refreshClientEnquiries()
        }
    }

    /**
     * Authenticates a staff member via Supabase Auth and runs server-side RBAC verification
     * (`POST /rest/v1/rpc/verify_admin_session`). Ordinary users are strictly rejected.
     */
    suspend fun signInAdminAndVerify(email: String, password: String): Result<AdminUser> {
        SupabaseAuthValidator.validateLogin(email, password)?.let { err ->
            return Result.failure(IllegalArgumentException(err))
        }
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("Configure SUPABASE_URL and SUPABASE_ANON_KEY in Secrets to enable Supabase staff verification."))
        }
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("email", email.trim())
                    put("password", password)
                }
                val req = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/token?grant_type=password")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(req).execute().use { res ->
                    val json = parseJsonOrNull(res.body?.string().orEmpty())
                    if (!res.isSuccessful || json == null) {
                        return@withContext Result.failure(
                            IllegalStateException(extractErrorMessage(json, "Invalid staff credentials."))
                        )
                    }
                    val token = json.optString("access_token")
                    if (token.isBlank()) {
                        return@withContext Result.failure(IllegalStateException("Missing session token."))
                    }
                    val verified = dbClient.verifyAdminSessionServerSide(token).getOrElse { err ->
                        return@withContext Result.failure(err)
                    }
                    if (verified == null || !verified.active) {
                        Result.failure(IllegalAccessException("Account is not authorised as a MILA administrator."))
                    } else {
                        Result.success(verified)
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
        _pendingDraftProfile.value = null
        _myEnquiries.value = emptyList()
        if (!activeToken.isNullOrBlank() && isConfigured()) {
            withContext(Dispatchers.IO) {
                try {
                    val httpRequest = Request.Builder()
                        .url("${normalizedBaseUrl()}/auth/v1/logout")
                        .addHeader("apikey", supabaseAnonKey.trim())
                        .addHeader("Authorization", "Bearer $activeToken")
                        .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                    httpClient.newCall(httpRequest).execute().close()
                } catch (_: Exception) {
                    // Best-effort remote token revocation; local in-memory session is already cleared.
                }
            }
        }
    }

    private suspend fun syncClientTables(session: SupabaseSession, metadata: SupabaseClientMetadata) {
        if (!isConfigured() || session.userId.isBlank()) return
        dbClient.upsertUserProfile(
            accessToken = session.accessToken,
            userId = session.userId,
            email = metadata.email,
            fullName = metadata.fullName,
            mobileNumber = metadata.mobileNumber,
            accountType = "CLIENT"
        )
        dbClient.upsertClientProfile(
            accessToken = session.accessToken,
            userId = session.userId,
            profile = ClientProfile(
                userId = session.userId,
                fullName = metadata.fullName,
                email = metadata.email,
                mobileNumber = metadata.mobileNumber,
                companyName = metadata.companyName,
                designation = metadata.designation,
                city = metadata.city,
                businessCategory = metadata.businessCategory.name
            )
        )
    }

    private fun normalizedBaseUrl(): String = supabaseUrl.trim().trimEnd('/')

    private fun SupabaseClientMetadata.toJson(): JSONObject = JSONObject().apply {
        put("full_name", fullName.trim())
        put("mobile_number", mobileNumber.trim())
        put("company_name", companyName.trim())
        put("designation", designation.trim())
        put("city", city.trim())
        put("business_category", businessCategory.name)
    }

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

    private fun parseSession(
        json: JSONObject,
        fallbackMetadata: SupabaseClientMetadata?
    ): SupabaseSession {
        val userObj = json.optJSONObject("user")
        val metaObj = userObj?.optJSONObject("user_metadata")
        val resolvedEmail = userObj?.optString("email")?.takeIf { it.isNotBlank() }
            ?: fallbackMetadata?.email.orEmpty()
        val metadata = SupabaseClientMetadata(
            fullName = metaObj?.optString("full_name")?.takeIf { it.isNotBlank() }
                ?: fallbackMetadata?.fullName.orEmpty(),
            email = resolvedEmail,
            mobileNumber = metaObj?.optString("mobile_number")?.takeIf { it.isNotBlank() }
                ?: fallbackMetadata?.mobileNumber.orEmpty(),
            companyName = metaObj?.optString("company_name")?.takeIf { it.isNotBlank() }
                ?: fallbackMetadata?.companyName.orEmpty(),
            designation = metaObj?.optString("designation")?.takeIf { it.isNotBlank() }
                ?: fallbackMetadata?.designation.orEmpty(),
            city = metaObj?.optString("city")?.takeIf { it.isNotBlank() }
                ?: fallbackMetadata?.city.orEmpty(),
            businessCategory = BusinessCategory.fromRaw(
                metaObj?.optString("business_category")?.takeIf { it.isNotBlank() }
                    ?: fallbackMetadata?.businessCategory?.name.orEmpty()
            )
        )
        return SupabaseSession(
            accessToken = json.optString("access_token"),
            refreshToken = json.optString("refresh_token"),
            userId = userObj?.optString("id").orEmpty(),
            email = resolvedEmail,
            metadata = metadata
        )
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
