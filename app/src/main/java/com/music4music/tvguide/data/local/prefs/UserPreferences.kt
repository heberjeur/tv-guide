package com.music4music.tvguide.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tvguide_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyNotificationsEnabled = booleanPreferencesKey("notifications_enabled")
    private val keyNotificationSound = stringPreferencesKey("notification_sound")
    private val keyReminderLeadTime = intPreferencesKey("reminder_lead_time")
    private val keyReminderRecurrence = stringPreferencesKey("reminder_recurrence")
    private val keyChannelGridMode = booleanPreferencesKey("channel_grid_mode")
    private val keyAppLanguage = stringPreferencesKey("app_language")
    private val keySyncFrequencyHours = intPreferencesKey("sync_frequency_hours")
    private val keySyncWifiOnly = booleanPreferencesKey("sync_wifi_only")
    private val keySyncChargingOnly = booleanPreferencesKey("sync_charging_only")
    private val keyAutoSyncEnabled = booleanPreferencesKey("auto_sync_enabled")
    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyDisplayDensity = stringPreferencesKey("display_density")
    private val keyFontSizeScale = stringPreferencesKey("font_size_scale")
    private val keyHideReruns = booleanPreferencesKey("hide_reruns")
    private val keyChannelSortOrder = stringPreferencesKey("channel_sort_order")
    private val keyFavoriteChannels = androidx.datastore.preferences.core.stringSetPreferencesKey("favorite_channels")
    private val keyHiddenChannels = androidx.datastore.preferences.core.stringSetPreferencesKey("hidden_channels")
    private val keyRetentionDays = intPreferencesKey("retention_days")
    private val keyVibrationEnabled = booleanPreferencesKey("vibration_enabled")
    private val keyHighlightAccessibility = booleanPreferencesKey("highlight_accessibility")
    private val keyCustomNotificationDateTime = androidx.datastore.preferences.core.longPreferencesKey("custom_notification_datetime")
    private val keyDisplayTimezone = stringPreferencesKey("display_timezone")
    private val keyAccentColor = stringPreferencesKey("accent_color")

    val accentColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyAccentColor] ?: "#E50914"
    }

    val displayTimezone: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyDisplayTimezone] ?: "system"
    }

    val customNotificationDateTime: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[keyCustomNotificationDateTime]
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyAppLanguage] ?: "fr"
    }

    val syncFrequencyHours: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keySyncFrequencyHours] ?: 6
    }

    val syncWifiOnly: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keySyncWifiOnly] ?: true
    }

    val syncChargingOnly: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keySyncChargingOnly] ?: false
    }

    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyAutoSyncEnabled] ?: true
    }

    val isChannelGridMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyChannelGridMode] ?: true
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyNotificationsEnabled] ?: true
    }

    val notificationSound: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[keyNotificationSound]
    }

    val reminderLeadTimeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyReminderLeadTime] ?: 0
    }

    val reminderRecurrence: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyReminderRecurrence] ?: "once"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyThemeMode] ?: "dark"
    }

    val displayDensity: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyDisplayDensity] ?: "comfort"
    }

    val fontSizeScale: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyFontSizeScale] ?: "normal"
    }

    val hideReruns: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyHideReruns] ?: false
    }

    val channelSortOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyChannelSortOrder] ?: "number"
    }

    val favoriteChannels: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[keyFavoriteChannels] ?: emptySet()
    }

    val hiddenChannels: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[keyHiddenChannels] ?: emptySet()
    }

    val retentionDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyRetentionDays] ?: 7
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyVibrationEnabled] ?: true
    }

    val highlightAccessibility: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyHighlightAccessibility] ?: true
    }



    suspend fun setChannelGridMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyChannelGridMode] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyNotificationsEnabled] = enabled
        }
    }

    suspend fun setNotificationSound(uriString: String?) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[keyNotificationSound] = uriString
            } else {
                preferences.remove(keyNotificationSound)
            }
        }
    }

    suspend fun setReminderLeadTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyReminderLeadTime] = minutes
        }
    }

    suspend fun setReminderRecurrence(recurrence: String) {
        context.dataStore.edit { preferences ->
            preferences[keyReminderRecurrence] = recurrence
        }
    }

    suspend fun setCustomNotificationDateTime(timestamp: Long?) {
        context.dataStore.edit { preferences ->
            if (timestamp != null) {
                preferences[keyCustomNotificationDateTime] = timestamp
            } else {
                preferences.remove(keyCustomNotificationDateTime)
            }
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[keyAppLanguage] = language
        }
    }

    suspend fun setSyncFrequency(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[keySyncFrequencyHours] = hours
        }
    }

    suspend fun setSyncWifiOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keySyncWifiOnly] = enabled
        }
    }

    suspend fun setSyncChargingOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keySyncChargingOnly] = enabled
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyAutoSyncEnabled] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[keyThemeMode] = mode
        }
    }

    suspend fun setDisplayDensity(density: String) {
        context.dataStore.edit { preferences ->
            preferences[keyDisplayDensity] = density
        }
    }

    suspend fun setFontSizeScale(scale: String) {
        context.dataStore.edit { preferences ->
            preferences[keyFontSizeScale] = scale
        }
    }

    suspend fun setHideReruns(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyHideReruns] = enabled
        }
    }

    suspend fun setChannelSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[keyChannelSortOrder] = order
        }
    }

    suspend fun toggleFavoriteChannel(channelId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[keyFavoriteChannels]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(channelId)) {
                current.remove(channelId)
            } else {
                current.add(channelId)
            }
            preferences[keyFavoriteChannels] = current
        }
    }

    suspend fun toggleHiddenChannel(channelId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[keyHiddenChannels]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(channelId)) {
                current.remove(channelId)
            } else {
                current.add(channelId)
            }
            preferences[keyHiddenChannels] = current
        }
    }

    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyRetentionDays] = days
        }
    }


    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyVibrationEnabled] = enabled
        }
    }

    suspend fun setHighlightAccessibility(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyHighlightAccessibility] = enabled
        }
    }

    suspend fun setDisplayTimezone(timezone: String) {
        context.dataStore.edit { preferences ->
            preferences[keyDisplayTimezone] = timezone
        }
    }

    suspend fun setAccentColor(colorHex: String) {
        context.dataStore.edit { preferences ->
            preferences[keyAccentColor] = colorHex
        }
    }
}
