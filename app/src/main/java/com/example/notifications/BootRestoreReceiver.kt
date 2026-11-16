package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.DZPlannerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (action !in supportedActions) return

        val pendingResult = goAsync()
        val app = context.applicationContext as DZPlannerApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.plannerRepository.rescheduleAllPendingReminders()
            } catch (_: Throwable) {
                // Keep boot restore resilient
            } finally {
                pendingResult.finish()
            }
        }
    }
}
