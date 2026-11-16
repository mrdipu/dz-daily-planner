package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupSummary
import com.example.data.local.EventEntity
import com.example.data.local.NoteCategory
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.PreferencesRepository
import com.example.data.prefs.ThemeMode
import com.example.data.prefs.UserPreferences
import com.example.data.repository.PlannerRepository
import com.example.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NotesTab { ACTIVE, FAVORITES, ARCHIVED, TRASH }
enum class NotesSort { NEWEST, OLDEST, ALPHABETICAL }
enum class CalendarViewMode { MONTH, WEEK, AGENDA }
enum class ReminderFilter { UPCOMING, TODAY, OVERDUE, COMPLETED }

data class PlannerUiState(
    val notes: List<NoteEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val selectedEpochDay: Long = DateTimeUtils.todayEpochDay(
        DateTimeUtils.resolveTimeZone(false, DateTimeUtils.DEFAULT_TIMEZONE_ID)
    ),
    val currentClockMillis: Long = System.currentTimeMillis()
)

class PlannerViewModel(
    private val repository: PlannerRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedEpochDayFlow = MutableStateFlow(
        DateTimeUtils.todayEpochDay(
            DateTimeUtils.resolveTimeZone(false, DateTimeUtils.DEFAULT_TIMEZONE_ID)
        )
    )
    private val _liveClockMillis = MutableStateFlow(System.currentTimeMillis())
    val liveClockMillis: StateFlow<Long> = _liveClockMillis.asStateFlow()

    // Minute-level tick for overdue badges/filters without forcing 1Hz full-screen recompositions
    private val minuteTickFlow = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<PlannerUiState> = combine(
        repository.notesFlow,
        repository.eventsFlow,
        repository.remindersFlow,
        preferencesRepository.preferencesFlow,
        selectedEpochDayFlow,
        minuteTickFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val notes = values[0] as List<NoteEntity>
        @Suppress("UNCHECKED_CAST")
        val events = values[1] as List<EventEntity>
        @Suppress("UNCHECKED_CAST")
        val reminders = values[2] as List<ReminderEntity>
        val prefs = values[3] as UserPreferences
        val selectedDay = values[4] as Long
        val clock = values[5] as Long
        PlannerUiState(
            notes = notes,
            events = events,
            reminders = reminders,
            preferences = prefs,
            selectedEpochDay = selectedDay,
            currentClockMillis = clock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState()
    )

    // Notes filter/search state
    private val _notesSearchQuery = MutableStateFlow("")
    val notesSearchQuery: StateFlow<String> = _notesSearchQuery.asStateFlow()

    private val _notesTab = MutableStateFlow(NotesTab.ACTIVE)
    val notesTab: StateFlow<NotesTab> = _notesTab.asStateFlow()

    private val _notesCategoryFilter = MutableStateFlow<NoteCategory?>(null)
    val notesCategoryFilter: StateFlow<NoteCategory?> = _notesCategoryFilter.asStateFlow()

    private val _notesDateFilterEpochDay = MutableStateFlow<Long?>(null)
    val notesDateFilterEpochDay: StateFlow<Long?> = _notesDateFilterEpochDay.asStateFlow()

    private val _notesSort = MutableStateFlow(NotesSort.NEWEST)
    val notesSort: StateFlow<NotesSort> = _notesSort.asStateFlow()

    // Calendar view state
    private val _calendarViewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val calendarViewMode: StateFlow<CalendarViewMode> = _calendarViewMode.asStateFlow()

    // Reminder filter state
    private val _reminderFilter = MutableStateFlow(ReminderFilter.TODAY)
    val reminderFilter: StateFlow<ReminderFilter> = _reminderFilter.asStateFlow()

    private val _highlightedReminderId = MutableStateFlow<Long?>(null)
    val highlightedReminderId: StateFlow<Long?> = _highlightedReminderId.asStateFlow()

    init {
        viewModelScope.launch {
            var secondsCounter = 0
            while (true) {
                val now = System.currentTimeMillis()
                _liveClockMillis.value = now
                if (secondsCounter % 30 == 0) {
                    minuteTickFlow.value = now
                }
                secondsCounter++
                delay(1000L)
            }
        }
    }

    fun selectEpochDay(epochDay: Long) {
        selectedEpochDayFlow.value = epochDay
    }

    fun jumpToToday() {
        val prefs = uiState.value.preferences
        val tz = DateTimeUtils.resolveTimeZone(prefs.useDeviceTimezone, prefs.customTimezoneId)
        selectedEpochDayFlow.value = DateTimeUtils.todayEpochDay(tz)
    }

    fun setNotesSearchQuery(query: String) {
        _notesSearchQuery.value = query
    }

    fun setNotesTab(tab: NotesTab) {
        _notesTab.value = tab
    }

    fun setNotesCategoryFilter(category: NoteCategory?) {
        _notesCategoryFilter.value = category
    }

    fun setNotesDateFilter(epochDay: Long?) {
        _notesDateFilterEpochDay.value = epochDay
    }

    fun setNotesSort(sort: NotesSort) {
        _notesSort.value = sort
    }

    fun setCalendarViewMode(mode: CalendarViewMode) {
        _calendarViewMode.value = mode
    }

    fun setReminderFilter(filter: ReminderFilter) {
        _reminderFilter.value = filter
    }

    fun highlightReminder(reminderId: Long?) {
        _highlightedReminderId.value = reminderId
    }

    // -------- Note Actions --------
    suspend fun saveNoteAndReturnId(note: NoteEntity): Long = repository.saveNote(note)

    fun saveNote(note: NoteEntity) {
        viewModelScope.launch { repository.saveNote(note) }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch { repository.duplicateNote(note) }
    }

    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch { repository.toggleNotePin(note) }
    }

    fun toggleNoteFavorite(note: NoteEntity) {
        viewModelScope.launch { repository.toggleNoteFavorite(note) }
    }

    fun toggleNoteArchive(note: NoteEntity) {
        viewModelScope.launch { repository.toggleNoteArchive(note) }
    }

    fun moveNoteToTrash(note: NoteEntity) {
        viewModelScope.launch { repository.moveNoteToTrash(note) }
    }

    fun restoreNoteFromTrash(note: NoteEntity) {
        viewModelScope.launch { repository.restoreNoteFromTrash(note) }
    }

    fun permanentlyDeleteNote(noteId: Long) {
        viewModelScope.launch { repository.permanentlyDeleteNote(noteId) }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch { repository.emptyRecycleBin() }
    }

    // -------- Event Actions --------
    fun saveEvent(event: EventEntity) {
        viewModelScope.launch { repository.saveEvent(event) }
    }

    fun toggleEventCompleted(event: EventEntity) {
        viewModelScope.launch { repository.toggleEventCompleted(event) }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch { repository.deleteEvent(eventId) }
    }

    // -------- Reminder Actions --------
    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repository.saveReminder(reminder) }
    }

    fun completeReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repository.completeReminder(reminder) }
    }

    fun reopenReminder(reminder: ReminderEntity) {
        viewModelScope.launch { repository.reopenReminder(reminder) }
    }

    fun snoozeReminder(reminderId: Long, minutes: Int) {
        viewModelScope.launch { repository.snoozeReminder(reminderId, minutes) }
    }

    fun rescheduleReminder(reminder: ReminderEntity, newTimeMillis: Long) {
        viewModelScope.launch { repository.rescheduleReminder(reminder, newTimeMillis) }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch { repository.deleteReminder(reminderId) }
    }

    // -------- Preferences --------
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setUse24HourFormat(use24Hour: Boolean) {
        viewModelScope.launch { preferencesRepository.setUse24HourFormat(use24Hour) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setUseDeviceTimezone(useDevice: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseDeviceTimezone(useDevice) }
    }

    fun setCustomTimezoneId(tzId: String) {
        viewModelScope.launch { preferencesRepository.setCustomTimezoneId(tzId) }
    }

    fun setDefaultReminderLeadMinutes(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultReminderLeadMinutes(minutes) }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultSnoozeMinutes(minutes) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) }
    }

    // -------- Backup / Restore --------
    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun importBackupJson(rawJson: String): Result<BackupSummary> =
        repository.importBackupJson(rawJson)

    class Factory(
        private val repository: PlannerRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlannerViewModel(repository, preferencesRepository) as T
        }
    }
}
