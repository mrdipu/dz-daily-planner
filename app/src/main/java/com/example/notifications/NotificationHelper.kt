package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.ReminderEntity
import com.example.data.prefs.UserPreferences

object NotificationHelper {

    const val CHANNEL_ID_REMINDERS = "dz_reminders_channel_v1"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    const val TAB_REMINDERS = "reminders"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID_REMINDERS)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    enableVibration(true)
                    enableLights(true)
                    lightColor = ContextCompat.getColor(context, R.color.dz_royal_blue)
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showReminderNotification(
        context: Context,
        reminder: ReminderEntity,
        prefs: UserPreferences
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val notificationId = notificationIdFor(reminder.id)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_OPEN_TAB, TAB_REMINDERS)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_COMPLETE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100_000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeMins = prefs.defaultSnoozeMinutes.coerceIn(5, 60)
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMins)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = reminder.description.ifBlank {
            context.getString(R.string.app_name)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_dz)
            .setColor(ContextCompat.getColor(context, R.color.dz_royal_blue))
            .setContentTitle(reminder.title.ifBlank { context.getString(R.string.app_name) })
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0,
                context.getString(R.string.action_complete),
                completePendingIntent
            )
            .addAction(
                0,
                "${context.getString(R.string.action_snooze)} ${snoozeMins}m",
                snoozePendingIntent
            )

        if (!prefs.soundEnabled) {
            builder.setSilent(true)
        } else {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        if (prefs.vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 220, 120, 220))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun showTestNotification(context: Context, title: String, message: String) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_REMINDERS)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            999_001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_dz)
            .setColor(ContextCompat.getColor(context, R.color.dz_royal_blue))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(999_001, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(reminderId))
    }

    private fun notificationIdFor(reminderId: Long): Int =
        ((reminderId % 90_000L) + 1_000L).toInt()
}
