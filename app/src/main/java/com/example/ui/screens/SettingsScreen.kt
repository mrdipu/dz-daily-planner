package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.ThemeMode
import com.example.ui.PlannerUiState
import com.example.ui.strings.Strings
import com.example.ui.theme.DzRoyalBlue
import com.example.ui.theme.DzWhite
import com.example.util.DateTimeUtils

@Composable
fun SettingsScreen(
    uiState: PlannerUiState,
    strings: Strings,
    hasNotificationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSendTestNotification: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onUse24HourChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onUseDeviceTimezoneChange: (Boolean) -> Unit,
    onCustomTimezoneChange: (String) -> Unit,
    onDefaultLeadChange: (Int) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    var showTimezoneDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Appearance Section
        item {
            SettingsSectionCard(title = strings.settingsAppearance) {
                Text(strings.themeMode, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to strings.themeSystem,
                        ThemeMode.LIGHT to strings.themeLight,
                        ThemeMode.DARK to strings.themeDark
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Time, Language & Timezone
        item {
            SettingsSectionCard(title = strings.settingsTimeLocale) {
                SettingsSwitchRow(
                    title = strings.timeFormat24h,
                    checked = prefs.use24HourFormat,
                    onCheckedChange = onUse24HourChange,
                    testTag = "settings_24h_switch"
                )

                Spacer(Modifier.height(10.dp))
                Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = prefs.language == AppLanguage.ENGLISH,
                        onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                        label = { Text("English") },
                        modifier = Modifier.testTag("settings_lang_en")
                    )
                    FilterChip(
                        selected = prefs.language == AppLanguage.BANGLA,
                        onClick = { onLanguageChange(AppLanguage.BANGLA) },
                        label = { Text("বাংলা") },
                        modifier = Modifier.testTag("settings_lang_bn")
                    )
                }

                Spacer(Modifier.height(10.dp))
                SettingsSwitchRow(
                    title = strings.useDeviceTimezone,
                    checked = prefs.useDeviceTimezone,
                    onCheckedChange = onUseDeviceTimezoneChange,
                    testTag = "settings_device_tz_switch"
                )

                if (!prefs.useDeviceTimezone) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimezoneDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.timezoneLabel, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = prefs.customTimezoneId,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Notifications & Reminders
        item {
            SettingsSectionCard(title = strings.settingsNotifications) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notification Permission", style = MaterialTheme.typography.bodyMedium)
                    if (hasNotificationPermission) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Granted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        OutlinedButton(onClick = onRequestNotificationPermission) {
                            Text(strings.grantPermission)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exact Alarm Timing", style = MaterialTheme.typography.bodyMedium)
                    if (canScheduleExactAlarms) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Enabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        OutlinedButton(onClick = onOpenExactAlarmSettings) {
                            Text(strings.openExactAlarmSettings)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(strings.defaultLeadTime, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = prefs.defaultReminderLeadMinutes == mins,
                            onClick = { onDefaultLeadChange(mins) },
                            label = { Text(if (mins == 60) "1h" else "${mins}m") }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(strings.defaultSnoozeDuration, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = prefs.defaultSnoozeMinutes == mins,
                            onClick = { onDefaultSnoozeChange(mins) },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                SettingsSwitchRow(
                    title = strings.soundLabel,
                    checked = prefs.soundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                    testTag = "settings_sound_switch"
                )
                SettingsSwitchRow(
                    title = strings.vibrationLabel,
                    checked = prefs.vibrationEnabled,
                    onCheckedChange = onVibrationEnabledChange,
                    testTag = "settings_vibration_switch"
                )

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSendTestNotification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_test_notification_button")
                ) {
                    Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.sendTestNotification)
                }
            }
        }

        // Backup & Privacy
        item {
            SettingsSectionCard(title = strings.settingsBackup) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportBackup,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_export_backup_button")
                    ) {
                        Icon(Icons.Filled.Backup, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportBackupJson)
                    }
                    OutlinedButton(
                        onClick = onRestoreBackup,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_restore_backup_button")
                    ) {
                        Icon(Icons.Filled.Restore, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.restoreBackupJson)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = DzRoyalBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.backupPrivacyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // About DZ Ai-STUDIO
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DzWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dz_studio_logo),
                        contentDescription = strings.aboutTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(128.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = androidx.compose.ui.graphics.Color(0xFF171717)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.aboutTagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color(0xFF525E70),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = DzRoyalBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${strings.versionLabel} ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DzRoyalBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = strings.offlineBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTimezoneDialog) {
        AlertDialog(
            onDismissRequest = { showTimezoneDialog = false },
            title = { Text(strings.timezoneLabel) },
            text = {
                Column {
                    DateTimeUtils.popularTimeZones.forEach { tzId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCustomTimezoneChange(tzId)
                                    showTimezoneDialog = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tzId, style = MaterialTheme.typography.bodyMedium)
                            if (tzId == prefs.customTimezoneId) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimezoneDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
