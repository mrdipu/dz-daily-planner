package com.example.data.backup

import com.example.data.local.EventCategory
import com.example.data.local.EventEntity
import com.example.data.local.NoteCategory
import com.example.data.local.NoteEntity
import com.example.data.local.PlannerDao
import com.example.data.local.RecurrenceRule
import com.example.data.local.ReminderEntity
import com.example.data.local.RepeatType
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.PreferencesRepository
import com.example.data.prefs.ThemeMode
import com.example.data.prefs.UserPreferences
import com.example.util.DateTimeUtils
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val notesCount: Int,
    val eventsCount: Int,
    val remindersCount: Int
)

class BackupManager(
    private val dao: PlannerDao,
    private val preferencesRepository: PreferencesRepository
) {
    companion object {
        private const val BACKUP_FORMAT_VERSION = 1
    }

    suspend fun exportToJson(): String {
        val notes = dao.getAllNotesSnapshot()
        val events = dao.getAllEventsSnapshot()
        val reminders = dao.getAllRemindersSnapshot()
        val prefs = preferencesRepository.getCurrentPreferences()

        val root = JSONObject().apply {
            put("app", "DZ Daily Planner")
            put("developer", "DZ Ai-STUDIO")
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())

            put("preferences", JSONObject().apply {
                put("themeMode", prefs.themeMode.name)
                put("use24HourFormat", prefs.use24HourFormat)
                put("language", prefs.language.name)
                put("useDeviceTimezone", prefs.useDeviceTimezone)
                put("customTimezoneId", prefs.customTimezoneId)
                put("defaultReminderLeadMinutes", prefs.defaultReminderLeadMinutes)
                put("defaultSnoozeMinutes", prefs.defaultSnoozeMinutes)
                put("soundEnabled", prefs.soundEnabled)
                put("vibrationEnabled", prefs.vibrationEnabled)
            })

            put("notes", JSONArray().apply {
                notes.forEach { n ->
                    put(JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("content", n.content)
                        put("checklistJson", n.checklistJson)
                        put("category", n.category.name)
                        put("colorKey", n.colorKey)
                        put("isPinned", n.isPinned)
                        put("isFavorite", n.isFavorite)
                        put("isArchived", n.isArchived)
                        put("isDeleted", n.isDeleted)
                        if (n.deletedAtMillis != null) put("deletedAtMillis", n.deletedAtMillis) else put("deletedAtMillis", JSONObject.NULL)
                        if (n.attachedDateEpochDay != null) put("attachedDateEpochDay", n.attachedDateEpochDay) else put("attachedDateEpochDay", JSONObject.NULL)
                        put("createdAtMillis", n.createdAtMillis)
                        put("updatedAtMillis", n.updatedAtMillis)
                    })
                }
            })

            put("events", JSONArray().apply {
                events.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("title", e.title)
                        put("description", e.description)
                        put("startEpochMillis", e.startEpochMillis)
                        put("endEpochMillis", e.endEpochMillis)
                        put("isAllDay", e.isAllDay)
                        put("location", e.location)
                        put("category", e.category.name)
                        put("colorKey", e.colorKey)
                        put("recurrence", e.recurrence.name)
                        put("reminderOffsetsMinutes", JSONArray(e.reminderOffsetsMinutes))
                        if (e.attachedNoteId != null) put("attachedNoteId", e.attachedNoteId) else put("attachedNoteId", JSONObject.NULL)
                        put("isCompleted", e.isCompleted)
                        put("createdAtMillis", e.createdAtMillis)
                        put("updatedAtMillis", e.updatedAtMillis)
                    })
                }
            })

            put("reminders", JSONArray().apply {
                reminders.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("description", r.description)
                        put("triggerAtMillis", r.triggerAtMillis)
                        put("repeatType", r.repeatType.name)
                        put("customIntervalDays", r.customIntervalDays)
                        if (r.linkedEventId != null) put("linkedEventId", r.linkedEventId) else put("linkedEventId", JSONObject.NULL)
                        if (r.linkedNoteId != null) put("linkedNoteId", r.linkedNoteId) else put("linkedNoteId", JSONObject.NULL)
                        if (r.eventOffsetMinutes != null) put("eventOffsetMinutes", r.eventOffsetMinutes) else put("eventOffsetMinutes", JSONObject.NULL)
                        put("isCompleted", r.isCompleted)
                        if (r.completedAtMillis != null) put("completedAtMillis", r.completedAtMillis) else put("completedAtMillis", JSONObject.NULL)
                        if (r.snoozedUntilMillis != null) put("snoozedUntilMillis", r.snoozedUntilMillis) else put("snoozedUntilMillis", JSONObject.NULL)
                        if (r.lastNotifiedMillis != null) put("lastNotifiedMillis", r.lastNotifiedMillis) else put("lastNotifiedMillis", JSONObject.NULL)
                        put("createdAtMillis", r.createdAtMillis)
                        put("updatedAtMillis", r.updatedAtMillis)
                    })
                }
            })
        }

        return root.toString(2)
    }

    suspend fun importFromJson(rawJson: String): Result<BackupSummary> = runCatching {
        val root = JSONObject(rawJson)
        val version = root.optInt("formatVersion", -1)
        require(version >= 1 && root.has("notes") && root.has("events") && root.has("reminders")) {
            "Invalid DZ Daily Planner backup file."
        }

        val notesJson = root.getJSONArray("notes")
        val notes = buildList {
            for (i in 0 until notesJson.length()) {
                val obj = notesJson.getJSONObject(i)
                add(
                    NoteEntity(
                        id = obj.getLong("id"),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        checklistJson = obj.optString("checklistJson", "[]"),
                        category = runCatching {
                            NoteCategory.valueOf(obj.optString("category", NoteCategory.PERSONAL.name))
                        }.getOrDefault(NoteCategory.PERSONAL),
                        colorKey = obj.optString("colorKey", "default"),
                        isPinned = obj.optBoolean("isPinned", false),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deletedAtMillis = if (obj.isNull("deletedAtMillis")) null else obj.optLong("deletedAtMillis"),
                        attachedDateEpochDay = if (obj.isNull("attachedDateEpochDay")) null else obj.optLong("attachedDateEpochDay"),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        val eventsJson = root.getJSONArray("events")
        val events = buildList {
            for (i in 0 until eventsJson.length()) {
                val obj = eventsJson.getJSONObject(i)
                val offsetsArray = obj.optJSONArray("reminderOffsetsMinutes") ?: JSONArray()
                val offsets = buildList {
                    for (j in 0 until offsetsArray.length()) {
                        add(offsetsArray.optInt(j, 15))
                    }
                }
                add(
                    EventEntity(
                        id = obj.getLong("id"),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        startEpochMillis = obj.getLong("startEpochMillis"),
                        endEpochMillis = obj.getLong("endEpochMillis"),
                        isAllDay = obj.optBoolean("isAllDay", false),
                        location = obj.optString("location", ""),
                        category = runCatching {
                            EventCategory.valueOf(obj.optString("category", EventCategory.WORK.name))
                        }.getOrDefault(EventCategory.WORK),
                        colorKey = obj.optString("colorKey", "royal_blue"),
                        recurrence = runCatching {
                            RecurrenceRule.valueOf(obj.optString("recurrence", RecurrenceRule.NONE.name))
                        }.getOrDefault(RecurrenceRule.NONE),
                        reminderOffsetsMinutes = offsets,
                        attachedNoteId = if (obj.isNull("attachedNoteId")) null else obj.optLong("attachedNoteId"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        val remindersJson = root.getJSONArray("reminders")
        val reminders = buildList {
            for (i in 0 until remindersJson.length()) {
                val obj = remindersJson.getJSONObject(i)
                add(
                    ReminderEntity(
                        id = obj.getLong("id"),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        triggerAtMillis = obj.getLong("triggerAtMillis"),
                        repeatType = runCatching {
                            RepeatType.valueOf(obj.optString("repeatType", RepeatType.NONE.name))
                        }.getOrDefault(RepeatType.NONE),
                        customIntervalDays = obj.optInt("customIntervalDays", 1).coerceAtLeast(1),
                        linkedEventId = if (obj.isNull("linkedEventId")) null else obj.optLong("linkedEventId"),
                        linkedNoteId = if (obj.isNull("linkedNoteId")) null else obj.optLong("linkedNoteId"),
                        eventOffsetMinutes = if (obj.isNull("eventOffsetMinutes")) null else obj.optInt("eventOffsetMinutes"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        completedAtMillis = if (obj.isNull("completedAtMillis")) null else obj.optLong("completedAtMillis"),
                        snoozedUntilMillis = if (obj.isNull("snoozedUntilMillis")) null else obj.optLong("snoozedUntilMillis"),
                        lastNotifiedMillis = if (obj.isNull("lastNotifiedMillis")) null else obj.optLong("lastNotifiedMillis"),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        dao.replaceAllData(notes = notes, events = events, reminders = reminders)

        if (root.has("preferences")) {
            val pObj = root.getJSONObject("preferences")
            val restoredPrefs = UserPreferences(
                themeMode = runCatching {
                    ThemeMode.valueOf(pObj.optString("themeMode", ThemeMode.SYSTEM.name))
                }.getOrDefault(ThemeMode.SYSTEM),
                use24HourFormat = pObj.optBoolean("use24HourFormat", false),
                language = runCatching {
                    AppLanguage.valueOf(pObj.optString("language", AppLanguage.ENGLISH.name))
                }.getOrDefault(AppLanguage.ENGLISH),
                useDeviceTimezone = pObj.optBoolean("useDeviceTimezone", false),
                customTimezoneId = pObj.optString("customTimezoneId", DateTimeUtils.DEFAULT_TIMEZONE_ID),
                defaultReminderLeadMinutes = pObj.optInt("defaultReminderLeadMinutes", 15),
                defaultSnoozeMinutes = pObj.optInt("defaultSnoozeMinutes", 10),
                soundEnabled = pObj.optBoolean("soundEnabled", true),
                vibrationEnabled = pObj.optBoolean("vibrationEnabled", true)
            )
            preferencesRepository.restorePreferences(restoredPrefs)
        }

        BackupSummary(
            notesCount = notes.size,
            eventsCount = events.size,
            remindersCount = reminders.size
        )
    }
}
