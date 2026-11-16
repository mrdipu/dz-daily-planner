package com.example.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromNoteCategory(value: NoteCategory): String = value.name

    @TypeConverter
    fun toNoteCategory(value: String): NoteCategory =
        runCatching { NoteCategory.valueOf(value) }.getOrDefault(NoteCategory.OTHERS)

    @TypeConverter
    fun fromEventCategory(value: EventCategory): String = value.name

    @TypeConverter
    fun toEventCategory(value: String): EventCategory =
        runCatching { EventCategory.valueOf(value) }.getOrDefault(EventCategory.OTHER)

    @TypeConverter
    fun fromRecurrenceRule(value: RecurrenceRule): String = value.name

    @TypeConverter
    fun toRecurrenceRule(value: String): RecurrenceRule =
        runCatching { RecurrenceRule.valueOf(value) }.getOrDefault(RecurrenceRule.NONE)

    @TypeConverter
    fun fromRepeatType(value: RepeatType): String = value.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType =
        runCatching { RepeatType.valueOf(value) }.getOrDefault(RepeatType.NONE)

    @TypeConverter
    fun fromIntList(list: List<Int>): String = list.joinToString(",")

    @TypeConverter
    fun toIntList(raw: String): List<Int> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
