package com.example.notifications

import androidx.annotation.Keep
import com.example.data.CandidateProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class NotificationChannelType(val label: String) {
    IN_APP("In-App"),
    ANDROID_PUSH("Android Push (FCM)"),
    IOS_PUSH("iOS Push (APNs/FCM)"),
    EMAIL("Email");

    companion object {
        fun fromRaw(raw: String): NotificationChannelType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: IN_APP
    }
}

enum class DeliveryStatus(val label: String) {
    DELIVERED("Delivered"),
    QUEUED("Queued"),
    SKIPPED("Skipped"),
    FAILED("Failed");

    companion object {
        fun fromRaw(raw: String): DeliveryStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: QUEUED
    }
}

enum class NotificationEventType(val label: String) {
    VACANCY_MATCH("Vacancy Match"),
    APPLICATION_STATUS("Application Status"),
    ADMIN_DIRECT("Admin Direct");

    companion object {
        fun fromRaw(raw: String): NotificationEventType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: VACANCY_MATCH
    }
}

@Keep
@IgnoreExtraProperties
data class NotificationControlSettings(
    @DocumentId val id: String = "notifications",
    val autoDispatchOnPublish: Boolean = true,
    val enableInApp: Boolean = true,
    val enableAndroidPush: Boolean = true,
    val enableIosPush: Boolean = true,
    val enableEmailNotifications: Boolean = true,
    val enableApplicationStatusAlerts: Boolean = true,
    val requireDepartmentMatch: Boolean = true,
    val requireLocationMatch: Boolean = true,
    val experienceToleranceYears: Long = 1L,
    val dailyCapPerCandidate: Long = 5L,
    val updatedAt: Timestamp? = null
) {
    fun toAdminMap(): Map<String, Any> = mapOf(
        "autoDispatchOnPublish" to autoDispatchOnPublish,
        "enableInApp" to enableInApp,
        "enableAndroidPush" to enableAndroidPush,
        "enableIosPush" to enableIosPush,
        "enableEmailNotifications" to enableEmailNotifications,
        "enableApplicationStatusAlerts" to enableApplicationStatusAlerts,
        "requireDepartmentMatch" to requireDepartmentMatch,
        "requireLocationMatch" to requireLocationMatch,
        "experienceToleranceYears" to experienceToleranceYears.coerceIn(0L, 5L),
        "dailyCapPerCandidate" to dailyCapPerCandidate.coerceIn(1L, 50L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class NotificationDeliveryLog(
    @DocumentId val id: String = "",
    val eventType: String = NotificationEventType.VACANCY_MATCH.name,
    val recipientUserId: String = "",
    val recipientName: String = "",
    val recipientEmail: String = "",
    val roleId: String = "",
    val roleTitle: String = "",
    val channel: String = NotificationChannelType.IN_APP.name,
    val status: String = DeliveryStatus.DELIVERED.name,
    val matchScore: Long = 0L,
    val matchSummary: String = "",
    val detail: String = "",
    val actorEmail: String = "",
    val createdAt: Timestamp? = null
) {
    val resolvedEventType: NotificationEventType
        get() = NotificationEventType.fromRaw(eventType)

    val resolvedChannel: NotificationChannelType
        get() = NotificationChannelType.fromRaw(channel)

    val resolvedStatus: DeliveryStatus
        get() = DeliveryStatus.fromRaw(status)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "eventType" to resolvedEventType.name,
        "recipientUserId" to recipientUserId.trim().ifBlank { "unknown" },
        "recipientName" to recipientName.trim().ifBlank { "Candidate" }.take(120),
        "recipientEmail" to recipientEmail.trim().take(120),
        "roleId" to roleId.trim().ifBlank { "general" }.take(128),
        "roleTitle" to roleTitle.trim().ifBlank { "Hospitality Mandate" }.take(140),
        "channel" to resolvedChannel.name,
        "status" to resolvedStatus.name,
        "matchScore" to matchScore.coerceIn(0L, 100L),
        "matchSummary" to matchSummary.trim().take(300),
        "detail" to detail.trim().take(400),
        "actorEmail" to actorEmail.trim().ifBlank { "system@milahospitality.in" }.take(120),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CandidateDeviceToken(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fcmToken: String = "",
    val platform: String = "ANDROID",
    val deviceLabel: String = "Android Device",
    val active: Boolean = true,
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val updatedAt: Timestamp? = null
) {
    fun toUpsertMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "fcmToken" to fcmToken.trim().take(512),
        "platform" to if (platform.equals("IOS", ignoreCase = true)) "IOS" else "ANDROID",
        "deviceLabel" to deviceLabel.trim().ifBlank { "Mobile Device" }.take(120),
        "active" to active,
        "pushEnabled" to pushEnabled,
        "emailEnabled" to emailEnabled,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class CandidateMatchResult(
    val candidate: CandidateProfile,
    val eligible: Boolean,
    val matchScore: Int,
    val matchedCriteria: List<String>,
    val exclusionReasons: List<String>
)

data class VacancyDispatchSummary(
    val vacancyTitle: String,
    val totalEvaluated: Int,
    val eligibleCount: Int,
    val excludedCount: Int,
    val logsWritten: Int
)
