package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object MilaNotificationChannels {

    const val CHANNEL_JOB_MATCHES = "mila_job_matches"
    const val CHANNEL_APPLICATION_STATUS = "mila_application_updates"
    const val EXTRA_NOTIFICATION_ROUTE = "mila_notification_route"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val jobMatchesChannel = NotificationChannel(
            CHANNEL_JOB_MATCHES,
            "Matched Job Vacancies",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for newly published MILA hospitality mandates matching your department, location, and experience."
            enableVibration(true)
        }

        val statusChannel = NotificationChannel(
            CHANNEL_APPLICATION_STATUS,
            "Application Status Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Real-time updates when MILA Talent shortlists, schedules interviews, or updates your application."
            enableVibration(true)
        }

        manager.createNotificationChannel(jobMatchesChannel)
        manager.createNotificationChannel(statusChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showAndroidNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        targetRoute: String = "job_seeker_login",
        notificationId: Int = (System.currentTimeMillis() and 0xFFFFFF).toInt()
    ): Boolean {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ROUTE, targetRoute)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
