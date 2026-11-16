package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // -------- NOTES --------
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSnapshot(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDeleteNote(id: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    // -------- EVENTS --------
    @Query("SELECT * FROM events ORDER BY startEpochMillis ASC")
    fun observeAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAllEventsSnapshot(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()

    // -------- REMINDERS --------
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, triggerAtMillis ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersSnapshot(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0")
    suspend fun getPendingReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE linkedEventId = :eventId")
    suspend fun getRemindersForEvent(eventId: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM reminders WHERE linkedEventId = :eventId")
    suspend fun deleteRemindersByEventId(eventId: Long)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()

    @Transaction
    suspend fun replaceAllData(
        notes: List<NoteEntity>,
        events: List<EventEntity>,
        reminders: List<ReminderEntity>
    ) {
        clearAllReminders()
        clearAllEvents()
        clearAllNotes()
        notes.forEach { insertNote(it) }
        events.forEach { insertEvent(it) }
        reminders.forEach { insertReminder(it) }
    }
}
