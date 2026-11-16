package com.example.data.repository

import com.example.data.backup.BackupManager
import com.example.data.backup.BackupSummary
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PlannerDao
import com.example.data.local.RecurrenceRule
import com.example.data.local.ReminderEntity
import com.example.data.local.RepeatType
import com.example.data.prefs.PreferencesRepository
import com.example.notifications.ReminderScheduler
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.TimeZone

class PlannerRepository(
    private val dao: PlannerDao,
    private val preferencesRepository: PreferencesRepository,
    private val scheduler: ReminderScheduler,
    private val backupManager: BackupManager
) {

    val notesFlow: Flow<List<NoteEntity>> = dao.observeAllNotes()
    val eventsFlow: Flow<List<EventEntity>> = dao.observeAllEvents()
    val remindersFlow: Flow<List<ReminderEntity>> = dao.observeAllReminders()

    // -------- NOTES --------

    suspend fun getNoteById(id: Long): NoteEntity? = dao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity): Long {
        val now = System.currentTimeMillis()
        return if (note.id == 0L) {
            dao.insertNote(note.copy(createdAtMillis = now, updatedAtMillis = now))
        } else {
            val updated = note.copy(updatedAtMillis = now)
            dao.updateNote(updated)
            note.id
        }
    }

    suspend fun duplicateNote(note: NoteEntity): Long {
        val now = System.currentTimeMillis()
        val copy = note.copy(
            id = 0L,
            title = if (note.title.isBlank()) "Copy" else "${note.title} (Copy)",
            isPinned = false,
            isDeleted = false,
            deletedAtMillis = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        return dao.insertNote(copy)
    }

    suspend fun toggleNotePin(note: NoteEntity) {
        dao.updateNote(note.copy(isPinned = !note.isPinned, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun toggleNoteFavorite(note: NoteEntity) {
        dao.updateNote(note.copy(isFavorite = !note.isFavorite, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun toggleNoteArchive(note: NoteEntity) {
        dao.updateNote(note.copy(isArchived = !note.isArchived, isPinned = false, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun moveNoteToTrash(note: NoteEntity) {
        val now = System.currentTimeMillis()
        dao.updateNote(
            note.copy(
                isDeleted = true,
                isPinned = false,
                deletedAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    suspend fun restoreNoteFromTrash(note: NoteEntity) {
        dao.updateNote(
            note.copy(
                isDeleted = false,
                deletedAtMillis = null,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun permanentlyDeleteNote(noteId: Long) {
        dao.hardDeleteNote(noteId)
    }

    suspend fun emptyRecycleBin() {
        dao.emptyRecycleBin()
    }

    // -------- EVENTS --------

    suspend fun saveEvent(event: EventEntity): Long {
        val now = System.currentTimeMillis()
        val savedId = if (event.id == 0L) {
            dao.insertEvent(event.copy(createdAtMillis = now, updatedAtMillis = now))
        } else {
            dao.updateEvent(event.copy(updatedAtMillis = now))
            event.id
        }
        val persistedEvent = event.copy(id = savedId)
        syncRemindersForEvent(persistedEvent)
        return savedId
    }

    suspend fun toggleEventCompleted(event: EventEntity) {
        val nextCompleted = !event.isCompleted
        val updated = event.copy(
            isCompleted = nextCompleted,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.updateEvent(updated)
        val linkedReminders = dao.getRemindersForEvent(event.id)
        if (nextCompleted) {
            linkedReminders.forEach { reminder ->
                val done = reminder.copy(
                    isCompleted = true,
                    completedAtMillis = System.currentTimeMillis(),
                    snoozedUntilMillis = null,
                    updatedAtMillis = System.currentTimeMillis()
                )
                dao.updateReminder(done)
                scheduler.cancelReminder(reminder.id)
            }
        } else {
            syncRemindersForEvent(updated)
        }
    }

    suspend fun deleteEvent(eventId: Long) {
        val linkedReminders = dao.getRemindersForEvent(eventId)
        linkedReminders.forEach { scheduler.cancelReminder(it.id) }
        dao.deleteRemindersByEventId(eventId)
        dao.deleteEventById(eventId)
    }

    private suspend fun syncRemindersForEvent(event: EventEntity) {
        val existing = dao.getRemindersForEvent(event.id)
        existing.forEach { scheduler.cancelReminder(it.id) }
        dao.deleteRemindersByEventId(event.id)

        if (event.isCompleted || event.reminderOffsetsMinutes.isEmpty()) return

        val repeatType = when (event.recurrence) {
            RecurrenceRule.NONE -> RepeatType.NONE
            RecurrenceRule.DAILY -> RepeatType.DAILY
            RecurrenceRule.WEEKLY -> RepeatType.WEEKLY
            RecurrenceRule.MONTHLY -> RepeatType.MONTHLY
            RecurrenceRule.YEARLY -> RepeatType.CUSTOM_DAYS
        }
        val customDays = if (event.recurrence == RecurrenceRule.YEARLY) 365 else 1

        val now = System.currentTimeMillis()
        event.reminderOffsetsMinutes.distinct().forEach { offsetMin ->
            val triggerAt = event.startEpochMillis - offsetMin * 60_000L
            val label = if (offsetMin == 0) {
                "Starting now"
            } else if (offsetMin >= 60 && offsetMin % 60 == 0) {
                val hrs = offsetMin / 60
                "In $hrs hour${if (hrs > 1) "s" else ""}"
            } else {
                "In $offsetMin min"
            }
            val desc = buildString {
                append(label)
                if (event.location.isNotBlank()) {
                    append(" · ")
                    append(event.location)
                }
            }
            val reminder = ReminderEntity(
                title = event.title,
                description = desc,
                triggerAtMillis = triggerAt,
                repeatType = repeatType,
                customIntervalDays = customDays,
                linkedEventId = event.id,
                linkedNoteId = event.attachedNoteId,
                eventOffsetMinutes = offsetMin,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            val insertedId = dao.insertReminder(reminder)
            val saved = reminder.copy(id = insertedId)
            if (saved.triggerAtMillis > now - 60_000L) {
                scheduler.scheduleReminder(saved)
            }
        }
    }

    // -------- REMINDERS --------

    suspend fun getReminderById(id: Long): ReminderEntity? = dao.getReminderById(id)

    suspend fun saveReminder(reminder: ReminderEntity): Long {
        val now = System.currentTimeMillis()
        val id = if (reminder.id == 0L) {
            dao.insertReminder(reminder.copy(createdAtMillis = now, updatedAtMillis = now))
        } else {
            dao.updateReminder(reminder.copy(updatedAtMillis = now))
            reminder.id
        }
        val saved = reminder.copy(id = id)
        if (saved.isCompleted) {
            scheduler.cancelReminder(saved.id)
        } else {
            scheduler.scheduleReminder(saved)
        }
        return id
    }

    suspend fun completeReminderById(reminderId: Long) {
        val reminder = dao.getReminderById(reminderId) ?: return
        completeReminder(reminder)
    }

    suspend fun completeReminder(reminder: ReminderEntity) {
        val now = System.currentTimeMillis()
        if (reminder.repeatType == RepeatType.NONE) {
            val completed = reminder.copy(
                isCompleted = true,
                completedAtMillis = now,
                snoozedUntilMillis = null,
                updatedAtMillis = now
            )
            dao.updateReminder(completed)
            scheduler.cancelReminder(reminder.id)
        } else {
            val prefs = preferencesRepository.getCurrentPreferences()
            val tz = DateTimeUtils.resolveTimeZone(prefs.useDeviceTimezone, prefs.customTimezoneId)
            val nextTrigger = computeNextTrigger(reminder, now, tz)
            val updated = reminder.copy(
                triggerAtMillis = nextTrigger,
                snoozedUntilMillis = null,
                lastNotifiedMillis = null,
                updatedAtMillis = now
            )
            dao.updateReminder(updated)
            scheduler.scheduleReminder(updated)
        }
    }

    suspend fun reopenReminder(reminder: ReminderEntity) {
        val now = System.currentTimeMillis()
        val nextTime = maxOf(reminder.triggerAtMillis, now + 60_000L)
        val reopened = reminder.copy(
            isCompleted = false,
            completedAtMillis = null,
            snoozedUntilMillis = null,
            lastNotifiedMillis = null,
            triggerAtMillis = nextTime,
            updatedAtMillis = now
        )
        dao.updateReminder(reopened)
        scheduler.scheduleReminder(reopened)
    }

    suspend fun snoozeReminder(reminderId: Long, minutes: Int) {
        val reminder = dao.getReminderById(reminderId) ?: return
        val now = System.currentTimeMillis()
        val snoozedUntil = now + minutes.coerceAtLeast(1) * 60_000L
        val updated = reminder.copy(
            snoozedUntilMillis = snoozedUntil,
            lastNotifiedMillis = null,
            isCompleted = false,
            updatedAtMillis = now
        )
        dao.updateReminder(updated)
        scheduler.scheduleReminder(updated)
    }

    suspend fun rescheduleReminder(reminder: ReminderEntity, newTriggerAtMillis: Long) {
        val now = System.currentTimeMillis()
        val updated = reminder.copy(
            triggerAtMillis = newTriggerAtMillis,
            snoozedUntilMillis = null,
            lastNotifiedMillis = null,
            isCompleted = false,
            completedAtMillis = null,
            updatedAtMillis = now
        )
        dao.updateReminder(updated)
        scheduler.scheduleReminder(updated)
    }

    suspend fun markReminderNotified(reminder: ReminderEntity, notifiedAtMillis: Long) {
        dao.updateReminder(
            reminder.copy(
                lastNotifiedMillis = notifiedAtMillis,
                snoozedUntilMillis = null,
                updatedAtMillis = notifiedAtMillis
            )
        )
    }

    suspend fun deleteReminder(reminderId: Long) {
        scheduler.cancelReminder(reminderId)
        dao.deleteReminderById(reminderId)
    }

    suspend fun rescheduleAllPendingReminders() {
        val pending = dao.getPendingReminders()
        val now = System.currentTimeMillis()
        pending.forEach { reminder ->
            val effective = reminder.snoozedUntilMillis ?: reminder.triggerAtMillis
            if (effective > now - 120_000L) {
                scheduler.scheduleReminder(reminder)
            }
        }
    }

    private fun computeNextTrigger(
        reminder: ReminderEntity,
        nowMillis: Long,
        tz: TimeZone
    ): Long {
        var candidate = reminder.triggerAtMillis
        var safety = 0
        while (candidate <= nowMillis && safety < 500) {
            candidate = when (reminder.repeatType) {
                RepeatType.NONE -> candidate
                RepeatType.DAILY -> DateTimeUtils.addDays(candidate, 1, tz)
                RepeatType.WEEKLY -> DateTimeUtils.addDays(candidate, 7, tz)
                RepeatType.MONTHLY -> DateTimeUtils.addMonths(candidate, 1, tz)
                RepeatType.CUSTOM_DAYS -> DateTimeUtils.addDays(
                    candidate,
                    reminder.customIntervalDays.coerceAtLeast(1),
                    tz
                )
            }
            safety++
        }
        return candidate
    }

    // -------- BACKUP & RESTORE --------

    suspend fun exportBackupJson(): String = backupManager.exportToJson()

    suspend fun importBackupJson(rawJson: String): Result<BackupSummary> {
        val result = backupManager.importFromJson(rawJson)
        if (result.isSuccess) {
            rescheduleAllPendingReminders()
        }
        return result
    }
}
