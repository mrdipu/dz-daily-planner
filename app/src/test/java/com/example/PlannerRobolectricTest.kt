package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.local.ChecklistItem
import com.example.data.local.ChecklistSerializer
import com.example.data.local.EventCategory
import com.example.data.local.EventEntity
import com.example.data.local.NoteCategory
import com.example.data.local.NoteEntity
import com.example.data.local.PlannerDatabase
import com.example.data.local.RecurrenceRule
import com.example.data.local.ReminderEntity
import com.example.data.local.RepeatType
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.PreferencesRepository
import com.example.data.prefs.ThemeMode
import com.example.data.repository.PlannerRepository
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlannerRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: PlannerDatabase
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var scheduler: ReminderScheduler
    private lateinit var repository: PlannerRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PlannerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = PreferencesRepository(context)
        scheduler = ReminderScheduler(context)
        val backupManager = BackupManager(database.plannerDao(), prefsRepo)
        repository = PlannerRepository(database.plannerDao(), prefsRepo, scheduler, backupManager)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `notes create edit checklist trash and restore persist accurately`() = runBlocking {
        val checklist = listOf(
            ChecklistItem("1", "Review architecture", true),
            ChecklistItem("2", "Ship release", false)
        )
        val noteId = repository.saveNote(
            NoteEntity(
                title = "Sprint Plan",
                content = "• Prepare demo",
                checklistJson = ChecklistSerializer.encode(checklist),
                category = NoteCategory.OFFICE,
                colorKey = "royal_blue",
                isPinned = true,
                isFavorite = true
            )
        )
        assertTrue(noteId > 0L)

        val saved = repository.getNoteById(noteId)
        assertNotNull(saved)
        assertEquals("Sprint Plan", saved!!.title)
        assertEquals(2, ChecklistSerializer.decode(saved.checklistJson).size)

        repository.moveNoteToTrash(saved)
        assertTrue(repository.getNoteById(noteId)!!.isDeleted)

        repository.restoreNoteFromTrash(repository.getNoteById(noteId)!!)
        assertFalse(repository.getNoteById(noteId)!!.isDeleted)
    }

    @Test
    fun `event creation synchronizes multiple reminders and schedules alarms`() = runBlocking {
        val start = System.currentTimeMillis() + 3_600_000L
        val eventId = repository.saveEvent(
            EventEntity(
                title = "Client Presentation",
                description = "Quarterly review",
                startEpochMillis = start,
                endEpochMillis = start + 3_600_000L,
                location = "Dhaka HQ",
                category = EventCategory.MEETING,
                recurrence = RecurrenceRule.WEEKLY,
                reminderOffsetsMinutes = listOf(15, 60)
            )
        )

        val reminders = repository.remindersFlow.first().filter { it.linkedEventId == eventId }
        assertEquals(2, reminders.size)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        assertTrue(shadowAlarmManager.scheduledAlarms.size >= 2)

        val event = repository.eventsFlow.first().first { it.id == eventId }
        repository.toggleEventCompleted(event)
        val updatedReminders = repository.remindersFlow.first().filter { it.linkedEventId == eventId }
        assertTrue(updatedReminders.all { it.isCompleted })
    }

    @Test
    fun `snooze and repeating reminder completion update trigger time properly`() = runBlocking {
        val now = System.currentTimeMillis()
        val id = repository.saveReminder(
            ReminderEntity(
                title = "Daily Standup",
                triggerAtMillis = now - 60_000L,
                repeatType = RepeatType.DAILY
            )
        )

        repository.snoozeReminder(id, 15)
        val snoozed = repository.getReminderById(id)!!
        assertNotNull(snoozed.snoozedUntilMillis)
        assertTrue(snoozed.snoozedUntilMillis!! > now + 10 * 60_000L)

        repository.completeReminder(snoozed)
        val advanced = repository.getReminderById(id)!!
        assertFalse(advanced.isCompleted)
        assertTrue(advanced.triggerAtMillis > now)
    }

    @Test
    fun `json backup export and restore roundtrip preserves all entities and preferences`() = runBlocking {
        prefsRepo.setThemeMode(ThemeMode.DARK)
        prefsRepo.setLanguage(AppLanguage.BANGLA)
        prefsRepo.setUse24HourFormat(true)

        repository.saveNote(NoteEntity(title = "Backup Note", content = "Important text"))
        val start = System.currentTimeMillis() + 7_200_000L
        repository.saveEvent(
            EventEntity(
                title = "Backup Event",
                startEpochMillis = start,
                endEpochMillis = start + 1_800_000L,
                reminderOffsetsMinutes = listOf(10)
            )
        )

        val json = repository.exportBackupJson()
        database.clearAllTables()
        prefsRepo.setThemeMode(ThemeMode.LIGHT)
        prefsRepo.setLanguage(AppLanguage.ENGLISH)

        val restoreResult = repository.importBackupJson(json)
        assertTrue(restoreResult.isSuccess)
        val summary = restoreResult.getOrThrow()
        assertEquals(1, summary.notesCount)
        assertEquals(1, summary.eventsCount)
        assertEquals(1, summary.remindersCount)

        val restoredPrefs = prefsRepo.getCurrentPreferences()
        assertEquals(ThemeMode.DARK, restoredPrefs.themeMode)
        assertEquals(AppLanguage.BANGLA, restoredPrefs.language)
        assertTrue(restoredPrefs.use24HourFormat)
    }

    @Test
    @Config(sdk = [29])
    fun `android 10 api 29 exact alarm and notification channel compatibility`() = runBlocking {
        assertTrue(scheduler.canScheduleExactAlarms())
        com.example.notifications.NotificationHelper.ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        assertNotNull(nm.getNotificationChannel(com.example.notifications.NotificationHelper.CHANNEL_ID_REMINDERS))

        val id = repository.saveReminder(
            ReminderEntity(
                title = "Galaxy M20 Reminder",
                triggerAtMillis = System.currentTimeMillis() + 60_000L
            )
        )
        assertTrue(id > 0L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isNotEmpty())
    }
}
