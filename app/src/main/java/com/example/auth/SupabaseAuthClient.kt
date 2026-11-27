package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.AdminUser
import com.example.data.BusinessCategory
import com.example.data.ClientProfile
import com.example.data.ConsultationInquiry
import com.example.data.EnquiryConfirmation
import com.example.data.InquiryStatus
import com.example.data.SupabaseAdminSession
import com.example.data.SupabaseConfig
import com.example.data.SupabaseDatabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

data class VerifiedPhoneOtpSession(
    val phone: String,
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)

data class PendingEmailOtpDraft(
    val email: String,
    val fullName: String,
    val mobileNumber: String
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

    fun normalizePhoneE164(rawInput: String): String? {
        val cleaned = rawInput.trim().replace(Regex("[\\s\\-()]"), "")
        if (cleaned.isEmpty()) return null
        val normalized = when {
            cleaned.startsWith("+") -> cleaned
            cleaned.length == 10 && cleaned.all { it.isDigit() } -> "+91$cleaned"
            cleaned.length in 11..15 && cleaned.all { it.isDigit() } -> "+$cleaned"
            else -> return null
        }
        val digits = normalized.removePrefix("+")
        return if (digits.length in 10..15 && digits.all { it.isDigit() }) normalized else null
    }

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun validateQuickMilaRegistration(
        fullName: String,
        email: String,
        password: String
    ): String? {
        return when {
            fullName.trim().length < 2 -> "Please enter your full name."
            !emailRegex.matches(email.trim()) -> "Please enter a valid email address."
            password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }
    }
}

class SupabaseAuthClient(
    private val context: Context? = null,
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

    private val sessionPrefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val refreshMutex = Mutex()
    @Volatile
    private var lastRefreshSuccessAtMs: Long = 0L

    private val _session = MutableStateFlow<SupabaseSession?>(null)
    val session: StateFlow<SupabaseSession?> = _session.asStateFlow()

    private val _pendingDraftProfile = MutableStateFlow<SupabaseClientMetadata?>(null)
    val pendingDraftProfile: StateFlow<SupabaseClientMetadata?> = _pendingDraftProfile.asStateFlow()

    private val _myEnquiries = MutableStateFlow<List<ConsultationInquiry>>(emptyList())
    val myEnquiries: StateFlow<List<ConsultationInquiry>> = _myEnquiries.asStateFlow()

    init {
        hydrateSessionFromStorage()
    }

    fun isConfigured(): Boolean = SupabaseConfig.isConfigured(supabaseUrl, supabaseAnonKey)

    fun hasSavedSession(): Boolean =
        _session.value != null || sessionPrefs?.getBoolean(KEY_IS_LOGGED_IN, false) == true

    fun loadPendingEmailOtpDraft(): PendingEmailOtpDraft? {
        val prefs = sessionPrefs ?: return null
        val email = prefs.getString(KEY_PENDING_OTP_EMAIL, null).orEmpty().trim()
        val sentAt = prefs.getLong(KEY_PENDING_OTP_SENT_AT, 0L)
        if (email.isBlank() || System.currentTimeMillis() - sentAt > PENDING_OTP_TTL_MS) {
            return null
        }
        return PendingEmailOtpDraft(
            email = email,
            fullName = prefs.getString(KEY_PENDING_OTP_NAME, null).orEmpty(),
            mobileNumber = prefs.getString(KEY_PENDING_OTP_MOBILE, null).orEmpty()
        )
    }

    fun clearPendingEmailOtpDraft() {
        sessionPrefs?.edit()
            ?.remove(KEY_PENDING_OTP_EMAIL)
            ?.remove(KEY_PENDING_OTP_NAME)
            ?.remove(KEY_PENDING_OTP_MOBILE)
            ?.remove(KEY_PENDING_OTP_SENT_AT)
            ?.commit()
    }

    private fun savePendingEmailOtpDraft(email: String, fullName: String, mobileNumber: String) {
        sessionPrefs?.edit()
            ?.putString(KEY_PENDING_OTP_EMAIL, email.trim())
            ?.putString(KEY_PENDING_OTP_NAME, fullName.trim())
            ?.putString(KEY_PENDING_OTP_MOBILE, mobileNumber.trim())
            ?.putLong(KEY_PENDING_OTP_SENT_AT, System.currentTimeMillis())
            ?.commit()
    }

    fun clearPendingDraftProfile() {
        _pendingDraftProfile.value = null
    }

    /**
     * Free (₹0) Email OTP step 1: sends a 6-digit verification code to the user's email inbox
     * via Supabase Auth (`POST /auth/v1/otp`) while storing their Name and Mobile Number in metadata.
     */
    suspend fun sendEmailOtp(
        email: String,
        fullName: String,
        rawPhone: String
    ): Result<String> {
        val cleanName = fullName.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        val normalizedPhone = SupabaseAuthValidator.normalizePhoneE164(rawPhone)
            ?: return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        val normalizedEmail = email.trim().lowercase()
        if (!SupabaseAuthValidator.isValidEmail(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException("Email OTP service is not configured yet. Please check your connection.")
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("email", normalizedEmail)
                    put("create_user", true)
                    put(
                        "data",
                        JSONObject().apply {
                            put("full_name", cleanName)
                            put("mobile_number", normalizedPhone)
                        }
                    )
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/otp")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful) {
                        val rawMsg = extractErrorMessage(json, "Could not send Email OTP (HTTP ${response.code}).")
                        val friendlyMsg = when {
                            response.code == 429 || rawMsg.contains("rate limit", ignoreCase = true) ->
                                "Too many OTP requests right now. Please wait a minute and try again, or use Continue with Google below."
                            rawMsg.contains("signups not allowed", ignoreCase = true) ->
                                "Email signups are currently disabled in Supabase settings. Please enable Email provider in Supabase."
                            else -> rawMsg
                        }
                        Result.failure(IllegalStateException(friendlyMsg))
                    } else {
                        savePendingEmailOtpDraft(
                            email = normalizedEmail,
                            fullName = cleanName,
                            mobileNumber = normalizedPhone
                        )
                        Result.success(normalizedEmail)
                    }
                }
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(e.localizedMessage ?: "Network error while sending Email OTP.")
                )
            }
        }
    }

    /**
     * Free (₹0) Email OTP step 2: verifies the 6-digit code sent to the user's email inbox,
     * persists the refresh token locally so returning users stay logged in, and syncs their
     * Name, Mobile Number, and Email to Supabase for the Admin Panel.
     */
    suspend fun verifyEmailOtp(
        email: String,
        otpCode: String,
        fullName: String,
        rawPhone: String
    ): Result<SupabaseSession> {
        val normalizedEmail = email.trim().lowercase()
        if (!SupabaseAuthValidator.isValidEmail(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Invalid email address."))
        }
        val cleanOtp = otpCode.trim()
        if (cleanOtp.length !in 6..8 || !cleanOtp.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Please enter the 6-digit OTP sent to your email."))
        }
        val normalizedPhone = SupabaseAuthValidator.normalizePhoneE164(rawPhone) ?: rawPhone.trim()
        val cleanName = fullName.trim()
        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException("Email OTP verification is not configured yet.")
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                // Supabase GoTrue accepts type="email" for both new signups and returning users;
                // fall back to "signup" / "magiclink" only if an older instance rejects "email".
                val verifyTypes = listOf("email", "signup", "magiclink")
                var verifiedJson: JSONObject? = null
                var lastErrorMsg: String? = null

                for (verifyType in verifyTypes) {
                    val payload = JSONObject().apply {
                        put("email", normalizedEmail)
                        put("token", cleanOtp)
                        put("type", verifyType)
                    }
                    val req = Request.Builder()
                        .url("${normalizedBaseUrl()}/auth/v1/verify")
                        .addHeader("apikey", supabaseAnonKey.trim())
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    httpClient.newCall(req).execute().use { response ->
                        val bodyText = response.body?.string().orEmpty()
                        val json = parseJsonOrNull(bodyText)
                        if (response.isSuccessful && json != null && !json.optString("access_token").isNullOrBlank()) {
                            verifiedJson = json
                        } else {
                            lastErrorMsg = extractErrorMessage(json, "Invalid or expired OTP code. Please try again.")
                        }
                    }
                    if (verifiedJson != null) break
                }

                val activeJson = verifiedJson ?: return@withContext Result.failure(
                    IllegalStateException(lastErrorMsg ?: "Invalid or expired OTP code. Please check your email and try again.")
                )

                val accessToken = activeJson.optString("access_token").orEmpty()
                val refreshToken = activeJson.optString("refresh_token").orEmpty()
                val userObj = activeJson.optJSONObject("user")
                val userId = userObj?.optString("id").orEmpty()
                val resolvedEmail = userObj?.optString("email")?.takeIf { it.isNotBlank() } ?: normalizedEmail
                if (accessToken.isBlank() || userId.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("OTP verification did not return a valid session.")
                    )
                }

                val remoteProfile = dbClient.fetchClientProfile(accessToken, userId).getOrNull()
                val resolvedName = cleanName.takeIf { it.length >= 2 }
                    ?: remoteProfile?.fullName?.trim()?.takeIf { it.length >= 2 }
                    ?: resolvedEmail.substringBefore("@").takeIf { it.length >= 2 }
                    ?: "MILA Member"
                val resolvedMobile = normalizedPhone.takeIf { it.length >= 7 }
                    ?: remoteProfile?.mobileNumber?.trim()?.takeIf { it.length >= 7 && it != "Not Provided" }
                    ?: "Not Provided"

                val metadata = SupabaseClientMetadata(
                    fullName = resolvedName,
                    email = resolvedEmail,
                    mobileNumber = resolvedMobile,
                    companyName = remoteProfile?.companyName?.ifBlank { "MILA Hospitality Member" }
                        ?: "MILA Hospitality Member",
                    designation = remoteProfile?.designation?.ifBlank { "Client" } ?: "Client",
                    city = remoteProfile?.city?.ifBlank { "India" } ?: "India",
                    businessCategory = remoteProfile?.resolvedBusinessCategory
                        ?: BusinessCategory.LUXURY_HOTEL_RESORT
                )
                val session = SupabaseSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    email = resolvedEmail,
                    metadata = metadata
                )

                syncClientTables(session, metadata, userProfileEmailOverride = resolvedEmail)
                clearPendingEmailOtpDraft()
                persistSession(session)
                lastRefreshSuccessAtMs = System.currentTimeMillis()
                _session.value = session
                _pendingDraftProfile.value = metadata
                refreshClientEnquiries()
                Result.success(session)
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(e.localizedMessage ?: "Network error while verifying Email OTP.")
                )
            }
        }
    }

    /**
     * Silently refreshes the saved Supabase session in the background (e.g., on launch or when
     * returning from another app) so JWT tokens stay fresh and Admin Panel sees updated activity.
     *
     * CRITICAL: Never logs the user out on network errors, timeouts, or token refresh failures.
     * The user stays signed in until they explicitly tap Sign Out.
     */
    suspend fun restoreSavedSession(force: Boolean = false): Boolean = refreshMutex.withLock {
        if (_session.value == null) {
            hydrateSessionFromStorage()
        }
        val prefs = sessionPrefs ?: return@withLock _session.value != null
        val currentInMemory = _session.value
        val savedRefreshToken = currentInMemory?.refreshToken?.takeIf { it.isNotBlank() }
            ?: prefs.getString(KEY_REFRESH_TOKEN, null).orEmpty()
        if (savedRefreshToken.isBlank() || !isConfigured()) {
            return@withLock currentInMemory != null
        }

        val now = System.currentTimeMillis()
        if (!force && currentInMemory != null && now - lastRefreshSuccessAtMs < REFRESH_MIN_INTERVAL_MS) {
            return@withLock true
        }

        val savedName = currentInMemory?.metadata?.fullName?.takeIf { it.isNotBlank() }
            ?: prefs.getString(KEY_FULL_NAME, null).orEmpty()
        val savedMobile = currentInMemory?.metadata?.mobileNumber?.takeIf { it.isNotBlank() }
            ?: prefs.getString(KEY_MOBILE_NUMBER, null).orEmpty()
        val savedRealEmail = currentInMemory?.metadata?.email?.takeIf { it.isNotBlank() }
            ?: prefs.getString(KEY_REAL_EMAIL, null).orEmpty()
        val savedAuthEmail = currentInMemory?.email?.takeIf { it.isNotBlank() }
            ?: prefs.getString(KEY_AUTH_EMAIL, null).orEmpty()

        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("refresh_token", savedRefreshToken)
                }
                val req = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful || json == null) {
                        // Keep existing local session active; never auto-logout the user
                        return@withContext _session.value != null
                    }

                    val accessToken = json.optString("access_token").orEmpty()
                    val nextRefreshToken = json.optString("refresh_token").ifBlank { savedRefreshToken }
                    val userObj = json.optJSONObject("user")
                    val userId = userObj?.optString("id").orEmpty()
                    if (accessToken.isBlank() || userId.isBlank()) {
                        return@withContext _session.value != null
                    }

                    val authEmail = userObj?.optString("email")?.takeIf { it.isNotBlank() }
                        ?: savedAuthEmail.ifBlank { savedRealEmail }
                    val remoteProfile = dbClient.fetchClientProfile(accessToken, userId).getOrNull()
                    val metaObj = userObj?.optJSONObject("user_metadata")

                    val resolvedName = remoteProfile?.fullName?.trim()?.takeIf { it.length >= 2 }
                        ?: savedName.takeIf { it.length >= 2 }
                        ?: metaObj?.optString("full_name")?.trim()?.takeIf { it.length >= 2 }
                        ?: "MILA Member"
                    val resolvedMobile = remoteProfile?.mobileNumber?.trim()?.takeIf { it.length >= 7 && it != "Not Provided" }
                        ?: savedMobile.takeIf { it.length >= 7 }
                        ?: metaObj?.optString("mobile_number")?.trim().orEmpty()
                    val resolvedDisplayEmail = remoteProfile?.email?.trim()?.takeIf { it.isNotBlank() }
                        ?: savedRealEmail.ifBlank { authEmail }

                    val metadata = SupabaseClientMetadata(
                        fullName = resolvedName,
                        email = resolvedDisplayEmail,
                        mobileNumber = resolvedMobile,
                        companyName = remoteProfile?.companyName?.ifBlank {
                            currentInMemory?.metadata?.companyName ?: "MILA Hospitality Member"
                        } ?: "MILA Hospitality Member",
                        designation = remoteProfile?.designation?.ifBlank {
                            currentInMemory?.metadata?.designation ?: "Client"
                        } ?: "Client",
                        city = remoteProfile?.city?.ifBlank {
                            currentInMemory?.metadata?.city ?: "India"
                        } ?: "India",
                        businessCategory = remoteProfile?.resolvedBusinessCategory
                            ?: currentInMemory?.metadata?.businessCategory
                            ?: BusinessCategory.LUXURY_HOTEL_RESORT
                    )

                    val restored = SupabaseSession(
                        accessToken = accessToken,
                        refreshToken = nextRefreshToken,
                        userId = userId,
                        email = authEmail,
                        metadata = metadata
                    )

                    persistSession(restored)
                    lastRefreshSuccessAtMs = System.currentTimeMillis()
                    syncClientTables(restored, metadata, userProfileEmailOverride = authEmail)
                    _session.value = restored
                    _pendingDraftProfile.value = metadata
                    refreshClientEnquiries()
                    true
                }
            } catch (_: Exception) {
                // Network hiccup or offline: preserve active session so user never gets logged out
                _session.value != null
            }
        }
    }

    suspend fun sendPhoneOtp(rawPhone: String, fullName: String = ""): Result<String> {
        val normalizedPhone = SupabaseAuthValidator.normalizePhoneE164(rawPhone)
            ?: return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))

        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException("Mobile OTP service is not configured yet. Please check your Supabase settings.")
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val cleanName = fullName.trim()
                val payload = JSONObject().apply {
                    put("phone", normalizedPhone)
                    put("create_user", true)
                    if (cleanName.isNotEmpty()) {
                        put(
                            "data",
                            JSONObject().apply {
                                put("full_name", cleanName)
                                put("mobile_number", normalizedPhone)
                            }
                        )
                    }
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/otp")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful) {
                        val rawMsg = extractErrorMessage(json, "Could not send OTP (HTTP ${response.code}).")
                        val friendlyMsg = if (
                            rawMsg.contains("provider", ignoreCase = true) ||
                            rawMsg.contains("sms", ignoreCase = true) ||
                            rawMsg.contains("phone logins are disabled", ignoreCase = true) ||
                            rawMsg.contains("unsupported", ignoreCase = true)
                        ) {
                            "SMS OTP provider is not enabled on Supabase yet. Please use Continue with Google below to sign in right away."
                        } else {
                            rawMsg
                        }
                        Result.failure(IllegalStateException(friendlyMsg))
                    } else {
                        Result.success(normalizedPhone)
                    }
                }
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.localizedMessage ?: "Network error while sending OTP."))
            }
        }
    }

    suspend fun verifyPhoneOtp(phone: String, otpCode: String): Result<VerifiedPhoneOtpSession> {
        val normalizedPhone = SupabaseAuthValidator.normalizePhoneE164(phone)
            ?: return Result.failure(IllegalArgumentException("Invalid mobile number."))
        val cleanOtp = otpCode.trim()
        if (cleanOtp.length < 4 || !cleanOtp.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Please enter the 6-digit OTP sent to your mobile."))
        }

        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException("Mobile OTP verification is not configured yet.")
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("phone", normalizedPhone)
                    put("token", cleanOtp)
                    put("type", "sms")
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/verify")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful || json == null) {
                        val msg = extractErrorMessage(json, "Invalid or expired OTP. Please try again.")
                        return@withContext Result.failure(IllegalStateException(msg))
                    }
                    val accessToken = json.optString("access_token").orEmpty()
                    val refreshToken = json.optString("refresh_token").orEmpty()
                    val userId = json.optJSONObject("user")?.optString("id").orEmpty()
                    if (accessToken.isBlank() || userId.isBlank()) {
                        return@withContext Result.failure(
                            IllegalStateException("OTP verification did not return a valid session.")
                        )
                    }
                    Result.success(
                        VerifiedPhoneOtpSession(
                            phone = normalizedPhone,
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            userId = userId
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.localizedMessage ?: "Network error while verifying OTP."))
            }
        }
    }

    suspend fun activatePhoneOtpSession(
        verifiedPhone: VerifiedPhoneOtpSession,
        enteredFullName: String = ""
    ): SupabaseSession =
        withContext(Dispatchers.IO) {
            val remoteProfile = dbClient
                .fetchClientProfile(verifiedPhone.accessToken, verifiedPhone.userId)
                .getOrNull()
            val cleanEnteredName = enteredFullName.trim().takeIf { it.length >= 2 }
            val existingRealName = remoteProfile?.fullName?.trim()?.takeIf {
                it.length >= 2 &&
                    !it.equals("MILA Guest", ignoreCase = true) &&
                    !it.equals("MILA Client", ignoreCase = true)
            }
            val resolvedName = cleanEnteredName ?: existingRealName ?: "MILA Member"
            val fallbackEmail = remoteProfile?.email?.takeIf { it.isNotBlank() }
                ?: "${verifiedPhone.phone.filter { it.isDigit() }}@phone.mila.local"
            val metadata = if (remoteProfile != null) {
                SupabaseClientMetadata(
                    fullName = resolvedName,
                    email = remoteProfile.email.ifBlank { fallbackEmail },
                    mobileNumber = verifiedPhone.phone.ifBlank { remoteProfile.mobileNumber },
                    companyName = remoteProfile.companyName,
                    designation = remoteProfile.designation,
                    city = remoteProfile.city,
                    businessCategory = remoteProfile.resolvedBusinessCategory
                )
            } else {
                SupabaseClientMetadata(
                    fullName = resolvedName,
                    email = fallbackEmail,
                    mobileNumber = verifiedPhone.phone,
                    companyName = "MILA Hospitality Member",
                    designation = "Client",
                    city = "India",
                    businessCategory = BusinessCategory.LUXURY_HOTEL_RESORT
                )
            }
            val session = SupabaseSession(
                accessToken = verifiedPhone.accessToken,
                refreshToken = verifiedPhone.refreshToken,
                userId = verifiedPhone.userId,
                email = metadata.email,
                metadata = metadata
            )
            // Always upsert on every login so Admin sees latest Name, Mobile Number, and Last Active time
            syncClientTables(session, metadata)
            persistSession(session)
            lastRefreshSuccessAtMs = System.currentTimeMillis()
            _session.value = session
            _pendingDraftProfile.value = metadata
            refreshClientEnquiries()
            session
        }

    suspend fun completeMilaAccountAfterOtp(
        verifiedPhone: VerifiedPhoneOtpSession,
        fullName: String,
        email: String,
        password: String,
        updatesOptIn: Boolean
    ): ClientAuthOutcome {
        SupabaseAuthValidator.validateQuickMilaRegistration(fullName, email, password)?.let { err ->
            return ClientAuthOutcome.Failure(err)
        }

        val metadata = SupabaseClientMetadata(
            fullName = fullName.trim(),
            email = email.trim(),
            mobileNumber = verifiedPhone.phone,
            companyName = "MILA Hospitality Member",
            designation = "Hospitality Member",
            city = "India",
            businessCategory = BusinessCategory.LUXURY_HOTEL_RESORT
        )

        return withContext(Dispatchers.IO) {
            try {
                val dataJson = metadata.toJson().apply {
                    put("updates_opt_in", updatesOptIn)
                }
                val payload = JSONObject().apply {
                    put("email", metadata.email)
                    put("password", password)
                    put("data", dataJson)
                }
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/user")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Authorization", "Bearer ${verifiedPhone.accessToken}")
                    .addHeader("Content-Type", "application/json")
                    .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    val json = parseJsonOrNull(bodyText)
                    if (!response.isSuccessful) {
                        val msg = extractErrorMessage(json, "Could not complete registration (HTTP ${response.code}).")
                        return@withContext ClientAuthOutcome.Failure(msg)
                    }
                    val activeSession = SupabaseSession(
                        accessToken = verifiedPhone.accessToken,
                        refreshToken = verifiedPhone.refreshToken,
                        userId = verifiedPhone.userId,
                        email = metadata.email,
                        metadata = metadata
                    )
                    persistSession(activeSession)
                    lastRefreshSuccessAtMs = System.currentTimeMillis()
                    _session.value = activeSession
                    _pendingDraftProfile.value = metadata
                    syncClientTables(activeSession, metadata)
                    refreshClientEnquiries()
                    ClientAuthOutcome.Authenticated(
                        session = activeSession,
                        message = "Welcome to MILA Hospitality, ${metadata.fullName}."
                    )
                }
            } catch (e: Exception) {
                ClientAuthOutcome.Failure(e.localizedMessage ?: "Network error while creating your MILA account.")
            }
        }
    }

    suspend fun quickEmailSignUp(
        fullName: String,
        email: String,
        mobileNumber: String,
        password: String,
        updatesOptIn: Boolean
    ): ClientAuthOutcome {
        SupabaseAuthValidator.validateQuickMilaRegistration(fullName, email, password)?.let { err ->
            return ClientAuthOutcome.Failure(err)
        }

        val cleanedMobile = mobileNumber.trim()
        val resolvedMobile = if (cleanedMobile.isEmpty()) {
            "Not Provided"
        } else {
            SupabaseAuthValidator.normalizePhoneE164(cleanedMobile)
                ?: return ClientAuthOutcome.Failure("Please enter a valid 10-digit mobile number or leave it blank.")
        }

        val metadata = SupabaseClientMetadata(
            fullName = fullName.trim(),
            email = email.trim(),
            mobileNumber = resolvedMobile,
            companyName = "MILA Hospitality Member",
            designation = "Hospitality Member",
            city = "India",
            businessCategory = BusinessCategory.LUXURY_HOTEL_RESORT
        )

        if (!isConfigured()) {
            _pendingDraftProfile.value = metadata
            return ClientAuthOutcome.RequiresGoogleSyncOrSecrets(
                message = "Email registration requires Supabase configuration. Use Continue with Google below or configure SUPABASE_URL and SUPABASE_ANON_KEY.",
                draftProfile = metadata
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val dataJson = metadata.toJson().apply {
                    put("updates_opt_in", updatesOptIn)
                }
                val payload = JSONObject().apply {
                    put("email", metadata.email)
                    put("password", password)
                    put("data", dataJson)
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
                        val msg = extractErrorMessage(json, "Could not create account (HTTP ${response.code}).")
                        return@withContext ClientAuthOutcome.Failure(msg)
                    }

                    val accessToken = json?.optString("access_token").orEmpty()
                    if (accessToken.isNotBlank()) {
                        val parsedSession = parseSession(json!!, fallbackMetadata = metadata)
                        persistSession(parsedSession)
                        lastRefreshSuccessAtMs = System.currentTimeMillis()
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
                            message = "Account created for ${metadata.email}. Please verify the link sent to your inbox, then sign in.",
                            draftProfile = metadata
                        )
                    }
                }
            } catch (e: Exception) {
                ClientAuthOutcome.Failure(e.localizedMessage ?: "Network error while creating your MILA account.")
            }
        }
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
                        persistSession(parsedSession)
                        lastRefreshSuccessAtMs = System.currentTimeMillis()
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
                    } else {
                        syncClientTables(parsedSession, parsedSession.metadata)
                    }
                    persistSession(parsedSession)
                    lastRefreshSuccessAtMs = System.currentTimeMillis()
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
                    persistSession(updatedSession)
                    _session.value = updatedSession
                    syncClientTables(updatedSession, metadata)
                    Result.success(updatedSession)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateQuickMemberDetails(
        fullName: String,
        mobileNumber: String,
        optionalEmail: String = ""
    ): Result<SupabaseSession> {
        val currentSession = _session.value
            ?: return Result.failure(IllegalStateException("No active session."))
        val cleanName = fullName.trim()
        val cleanMobile = mobileNumber.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (cleanMobile.length < 7) {
            return Result.failure(IllegalArgumentException("Please enter a valid mobile number."))
        }
        val resolvedEmail = optionalEmail.trim().takeIf { it.contains("@") }
            ?: currentSession.email
        val updatedMetadata = currentSession.metadata.copy(
            fullName = cleanName,
            mobileNumber = cleanMobile,
            email = resolvedEmail
        )
        val updatedSession = currentSession.copy(
            email = resolvedEmail,
            metadata = updatedMetadata
        )
        return withContext(Dispatchers.IO) {
            try {
                syncClientTables(updatedSession, updatedMetadata)
                persistSession(updatedSession)
                _session.value = updatedSession
                _pendingDraftProfile.value = updatedMetadata
                Result.success(updatedSession)
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
        // Guarantee user_profiles & client_profiles rows exist before inserting into service_enquiries
        val syncedMeta = current.metadata.copy(
            fullName = inquiry.contactName.trim().ifBlank { current.metadata.fullName },
            email = inquiry.contactEmail.trim().ifBlank { current.email },
            mobileNumber = inquiry.contactPhone.trim().ifBlank { current.metadata.mobileNumber },
            companyName = inquiry.propertyName.trim().ifBlank { current.metadata.companyName },
            city = inquiry.city.trim().ifBlank { current.metadata.city }
        )
        syncClientTables(current, syncedMeta)
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
     * (`POST /rest/v1/rpc/verify_admin_session`), returning a completely isolated
     * [SupabaseAdminSession] (does not touch the client/member session state).
     */
    suspend fun signInAdminWithSession(email: String, password: String): Result<SupabaseAdminSession> {
        SupabaseAuthValidator.validateLogin(email, password)?.let { err ->
            return Result.failure(IllegalArgumentException(err))
        }
        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException("Staff sign-in requires cloud configuration. Please check your connection or contact MILA IT.")
            )
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
                            IllegalStateException(extractErrorMessage(json, "Invalid administrator email or password."))
                        )
                    }
                    val token = json.optString("access_token")
                    if (token.isBlank()) {
                        return@withContext Result.failure(IllegalStateException("Missing staff session token."))
                    }
                    val verified = dbClient.verifyAdminSessionServerSide(token).getOrElse { err ->
                        signOutAdminToken(token)
                        return@withContext Result.failure(err)
                    }
                    if (verified == null || !verified.active) {
                        signOutAdminToken(token)
                        Result.failure(
                            IllegalAccessException("This account is not authorised as a MILA administrator. Ordinary client and candidate accounts cannot access the Admin Portal.")
                        )
                    } else {
                        Result.success(SupabaseAdminSession(admin = verified, accessToken = token))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signInAdminAndVerify(email: String, password: String): Result<AdminUser> =
        signInAdminWithSession(email, password).map { it.admin }

    suspend fun signOutAdminToken(accessToken: String?) {
        if (accessToken.isNullOrBlank() || !isConfigured()) return
        withContext(Dispatchers.IO) {
            try {
                val httpRequest = Request.Builder()
                    .url("${normalizedBaseUrl()}/auth/v1/logout")
                    .addHeader("apikey", supabaseAnonKey.trim())
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(httpRequest).execute().close()
            } catch (_: Exception) {
                // Best-effort remote token revocation.
            }
        }
    }

    suspend fun signOut() {
        val activeToken = _session.value?.accessToken
        clearPersistedSession()
        clearPendingEmailOtpDraft()
        lastRefreshSuccessAtMs = 0L
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
                    // Best-effort remote token revocation; local session is already cleared.
                }
            }
        }
    }

    private fun hydrateSessionFromStorage() {
        val prefs = sessionPrefs ?: return
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null).orEmpty()
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null).orEmpty()
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null).orEmpty()
        if (!isLoggedIn || userId.isBlank() || (accessToken.isBlank() && refreshToken.isBlank())) {
            return
        }
        val authEmail = prefs.getString(KEY_AUTH_EMAIL, null).orEmpty()
        val displayEmail = prefs.getString(KEY_REAL_EMAIL, null).orEmpty().ifBlank { authEmail }
        val metadata = SupabaseClientMetadata(
            fullName = prefs.getString(KEY_FULL_NAME, null).orEmpty().ifBlank { "MILA Member" },
            email = displayEmail,
            mobileNumber = prefs.getString(KEY_MOBILE_NUMBER, null).orEmpty().ifBlank { "Not Provided" },
            companyName = prefs.getString(KEY_COMPANY_NAME, null).orEmpty().ifBlank { "MILA Hospitality Member" },
            designation = prefs.getString(KEY_DESIGNATION, null).orEmpty().ifBlank { "Client" },
            city = prefs.getString(KEY_CITY, null).orEmpty().ifBlank { "India" },
            businessCategory = BusinessCategory.fromRaw(
                prefs.getString(KEY_BUSINESS_CATEGORY, null).orEmpty()
            )
        )
        _session.value = SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = authEmail.ifBlank { displayEmail },
            metadata = metadata
        )
        _pendingDraftProfile.value = metadata
    }

    private fun persistSession(session: SupabaseSession) {
        if (session.userId.isBlank()) return
        sessionPrefs?.edit()
            ?.putBoolean(KEY_IS_LOGGED_IN, true)
            ?.putString(KEY_USER_ID, session.userId)
            ?.putString(KEY_ACCESS_TOKEN, session.accessToken)
            ?.putString(KEY_REFRESH_TOKEN, session.refreshToken)
            ?.putString(KEY_FULL_NAME, session.metadata.fullName)
            ?.putString(KEY_MOBILE_NUMBER, session.metadata.mobileNumber)
            ?.putString(KEY_REAL_EMAIL, session.metadata.email)
            ?.putString(KEY_AUTH_EMAIL, session.email)
            ?.putString(KEY_COMPANY_NAME, session.metadata.companyName)
            ?.putString(KEY_DESIGNATION, session.metadata.designation)
            ?.putString(KEY_CITY, session.metadata.city)
            ?.putString(KEY_BUSINESS_CATEGORY, session.metadata.businessCategory.name)
            ?.commit()
    }

    private fun clearPersistedSession() {
        sessionPrefs?.edit()
            ?.remove(KEY_IS_LOGGED_IN)
            ?.remove(KEY_USER_ID)
            ?.remove(KEY_ACCESS_TOKEN)
            ?.remove(KEY_REFRESH_TOKEN)
            ?.remove(KEY_FULL_NAME)
            ?.remove(KEY_MOBILE_NUMBER)
            ?.remove(KEY_REAL_EMAIL)
            ?.remove(KEY_AUTH_EMAIL)
            ?.remove(KEY_COMPANY_NAME)
            ?.remove(KEY_DESIGNATION)
            ?.remove(KEY_CITY)
            ?.remove(KEY_BUSINESS_CATEGORY)
            ?.commit()
    }

    private suspend fun syncClientTables(
        session: SupabaseSession,
        metadata: SupabaseClientMetadata,
        userProfileEmailOverride: String? = null
    ) {
        if (!isConfigured() || session.userId.isBlank()) return
        val safeClientEmail = metadata.email.trim().ifBlank { session.email.trim() }
        val safeUserProfileEmail = userProfileEmailOverride?.trim()?.takeIf { it.isNotBlank() }
            ?: session.email.trim().ifBlank { safeClientEmail }
        val safeName = metadata.fullName.trim().takeIf { it.length >= 2 }
            ?: safeClientEmail.substringBefore("@").takeIf { it.length >= 2 }
            ?: "MILA Client"
        val safeMobile = metadata.mobileNumber.trim().takeIf { it.length >= 7 } ?: "Not Provided"
        val safeCompany = metadata.companyName.trim().takeIf { it.length >= 2 } ?: "Hospitality Venture"
        val safeDesignation = metadata.designation.trim().takeIf { it.length >= 2 } ?: "Client Member"
        val safeCity = metadata.city.trim().takeIf { it.length >= 2 } ?: "India"
        dbClient.upsertUserProfile(
            accessToken = session.accessToken,
            userId = session.userId,
            email = safeUserProfileEmail,
            fullName = safeName,
            mobileNumber = safeMobile,
            accountType = "CLIENT"
        )
        dbClient.upsertClientProfile(
            accessToken = session.accessToken,
            userId = session.userId,
            profile = ClientProfile(
                userId = session.userId,
                fullName = safeName,
                email = safeClientEmail,
                mobileNumber = safeMobile,
                companyName = safeCompany,
                designation = safeDesignation,
                city = safeCity,
                businessCategory = metadata.businessCategory.name
            )
        )
    }

    private fun normalizedBaseUrl(): String = SupabaseConfig.normalizedBaseUrl(supabaseUrl)

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
        private const val PREFS_NAME = "mila_member_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_MOBILE_NUMBER = "mobile_number"
        private const val KEY_REAL_EMAIL = "real_email"
        private const val KEY_AUTH_EMAIL = "auth_email"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_DESIGNATION = "designation"
        private const val KEY_CITY = "city"
        private const val KEY_BUSINESS_CATEGORY = "business_category"
        private const val KEY_PENDING_OTP_EMAIL = "pending_otp_email"
        private const val KEY_PENDING_OTP_NAME = "pending_otp_name"
        private const val KEY_PENDING_OTP_MOBILE = "pending_otp_mobile"
        private const val KEY_PENDING_OTP_SENT_AT = "pending_otp_sent_at"
        private const val PENDING_OTP_TTL_MS = 30L * 60L * 1000L
        private const val REFRESH_MIN_INTERVAL_MS = 5L * 60L * 1000L

        @Volatile
        private var sharedInstance: SupabaseAuthClient? = null

        fun getInstance(context: Context): SupabaseAuthClient {
            return sharedInstance ?: synchronized(this) {
                sharedInstance ?: SupabaseAuthClient(context = context.applicationContext).also {
                    sharedInstance = it
                }
            }
        }
    }
}
