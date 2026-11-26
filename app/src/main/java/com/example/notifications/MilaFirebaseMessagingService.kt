package com.example.notifications

import android.os.Build
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging receiver for MILA Talent mobile push notifications.
 *
 * Handles:
 * - Newly matched hospitality vacancy alerts (`VACANCY_MATCH`)
 * - Candidate application status transitions (`APPLICATION_STATUS`)
 * - Automatic FCM token rotation persistence for authenticated candidates
 */
class MilaFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbId = runCatching { getString(R.string.firestore_database_id) }.getOrDefault("(default)")
        val db = FirebaseFirestore.getInstance(dbId)
        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80)
        val record = CandidateDeviceToken(
            userId = uid,
            fcmToken = token,
            platform = "ANDROID",
            deviceLabel = deviceLabel,
            active = true,
            pushEnabled = true
        )
        serviceScope.launch {
            runCatching {
                db.collection("users").document(uid)
                    .collection("deviceTokens").document("android_primary")
                    .set(record.toUpsertMap(uid))
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val eventType = message.data["event_type"].orEmpty()
        val channelId = if (eventType.equals("APPLICATION_STATUS", ignoreCase = true)) {
            MilaNotificationChannels.CHANNEL_APPLICATION_STATUS
        } else {
            MilaNotificationChannels.CHANNEL_JOB_MATCHES
        }
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "MILA Talent Update"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Open MILA Hospitality to view your latest recruitment alert."
        val targetRoute = message.data["route"]
            ?: if (eventType.equals("APPLICATION_STATUS", ignoreCase = true)) "job_seeker_login" else "careers"

        MilaNotificationChannels.showAndroidNotification(
            context = applicationContext,
            channelId = channelId,
            title = title,
            body = body,
            targetRoute = targetRoute
        )
    }
}
