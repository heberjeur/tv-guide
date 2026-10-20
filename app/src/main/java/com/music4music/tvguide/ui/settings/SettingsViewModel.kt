package com.music4music.tvguide.ui.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.imageLoader
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.usecase.SyncEpgUseCase
import com.music4music.tvguide.worker.EpgSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.repository.EpgRepository

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val syncEpgUseCase: SyncEpgUseCase,
    private val epgRepository: EpgRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {


    val appLanguage: StateFlow<String> = userPreferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "fr")

    val accentColor: StateFlow<String> = userPreferences.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#E50914")

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferences.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationSound: StateFlow<String?> = userPreferences.notificationSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reminderLeadTime: StateFlow<Int> = userPreferences.reminderLeadTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reminderRecurrence: StateFlow<String> = userPreferences.reminderRecurrence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "once")

    val customNotificationDateTime: StateFlow<Long?> = userPreferences.customNotificationDateTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syncFrequencyHours: StateFlow<Int> = userPreferences.syncFrequencyHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val syncWifiOnly: StateFlow<Boolean> = userPreferences.syncWifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val syncChargingOnly: StateFlow<Boolean> = userPreferences.syncChargingOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSyncEnabled: StateFlow<Boolean> = userPreferences.autoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeMode: StateFlow<String> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    val displayDensity: StateFlow<String> = userPreferences.displayDensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "comfort")

    val displayTimezone: StateFlow<String> = userPreferences.displayTimezone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val fontSizeScale: StateFlow<String> = userPreferences.fontSizeScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "normal")

    val hideReruns: StateFlow<Boolean> = userPreferences.hideReruns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val channelSortOrder: StateFlow<String> = userPreferences.channelSortOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "number")

    val favoriteChannels: StateFlow<Set<String>> = userPreferences.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val hiddenChannels: StateFlow<Set<String>> = userPreferences.hiddenChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val retentionDays: StateFlow<Int> = userPreferences.retentionDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)


    val vibrationEnabled: StateFlow<Boolean> = userPreferences.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val highlightAccessibility: StateFlow<Boolean> = userPreferences.highlightAccessibility
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allChannels: StateFlow<List<Channel>> = epgRepository.getChannels(ChannelPackage.COMMUNITY_ENRICHED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _cacheSizeText = MutableStateFlow("")
    val cacheSizeText: StateFlow<String> = _cacheSizeText.asStateFlow()

    init {
        updateCacheSize()
    }



    fun setLanguage(langCode: String, activity: Activity?) {
        viewModelScope.launch {
            userPreferences.setAppLanguage(langCode)
            val locale = Locale.forLanguageTag(langCode)
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList.forLanguageTags(langCode)
            } else {
                val config = context.resources.configuration
                config.setLocale(locale)
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
            }
            activity?.recreate()
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
        }
    }

    fun setNotificationSound(uriString: String?) {
        viewModelScope.launch {
            userPreferences.setNotificationSound(uriString)
        }
    }

    fun setLeadTime(minutes: Int) {
        viewModelScope.launch {
            userPreferences.setReminderLeadTime(minutes)
        }
    }

    fun setReminderRecurrence(recurrence: String) {
        viewModelScope.launch {
            userPreferences.setReminderRecurrence(recurrence)
        }
    }

    fun setCustomNotificationDateTime(timestamp: Long?) {
        viewModelScope.launch {
            userPreferences.setCustomNotificationDateTime(timestamp)
        }
    }

    fun setAccentColor(colorHex: String) {
        viewModelScope.launch {
            userPreferences.setAccentColor(colorHex)
        }
    }

    fun setSyncFrequency(hours: Int) {
        viewModelScope.launch {
            userPreferences.setSyncFrequency(hours)
            rescheduleSyncWorker(
                frequencyHours = hours,
                wifiOnly = syncWifiOnly.value,
                chargingOnly = syncChargingOnly.value,
                autoEnabled = autoSyncEnabled.value
            )
        }
    }

    fun setSyncWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSyncWifiOnly(enabled)
            rescheduleSyncWorker(
                frequencyHours = syncFrequencyHours.value,
                wifiOnly = enabled,
                chargingOnly = syncChargingOnly.value,
                autoEnabled = autoSyncEnabled.value
            )
        }
    }

    fun setSyncChargingOnly(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSyncChargingOnly(enabled)
            rescheduleSyncWorker(
                frequencyHours = syncFrequencyHours.value,
                wifiOnly = syncWifiOnly.value,
                chargingOnly = enabled,
                autoEnabled = autoSyncEnabled.value
            )
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoSyncEnabled(enabled)
            rescheduleSyncWorker(
                frequencyHours = syncFrequencyHours.value,
                wifiOnly = syncWifiOnly.value,
                chargingOnly = syncChargingOnly.value,
                autoEnabled = enabled
            )
        }
    }

    fun triggerSyncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                syncEpgUseCase(ChannelPackage.COMMUNITY_ENRICHED, forceRefresh = true)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun rescheduleSyncWorker(
        frequencyHours: Int,
        wifiOnly: Boolean,
        chargingOnly: Boolean,
        autoEnabled: Boolean
    ) {
        val workManager = WorkManager.getInstance(context)
        if (!autoEnabled || frequencyHours <= 0) {
            workManager.cancelUniqueWork("epg_sync_periodic")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(chargingOnly)
            .build()

        val request = PeriodicWorkRequestBuilder<EpgSyncWorker>(
            frequencyHours.toLong(),
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "epg_sync_periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun setDisplayDensity(density: String) {
        viewModelScope.launch {
            userPreferences.setDisplayDensity(density)
        }
    }

    fun setDisplayTimezone(timezone: String) {
        viewModelScope.launch {
            userPreferences.setDisplayTimezone(timezone)
        }
    }

    fun setFontSizeScale(scale: String) {
        viewModelScope.launch {
            userPreferences.setFontSizeScale(scale)
        }
    }

    fun setHideReruns(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setHideReruns(enabled)
        }
    }

    fun setChannelSortOrder(order: String) {
        viewModelScope.launch {
            userPreferences.setChannelSortOrder(order)
        }
    }

    fun toggleFavoriteChannel(channelId: String) {
        viewModelScope.launch {
            userPreferences.toggleFavoriteChannel(channelId)
            val currentFavorites = userPreferences.favoriteChannels.first()
            val isNowFavorite = currentFavorites.contains(channelId)
            epgRepository.toggleFavoriteChannel(channelId, isNowFavorite)
        }
    }

    fun toggleHiddenChannel(channelId: String) {
        viewModelScope.launch {
            userPreferences.toggleHiddenChannel(channelId)
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            userPreferences.setRetentionDays(days)
        }
    }


    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVibrationEnabled(enabled)
        }
    }

    fun setHighlightAccessibility(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setHighlightAccessibility(enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            context.imageLoader.diskCache?.clear()
            context.imageLoader.memoryCache?.clear()
            val imageCacheDir = File(context.cacheDir, "image_cache")
            if (imageCacheDir.exists()) {
                imageCacheDir.deleteRecursively()
                imageCacheDir.mkdirs()
            }
            updateCacheSize()
        }
    }

    fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val totalBytes = if (cacheDir != null && cacheDir.exists()) {
                getDirSize(cacheDir)
            } else {
                0L
            }
            val formatted = android.text.format.Formatter.formatFileSize(context, totalBytes)
            withContext(Dispatchers.Main) {
                _cacheSizeText.value = formatted
            }
        }
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }
}
