package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

enum class NoteCategory {
    PERSONAL,
    OFFICE,
    IDEAS,
    IMPORTANT,
    OTHERS
}

enum class EventCategory {
    WORK,
    PERSONAL,
    MEETING,
    HEALTH,
    DEADLINE,
    OTHER
}

enum class RecurrenceRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class RepeatType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM_DAYS
}

data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean
)

object ChecklistSerializer {
    fun encode(items: List<ChecklistItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("isChecked", item.isChecked)
            array.put(obj)
        }
        return array.toString()
    }

    fun decode(json: String): List<ChecklistItem> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        ChecklistItem(
                            id = obj.optString("id", "chk_$i"),
                            text = obj.optString("text", ""),
                            isChecked = obj.optBoolean("isChecked", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["isDeleted", "isArchived", "isPinned", "updatedAtMillis"]),
        Index(value = ["attachedDateEpochDay"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val checklistJson: String = "[]",
    val category: NoteCategory = NoteCategory.PERSONAL,
    val colorKey: String = "default",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAtMillis: Long? = null,
    val attachedDateEpochDay: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["startEpochMillis", "endEpochMillis"]),
        Index(value = ["isCompleted"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val isAllDay: Boolean = false,
    val location: String = "",
    val category: EventCategory = EventCategory.WORK,
    val colorKey: String = "royal_blue",
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val reminderOffsetsMinutes: List<Int> = listOf(15),
    val attachedNoteId: Long? = null,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["isCompleted", "triggerAtMillis"]),
        Index(value = ["linkedEventId"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val triggerAtMillis: Long,
    val repeatType: RepeatType = RepeatType.NONE,
    val customIntervalDays: Int = 1,
    val linkedEventId: Long? = null,
    val linkedNoteId: Long? = null,
    val eventOffsetMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val snoozedUntilMillis: Long? = null,
    val lastNotifiedMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
