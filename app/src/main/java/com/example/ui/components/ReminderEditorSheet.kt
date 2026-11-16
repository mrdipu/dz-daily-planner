package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ReminderEntity
import com.example.data.local.RepeatType
import com.example.data.prefs.AppLanguage
import com.example.ui.strings.Strings
import com.example.util.DateTimeUtils
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorSheet(
    initialReminder: ReminderEntity?,
    prefilledTitle: String = "",
    prefilledDescription: String = "",
    linkedNoteId: Long? = null,
    strings: Strings,
    language: AppLanguage,
    use24Hour: Boolean,
    timeZone: TimeZone,
    onSave: (ReminderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultTrigger = remember(initialReminder) {
        initialReminder?.triggerAtMillis ?: (System.currentTimeMillis() + 30 * 60_000L)
    }

    var title by remember { mutableStateOf(initialReminder?.title ?: prefilledTitle) }
    var description by remember { mutableStateOf(initialReminder?.description ?: prefilledDescription) }
    var triggerMillis by remember { mutableStateOf(defaultTrigger) }
    var repeatType by remember { mutableStateOf(initialReminder?.repeatType ?: RepeatType.NONE) }
    var customIntervalDays by remember {
        mutableIntStateOf((initialReminder?.customIntervalDays ?: 2).coerceAtLeast(1))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
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
                    text = if (initialReminder == null) strings.newReminder else initialReminder.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                validationError = "Please enter a reminder title."
                                return@Button
                            }
                            val now = System.currentTimeMillis()
                            onSave(
                                ReminderEntity(
                                    id = initialReminder?.id ?: 0L,
                                    title = title.trim(),
                                    description = description.trim(),
                                    triggerAtMillis = triggerMillis,
                                    repeatType = repeatType,
                                    customIntervalDays = customIntervalDays.coerceAtLeast(1),
                                    linkedEventId = initialReminder?.linkedEventId,
                                    linkedNoteId = initialReminder?.linkedNoteId ?: linkedNoteId,
                                    eventOffsetMinutes = initialReminder?.eventOffsetMinutes,
                                    isCompleted = false,
                                    completedAtMillis = null,
                                    snoozedUntilMillis = null,
                                    lastNotifiedMillis = null,
                                    createdAtMillis = initialReminder?.createdAtMillis ?: now,
                                    updatedAtMillis = now
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("reminder_editor_save_button")
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
                placeholder = { Text(strings.newReminder) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_title_input")
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
                    .testTag("reminder_description_input")
            )

            DateTimeChipRow(
                label = "Alert Time",
                epochMillis = triggerMillis,
                use24Hour = use24Hour,
                timeZone = timeZone,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true }
            )

            // Quick relative time chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 30, 60, 180).forEach { mins ->
                    val label = if (mins >= 60) "+${mins / 60}h" else "+${mins}m"
                    OutlinedButton(
                        onClick = { triggerMillis = System.currentTimeMillis() + mins * 60_000L }
                    ) {
                        Text(label)
                    }
                }
            }

            // Repeat schedule chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatType.entries.forEach { type ->
                    FilterChip(
                        selected = repeatType == type,
                        onClick = { repeatType = type },
                        label = { Text(strings.repeatTypeLabel(type, customIntervalDays, language)) }
                    )
                }
            }

            if (repeatType == RepeatType.CUSTOM_DAYS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.customEveryDays, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { if (customIntervalDays > 1) customIntervalDays-- }) {
                            Text("-")
                        }
                        Text(
                            text = "$customIntervalDays",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(onClick = { customIntervalDays++ }) {
                            Text("+")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        DzDatePickerModal(
            initialEpochDay = DateTimeUtils.epochDayFromMillis(triggerMillis, timeZone),
            onDateSelected = { newDay ->
                val cal = DateTimeUtils.calendarAt(triggerMillis, timeZone)
                triggerMillis = DateTimeUtils.millisFromEpochDay(
                    newDay,
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    timeZone
                )
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val cal = DateTimeUtils.calendarAt(triggerMillis, timeZone)
        DzTimePickerModal(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = use24Hour,
            onTimeSelected = { hour, minute ->
                val day = DateTimeUtils.epochDayFromMillis(triggerMillis, timeZone)
                triggerMillis = DateTimeUtils.millisFromEpochDay(day, hour, minute, timeZone)
            },
            onDismiss = { showTimePicker = false }
        )
    }
}
