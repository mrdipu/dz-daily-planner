package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.ReminderEntity
import com.example.data.local.RepeatType
import com.example.ui.PlannerUiState
import com.example.ui.ReminderFilter
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.strings.Strings
import com.example.ui.theme.DzCrimsonRed
import com.example.util.DateTimeUtils

private val SNOOZE_CHOICES = listOf(5, 10, 15, 30, 60)

@Composable
fun RemindersScreen(
    uiState: PlannerUiState,
    strings: Strings,
    currentFilter: ReminderFilter,
    highlightedReminderId: Long?,
    hasNotificationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onFilterChange: (ReminderFilter) -> Unit,
    onOpenReminder: (ReminderEntity) -> Unit,
    onCompleteReminder: (ReminderEntity) -> Unit,
    onReopenReminder: (ReminderEntity) -> Unit,
    onSnoozeReminder: (Long, Int) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val tz = DateTimeUtils.resolveTimeZone(prefs.useDeviceTimezone, prefs.customTimezoneId)
    val now = uiState.currentClockMillis
    val todayDay = DateTimeUtils.todayEpochDay(tz)
    val todayStart = DateTimeUtils.startOfDayMillis(todayDay, tz)
    val todayEnd = DateTimeUtils.endOfDayMillis(todayDay, tz)

    var reminderPendingDelete by remember { mutableStateOf<ReminderEntity?>(null) }

    val activeReminders = remember(uiState.reminders) {
        uiState.reminders.filter { !it.isCompleted }
    }
    val todayList = remember(activeReminders, todayStart, todayEnd) {
        activeReminders.filter {
            val t = it.snoozedUntilMillis ?: it.triggerAtMillis
            t in todayStart..todayEnd
        }
    }
    val overdueList = remember(activeReminders, now) {
        activeReminders.filter {
            val t = it.snoozedUntilMillis ?: it.triggerAtMillis
            t < now
        }
    }
    val upcomingList = remember(activeReminders, now) {
        activeReminders.filter {
            val t = it.snoozedUntilMillis ?: it.triggerAtMillis
            t >= now
        }
    }
    val completedList = remember(uiState.reminders) {
        uiState.reminders.filter { it.isCompleted }
    }

    val displayed = when (currentFilter) {
        ReminderFilter.TODAY -> todayList
        ReminderFilter.UPCOMING -> upcomingList
        ReminderFilter.OVERDUE -> overdueList
        ReminderFilter.COMPLETED -> completedList
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reminders_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!hasNotificationPermission) {
            item {
                NotificationPermissionBanner(
                    title = strings.notificationPermissionTitle,
                    body = strings.notificationPermissionBody,
                    actionLabel = strings.grantPermission,
                    accentColor = DzCrimsonRed,
                    onActionClick = onRequestNotificationPermission
                )
            }
        } else if (!canScheduleExactAlarms) {
            item {
                NotificationPermissionBanner(
                    title = strings.exactAlarmTitle,
                    body = strings.exactAlarmBody,
                    actionLabel = strings.openExactAlarmSettings,
                    onActionClick = onOpenExactAlarmSettings
                )
            }
        }

        // Filter Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(ReminderFilter.TODAY, strings.remindersToday, todayList.size),
                    Triple(ReminderFilter.UPCOMING, strings.remindersUpcoming, upcomingList.size),
                    Triple(ReminderFilter.OVERDUE, strings.remindersOverdue, overdueList.size),
                    Triple(ReminderFilter.COMPLETED, strings.remindersCompleted, completedList.size)
                ).forEach { (filter, label, count) ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text("$label ($count)") },
                        modifier = Modifier.testTag("reminder_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (displayed.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.NotificationsOff,
                    message = strings.noRemindersUpcoming
                )
            }
        } else {
            items(displayed, key = { it.id }) { reminder ->
                val triggerAt = reminder.snoozedUntilMillis ?: reminder.triggerAtMillis
                val isOverdue = !reminder.isCompleted && triggerAt < now
                val isHighlighted = highlightedReminderId == reminder.id
                var snoozeMenuOpen by remember { mutableStateOf(false) }

                Card(
                    onClick = { onOpenReminder(reminder) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            isOverdue -> DzCrimsonRed.copy(alpha = 0.06f)
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    border = BorderStroke(
                        width = if (isHighlighted) 2.dp else 1.dp,
                        color = when {
                            isHighlighted -> MaterialTheme.colorScheme.primary
                            isOverdue -> DzCrimsonRed.copy(alpha = 0.35f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_card_${reminder.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (reminder.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = when {
                                    reminder.isCompleted -> MaterialTheme.colorScheme.tertiary
                                    isOverdue -> DzCrimsonRed
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                )
                                Text(
                                    text = DateTimeUtils.formatDateTime(triggerAt, prefs.use24HourFormat, tz),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOverdue) DzCrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { reminderPendingDelete = reminder }) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = strings.delete,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (reminder.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = reminder.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (reminder.repeatType != RepeatType.NONE || reminder.snoozedUntilMillis != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (reminder.repeatType != RepeatType.NONE) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Repeat,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = strings.repeatTypeLabel(
                                                    reminder.repeatType,
                                                    reminder.customIntervalDays,
                                                    prefs.language
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                if (reminder.snoozedUntilMillis != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "${strings.snooze}: ${
                                                DateTimeUtils.formatTime(
                                                    reminder.snoozedUntilMillis,
                                                    prefs.use24HourFormat,
                                                    tz
                                                )
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (reminder.isCompleted) {
                                OutlinedButton(onClick = { onReopenReminder(reminder) }) {
                                    Text(strings.reopen)
                                }
                            } else {
                                Button(
                                    onClick = { onCompleteReminder(reminder) },
                                    modifier = Modifier.testTag("reminder_complete_${reminder.id}")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.markDone)
                                }

                                Box {
                                    OutlinedButton(
                                        onClick = { snoozeMenuOpen = true },
                                        modifier = Modifier.testTag("reminder_snooze_${reminder.id}")
                                    ) {
                                        Icon(Icons.Filled.Snooze, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.snooze)
                                    }
                                    DropdownMenu(
                                        expanded = snoozeMenuOpen,
                                        onDismissRequest = { snoozeMenuOpen = false }
                                    ) {
                                        SNOOZE_CHOICES.forEach { mins ->
                                            DropdownMenuItem(
                                                text = { Text("$mins ${strings.minutesShort}") },
                                                onClick = {
                                                    snoozeMenuOpen = false
                                                    onSnoozeReminder(reminder.id, mins)
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { onOpenReminder(reminder) }) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = strings.reschedule
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reminderPendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = strings.confirmDeleteTitle,
            message = strings.confirmDeleteMessage,
            confirmLabel = strings.delete,
            cancelLabel = strings.cancel,
            onConfirm = {
                onDeleteReminder(target.id)
                reminderPendingDelete = null
            },
            onDismiss = { reminderPendingDelete = null }
        )
    }
}
