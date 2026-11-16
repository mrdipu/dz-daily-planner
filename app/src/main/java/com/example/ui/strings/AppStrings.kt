package com.example.ui.strings

import com.example.data.local.EventCategory
import com.example.data.local.NoteCategory
import com.example.data.local.RecurrenceRule
import com.example.data.local.RepeatType
import com.example.data.prefs.AppLanguage

data class Strings(
    val appName: String,
    val poweredBy: String,
    val navHome: String,
    val navNotes: String,
    val navCalendar: String,
    val navReminders: String,
    val navSettings: String,
    val goodMorning: String,
    val goodAfternoon: String,
    val goodEvening: String,
    val todayProductivity: String,
    val notesCountLabel: String,
    val eventsTodayLabel: String,
    val remindersDoneLabel: String,
    val overdueAlertTitle: String,
    val overdueAlertSubtitle: String,
    val todaysEvents: String,
    val todaysNotes: String,
    val upcomingReminders: String,
    val quickCreate: String,
    val newNote: String,
    val newEvent: String,
    val newReminder: String,
    val viewAll: String,
    val noEventsToday: String,
    val noNotesYet: String,
    val noRemindersUpcoming: String,
    val searchNotesHint: String,
    val allNotes: String,
    val favorites: String,
    val archived: String,
    val recycleBin: String,
    val sortNewest: String,
    val sortOldest: String,
    val sortAlpha: String,
    val emptyTrash: String,
    val restore: String,
    val deleteForever: String,
    val pin: String,
    val unpin: String,
    val duplicate: String,
    val archive: String,
    val unarchive: String,
    val moveToTrash: String,
    val createReminder: String,
    val attachToDate: String,
    val clearDate: String,
    val noteTitleHint: String,
    val noteBodyHint: String,
    val addChecklistItem: String,
    val addBullet: String,
    val autoSaved: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val confirmDeleteTitle: String,
    val confirmDeleteMessage: String,
    val monthView: String,
    val weekView: String,
    val agendaView: String,
    val today: String,
    val eventsOnDay: String,
    val notesOnDay: String,
    val futureEvents: String,
    val completedEvents: String,
    val exportToPhoneCalendar: String,
    val allDay: String,
    val locationHint: String,
    val eventTitleHint: String,
    val descriptionHint: String,
    val attachNoteOptional: String,
    val noNoteAttached: String,
    val reminderAlertsBefore: String,
    val remindersUpcoming: String,
    val remindersToday: String,
    val remindersOverdue: String,
    val remindersCompleted: String,
    val snooze: String,
    val reschedule: String,
    val markDone: String,
    val reopen: String,
    val minutesShort: String,
    val customEveryDays: String,
    val notificationPermissionTitle: String,
    val notificationPermissionBody: String,
    val grantPermission: String,
    val exactAlarmTitle: String,
    val exactAlarmBody: String,
    val openExactAlarmSettings: String,
    val settingsAppearance: String,
    val themeMode: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val settingsTimeLocale: String,
    val timeFormat24h: String,
    val languageLabel: String,
    val useDeviceTimezone: String,
    val timezoneLabel: String,
    val settingsNotifications: String,
    val defaultLeadTime: String,
    val defaultSnoozeDuration: String,
    val soundLabel: String,
    val vibrationLabel: String,
    val sendTestNotification: String,
    val settingsBackup: String,
    val exportBackupJson: String,
    val restoreBackupJson: String,
    val backupPrivacyNote: String,
    val aboutTitle: String,
    val aboutTagline: String,
    val versionLabel: String,
    val offlineBadge: String
) {
    fun noteCategoryLabel(cat: NoteCategory, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (cat) {
            NoteCategory.PERSONAL -> "Personal"
            NoteCategory.OFFICE -> "Office"
            NoteCategory.IDEAS -> "Ideas"
            NoteCategory.IMPORTANT -> "Important"
            NoteCategory.OTHERS -> "Others"
        }
        AppLanguage.BANGLA -> when (cat) {
            NoteCategory.PERSONAL -> "ব্যক্তিগত"
            NoteCategory.OFFICE -> "অফিস"
            NoteCategory.IDEAS -> "ধারণা"
            NoteCategory.IMPORTANT -> "গুরুত্বপূর্ণ"
            NoteCategory.OTHERS -> "অন্যান্য"
        }
    }

    fun eventCategoryLabel(cat: EventCategory, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (cat) {
            EventCategory.WORK -> "Work"
            EventCategory.PERSONAL -> "Personal"
            EventCategory.MEETING -> "Meeting"
            EventCategory.HEALTH -> "Health"
            EventCategory.DEADLINE -> "Deadline"
            EventCategory.OTHER -> "Other"
        }
        AppLanguage.BANGLA -> when (cat) {
            EventCategory.WORK -> "কাজ"
            EventCategory.PERSONAL -> "ব্যক্তিগত"
            EventCategory.MEETING -> "মিটিং"
            EventCategory.HEALTH -> "স্বাস্থ্য"
            EventCategory.DEADLINE -> "শেষ সময়"
            EventCategory.OTHER -> "অন্যান্য"
        }
    }

    fun recurrenceLabel(rule: RecurrenceRule, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (rule) {
            RecurrenceRule.NONE -> "Does not repeat"
            RecurrenceRule.DAILY -> "Daily"
            RecurrenceRule.WEEKLY -> "Weekly"
            RecurrenceRule.MONTHLY -> "Monthly"
            RecurrenceRule.YEARLY -> "Yearly"
        }
        AppLanguage.BANGLA -> when (rule) {
            RecurrenceRule.NONE -> "একবার"
            RecurrenceRule.DAILY -> "প্রতিদিন"
            RecurrenceRule.WEEKLY -> "প্রতি সপ্তাহে"
            RecurrenceRule.MONTHLY -> "প্রতি মাসে"
            RecurrenceRule.YEARLY -> "প্রতি বছরে"
        }
    }

    fun repeatTypeLabel(type: RepeatType, customDays: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> when (type) {
            RepeatType.NONE -> "One-time"
            RepeatType.DAILY -> "Daily"
            RepeatType.WEEKLY -> "Weekly"
            RepeatType.MONTHLY -> "Monthly"
            RepeatType.CUSTOM_DAYS -> "Every $customDays day${if (customDays > 1) "s" else ""}"
        }
        AppLanguage.BANGLA -> when (type) {
            RepeatType.NONE -> "একবার"
            RepeatType.DAILY -> "প্রতিদিন"
            RepeatType.WEEKLY -> "প্রতি সপ্তাহে"
            RepeatType.MONTHLY -> "প্রতি মাসে"
            RepeatType.CUSTOM_DAYS -> "প্রতি $customDays দিন পর"
        }
    }
}

private val EnglishStrings = Strings(
    appName = "DZ Daily Planner",
    poweredBy = "Powered by DZ Ai-STUDIO",
    navHome = "Home",
    navNotes = "Notes",
    navCalendar = "Calendar",
    navReminders = "Reminders",
    navSettings = "Settings",
    goodMorning = "Good Morning",
    goodAfternoon = "Good Afternoon",
    goodEvening = "Good Evening",
    todayProductivity = "Daily Productivity",
    notesCountLabel = "Notes",
    eventsTodayLabel = "Events Today",
    remindersDoneLabel = "Done",
    overdueAlertTitle = "Overdue Reminders",
    overdueAlertSubtitle = "Tap to review, snooze, or complete pending alerts.",
    todaysEvents = "Today's Schedule",
    todaysNotes = "Today's & Pinned Notes",
    upcomingReminders = "Upcoming Reminders",
    quickCreate = "Quick Add",
    newNote = "New Note",
    newEvent = "New Event",
    newReminder = "New Reminder",
    viewAll = "View All",
    noEventsToday = "No events scheduled for today. Tap + to plan your day.",
    noNotesYet = "No notes for today yet. Capture a quick thought or checklist.",
    noRemindersUpcoming = "All caught up! No upcoming reminders right now.",
    searchNotesHint = "Search notes by title or content...",
    allNotes = "Active",
    favorites = "Favorites",
    archived = "Archive",
    recycleBin = "Trash",
    sortNewest = "Newest First",
    sortOldest = "Oldest First",
    sortAlpha = "A – Z",
    emptyTrash = "Empty Trash",
    restore = "Restore",
    deleteForever = "Delete Forever",
    pin = "Pin",
    unpin = "Unpin",
    duplicate = "Duplicate",
    archive = "Archive",
    unarchive = "Unarchive",
    moveToTrash = "Move to Trash",
    createReminder = "Set Reminder",
    attachToDate = "Attach Date",
    clearDate = "Clear Date",
    noteTitleHint = "Note title",
    noteBodyHint = "Write your daily note, ideas, or bullet points...",
    addChecklistItem = "Add checklist item",
    addBullet = "• Bullet",
    autoSaved = "Auto-saved locally",
    save = "Save",
    cancel = "Cancel",
    delete = "Delete",
    confirmDeleteTitle = "Confirm Deletion",
    confirmDeleteMessage = "This action cannot be undone. Are you sure you want to continue?",
    monthView = "Month",
    weekView = "Week",
    agendaView = "Agenda",
    today = "Today",
    eventsOnDay = "Events",
    notesOnDay = "Attached Notes",
    futureEvents = "Upcoming Events",
    completedEvents = "Completed Events",
    exportToPhoneCalendar = "Export to Phone Calendar",
    allDay = "All-day event",
    locationHint = "Location (optional)",
    eventTitleHint = "Event title",
    descriptionHint = "Description (optional)",
    attachNoteOptional = "Attach Note",
    noNoteAttached = "No note attached",
    reminderAlertsBefore = "Reminder Alerts",
    remindersUpcoming = "Upcoming",
    remindersToday = "Today",
    remindersOverdue = "Overdue",
    remindersCompleted = "Completed",
    snooze = "Snooze",
    reschedule = "Reschedule",
    markDone = "Mark Done",
    reopen = "Reopen",
    minutesShort = "min",
    customEveryDays = "Repeat interval (days)",
    notificationPermissionTitle = "Enable Notifications",
    notificationPermissionBody = "Allow Android notifications so DZ Daily Planner can alert you on time even when the app is closed.",
    grantPermission = "Allow Notifications",
    exactAlarmTitle = "Exact Alarm Timing Unavailable",
    exactAlarmBody = "Your device currently restricts exact alarms. Reminders still fire using battery-friendly scheduling, or you can enable Alarms & Reminders for exact timing.",
    openExactAlarmSettings = "Enable Exact Timing",
    settingsAppearance = "Appearance",
    themeMode = "App Theme",
    themeSystem = "System",
    themeLight = "Light",
    themeDark = "Dark",
    settingsTimeLocale = "Time, Language & Timezone",
    timeFormat24h = "24-Hour Clock",
    languageLabel = "Language",
    useDeviceTimezone = "Use Device Timezone",
    timezoneLabel = "Active Timezone",
    settingsNotifications = "Notifications & Reminders",
    defaultLeadTime = "Default Event Reminder",
    defaultSnoozeDuration = "Default Snooze",
    soundLabel = "Notification Sound",
    vibrationLabel = "Vibration",
    sendTestNotification = "Send Test Notification",
    settingsBackup = "Backup & Privacy",
    exportBackupJson = "Export JSON Backup",
    restoreBackupJson = "Restore from JSON Backup",
    backupPrivacyNote = "100% Offline & Private: All notes, calendar events, and reminders remain strictly on your device in local Room & DataStore storage. No cloud sync, no ads, and no tracking.",
    aboutTitle = "About DZ Ai-STUDIO",
    aboutTagline = "Designed for better daily planning and productivity. Powered by DZ Ai-STUDIO.",
    versionLabel = "Version",
    offlineBadge = "Offline First"
)

private val BanglaStrings = Strings(
    appName = "DZ ডেইলি প্ল্যানার",
    poweredBy = "DZ Ai-STUDIO দ্বারা চালিত",
    navHome = "হোম",
    navNotes = "নোটস",
    navCalendar = "ক্যালেন্ডার",
    navReminders = "রিমাইন্ডার",
    navSettings = "সেটিংস",
    goodMorning = "শুভ সকাল",
    goodAfternoon = "শুভ অপরাহ্ণ",
    goodEvening = "শুভ সন্ধ্যা",
    todayProductivity = "দৈনিক অগ্রগতি",
    notesCountLabel = "নোটস",
    eventsTodayLabel = "আজকের ইভেন্ট",
    remindersDoneLabel = "সম্পন্ন",
    overdueAlertTitle = "বকেয়া রিমাইন্ডার",
    overdueAlertSubtitle = "দেখতে, স্নুজ করতে বা সম্পন্ন করতে ট্যাপ করুন।",
    todaysEvents = "আজকের সময়সূচী",
    todaysNotes = "আজকের ও পিন করা নোট",
    upcomingReminders = "আসন্ন রিমাইন্ডার",
    quickCreate = "দ্রুত যোগ করুন",
    newNote = "নতুন নোট",
    newEvent = "নতুন ইভেন্ট",
    newReminder = "নতুন রিমাইন্ডার",
    viewAll = "সব দেখুন",
    noEventsToday = "আজ কোনো ইভেন্ট নেই। পরিকল্পনা যোগ করতে + চাপুন।",
    noNotesYet = "আজ এখনো কোনো নোট নেই। আপনার ভাবনা লিখে রাখুন।",
    noRemindersUpcoming = "সব সম্পন্ন! আপাতত কোনো আসন্ন রিমাইন্ডার নেই।",
    searchNotesHint = "শিরোনাম বা লেখা দিয়ে নোট খুঁজুন...",
    allNotes = "সক্রিয়",
    favorites = "প্রিয়",
    archived = "আর্কাইভ",
    recycleBin = "ট্র্যাশ",
    sortNewest = "নতুন আগে",
    sortOldest = "পুরোনো আগে",
    sortAlpha = "বর্ণানুক্রমিক",
    emptyTrash = "ট্র্যাশ খালি করুন",
    restore = "পুনরুদ্ধার",
    deleteForever = "স্থায়ীভাবে মুছুন",
    pin = "পিন করুন",
    unpin = "আনপিন",
    duplicate = "কপি করুন",
    archive = "আর্কাইভ",
    unarchive = "আনআর্কাইভ",
    moveToTrash = "ট্র্যাশে পাঠান",
    createReminder = "রিমাইন্ডার সেট",
    attachToDate = "তারিখ যুক্ত করুন",
    clearDate = "তারিখ সরান",
    noteTitleHint = "নোটের শিরোনাম",
    noteBodyHint = "আপনার নোট, পরিকল্পনা বা পয়েন্ট লিখুন...",
    addChecklistItem = "চেকলিস্ট আইটেম যোগ করুন",
    addBullet = "• বুলেট",
    autoSaved = "স্বয়ংক্রিয়ভাবে সংরক্ষিত",
    save = "সংরক্ষণ",
    cancel = "বাতিল",
    delete = "মুছুন",
    confirmDeleteTitle = "মুছে ফেলা নিশ্চিত করুন",
    confirmDeleteMessage = "এটি আর ফিরিয়ে আনা যাবে না। আপনি কি নিশ্চিত?",
    monthView = "মাস",
    weekView = "সপ্তাহ",
    agendaView = "এজেন্ডা",
    today = "আজ",
    eventsOnDay = "ইভেন্টসমূহ",
    notesOnDay = "সংযুক্ত নোট",
    futureEvents = "আসন্ন ইভেন্ট",
    completedEvents = "সম্পন্ন ইভেন্ট",
    exportToPhoneCalendar = "ফোনের ক্যালেন্ডারে যুক্ত করুন",
    allDay = "সারাদিনের ইভেন্ট",
    locationHint = "স্থান (ঐচ্ছিক)",
    eventTitleHint = "ইভেন্টের শিরোনাম",
    descriptionHint = "বিবরণ (ঐচ্ছিক)",
    attachNoteOptional = "নোট সংযুক্ত করুন",
    noNoteAttached = "কোনো নোট সংযুক্ত নেই",
    reminderAlertsBefore = "রিমাইন্ডার সময়",
    remindersUpcoming = "আসন্ন",
    remindersToday = "আজকের",
    remindersOverdue = "বকেয়া",
    remindersCompleted = "সম্পন্ন",
    snooze = "স্নুজ",
    reschedule = "পুনঃনির্ধারণ",
    markDone = "সম্পন্ন",
    reopen = "পুনরায় চালু",
    minutesShort = "মি.",
    customEveryDays = "পুনরাবৃত্তি (দিন)",
    notificationPermissionTitle = "নোটিফিকেশন চালু করুন",
    notificationPermissionBody = "অ্যাপ বন্ধ থাকলেও সময়মতো রিমাইন্ডার পেতে নোটিফিকেশন অনুমতি দিন।",
    grantPermission = "অনুমতি দিন",
    exactAlarmTitle = "সঠিক অ্যালার্ম অনুমতি সীমিত",
    exactAlarmBody = "ব্যাটারি-বান্ধব রিমাইন্ডার সক্রিয় আছে। একদম সঠিক সময়ে অ্যালার্মের জন্য সিস্টেম সেটিংস থেকে অনুমতি দিন।",
    openExactAlarmSettings = "সেটিংস খুলুন",
    settingsAppearance = "থিম ও ডিজাইন",
    themeMode = "অ্যাপ থিম",
    themeSystem = "সিস্টেম",
    themeLight = "লাইট",
    themeDark = "ডার্ক",
    settingsTimeLocale = "সময়, ভাষা ও টাইমজোন",
    timeFormat24h = "২৪-ঘণ্টার ঘড়ি",
    languageLabel = "ভাষা",
    useDeviceTimezone = "ডিভাইসের টাইমজোন ব্যবহার করুন",
    timezoneLabel = "সক্রিয় টাইমজোন",
    settingsNotifications = "নোটিফিকেশন ও রিমাইন্ডার",
    defaultLeadTime = "ইভেন্টের পূর্বে রিমাইন্ডার",
    defaultSnoozeDuration = "ডিফল্ট স্নুজ সময়",
    soundLabel = "নোটিফিকেশন শব্দ",
    vibrationLabel = "ভাইব্রেশন",
    sendTestNotification = "টেস্ট নোটিফিকেশন পাঠান",
    settingsBackup = "ব্যাকআপ ও গোপনীয়তা",
    exportBackupJson = "JSON ব্যাকআপ এক্সপোর্ট",
    restoreBackupJson = "JSON ব্যাকআপ রিস্টোর",
    backupPrivacyNote = "১০০% অফলাইন ও নিরাপদ: আপনার সব নোট, ইভেন্ট ও রিমাইন্ডার কেবল আপনার ডিভাইসেই সংরক্ষিত থাকে।",
    aboutTitle = "DZ Ai-STUDIO সম্পর্কে",
    aboutTagline = "Designed for better daily planning and productivity. Powered by DZ Ai-STUDIO.",
    versionLabel = "ভার্সন",
    offlineBadge = "সম্পূর্ণ অফলাইন"
)

fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.BANGLA -> BanglaStrings
}
