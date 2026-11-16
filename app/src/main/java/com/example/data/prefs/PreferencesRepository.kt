package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    ENGLISH,
    BANGLA
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val use24HourFormat: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val useDeviceTimezone: Boolean = false,
    val customTimezoneId: String = DateTimeUtils.DEFAULT_TIMEZONE_ID,
    val defaultReminderLeadMinutes: Int = 15,
    val defaultSnoozeMinutes: Int = 10,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dz_planner_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val LANGUAGE = stringPreferencesKey("language")
        val USE_DEVICE_TIMEZONE = booleanPreferencesKey("use_device_timezone")
        val CUSTOM_TIMEZONE_ID = stringPreferencesKey("custom_timezone_id")
        val DEFAULT_LEAD_MINUTES = intPreferencesKey("default_lead_minutes")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            use24HourFormat = prefs[Keys.USE_24_HOUR] ?: false,
            language = runCatching {
                AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.name)
            }.getOrDefault(AppLanguage.ENGLISH),
            useDeviceTimezone = prefs[Keys.USE_DEVICE_TIMEZONE] ?: false,
            customTimezoneId = prefs[Keys.CUSTOM_TIMEZONE_ID] ?: DateTimeUtils.DEFAULT_TIMEZONE_ID,
            defaultReminderLeadMinutes = prefs[Keys.DEFAULT_LEAD_MINUTES] ?: 15,
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MINUTES] ?: 10,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true
        )
    }

    suspend fun getCurrentPreferences(): UserPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUse24HourFormat(use24Hour: Boolean) {
        context.dataStore.edit { it[Keys.USE_24_HOUR] = use24Hour }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setUseDeviceTimezone(useDevice: Boolean) {
        context.dataStore.edit { it[Keys.USE_DEVICE_TIMEZONE] = useDevice }
    }

    suspend fun setCustomTimezoneId(tzId: String) {
        context.dataStore.edit { it[Keys.CUSTOM_TIMEZONE_ID] = tzId }
    }

    suspend fun setDefaultReminderLeadMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_LEAD_MINUTES] = minutes }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SNOOZE_MINUTES] = minutes }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun restorePreferences(prefs: UserPreferences) {
        context.dataStore.edit {
            it[Keys.THEME_MODE] = prefs.themeMode.name
            it[Keys.USE_24_HOUR] = prefs.use24HourFormat
            it[Keys.LANGUAGE] = prefs.language.name
            it[Keys.USE_DEVICE_TIMEZONE] = prefs.useDeviceTimezone
            it[Keys.CUSTOM_TIMEZONE_ID] = prefs.customTimezoneId
            it[Keys.DEFAULT_LEAD_MINUTES] = prefs.defaultReminderLeadMinutes
            it[Keys.DEFAULT_SNOOZE_MINUTES] = prefs.defaultSnoozeMinutes
            it[Keys.SOUND_ENABLED] = prefs.soundEnabled
            it[Keys.VIBRATION_ENABLED] = prefs.vibrationEnabled
        }
    }
}
