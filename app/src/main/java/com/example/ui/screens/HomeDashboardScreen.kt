package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.prefs.ThemeMode
import com.example.ui.PlannerUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.strings.Strings
import com.example.ui.theme.DzCrimsonRed
import com.example.ui.theme.DzRoyalBlue
import com.example.ui.theme.DzRoyalBlueDark
import com.example.ui.theme.DzWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.eventAccentColor
import com.example.ui.theme.noteAccentDot
import com.example.ui.theme.noteCardColor
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeDashboardScreen(
    uiState: PlannerUiState,
    liveClockFlow: StateFlow<Long>,
    strings: Strings,
    onSelectEpochDay: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onOpenEvent: (EventEntity) -> Unit,
    onToggleEventComplete: (EventEntity) -> Unit,
    onCompleteReminder: (ReminderEntity) -> Unit,
    onNavigateNotes: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateReminders: (overdueOnly: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val tz = remember(prefs.useDeviceTimezone, prefs.customTimezoneId) {
        DateTimeUtils.resolveTimeZone(prefs.useDeviceTimezone, prefs.customTimezoneId)
    }
    val now = uiState.currentClockMillis
    val todayEpochDay = remember(now / 60_000L, tz.id) {
        DateTimeUtils.todayEpochDay(tz)
    }
    val isDark = when (prefs.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val greeting = when (DateTimeUtils.greetingPeriod(now, tz)) {
        0 -> strings.goodMorning
        1 -> strings.goodAfternoon
        else -> strings.goodEvening
    }

    val activeNotes = remember(uiState.notes) {
        uiState.notes.filter { !it.isDeleted && !it.isArchived }
    }
    val dayEvents = remember(uiState.events, uiState.selectedEpochDay, tz.id) {
        val dayStart = DateTimeUtils.startOfDayMillis(uiState.selectedEpochDay, tz)
        val dayEnd = DateTimeUtils.endOfDayMillis(uiState.selectedEpochDay, tz)
        uiState.events.filter { it.startEpochMillis <= dayEnd && it.endEpochMillis >= dayStart }
    }
    val dayNotes = remember(activeNotes, uiState.selectedEpochDay) {
        activeNotes.filter {
            it.attachedDateEpochDay == uiState.selectedEpochDay || it.isPinned
        }.take(4)
    }

    val overdueReminders = remember(uiState.reminders, now) {
        uiState.reminders.filter {
            !it.isCompleted && (it.snoozedUntilMillis ?: it.triggerAtMillis) < now
        }
    }
    val upcomingReminders = remember(uiState.reminders, now) {
        uiState.reminders.filter {
            !it.isCompleted && (it.snoozedUntilMillis ?: it.triggerAtMillis) >= now
        }.take(4)
    }

    val todayEvents = remember(uiState.events, todayEpochDay, tz.id) {
        val todayStart = DateTimeUtils.startOfDayMillis(todayEpochDay, tz)
        val todayEnd = DateTimeUtils.endOfDayMillis(todayEpochDay, tz)
        uiState.events.filter { it.startEpochMillis <= todayEnd && it.endEpochMillis >= todayStart }
    }
    val todayReminders = remember(uiState.reminders, todayEpochDay, tz.id) {
        val todayStart = DateTimeUtils.startOfDayMillis(todayEpochDay, tz)
        val todayEnd = DateTimeUtils.endOfDayMillis(todayEpochDay, tz)
        uiState.reminders.filter {
            val t = it.snoozedUntilMillis ?: it.triggerAtMillis
            t in todayStart..todayEnd
        }
    }
    val totalTodayTasks = todayEvents.size + todayReminders.size
    val doneTodayTasks = remember(todayEvents, todayReminders) {
        todayEvents.count { it.isCompleted } + todayReminders.count { it.isCompleted }
    }
    val productivityRatio = if (totalTodayTasks == 0) 1f else doneTodayTasks.toFloat() / totalTodayTasks

    val weekDays = remember(uiState.selectedEpochDay) {
        DateTimeUtils.weekDaysAround(uiState.selectedEpochDay)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Brand Pill + Greeting + Live Time
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DzWhite,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dz_studio_logo_badge),
                                contentDescription = strings.poweredBy,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "DZ Ai-STUDIO",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF171717)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = DateTimeUtils.formatDateFull(now, tz),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    LiveClockPill(
                        liveClockFlow = liveClockFlow,
                        use24HourFormat = prefs.use24HourFormat,
                        timeZone = tz
                    )
                    if (uiState.selectedEpochDay != todayEpochDay) {
                        TextButton(onClick = onJumpToToday) {
                            Text(strings.today)
                        }
                    }
                }
            }
        }

        // Interactive Week Date Strip
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { day ->
                        val isSelected = day == uiState.selectedEpochDay
                        val isToday = day == todayEpochDay
                        val dStart = DateTimeUtils.startOfDayMillis(day, tz)
                        val dEnd = DateTimeUtils.endOfDayMillis(day, tz)
                        val hasEvents = uiState.events.any { it.startEpochMillis <= dEnd && it.endEpochMillis >= dStart }
                        val dayShort = DateTimeUtils.formatEpochDay(day, "EEE").take(3)
                        val dayNum = DateTimeUtils.formatEpochDay(day, "d")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onSelectEpochDay(day) }
                                .padding(vertical = 8.dp)
                                .testTag("week_strip_day_$dayNum")
                        ) {
                            Text(
                                text = dayShort,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            hasEvents && isSelected -> DzWhite
                                            hasEvents -> DzCrimsonRed
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Daily Productivity Summary Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DzRoyalBlue, DzRoyalBlueDark)
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.todayProductivity,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DzWhite
                            )
                            Text(
                                text = if (totalTodayTasks == 0) "Ready to plan today"
                                else "$doneTodayTasks / $totalTodayTasks completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = DzWhite.copy(alpha = 0.85f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DzWhite.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${(productivityRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = DzWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { productivityRatio },
                        color = DzWhite,
                        trackColor = DzWhite.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatPill(
                            label = strings.notesCountLabel,
                            value = "${activeNotes.size}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            label = strings.eventsTodayLabel,
                            value = "${todayEvents.size}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            label = strings.remindersDoneLabel,
                            value = "${uiState.reminders.count { it.isCompleted }}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Overdue Reminders Indicator
        if (overdueReminders.isNotEmpty()) {
            item {
                Card(
                    onClick = { onNavigateReminders(true) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DzCrimsonRed.copy(alpha = 0.10f)),
                    border = BorderStroke(1.dp, DzCrimsonRed.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_overdue_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DzCrimsonRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = strings.overdueAlertTitle,
                                tint = DzWhite
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${overdueReminders.size} ${strings.overdueAlertTitle}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = DzCrimsonRed
                            )
                            Text(
                                text = strings.overdueAlertSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Today's Upcoming Events
        item {
            SectionHeader(
                title = if (uiState.selectedEpochDay == todayEpochDay) strings.todaysEvents
                else "${strings.eventsOnDay} · ${DateTimeUtils.formatEpochDay(uiState.selectedEpochDay, "MMM d")}",
                actionLabel = strings.viewAll,
                onAction = onNavigateCalendar
            )
        }

        if (dayEvents.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.CalendarToday,
                    message = strings.noEventsToday
                )
            }
        } else {
            items(dayEvents.take(4), key = { "home_event_${it.id}" }) { event ->
                val accent = eventAccentColor(event.colorKey)
                Card(
                    onClick = { onOpenEvent(event) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(accent)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val timeStr = if (event.isAllDay) strings.allDay
                            else "${DateTimeUtils.formatTime(event.startEpochMillis, prefs.use24HourFormat, tz)} - ${
                                DateTimeUtils.formatTime(event.endEpochMillis, prefs.use24HourFormat, tz)
                            }"
                            Text(
                                text = if (event.location.isNotBlank()) "$timeStr · ${event.location}" else timeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onToggleEventComplete(event) }) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = strings.markDone,
                                tint = if (event.isCompleted) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        // Today's & Pinned Notes
        item {
            SectionHeader(
                title = strings.todaysNotes,
                actionLabel = strings.viewAll,
                onAction = onNavigateNotes
            )
        }

        if (dayNotes.isEmpty() && activeNotes.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.EditNote,
                    message = strings.noNotesYet
                )
            }
        } else {
            val notesPreview = if (dayNotes.isNotEmpty()) dayNotes else activeNotes.take(3)
            items(notesPreview, key = { "home_note_${it.id}" }) { note ->
                Card(
                    onClick = { onOpenNote(note) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = noteCardColor(note.colorKey, isDark)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(noteAccentDot(note.colorKey))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title.ifBlank { "Untitled Note" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (note.content.isNotBlank()) {
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = strings.pin,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Upcoming Reminders
        item {
            SectionHeader(
                title = strings.upcomingReminders,
                actionLabel = strings.viewAll,
                onAction = { onNavigateReminders(false) }
            )
        }

        if (upcomingReminders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.NotificationsOff,
                    message = strings.noRemindersUpcoming
                )
            }
        } else {
            items(upcomingReminders, key = { "home_rem_${it.id}" }) { reminder ->
                val triggerAt = reminder.snoozedUntilMillis ?: reminder.triggerAtMillis
                Card(
                    onClick = { onNavigateReminders(false) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = DateTimeUtils.formatDateTime(triggerAt, prefs.use24HourFormat, tz),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onCompleteReminder(reminder) }) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = strings.markDone,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DzWhite.copy(alpha = 0.14f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = DzWhite
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DzWhite.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LiveClockPill(
    liveClockFlow: StateFlow<Long>,
    use24HourFormat: Boolean,
    timeZone: java.util.TimeZone
) {
    val clockMillis by liveClockFlow.collectAsStateWithLifecycle()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = DateTimeUtils.formatLiveClock(clockMillis, use24HourFormat, timeZone),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
