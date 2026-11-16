package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.EventCategory
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.data.local.RecurrenceRule
import com.example.data.prefs.AppLanguage
import com.example.ui.strings.Strings
import com.example.ui.theme.eventAccentColor
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.TimeZone

private val EVENT_COLOR_KEYS = listOf(
    "royal_blue", "crimson_tint", "amber", "emerald", "lavender", "slate"
)

private val REMINDER_OFFSETS = listOf(0, 5, 15, 30, 60, 1440)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorSheet(
    initialEvent: EventEntity?,
    initialEpochDay: Long,
    defaultReminderLeadMinutes: Int,
    availableNotes: List<NoteEntity>,
    strings: Strings,
    language: AppLanguage,
    use24Hour: Boolean,
    timeZone: TimeZone,
    onSave: (EventEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultStart = remember(initialEvent, initialEpochDay) {
        if (initialEvent != null) {
            initialEvent.startEpochMillis
        } else {
            val cal = DateTimeUtils.calendarAt(System.currentTimeMillis(), timeZone)
            val nextHour = (cal.get(Calendar.HOUR_OF_DAY) + 1).coerceAtMost(23)
            DateTimeUtils.millisFromEpochDay(initialEpochDay, nextHour, 0, timeZone)
        }
    }
    val defaultEnd = remember(initialEvent, defaultStart) {
        initialEvent?.endEpochMillis ?: (defaultStart + 3_600_000L)
    }

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var startMillis by remember { mutableStateOf(defaultStart) }
    var endMillis by remember { mutableStateOf(defaultEnd) }
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var category by remember { mutableStateOf(initialEvent?.category ?: EventCategory.WORK) }
    var colorKey by remember { mutableStateOf(initialEvent?.colorKey ?: "royal_blue") }
    var recurrence by remember { mutableStateOf(initialEvent?.recurrence ?: RecurrenceRule.NONE) }
    var selectedOffsets by remember {
        mutableStateOf(
            initialEvent?.reminderOffsetsMinutes ?: listOf(defaultReminderLeadMinutes)
        )
    }
    var attachedNoteId by remember { mutableStateOf(initialEvent?.attachedNoteId) }
    var showNoteDropdown by remember { mutableStateOf(false) }

    var pickTarget by remember { mutableStateOf<String?>(null) } // "start_date", "start_time", "end_date", "end_time"
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialEvent == null) strings.newEvent else initialEvent.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                validationError = "Please enter an event title."
                                return@Button
                            }
                            val finalStart = if (isAllDay) {
                                DateTimeUtils.startOfDayMillis(
                                    DateTimeUtils.epochDayFromMillis(startMillis, timeZone),
                                    timeZone
                                )
                            } else startMillis
                            val finalEnd = if (isAllDay) {
                                DateTimeUtils.endOfDayMillis(
                                    DateTimeUtils.epochDayFromMillis(endMillis, timeZone),
                                    timeZone
                                )
                            } else maxOf(endMillis, finalStart + 15 * 60_000L)

                            val now = System.currentTimeMillis()
                            onSave(
                                EventEntity(
                                    id = initialEvent?.id ?: 0L,
                                    title = title.trim(),
                                    description = description.trim(),
                                    startEpochMillis = finalStart,
                                    endEpochMillis = finalEnd,
                                    isAllDay = isAllDay,
                                    location = location.trim(),
                                    category = category,
                                    colorKey = colorKey,
                                    recurrence = recurrence,
                                    reminderOffsetsMinutes = selectedOffsets.sorted(),
                                    attachedNoteId = attachedNoteId,
                                    isCompleted = initialEvent?.isCompleted ?: false,
                                    createdAtMillis = initialEvent?.createdAtMillis ?: now,
                                    updatedAtMillis = now
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("event_editor_save_button")
                    ) {
                        Text(strings.save)
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                placeholder = { Text(strings.eventTitleHint) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text(strings.descriptionHint) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_description_input")
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = { Text(strings.locationHint) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_location_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.allDay, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it },
                    modifier = Modifier.testTag("event_all_day_switch")
                )
            }

            DateTimeChipRow(
                label = "Start",
                epochMillis = startMillis,
                use24Hour = use24Hour,
                timeZone = timeZone,
                onPickDate = { pickTarget = "start_date" },
                onPickTime = if (isAllDay) null else ({ pickTarget = "start_time" })
            )

            DateTimeChipRow(
                label = "End",
                epochMillis = endMillis,
                use24Hour = use24Hour,
                timeZone = timeZone,
                onPickDate = { pickTarget = "end_date" },
                onPickTime = if (isAllDay) null else ({ pickTarget = "end_time" })
            )

            // Category chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(strings.eventCategoryLabel(cat, language)) }
                    )
                }
            }

            // Color indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EVENT_COLOR_KEYS.forEach { key ->
                    val selected = colorKey == key
                    val dot = eventAccentColor(key)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(dot.copy(alpha = 0.18f))
                            .border(
                                BorderStroke(if (selected) 2.5.dp else 1.dp, dot),
                                CircleShape
                            )
                            .clickable { colorKey = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(dot)
                        )
                    }
                }
            }

            // Recurrence selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrenceRule.entries.forEach { rule ->
                    FilterChip(
                        selected = recurrence == rule,
                        onClick = { recurrence = rule },
                        label = { Text(strings.recurrenceLabel(rule, language)) }
                    )
                }
            }

            // Multiple reminder alerts per event
            Text(
                text = strings.reminderAlertsBefore,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                REMINDER_OFFSETS.forEach { offset ->
                    val label = when (offset) {
                        0 -> "At start"
                        60 -> "1 hr before"
                        1440 -> "1 day before"
                        else -> "$offset ${strings.minutesShort} before"
                    }
                    val selected = offset in selectedOffsets
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedOffsets = if (selected) {
                                selectedOffsets - offset
                            } else {
                                selectedOffsets + offset
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            // Attach a Note
            val activeNotes = availableNotes.filter { !it.isDeleted }
            val attachedTitle = activeNotes.firstOrNull { it.id == attachedNoteId }?.title
            Box {
                OutlinedButton(
                    onClick = { showNoteDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (attachedTitle != null) {
                            "${strings.attachNoteOptional}: $attachedTitle"
                        } else {
                            "${strings.attachNoteOptional}: ${strings.noNoteAttached}"
                        }
                    )
                }
                DropdownMenu(
                    expanded = showNoteDropdown,
                    onDismissRequest = { showNoteDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.noNoteAttached) },
                        onClick = {
                            attachedNoteId = null
                            showNoteDropdown = false
                        }
                    )
                    activeNotes.take(15).forEach { note ->
                        DropdownMenuItem(
                            text = { Text(note.title.ifBlank { "Untitled Note" }) },
                            onClick = {
                                attachedNoteId = note.id
                                showNoteDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    when (pickTarget) {
        "start_date", "end_date" -> {
            val baseMillis = if (pickTarget == "start_date") startMillis else endMillis
            DzDatePickerModal(
                initialEpochDay = DateTimeUtils.epochDayFromMillis(baseMillis, timeZone),
                onDateSelected = { newDay ->
                    val cal = DateTimeUtils.calendarAt(baseMillis, timeZone)
                    val updated = DateTimeUtils.millisFromEpochDay(
                        newDay,
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        timeZone
                    )
                    if (pickTarget == "start_date") {
                        val delta = (endMillis - startMillis).coerceAtLeast(3_600_000L)
                        startMillis = updated
                        endMillis = updated + delta
                    } else {
                        endMillis = maxOf(updated, startMillis)
                    }
                },
                onDismiss = { pickTarget = null }
            )
        }
        "start_time", "end_time" -> {
            val baseMillis = if (pickTarget == "start_time") startMillis else endMillis
            val cal = DateTimeUtils.calendarAt(baseMillis, timeZone)
            DzTimePickerModal(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
                is24Hour = use24Hour,
                onTimeSelected = { hour, minute ->
                    val day = DateTimeUtils.epochDayFromMillis(baseMillis, timeZone)
                    val updated = DateTimeUtils.millisFromEpochDay(day, hour, minute, timeZone)
                    if (pickTarget == "start_time") {
                        val delta = (endMillis - startMillis).coerceAtLeast(1_800_000L)
                        startMillis = updated
                        endMillis = updated + delta
                    } else {
                        endMillis = maxOf(updated, startMillis + 15 * 60_000L)
                    }
                },
                onDismiss = { pickTarget = null }
            )
        }
    }
}
