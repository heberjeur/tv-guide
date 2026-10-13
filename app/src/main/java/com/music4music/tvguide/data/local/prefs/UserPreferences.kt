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
    private val keyCalendarSyncEnabled = booleanPreferencesKey("calendar_sync_enabled")

    val selectedPackage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyPackage] ?: "community_enriched"
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

    val isCalendarSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyCalendarSyncEnabled] ?: false
    }

    suspend fun setSelectedPackage(packageId: String) {
        context.dataStore.edit { preferences ->
            preferences[keyPackage] = packageId
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

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyCalendarSyncEnabled] = enabled
        }
    }
}
