package com.example.notifications

import android.os.Build
import com.example.data.SupabaseConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Registers and updates the candidate's Android FCM device token and channel opt-ins
 * across Firestore (`users/{uid}/deviceTokens/android_primary`) and Supabase (`device_push_tokens`).
 */
class PushTokenManager(
    private val db: FirebaseFirestore,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun registerAndroidToken(
        firebaseUid: String?,
        supabaseUserId: String?,
        supabaseAccessToken: String?,
        pushEnabled: Boolean,
        emailEnabled: Boolean
    ): Result<String?> = withContext(Dispatchers.IO) {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android Device" }.take(80)

        if (!firebaseUid.isNullOrBlank()) {
            val tokenValue = token?.takeIf { it.length >= 20 }
                ?: "pending_fcm_${firebaseUid.take(12)}_${System.currentTimeMillis()}"
            val doc = CandidateDeviceToken(
                userId = firebaseUid,
                fcmToken = tokenValue,
                platform = "ANDROID",
                deviceLabel = deviceLabel,
                active = pushEnabled,
                pushEnabled = pushEnabled,
                emailEnabled = emailEnabled
            )
            runCatching {
                db.collection("users").document(firebaseUid)
                    .collection("deviceTokens").document("android_primary")
                    .set(doc.toUpsertMap(firebaseUid))
                    .await()
            }
        }

        if (!supabaseUserId.isNullOrBlank() &&
            !supabaseAccessToken.isNullOrBlank() &&
            !token.isNullOrBlank() &&
            SupabaseConfig.isConfigured()
        ) {
            runCatching {
                val payload = JSONObject().apply {
                    put("user_id", supabaseUserId)
                    put("fcm_token", token)
                    put("platform", "ANDROID")
                    put("device_label", deviceLabel)
                    put("is_active", pushEnabled)
                }
                val req = Request.Builder()
                    .url("${SupabaseConfig.normalizedBaseUrl()}/rest/v1/device_push_tokens")
                    .addHeader("apikey", com.example.BuildConfig.SUPABASE_ANON_KEY.trim())
                    .addHeader("Authorization", "Bearer $supabaseAccessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(req).execute().close()
            }

            runCatching {
                val prefPayload = JSONObject().apply {
                    put("user_id", supabaseUserId)
                    put("push_notifications_enabled", pushEnabled)
                    put("email_notifications_enabled", emailEnabled)
                }
                val prefReq = Request.Builder()
                    .url("${SupabaseConfig.normalizedBaseUrl()}/rest/v1/candidate_preferences")
                    .addHeader("apikey", com.example.BuildConfig.SUPABASE_ANON_KEY.trim())
                    .addHeader("Authorization", "Bearer $supabaseAccessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(prefPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                httpClient.newCall(prefReq).execute().close()
            }
        }

        Result.success(token)
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
