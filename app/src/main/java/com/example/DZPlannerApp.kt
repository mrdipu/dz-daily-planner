package com.example

import android.app.Application
import com.example.data.backup.BackupManager
import com.example.data.local.PlannerDatabase
import com.example.data.prefs.PreferencesRepository
import com.example.data.repository.PlannerRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler

class AppContainer(application: Application) {
    val database: PlannerDatabase by lazy { PlannerDatabase.getInstance(application) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(application) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(application) }
    val backupManager: BackupManager by lazy {
        BackupManager(database.plannerDao(), preferencesRepository)
    }
    val plannerRepository: PlannerRepository by lazy {
        PlannerRepository(
            dao = database.plannerDao(),
            preferencesRepository = preferencesRepository,
            scheduler = reminderScheduler,
            backupManager = backupManager
        )
    }
}

class DZPlannerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannels(this)
    }
}
