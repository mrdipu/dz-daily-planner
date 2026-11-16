package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.notifications.NotificationHelper
import com.example.ui.PlannerViewModel
import com.example.ui.ReminderFilter
import com.example.ui.components.AnimatedSplashScreen
import com.example.ui.components.EventEditorSheet
import com.example.ui.components.NoteEditorSheet
import com.example.ui.components.QuickCreateSpeedDial
import com.example.ui.components.ReminderEditorSheet
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.strings.stringsFor
import com.example.ui.theme.DZDailyPlannerTheme
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_HOME = "home"
const val ROUTE_NOTES = "notes"
const val ROUTE_CALENDAR = "calendar"
const val ROUTE_REMINDERS = "reminders"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val pendingNotificationReminderId = mutableLongStateOf(-1L)
    private val pendingOpenTab = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDeepLinkIntent(intent)

        val app = application as DZPlannerApp
        val factory = PlannerViewModel.Factory(
            repository = app.container.plannerRepository,
            preferencesRepository = app.container.preferencesRepository
        )

        setContent {
            val viewModel: PlannerViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var showSplash by rememberSaveable { mutableStateOf(true) }

            DZDailyPlannerTheme(
                themeMode = uiState.preferences.themeMode,
                forceLightSystemBars = showSplash
            ) {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 360),
                    label = "splash_transition"
                ) { splashing ->
                    if (splashing) {
                        AnimatedSplashScreen(
                            onFinished = { showSplash = false }
                        )
                    } else {
                        DZPlannerRootShell(
                            viewModel = viewModel,
                            deepLinkReminderId = pendingNotificationReminderId.longValue,
                            deepLinkTab = pendingOpenTab.value,
                            onDeepLinkConsumed = {
                                pendingNotificationReminderId.longValue = -1L
                                pendingOpenTab.value = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLinkIntent(intent)
    }

    private fun consumeDeepLinkIntent(intent: Intent?) {
        if (intent == null) return
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        val openTab = intent.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB)
        if (reminderId > 0L) {
            pendingNotificationReminderId.longValue = reminderId
        }
        if (!openTab.isNullOrBlank()) {
            pendingOpenTab.value = openTab
        }
    }
}

@Composable
private fun DZPlannerRootShell(
    viewModel: PlannerViewModel,
    deepLinkReminderId: Long,
    deepLinkTab: String?,
    onDeepLinkConsumed: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as DZPlannerApp
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notesSearchQuery by viewModel.notesSearchQuery.collectAsStateWithLifecycle()
    val notesTab by viewModel.notesTab.collectAsStateWithLifecycle()
    val notesCategoryFilter by viewModel.notesCategoryFilter.collectAsStateWithLifecycle()
    val notesDateFilter by viewModel.notesDateFilterEpochDay.collectAsStateWithLifecycle()
    val notesSort by viewModel.notesSort.collectAsStateWithLifecycle()
    val calendarViewMode by viewModel.calendarViewMode.collectAsStateWithLifecycle()
    val reminderFilter by viewModel.reminderFilter.collectAsStateWithLifecycle()
    val highlightedReminderId by viewModel.highlightedReminderId.collectAsStateWithLifecycle()

    val prefs = uiState.preferences
    val strings = remember(prefs.language) { stringsFor(prefs.language) }
    val tz = remember(prefs.useDeviceTimezone, prefs.customTimezoneId) {
        DateTimeUtils.resolveTimeZone(prefs.useDeviceTimezone, prefs.customTimezoneId)
    }

    // Permission states refreshed on resume
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(app.container.reminderScheduler.canScheduleExactAlarms())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
                canScheduleExactAlarms = app.container.reminderScheduler.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Notification permission is needed to alert you on time.")
            }
        }
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun openExactAlarmSystemSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            runCatching { context.startActivity(intent) }
        }
    }

    // SAF Backup Export / Import Launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = viewModel.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Could not open output stream")
                    }
                }
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) "Backup exported successfully."
                    else "Backup export failed: ${result.exceptionOrNull()?.localizedMessage}"
                )
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Could not read selected file")
                    }
                    viewModel.importBackupJson(json).getOrThrow()
                }
                result.onSuccess { summary ->
                    snackbarHostState.showSnackbar(
                        "Restored ${summary.notesCount} notes, ${summary.eventsCount} events, ${summary.remindersCount} reminders."
                    )
                }.onFailure { err ->
                    snackbarHostState.showSnackbar("Restore failed: ${err.localizedMessage}")
                }
            }
        }
    }

    // Handle deep link from tapped notification
    LaunchedEffect(deepLinkReminderId, deepLinkTab) {
        if (deepLinkTab == NotificationHelper.TAB_REMINDERS || deepLinkReminderId > 0L) {
            navController.navigate(ROUTE_REMINDERS) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            if (deepLinkReminderId > 0L) {
                viewModel.highlightReminder(deepLinkReminderId)
                val target = uiState.reminders.firstOrNull { it.id == deepLinkReminderId }
                if (target != null) {
                    val now = System.currentTimeMillis()
                    val t = target.snoozedUntilMillis ?: target.triggerAtMillis
                    viewModel.setReminderFilter(
                        when {
                            target.isCompleted -> ReminderFilter.COMPLETED
                            t < now -> ReminderFilter.OVERDUE
                            else -> ReminderFilter.UPCOMING
                        }
                    )
                }
            }
            onDeepLinkConsumed()
        }
    }

    // Editor Modal States
    var quickMenuExpanded by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var noteEditorVisible by remember { mutableStateOf(false) }
    var noteEditorInitialDay by remember { mutableStateOf<Long?>(null) }

    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var eventEditorVisible by remember { mutableStateOf(false) }
    var eventEditorInitialDay by remember { mutableLongStateOf(uiState.selectedEpochDay) }

    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderEditorVisible by remember { mutableStateOf(false) }
    var reminderPrefillTitle by remember { mutableStateOf("") }
    var reminderPrefillDesc by remember { mutableStateOf("") }
    var reminderLinkedNoteId by remember { mutableStateOf<Long?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    val overdueCount = remember(uiState.reminders, uiState.currentClockMillis) {
        val now = uiState.currentClockMillis
        uiState.reminders.count { !it.isCompleted && (it.snoozedUntilMillis ?: it.triggerAtMillis) < now }
    }

    val destinations = remember(strings) {
        listOf(
            NavDestinationItem(ROUTE_HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
            NavDestinationItem(ROUTE_NOTES, strings.navNotes, Icons.Filled.EditNote, Icons.Outlined.EditNote),
            NavDestinationItem(ROUTE_CALENDAR, strings.navCalendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
            NavDestinationItem(ROUTE_REMINDERS, strings.navReminders, Icons.Filled.Alarm, Icons.Outlined.Alarm),
            NavDestinationItem(ROUTE_SETTINGS, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    fun navigateToTab(route: String) {
        quickMenuExpanded = false
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    destinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(dest.route) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (dest.route == ROUTE_REMINDERS && overdueCount > 0) {
                                            Badge { Text("$overdueCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = dest.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_rail_${dest.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest.route == ROUTE_REMINDERS && overdueCount > 0) {
                                                    Badge { Text("$overdueCount") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_bottom_${dest.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentRoute != ROUTE_SETTINGS) {
                        QuickCreateSpeedDial(
                            expanded = quickMenuExpanded,
                            strings = strings,
                            onToggle = { quickMenuExpanded = !quickMenuExpanded },
                            onNewNote = {
                                editingNote = null
                                noteEditorInitialDay = if (currentRoute == ROUTE_CALENDAR) uiState.selectedEpochDay else null
                                noteEditorVisible = true
                            },
                            onNewEvent = {
                                editingEvent = null
                                eventEditorInitialDay = uiState.selectedEpochDay
                                eventEditorVisible = true
                            },
                            onNewReminder = {
                                editingReminder = null
                                reminderPrefillTitle = ""
                                reminderPrefillDesc = ""
                                reminderLinkedNoteId = null
                                reminderEditorVisible = true
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier.widthIn(max = 760.dp)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeDashboardScreen(
                                uiState = uiState,
                                liveClockFlow = viewModel.liveClockMillis,
                                strings = strings,
                                onSelectEpochDay = viewModel::selectEpochDay,
                                onJumpToToday = viewModel::jumpToToday,
                                onOpenNote = { note ->
                                    editingNote = note
                                    noteEditorInitialDay = note.attachedDateEpochDay
                                    noteEditorVisible = true
                                },
                                onOpenEvent = { event ->
                                    editingEvent = event
                                    eventEditorInitialDay = DateTimeUtils.epochDayFromMillis(event.startEpochMillis, tz)
                                    eventEditorVisible = true
                                },
                                onToggleEventComplete = viewModel::toggleEventCompleted,
                                onCompleteReminder = viewModel::completeReminder,
                                onNavigateNotes = { navigateToTab(ROUTE_NOTES) },
                                onNavigateCalendar = { navigateToTab(ROUTE_CALENDAR) },
                                onNavigateReminders = { overdueOnly ->
                                    if (overdueOnly) {
                                        viewModel.setReminderFilter(ReminderFilter.OVERDUE)
                                    }
                                    navigateToTab(ROUTE_REMINDERS)
                                }
                            )
                        }

                        composable(ROUTE_NOTES) {
                            NotesScreen(
                                uiState = uiState,
                                strings = strings,
                                searchQuery = notesSearchQuery,
                                currentTab = notesTab,
                                categoryFilter = notesCategoryFilter,
                                dateFilterEpochDay = notesDateFilter,
                                sortOrder = notesSort,
                                onSearchChange = viewModel::setNotesSearchQuery,
                                onTabChange = viewModel::setNotesTab,
                                onCategoryFilterChange = viewModel::setNotesCategoryFilter,
                                onDateFilterClear = { viewModel.setNotesDateFilter(null) },
                                onSortChange = viewModel::setNotesSort,
                                onOpenNote = { note ->
                                    editingNote = note
                                    noteEditorInitialDay = note.attachedDateEpochDay
                                    noteEditorVisible = true
                                },
                                onTogglePin = viewModel::toggleNotePin,
                                onToggleFavorite = viewModel::toggleNoteFavorite,
                                onDuplicate = viewModel::duplicateNote,
                                onToggleArchive = viewModel::toggleNoteArchive,
                                onMoveToTrash = viewModel::moveNoteToTrash,
                                onRestoreFromTrash = viewModel::restoreNoteFromTrash,
                                onPermanentDelete = viewModel::permanentlyDeleteNote,
                                onEmptyTrash = viewModel::emptyRecycleBin,
                                onCreateReminderFromNote = { note ->
                                    editingReminder = null
                                    reminderPrefillTitle = note.title.ifBlank { "Note Reminder" }
                                    reminderPrefillDesc = note.content.take(160)
                                    reminderLinkedNoteId = note.id
                                    reminderEditorVisible = true
                                }
                            )
                        }

                        composable(ROUTE_CALENDAR) {
                            CalendarScreen(
                                uiState = uiState,
                                strings = strings,
                                viewMode = calendarViewMode,
                                onViewModeChange = viewModel::setCalendarViewMode,
                                onSelectEpochDay = viewModel::selectEpochDay,
                                onJumpToToday = viewModel::jumpToToday,
                                onAddEventForDay = { day ->
                                    editingEvent = null
                                    eventEditorInitialDay = day
                                    eventEditorVisible = true
                                },
                                onAddNoteForDay = { day ->
                                    editingNote = null
                                    noteEditorInitialDay = day
                                    noteEditorVisible = true
                                },
                                onOpenEvent = { event ->
                                    editingEvent = event
                                    eventEditorInitialDay = DateTimeUtils.epochDayFromMillis(event.startEpochMillis, tz)
                                    eventEditorVisible = true
                                },
                                onToggleEventCompleted = viewModel::toggleEventCompleted,
                                onDeleteEvent = viewModel::deleteEvent,
                                onOpenNote = { note ->
                                    editingNote = note
                                    noteEditorInitialDay = note.attachedDateEpochDay
                                    noteEditorVisible = true
                                }
                            )
                        }

                        composable(ROUTE_REMINDERS) {
                            RemindersScreen(
                                uiState = uiState,
                                strings = strings,
                                currentFilter = reminderFilter,
                                highlightedReminderId = highlightedReminderId,
                                hasNotificationPermission = hasNotificationPermission,
                                canScheduleExactAlarms = canScheduleExactAlarms,
                                onRequestNotificationPermission = { requestNotificationsIfNeeded() },
                                onOpenExactAlarmSettings = { openExactAlarmSystemSettings() },
                                onFilterChange = viewModel::setReminderFilter,
                                onOpenReminder = { reminder ->
                                    editingReminder = reminder
                                    reminderPrefillTitle = ""
                                    reminderPrefillDesc = ""
                                    reminderLinkedNoteId = reminder.linkedNoteId
                                    reminderEditorVisible = true
                                },
                                onCompleteReminder = viewModel::completeReminder,
                                onReopenReminder = viewModel::reopenReminder,
                                onSnoozeReminder = viewModel::snoozeReminder,
                                onDeleteReminder = viewModel::deleteReminder
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                strings = strings,
                                hasNotificationPermission = hasNotificationPermission,
                                canScheduleExactAlarms = canScheduleExactAlarms,
                                onRequestNotificationPermission = { requestNotificationsIfNeeded() },
                                onOpenExactAlarmSettings = { openExactAlarmSystemSettings() },
                                onSendTestNotification = {
                                    if (!hasNotificationPermission) {
                                        requestNotificationsIfNeeded()
                                    } else {
                                        NotificationHelper.showTestNotification(
                                            context = context,
                                            title = strings.appName,
                                            message = "Reminder notifications are working cleanly on your device."
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Test notification dispatched.")
                                        }
                                    }
                                },
                                onThemeModeChange = viewModel::setThemeMode,
                                onUse24HourChange = viewModel::setUse24HourFormat,
                                onLanguageChange = viewModel::setLanguage,
                                onUseDeviceTimezoneChange = viewModel::setUseDeviceTimezone,
                                onCustomTimezoneChange = viewModel::setCustomTimezoneId,
                                onDefaultLeadChange = viewModel::setDefaultReminderLeadMinutes,
                                onDefaultSnoozeChange = viewModel::setDefaultSnoozeMinutes,
                                onSoundEnabledChange = viewModel::setSoundEnabled,
                                onVibrationEnabledChange = viewModel::setVibrationEnabled,
                                onExportBackup = {
                                    exportBackupLauncher.launch("dz_daily_planner_backup.json")
                                },
                                onRestoreBackup = {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (noteEditorVisible) {
        NoteEditorSheet(
            initialNote = editingNote,
            initialAttachedEpochDay = noteEditorInitialDay,
            strings = strings,
            language = prefs.language,
            themeMode = prefs.themeMode,
            use24Hour = prefs.use24HourFormat,
            timeZone = tz,
            onAutoSave = viewModel::saveNoteAndReturnId,
            onCreateReminderFromNote = { savedNote ->
                noteEditorVisible = false
                editingReminder = null
                reminderPrefillTitle = savedNote.title.ifBlank { "Note Reminder" }
                reminderPrefillDesc = savedNote.content.take(160)
                reminderLinkedNoteId = savedNote.id
                reminderEditorVisible = true
            },
            onDismiss = { noteEditorVisible = false }
        )
    }

    if (eventEditorVisible) {
        EventEditorSheet(
            initialEvent = editingEvent,
            initialEpochDay = eventEditorInitialDay,
            defaultReminderLeadMinutes = prefs.defaultReminderLeadMinutes,
            availableNotes = uiState.notes,
            strings = strings,
            language = prefs.language,
            use24Hour = prefs.use24HourFormat,
            timeZone = tz,
            onSave = viewModel::saveEvent,
            onDismiss = { eventEditorVisible = false }
        )
    }

    if (reminderEditorVisible) {
        ReminderEditorSheet(
            initialReminder = editingReminder,
            prefilledTitle = reminderPrefillTitle,
            prefilledDescription = reminderPrefillDesc,
            linkedNoteId = reminderLinkedNoteId,
            strings = strings,
            language = prefs.language,
            use24Hour = prefs.use24HourFormat,
            timeZone = tz,
            onSave = { reminder ->
                viewModel.saveReminder(reminder)
                if (!hasNotificationPermission) {
                    requestNotificationsIfNeeded()
                }
            },
            onDismiss = { reminderEditorVisible = false }
        )
    }
}
