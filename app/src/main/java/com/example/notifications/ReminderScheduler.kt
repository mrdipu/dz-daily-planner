package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.ReminderEntity

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleReminder(reminder: ReminderEntity) {
        if (reminder.isCompleted) {
            cancelReminder(reminder.id)
            return
        }

        val effectiveTime = reminder.snoozedUntilMillis ?: reminder.triggerAtMillis
        val triggerAt = maxOf(effectiveTime, System.currentTimeMillis() + 1_000L)

        val pendingIntent = buildAlarmPendingIntent(reminder.id)
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminderId: Long) {
        val pendingIntent = buildAlarmPendingIntent(reminderId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        NotificationHelper.cancelNotification(context, reminderId)
    }

    private fun buildAlarmPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
        }
        val requestCode = ((reminderId % 90_000L) + 1_000L).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
