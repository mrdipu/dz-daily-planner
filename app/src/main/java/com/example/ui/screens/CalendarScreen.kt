package com.example.ui.screens

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.data.local.RecurrenceRule
import com.example.data.prefs.ThemeMode
import com.example.ui.CalendarViewMode
import com.example.ui.PlannerUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.strings.Strings
import com.example.ui.theme.DzCrimsonRed
import com.example.ui.theme.eventAccentColor
import com.example.ui.theme.noteCardColor
import com.example.util.DateTimeUtils

@Composable
fun CalendarScreen(
    uiState: PlannerUiState,
    strings: Strings,
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onSelectEpochDay: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onAddEventForDay: (Long) -> Unit,
    onAddNoteForDay: (Long) -> Unit,
    onOpenEvent: (EventEntity) -> Unit,
    onToggleEventCompleted: (EventEntity) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = uiState.preferences
    val tz = DateTimeUtils.resolveTimeZone(prefs.useDeviceTimezone, prefs.customTimezoneId)
    val todayEpochDay = DateTimeUtils.todayEpochDay(tz)
    val isDark = when (prefs.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val (selectedYear, selectedMonth, _) = remember(uiState.selectedEpochDay) {
        DateTimeUtils.yearMonthDayFromEpochDay(uiState.selectedEpochDay)
    }
    var visibleYear by remember(selectedYear) { mutableIntStateOf(selectedYear) }
    var visibleMonth by remember(selectedMonth) { mutableIntStateOf(selectedMonth) }
    var eventPendingDelete by remember { mutableStateOf<EventEntity?>(null) }

    val dayStart = DateTimeUtils.startOfDayMillis(uiState.selectedEpochDay, tz)
    val dayEnd = DateTimeUtils.endOfDayMillis(uiState.selectedEpochDay, tz)
    val dayEvents = remember(uiState.events, dayStart, dayEnd) {
        uiState.events.filter { it.startEpochMillis <= dayEnd && it.endEpochMillis >= dayStart }
    }
    val dayNotes = remember(uiState.notes, uiState.selectedEpochDay) {
        uiState.notes.filter {
            !it.isDeleted && it.attachedDateEpochDay == uiState.selectedEpochDay
        }
    }

    val todayStart = DateTimeUtils.startOfDayMillis(todayEpochDay, tz)
    val todayEnd = DateTimeUtils.endOfDayMillis(todayEpochDay, tz)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // View Mode Switcher + Today Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        CalendarViewMode.MONTH to strings.monthView,
                        CalendarViewMode.WEEK to strings.weekView,
                        CalendarViewMode.AGENDA to strings.agendaView
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { onViewModeChange(mode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("calendar_mode_${mode.name.lowercase()}")
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onJumpToToday()
                        val (ty, tm, _) = DateTimeUtils.yearMonthDayFromEpochDay(todayEpochDay)
                        visibleYear = ty
                        visibleMonth = tm
                    },
                    modifier = Modifier.testTag("calendar_today_button")
                ) {
                    Text(strings.today, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (viewMode == CalendarViewMode.MONTH) {
            item {
                val monthGrid = remember(visibleYear, visibleMonth) {
                    DateTimeUtils.monthGridDays(visibleYear, visibleMonth)
                }
                val gridEventsByDay = remember(monthGrid, uiState.events, tz.id) {
                    val map = HashMap<Long, MutableList<EventEntity>>(42)
                    val firstDay = monthGrid.first()
                    val lastDay = monthGrid.last()
                    val rangeStart = DateTimeUtils.startOfDayMillis(firstDay, tz)
                    val rangeEnd = DateTimeUtils.endOfDayMillis(lastDay, tz)
                    uiState.events.forEach { ev ->
                        if (ev.startEpochMillis <= rangeEnd && ev.endEpochMillis >= rangeStart) {
                            val startDay = maxOf(firstDay, DateTimeUtils.epochDayFromMillis(ev.startEpochMillis, tz))
                            val endDay = minOf(lastDay, DateTimeUtils.epochDayFromMillis(ev.endEpochMillis, tz))
                            for (d in startDay..endDay) {
                                map.getOrPut(d) { mutableListOf() }.add(ev)
                            }
                        }
                    }
                    map
                }
                val gridNoteDays = remember(uiState.notes) {
                    uiState.notes.asSequence()
                        .filter { !it.isDeleted && it.attachedDateEpochDay != null }
                        .mapNotNull { it.attachedDateEpochDay }
                        .toHashSet()
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (visibleMonth == 0) {
                                    visibleYear -= 1
                                    visibleMonth = 11
                                } else {
                                    visibleMonth -= 1
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                            }
                            Text(
                                text = DateTimeUtils.formatMonthYear(visibleYear, visibleMonth),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = {
                                if (visibleMonth == 11) {
                                    visibleYear += 1
                                    visibleMonth = 0
                                } else {
                                    visibleMonth += 1
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { wd ->
                                Text(
                                    text = wd,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        monthGrid.chunked(7).forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                week.forEach { day ->
                                    val (_, m, d) = DateTimeUtils.yearMonthDayFromEpochDay(day)
                                    val inMonth = m == visibleMonth
                                    val isSelected = day == uiState.selectedEpochDay
                                    val isToday = day == todayEpochDay
                                    val dayEvs = gridEventsByDay[day].orEmpty()
                                    val hasNotes = day in gridNoteDays

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                } else Modifier
                                            )
                                            .clickable { onSelectEpochDay(day) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$d",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    inMonth -> MaterialTheme.colorScheme.onSurface
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                }
                                            )
                                            if (dayEvs.isNotEmpty() || hasNotes) {
                                                Spacer(Modifier.height(2.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    dayEvs.take(2).forEach { ev ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected) Color.White
                                                                    else eventAccentColor(ev.colorKey)
                                                                )
                                                        )
                                                    }
                                                    if (hasNotes) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected) Color.White.copy(alpha = 0.8f)
                                                                    else DzCrimsonRed
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (viewMode == CalendarViewMode.WEEK) {
            item {
                val weekDays = DateTimeUtils.weekDaysAround(uiState.selectedEpochDay)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { day ->
                            val isSelected = day == uiState.selectedEpochDay
                            val dStart = DateTimeUtils.startOfDayMillis(day, tz)
                            val dEnd = DateTimeUtils.endOfDayMillis(day, tz)
                            val count = uiState.events.count { it.startEpochMillis <= dEnd && it.endEpochMillis >= dStart }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable { onSelectEpochDay(day) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = DateTimeUtils.formatEpochDay(day, "EEE").take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateTimeUtils.formatEpochDay(day, "d"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (count > 0) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else DzCrimsonRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (viewMode == CalendarViewMode.AGENDA) {
            val todayEvents = uiState.events.filter {
                !it.isCompleted && it.startEpochMillis <= todayEnd && it.endEpochMillis >= todayStart
            }
            val futureEvents = uiState.events.filter {
                !it.isCompleted && it.startEpochMillis > todayEnd
            }
            val pastOrDone = uiState.events.filter { it.isCompleted }

            item {
                Text(
                    text = strings.todaysEvents,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (todayEvents.isEmpty()) {
                item {
                    EmptyStateCard(icon = Icons.Filled.Event, message = strings.noEventsToday)
                }
            } else {
                items(todayEvents, key = { "ag_today_${it.id}" }) { event ->
                    EventCardItem(
                        event = event,
                        isTodayEvent = true,
                        strings = strings,
                        use24Hour = prefs.use24HourFormat,
                        timeZone = tz,
                        onOpen = { onOpenEvent(event) },
                        onToggleComplete = { onToggleEventCompleted(event) },
                        onRequestDelete = { eventPendingDelete = event },
                        onExportCalendar = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, event.title)
                                putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                                putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startEpochMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endEpochMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.isAllDay)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                }
            }

            if (futureEvents.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.futureEvents,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(futureEvents, key = { "ag_future_${it.id}" }) { event ->
                    EventCardItem(
                        event = event,
                        isTodayEvent = false,
                        strings = strings,
                        use24Hour = prefs.use24HourFormat,
                        timeZone = tz,
                        onOpen = { onOpenEvent(event) },
                        onToggleComplete = { onToggleEventCompleted(event) },
                        onRequestDelete = { eventPendingDelete = event },
                        onExportCalendar = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, event.title)
                                putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                                putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startEpochMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endEpochMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.isAllDay)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                }
            }

            if (pastOrDone.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.completedEvents,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(pastOrDone, key = { "ag_done_${it.id}" }) { event ->
                    EventCardItem(
                        event = event,
                        isTodayEvent = false,
                        strings = strings,
                        use24Hour = prefs.use24HourFormat,
                        timeZone = tz,
                        onOpen = { onOpenEvent(event) },
                        onToggleComplete = { onToggleEventCompleted(event) },
                        onRequestDelete = { eventPendingDelete = event },
                        onExportCalendar = { }
                    )
                }
            }
        } else {
            // Selected Day Header + Quick Add buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateTimeUtils.formatEpochDay(uiState.selectedEpochDay, "EEEE, MMM d"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onAddEventForDay(uiState.selectedEpochDay) }) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.newEvent)
                        }
                    }
                }
            }

            if (dayEvents.isEmpty()) {
                item {
                    EmptyStateCard(icon = Icons.Filled.Event, message = strings.noEventsToday)
                }
            } else {
                items(dayEvents, key = { "day_ev_${it.id}" }) { event ->
                    EventCardItem(
                        event = event,
                        isTodayEvent = uiState.selectedEpochDay == todayEpochDay,
                        strings = strings,
                        use24Hour = prefs.use24HourFormat,
                        timeZone = tz,
                        onOpen = { onOpenEvent(event) },
                        onToggleComplete = { onToggleEventCompleted(event) },
                        onRequestDelete = { eventPendingDelete = event },
                        onExportCalendar = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, event.title)
                                putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                                putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startEpochMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endEpochMillis)
                                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.isAllDay)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                }
            }

            // Attached Notes for Selected Day
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.notesOnDay,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { onAddNoteForDay(uiState.selectedEpochDay) }) {
                        Text("+ ${strings.newNote}")
                    }
                }
            }

            if (dayNotes.isEmpty()) {
                item {
                    EmptyStateCard(icon = Icons.Filled.EditNote, message = strings.noNotesYet)
                }
            } else {
                items(dayNotes, key = { "day_note_${it.id}" }) { note ->
                    Card(
                        onClick = { onOpenNote(note) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = noteCardColor(note.colorKey, isDark)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = note.title.ifBlank { "Untitled Note" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (note.content.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    eventPendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = strings.confirmDeleteTitle,
            message = strings.confirmDeleteMessage,
            confirmLabel = strings.delete,
            cancelLabel = strings.cancel,
            onConfirm = {
                onDeleteEvent(target.id)
                eventPendingDelete = null
            },
            onDismiss = { eventPendingDelete = null }
        )
    }
}

@Composable
private fun EventCardItem(
    event: EventEntity,
    isTodayEvent: Boolean,
    strings: Strings,
    use24Hour: Boolean,
    timeZone: java.util.TimeZone,
    onOpen: () -> Unit,
    onToggleComplete: () -> Unit,
    onRequestDelete: () -> Unit,
    onExportCalendar: () -> Unit
) {
    val accent = eventAccentColor(event.colorKey)
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTodayEvent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isTodayEvent) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isTodayEvent && !event.isCompleted) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = strings.today,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    val timeText = if (event.isAllDay) {
                        "${DateTimeUtils.formatDateShort(event.startEpochMillis, timeZone)} · ${strings.allDay}"
                    } else {
                        "${DateTimeUtils.formatDateShort(event.startEpochMillis, timeZone)} · ${
                            DateTimeUtils.formatTime(event.startEpochMillis, use24Hour, timeZone)
                        } – ${DateTimeUtils.formatTime(event.endEpochMillis, use24Hour, timeZone)}"
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleComplete) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = strings.markDone,
                        tint = if (event.isCompleted) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (event.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = event.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (event.recurrence != RecurrenceRule.NONE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = event.recurrence.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!event.isCompleted) {
                        IconButton(onClick = onExportCalendar) {
                            Icon(
                                Icons.Filled.OpenInNew,
                                contentDescription = strings.exportToPhoneCalendar,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = onRequestDelete) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
