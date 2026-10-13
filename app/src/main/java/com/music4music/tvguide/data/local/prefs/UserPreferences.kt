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
    private val keyPackage = stringPreferencesKey("selected_package")
    private val keyNotificationsEnabled = booleanPreferencesKey("notifications_enabled")
    private val keyNotificationSound = stringPreferencesKey("notification_sound")
    private val keyReminderLeadTime = intPreferencesKey("reminder_lead_time")
    private val keySelectedBundle = stringPreferencesKey("selected_bundle")
    private val keyChannelGridMode = booleanPreferencesKey("channel_grid_mode")
    private val keyAppLanguage = stringPreferencesKey("app_language")
    private val keySyncFrequencyHours = intPreferencesKey("sync_frequency_hours")
    private val keySyncWifiOnly = booleanPreferencesKey("sync_wifi_only")
    private val keySyncChargingOnly = booleanPreferencesKey("sync_charging_only")
    private val keyAutoSyncEnabled = booleanPreferencesKey("auto_sync_enabled")

    val selectedPackage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyPackage] ?: "community_enriched"
    }

    val selectedBundle: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keySelectedBundle] ?: "all"
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
        preferences[keyChannelGridMode] ?: false
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyNotificationsEnabled] ?: true
    }

    val notificationSound: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[keyNotificationSound]
    }

    val reminderLeadTimeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyReminderLeadTime] ?: 10
    }

    suspend fun setSelectedPackage(packageId: String) {
        context.dataStore.edit { preferences ->
            preferences[keyPackage] = packageId
        }
    }

    suspend fun setSelectedBundle(bundleId: String) {
        context.dataStore.edit { preferences ->
            preferences[keySelectedBundle] = bundleId
        }
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
}
