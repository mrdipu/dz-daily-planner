package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ChecklistSerializer
import com.example.data.local.NoteCategory
import com.example.data.local.NoteEntity
import com.example.data.prefs.ThemeMode
import com.example.ui.NotesSort
import com.example.ui.NotesTab
import com.example.ui.PlannerUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.strings.Strings
import com.example.ui.theme.noteAccentDot
import com.example.ui.theme.noteCardColor
import com.example.util.DateTimeUtils

@Composable
fun NotesScreen(
    uiState: PlannerUiState,
    strings: Strings,
    searchQuery: String,
    currentTab: NotesTab,
    categoryFilter: NoteCategory?,
    dateFilterEpochDay: Long?,
    sortOrder: NotesSort,
    onSearchChange: (String) -> Unit,
    onTabChange: (NotesTab) -> Unit,
    onCategoryFilterChange: (NoteCategory?) -> Unit,
    onDateFilterClear: () -> Unit,
    onSortChange: (NotesSort) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onToggleFavorite: (NoteEntity) -> Unit,
    onDuplicate: (NoteEntity) -> Unit,
    onToggleArchive: (NoteEntity) -> Unit,
    onMoveToTrash: (NoteEntity) -> Unit,
    onRestoreFromTrash: (NoteEntity) -> Unit,
    onPermanentDelete: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    onCreateReminderFromNote: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val tz = DateTimeUtils.resolveTimeZone(prefs.useDeviceTimezone, prefs.customTimezoneId)
    val isDark = when (prefs.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var notePendingDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }

    val filteredNotes = remember(
        uiState.notes,
        searchQuery,
        currentTab,
        categoryFilter,
        dateFilterEpochDay,
        sortOrder
    ) {
        val base = when (currentTab) {
            NotesTab.ACTIVE -> uiState.notes.filter { !it.isDeleted && !it.isArchived }
            NotesTab.FAVORITES -> uiState.notes.filter { !it.isDeleted && !it.isArchived && it.isFavorite }
            NotesTab.ARCHIVED -> uiState.notes.filter { !it.isDeleted && it.isArchived }
            NotesTab.TRASH -> uiState.notes.filter { it.isDeleted }
        }
        val byCat = if (categoryFilter != null) base.filter { it.category == categoryFilter } else base
        val byDate = if (dateFilterEpochDay != null) {
            byCat.filter {
                it.attachedDateEpochDay == dateFilterEpochDay ||
                    DateTimeUtils.epochDayFromMillis(it.createdAtMillis, tz) == dateFilterEpochDay
            }
        } else byCat

        val q = searchQuery.trim().lowercase()
        val searched = if (q.isEmpty()) byDate else byDate.filter {
            it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
        }

        val comparator: Comparator<NoteEntity> = when (sortOrder) {
            NotesSort.NEWEST -> compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAtMillis }
            NotesSort.OLDEST -> compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.createdAtMillis }
            NotesSort.ALPHABETICAL -> compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.title.lowercase() }
        }
        searched.sortedWith(comparator)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("notes_screen")
    ) {
        // Search + Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(strings.searchNotesHint) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("notes_search_input")
            )

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("notes_sort_button")
                ) {
                    Icon(Icons.Filled.Sort, contentDescription = strings.sortNewest)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.sortNewest) },
                        onClick = {
                            onSortChange(NotesSort.NEWEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.sortOldest) },
                        onClick = {
                            onSortChange(NotesSort.OLDEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.sortAlpha) },
                        onClick = {
                            onSortChange(NotesSort.ALPHABETICAL)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // Primary Folder Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabItems = listOf(
                NotesTab.ACTIVE to strings.allNotes,
                NotesTab.FAVORITES to strings.favorites,
                NotesTab.ARCHIVED to strings.archived,
                NotesTab.TRASH to strings.recycleBin
            )
            tabItems.forEach { (tab, label) ->
                FilterChip(
                    selected = currentTab == tab,
                    onClick = { onTabChange(tab) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("notes_tab_${tab.name.lowercase()}")
                )
            }
        }

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (dateFilterEpochDay != null) {
                FilterChip(
                    selected = true,
                    onClick = onDateFilterClear,
                    label = { Text("${DateTimeUtils.formatEpochDay(dateFilterEpochDay, "MMM d")} ×") }
                )
            }
            NoteCategory.entries.forEach { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = {
                        onCategoryFilterChange(if (categoryFilter == cat) null else cat)
                    },
                    label = { Text(strings.noteCategoryLabel(cat, prefs.language)) }
                )
            }
        }

        if (currentTab == NotesTab.TRASH && filteredNotes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { confirmEmptyTrash = true },
                    modifier = Modifier.testTag("notes_empty_trash_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.emptyTrash)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredNotes.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.EditNote,
                        message = strings.noNotesYet
                    )
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteCardItem(
                        note = note,
                        strings = strings,
                        isDark = isDark,
                        use24Hour = prefs.use24HourFormat,
                        timeZone = tz,
                        onOpen = { onOpenNote(note) },
                        onTogglePin = { onTogglePin(note) },
                        onToggleFavorite = { onToggleFavorite(note) },
                        onDuplicate = { onDuplicate(note) },
                        onToggleArchive = { onToggleArchive(note) },
                        onMoveToTrash = { onMoveToTrash(note) },
                        onRestore = { onRestoreFromTrash(note) },
                        onRequestPermanentDelete = { notePendingDelete = note },
                        onCreateReminder = { onCreateReminderFromNote(note) }
                    )
                }
            }
        }
    }

    notePendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = strings.confirmDeleteTitle,
            message = strings.confirmDeleteMessage,
            confirmLabel = strings.deleteForever,
            cancelLabel = strings.cancel,
            onConfirm = {
                onPermanentDelete(target.id)
                notePendingDelete = null
            },
            onDismiss = { notePendingDelete = null }
        )
    }

    if (confirmEmptyTrash) {
        ConfirmDeleteDialog(
            title = strings.emptyTrash,
            message = strings.confirmDeleteMessage,
            confirmLabel = strings.emptyTrash,
            cancelLabel = strings.cancel,
            onConfirm = {
                onEmptyTrash()
                confirmEmptyTrash = false
            },
            onDismiss = { confirmEmptyTrash = false }
        )
    }
}

@Composable
private fun NoteCardItem(
    note: NoteEntity,
    strings: Strings,
    isDark: Boolean,
    use24Hour: Boolean,
    timeZone: java.util.TimeZone,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleArchive: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    onRequestPermanentDelete: () -> Unit,
    onCreateReminder: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val checklist = remember(note.checklistJson) { ChecklistSerializer.decode(note.checklistJson) }

    Card(
        onClick = { if (!note.isDeleted) onOpen() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = noteCardColor(note.colorKey, isDark)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(noteAccentDot(note.colorKey))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = strings.pin,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!note.isDeleted) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = strings.favorites,
                                tint = if (note.isFavorite) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Note options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (note.isDeleted) {
                                DropdownMenuItem(
                                    text = { Text(strings.restore) },
                                    leadingIcon = { Icon(Icons.Filled.RestoreFromTrash, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRestore()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.deleteForever) },
                                    leadingIcon = { Icon(Icons.Filled.DeleteForever, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestPermanentDelete()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(if (note.isPinned) strings.unpin else strings.pin) },
                                    leadingIcon = { Icon(Icons.Filled.PushPin, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onTogglePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.duplicate) },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onDuplicate()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.createReminder) },
                                    leadingIcon = { Icon(Icons.Filled.AlarmAdd, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onCreateReminder()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (note.isArchived) strings.unarchive else strings.archive) },
                                    leadingIcon = {
                                        Icon(
                                            if (note.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                            null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleArchive()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.moveToTrash) },
                                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveToTrash()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (checklist.isNotEmpty()) {
                val checked = checklist.count { it.isChecked }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "☑ $checked / ${checklist.size} checklist items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = note.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (note.attachedDateEpochDay != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = DateTimeUtils.formatEpochDay(note.attachedDateEpochDay, "MMM d"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = DateTimeUtils.formatDateTime(note.updatedAtMillis, use24Hour, timeZone),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
