package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.ChecklistItem
import com.example.data.local.ChecklistSerializer
import com.example.data.local.NoteCategory
import com.example.data.local.NoteEntity
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.ThemeMode
import com.example.ui.strings.Strings
import com.example.ui.theme.NoteColorPalette
import com.example.ui.theme.noteAccentDot
import com.example.ui.theme.noteCardColor
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    initialNote: NoteEntity?,
    initialAttachedEpochDay: Long? = null,
    strings: Strings,
    language: AppLanguage,
    themeMode: ThemeMode,
    use24Hour: Boolean,
    timeZone: TimeZone,
    onAutoSave: suspend (NoteEntity) -> Long,
    onCreateReminderFromNote: (NoteEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var currentNoteId by remember { mutableStateOf(initialNote?.id ?: 0L) }
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var checklist by remember {
        mutableStateOf(
            if (initialNote != null) ChecklistSerializer.decode(initialNote.checklistJson) else emptyList()
        )
    }
    var newChecklistText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialNote?.category ?: NoteCategory.PERSONAL) }
    var colorKey by remember { mutableStateOf(initialNote?.colorKey ?: "default") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(initialNote?.isFavorite ?: false) }
    var attachedEpochDay by remember {
        mutableStateOf(initialNote?.attachedDateEpochDay ?: initialAttachedEpochDay)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var autoSaveLabelVisible by remember { mutableStateOf(false) }

    fun buildCurrentEntity(): NoteEntity {
        val now = System.currentTimeMillis()
        return NoteEntity(
            id = currentNoteId,
            title = title.trim(),
            content = content,
            checklistJson = ChecklistSerializer.encode(checklist),
            category = category,
            colorKey = colorKey,
            isPinned = isPinned,
            isFavorite = isFavorite,
            isArchived = initialNote?.isArchived ?: false,
            isDeleted = initialNote?.isDeleted ?: false,
            deletedAtMillis = initialNote?.deletedAtMillis,
            attachedDateEpochDay = attachedEpochDay,
            createdAtMillis = initialNote?.createdAtMillis ?: now,
            updatedAtMillis = now
        )
    }

    // Auto-save while typing (debounced 450ms) whenever meaningful content exists
    LaunchedEffect(title, content, checklist, category, colorKey, isPinned, isFavorite, attachedEpochDay) {
        if (title.isBlank() && content.isBlank() && checklist.isEmpty() && currentNoteId == 0L) {
            return@LaunchedEffect
        }
        delay(450L)
        val savedId = onAutoSave(buildCurrentEntity())
        if (currentNoteId == 0L && savedId > 0L) {
            currentNoteId = savedId
        }
        autoSaveLabelVisible = true
    }

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                if (title.isNotBlank() || content.isNotBlank() || checklist.isNotEmpty()) {
                    onAutoSave(buildCurrentEntity())
                }
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = noteCardColor(colorKey, isDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (autoSaveLabelVisible) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.autoSaved,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.testTag("note_editor_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) strings.unpin else strings.pin,
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.testTag("note_editor_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = strings.favorites,
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                if (title.isNotBlank() || content.isNotBlank() || checklist.isNotEmpty()) {
                                    val savedId = onAutoSave(buildCurrentEntity())
                                    if (currentNoteId == 0L) currentNoteId = savedId
                                }
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("note_editor_save_button")
                    ) {
                        Text(strings.save)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(strings.noteTitleHint) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text(strings.noteBodyHint) },
                minLines = 4,
                maxLines = 10,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Formatting & Date Attachment Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        content = if (content.isEmpty() || content.endsWith("\n")) {
                            "$content• "
                        } else {
                            "$content\n• "
                        }
                    },
                    modifier = Modifier.testTag("note_add_bullet_button")
                ) {
                    Text(strings.addBullet)
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.testTag("note_attach_date_button")
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = attachedEpochDay?.let { DateTimeUtils.formatEpochDay(it, "MMM d, yyyy") }
                            ?: strings.attachToDate
                    )
                }

                if (attachedEpochDay != null) {
                    TextButton(onClick = { attachedEpochDay = null }) {
                        Text(strings.clearDate)
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val entity = buildCurrentEntity().let {
                                if (it.title.isBlank() && it.content.isBlank()) {
                                    it.copy(title = "Note")
                                } else it
                            }
                            val savedId = onAutoSave(entity)
                            currentNoteId = savedId
                            onCreateReminderFromNote(entity.copy(id = savedId))
                        }
                    },
                    modifier = Modifier.testTag("note_create_reminder_button")
                ) {
                    Icon(Icons.Filled.AlarmAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.createReminder)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Checklist Section
            Text(
                text = strings.addChecklistItem,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            checklist.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { checked ->
                            checklist = checklist.map {
                                if (it.id == item.id) it.copy(isChecked = checked) else it
                            }
                        }
                    )
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium.merge(
                            TextStyle(
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                            )
                        ),
                        color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        checklist = checklist.filterNot { it.id == item.id }
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = strings.delete,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newChecklistText,
                    onValueChange = { newChecklistText = it },
                    placeholder = { Text(strings.addChecklistItem) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("note_checklist_input")
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val trimmed = newChecklistText.trim()
                        if (trimmed.isNotEmpty()) {
                            checklist = checklist + ChecklistItem(
                                id = UUID.randomUUID().toString(),
                                text = trimmed,
                                isChecked = false
                            )
                            newChecklistText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .testTag("note_checklist_add_button")
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = strings.addChecklistItem,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(strings.noteCategoryLabel(cat, language)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Color Swatches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteColorPalette.forEach { option ->
                    val selected = colorKey == option.key
                    val bg = noteCardColor(option.key, isDark)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .border(
                                BorderStroke(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) noteAccentDot(option.key) else MaterialTheme.colorScheme.outline
                                ),
                                shape = CircleShape
                            )
                            .clickable { colorKey = option.key },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(option.accentDot)
                        )
                    }
                }
            }

            if (initialNote != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Created: ${DateTimeUtils.formatDateTime(initialNote.createdAtMillis, use24Hour, timeZone)}  ·  Modified: ${DateTimeUtils.formatDateTime(initialNote.updatedAtMillis, use24Hour, timeZone)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val defaultDay = attachedEpochDay ?: DateTimeUtils.todayEpochDay(timeZone)
        DzDatePickerModal(
            initialEpochDay = defaultDay,
            onDateSelected = { attachedEpochDay = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
