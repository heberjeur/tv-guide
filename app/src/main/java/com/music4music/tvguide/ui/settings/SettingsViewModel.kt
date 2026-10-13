package com.music4music.tvguide.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.usecase.SyncEpgUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val syncEpgUseCase: SyncEpgUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val selectedPackage: StateFlow<String> = userPreferences.selectedPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "community_enriched")

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferences.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderLeadTime: StateFlow<Int> = userPreferences.reminderLeadTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val isCalendarSyncEnabled: StateFlow<Boolean> = userPreferences.isCalendarSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun selectPackage(packageId: String) {
        viewModelScope.launch {
            userPreferences.setSelectedPackage(packageId)
            val channelPackage = ChannelPackage.fromId(packageId)
            syncEpgUseCase(channelPackage, forceRefresh = true)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
        }
    }

    fun setLeadTime(minutes: Int) {
        viewModelScope.launch {
            userPreferences.setReminderLeadTime(minutes)
        }
    }

    fun toggleCalendarSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setCalendarSyncEnabled(enabled)
        }
    }

    fun clearCache() {
        context.imageLoader.diskCache?.clear()
        context.imageLoader.memoryCache?.clear()
    }
}
