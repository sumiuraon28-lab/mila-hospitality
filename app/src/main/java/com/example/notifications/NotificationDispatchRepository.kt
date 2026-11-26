package com.example.notifications

import com.example.BuildConfig
import com.example.data.AdminModule
import com.example.data.AdminRepository
import com.example.data.AdminRole
import com.example.data.AdminUser
import com.example.data.ApplicationStatus
import com.example.data.CandidateProfile
import com.example.data.JobApplication
import com.example.data.JobNotification
import com.example.data.JobVacancyRecord
import com.example.data.OperationType
import com.example.data.SupabaseConfig
import com.example.data.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

/**
 * Automated multi-channel job notification dispatcher & delivery log repository.
 *
 * Strictly enforces candidate eligibility via [JobMatchingEngine] before sending
 * vacancy alerts across In-App, Android Push (FCM), iOS Push (APNs via FCM),
 * and Optional Email channels, recording every attempt in `notificationDeliveryLogs`.
 */
class NotificationDispatchRepository(
    private val db: FirebaseFirestore,
    private val adminRepository: AdminRepository,
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    fun observeControls(): Flow<NotificationControlSettings> = flow {
        db.collection("adminSettings").document("notifications")
            .snapshots()
            .map { snap ->
                snap.toObject(
                    NotificationControlSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: NotificationControlSettings()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(NotificationControlSettings())
            }
            .collect { emit(it) }
    }

    fun observeDeliveryLogs(): Flow<List<NotificationDeliveryLog>> = flow {
        db.collection("notificationDeliveryLogs")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    NotificationDeliveryLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeAllDeviceTokens(): Flow<List<CandidateDeviceToken>> = flow {
        db.collectionGroup("deviceTokens")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CandidateDeviceToken::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun saveControls(
        actor: AdminUser,
        controls: NotificationControlSettings
    ): Result<Unit> = try {
        require(
            actor.resolvedRole == AdminRole.SUPER_ADMIN ||
                actor.resolvedRole == AdminRole.RECRUITMENT_ADMIN
        ) { "Recruitment or Super Admin role required." }
        db.collection("adminSettings").document("notifications")
            .set(controls.toAdminMap())
            .await()
        adminRepository.logActivity(
            actor = actor,
            module = AdminModule.NOTIFICATION_MANAGEMENT,
            action = "UPDATE_CONTROLS",
            targetId = "notifications",
            summary = "AutoPublish=${controls.autoDispatchOnPublish}, AndroidFCM=${controls.enableAndroidPush}, iOSPush=${controls.enableIosPush}, Email=${controls.enableEmailNotifications}"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "adminSettings/notifications")
        Result.failure(e)
    }

    /**
     * Evaluates all registered candidates against the published vacancy and sends
     * notifications ONLY to eligible matched candidates across enabled channels.
     */
    suspend fun dispatchMatchedVacancyNotifications(
        actor: AdminUser,
        vacancy: JobVacancyRecord,
        candidates: List<CandidateProfile>,
        deviceTokens: List<CandidateDeviceToken>,
        controls: NotificationControlSettings,
        supabaseAccessToken: String? = null
    ): Result<VacancyDispatchSummary> = try {
        require(actor.resolvedRole.canMutate(AdminModule.NOTIFICATION_MANAGEMENT)) {
            "Not authorized to dispatch job notifications."
        }
        require(vacancy.published) { "Only published vacancies can trigger candidate alerts." }

        val evaluations = JobMatchingEngine.evaluateAllCandidates(vacancy, candidates, controls)
        val eligible = evaluations.filter { it.eligible }
        val excludedCount = evaluations.size - eligible.size
        val tokensByUser = deviceTokens.groupBy { it.userId }
        val roleId = vacancy.id.ifBlank { "vacancy_${System.currentTimeMillis()}" }
        var logsWritten = 0

        eligible.forEach { match ->
            val cand = match.candidate
            if (cand.userId.isBlank()) return@forEach
            val matchSummary = match.matchedCriteria.joinToString(" • ").take(280)
            val userTokens = tokensByUser[cand.userId].orEmpty()

            // 1. In-App Notification
            if (controls.enableInApp) {
                val notifId = "match_${roleId.take(40)}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
                val notif = JobNotification(
                    userId = cand.userId,
                    roleId = roleId,
                    title = "Matched Mandate: ${vacancy.title}",
                    message = "${vacancy.propertyTier} • ${vacancy.location} (${vacancy.experienceRequired}). Match score: ${match.matchScore}%.",
                    department = vacancy.resolvedCategory.displayLabel,
                    location = vacancy.location,
                    isRead = false
                )
                db.collection("users").document(cand.userId)
                    .collection("jobNotifications").document(notifId)
                    .set(notif.toCreateMap(cand.userId))
                    .await()

                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.VACANCY_MATCH.name,
                        recipientUserId = cand.userId,
                        recipientName = cand.fullName,
                        recipientEmail = cand.email,
                        roleId = roleId,
                        roleTitle = vacancy.title,
                        channel = NotificationChannelType.IN_APP.name,
                        status = DeliveryStatus.DELIVERED.name,
                        matchScore = match.matchScore.toLong(),
                        matchSummary = matchSummary,
                        detail = "Delivered to candidate MILA Talent inbox",
                        actorEmail = actor.email
                    )
                )
                logsWritten++
            }

            // 2. Android Push (FCM)
            val androidTokens = userTokens.filter { it.platform.equals("ANDROID", ignoreCase = true) && it.active }
            if (controls.enableAndroidPush && androidTokens.isNotEmpty()) {
                val anyPushOptIn = androidTokens.any { it.pushEnabled }
                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.VACANCY_MATCH.name,
                        recipientUserId = cand.userId,
                        recipientName = cand.fullName,
                        recipientEmail = cand.email,
                        roleId = roleId,
                        roleTitle = vacancy.title,
                        channel = NotificationChannelType.ANDROID_PUSH.name,
                        status = if (anyPushOptIn) DeliveryStatus.QUEUED.name else DeliveryStatus.SKIPPED.name,
                        matchScore = match.matchScore.toLong(),
                        matchSummary = matchSummary,
                        detail = if (anyPushOptIn) {
                            "FCM Android payload queued (${androidTokens.size} registered token)"
                        } else {
                            "Skipped: candidate muted mobile push"
                        },
                        actorEmail = actor.email
                    )
                )
                logsWritten++
            }

            // 3. iOS Push (APNs via FCM)
            val iosTokens = userTokens.filter { it.platform.equals("IOS", ignoreCase = true) && it.active }
            if (controls.enableIosPush && iosTokens.isNotEmpty()) {
                val anyPushOptIn = iosTokens.any { it.pushEnabled }
                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.VACANCY_MATCH.name,
                        recipientUserId = cand.userId,
                        recipientName = cand.fullName,
                        recipientEmail = cand.email,
                        roleId = roleId,
                        roleTitle = vacancy.title,
                        channel = NotificationChannelType.IOS_PUSH.name,
                        status = if (anyPushOptIn) DeliveryStatus.QUEUED.name else DeliveryStatus.SKIPPED.name,
                        matchScore = match.matchScore.toLong(),
                        matchSummary = matchSummary,
                        detail = if (anyPushOptIn) {
                            "FCM iOS/APNs payload queued (${iosTokens.size} registered token)"
                        } else {
                            "Skipped: candidate muted iOS push"
                        },
                        actorEmail = actor.email
                    )
                )
                logsWritten++
            }

            // 4. Optional Email Notification (only when candidate opted in AND admin enabled)
            val emailOptIn = userTokens.any { it.emailEnabled }
            if (controls.enableEmailNotifications && emailOptIn && cand.email.isNotBlank()) {
                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.VACANCY_MATCH.name,
                        recipientUserId = cand.userId,
                        recipientName = cand.fullName,
                        recipientEmail = cand.email,
                        roleId = roleId,
                        roleTitle = vacancy.title,
                        channel = NotificationChannelType.EMAIL.name,
                        status = DeliveryStatus.QUEUED.name,
                        matchScore = match.matchScore.toLong(),
                        matchSummary = matchSummary,
                        detail = "Optional email alert queued for ${cand.email}",
                        actorEmail = actor.email
                    )
                )
                logsWritten++
            }
        }

        // Trigger server-side Supabase Edge Function for live FCM HTTP v1 (Android + iOS) & Email dispatch
        if (!supabaseAccessToken.isNullOrBlank() && SupabaseConfig.isConfigured()) {
            triggerEdgeFunction(
                accessToken = supabaseAccessToken,
                payload = JSONObject().apply {
                    put("event_type", "VACANCY_PUBLISHED")
                    put("vacancy_id", vacancy.id)
                }
            )
        }

        adminRepository.logActivity(
            actor = actor,
            module = AdminModule.NOTIFICATION_MANAGEMENT,
            action = "AUTO_MATCH_DISPATCH",
            targetId = roleId,
            summary = "${vacancy.title}: notified ${eligible.size} matched candidates (excluded $excludedCount ineligible)"
        )

        Result.success(
            VacancyDispatchSummary(
                vacancyTitle = vacancy.title,
                totalEvaluated = evaluations.size,
                eligibleCount = eligible.size,
                excludedCount = excludedCount,
                logsWritten = logsWritten
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "notificationDeliveryLogs")
        Result.failure(e)
    }

    /**
     * Logs and triggers multi-channel Application Status notifications when an admin advances a candidate.
     */
    suspend fun logApplicationStatusDispatch(
        actor: AdminUser,
        application: JobApplication,
        newStatus: ApplicationStatus,
        deviceTokens: List<CandidateDeviceToken>,
        controls: NotificationControlSettings,
        supabaseAccessToken: String? = null
    ) {
        if (!controls.enableApplicationStatusAlerts) return
        val userTokens = deviceTokens.filter { it.userId == application.userId && it.active }
        val summary = "Application stage updated to ${newStatus.label}"

        if (controls.enableInApp) {
            runCatching {
                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.APPLICATION_STATUS.name,
                        recipientUserId = application.userId,
                        recipientName = application.candidateName,
                        recipientEmail = application.candidateEmail,
                        roleId = application.roleId,
                        roleTitle = application.roleTitle,
                        channel = NotificationChannelType.IN_APP.name,
                        status = DeliveryStatus.DELIVERED.name,
                        matchScore = 100L,
                        matchSummary = summary,
                        detail = "Delivered to candidate Alerts tab",
                        actorEmail = actor.email
                    )
                )
            }
        }

        val androidTokens = userTokens.filter { it.platform.equals("ANDROID", ignoreCase = true) && it.pushEnabled }
        if (controls.enableAndroidPush && androidTokens.isNotEmpty()) {
            runCatching {
                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.APPLICATION_STATUS.name,
                        recipientUserId = application.userId,
                        recipientName = application.candidateName,
                        recipientEmail = application.candidateEmail,
                        roleId = application.roleId,
                        roleTitle = application.roleTitle,
                        channel = NotificationChannelType.ANDROID_PUSH.name,
                        status = DeliveryStatus.QUEUED.name,
                        matchScore = 100L,
                        matchSummary = summary,
                        detail = "FCM Android status push queued",
                        actorEmail = actor.email
                    )
                )
            }
        }

        val iosTokens = userTokens.filter { it.platform.equals("IOS", ignoreCase = true) && it.pushEnabled }
        if (controls.enableIosPush && iosTokens.isNotEmpty()) {
            runCatching {
                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.APPLICATION_STATUS.name,
                        recipientUserId = application.userId,
                        recipientName = application.candidateName,
                        recipientEmail = application.candidateEmail,
                        roleId = application.roleId,
                        roleTitle = application.roleTitle,
                        channel = NotificationChannelType.IOS_PUSH.name,
                        status = DeliveryStatus.QUEUED.name,
                        matchScore = 100L,
                        matchSummary = summary,
                        detail = "FCM iOS/APNs status push queued",
                        actorEmail = actor.email
                    )
                )
            }
        }

        if (controls.enableEmailNotifications && userTokens.any { it.emailEnabled } && application.candidateEmail.isNotBlank()) {
            runCatching {
                recordLog(
                    NotificationDeliveryLog(
                        eventType = NotificationEventType.APPLICATION_STATUS.name,
                        recipientUserId = application.userId,
                        recipientName = application.candidateName,
                        recipientEmail = application.candidateEmail,
                        roleId = application.roleId,
                        roleTitle = application.roleTitle,
                        channel = NotificationChannelType.EMAIL.name,
                        status = DeliveryStatus.QUEUED.name,
                        matchScore = 100L,
                        matchSummary = summary,
                        detail = "Optional email status update queued for ${application.candidateEmail}",
                        actorEmail = actor.email
                    )
                )
            }
        }

        if (!supabaseAccessToken.isNullOrBlank() && SupabaseConfig.isConfigured()) {
            triggerEdgeFunction(
                accessToken = supabaseAccessToken,
                payload = JSONObject().apply {
                    put("event_type", "APPLICATION_STATUS_CHANGED")
                    put("application_id", application.id)
                    put("status_label", newStatus.label)
                }
            )
        }
    }

    private suspend fun recordLog(log: NotificationDeliveryLog) {
        val logId = "ndl_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        db.collection("notificationDeliveryLogs")
            .document(logId)
            .set(log.toCreateMap())
            .await()
    }

    private suspend fun triggerEdgeFunction(
        accessToken: String,
        payload: JSONObject
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${SupabaseConfig.normalizedBaseUrl()}/functions/v1/dispatch-job-notifications")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY.trim())
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(req).execute().close()
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
