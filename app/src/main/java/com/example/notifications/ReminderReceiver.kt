package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.DZPlannerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.example.ACTION_COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.ACTION_SNOOZE_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as DZPlannerApp
        val repo = app.container.plannerRepository
        val prefsRepo = app.container.preferencesRepository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TRIGGER_REMINDER -> {
                        val reminder = repo.getReminderById(reminderId) ?: return@launch
                        if (reminder.isCompleted) return@launch

                        val now = System.currentTimeMillis()
                        val lastNotified = reminder.lastNotifiedMillis ?: 0L
                        // Duplicate guard within 30 seconds
                        if (now - lastNotified < 30_000L) return@launch

                        val prefs = prefsRepo.getCurrentPreferences()
                        NotificationHelper.showReminderNotification(context, reminder, prefs)
                        repo.markReminderNotified(reminder, now)
                    }

                    ACTION_COMPLETE_REMINDER -> {
                        NotificationHelper.cancelNotification(context, reminderId)
                        repo.completeReminderById(reminderId)
                    }

                    ACTION_SNOOZE_REMINDER -> {
                        val snoozeMinutes = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 10)
                        NotificationHelper.cancelNotification(context, reminderId)
                        repo.snoozeReminder(reminderId, snoozeMinutes)
                    }
                }
            } catch (_: Throwable) {
                // Keep receiver resilient on background delivery
            } finally {
                pendingResult.finish()
            }
        }
    }
}
