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

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val syncEpgUseCase: SyncEpgUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val selectedPackage: StateFlow<String> = userPreferences.selectedPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "community_enriched")

    val selectedBundle: StateFlow<String> = userPreferences.selectedBundle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "all")

    val appLanguage: StateFlow<String> = userPreferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "fr")

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferences.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationSound: StateFlow<String?> = userPreferences.notificationSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reminderLeadTime: StateFlow<Int> = userPreferences.reminderLeadTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val syncFrequencyHours: StateFlow<Int> = userPreferences.syncFrequencyHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val syncWifiOnly: StateFlow<Boolean> = userPreferences.syncWifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val syncChargingOnly: StateFlow<Boolean> = userPreferences.syncChargingOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSyncEnabled: StateFlow<Boolean> = userPreferences.autoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _cacheSizeText = MutableStateFlow("")
    val cacheSizeText: StateFlow<String> = _cacheSizeText.asStateFlow()

    init {
        updateCacheSize()
    }

    fun selectPackage(packageId: String) {
        viewModelScope.launch {
            userPreferences.setSelectedPackage(packageId)
            val channelPackage = ChannelPackage.fromId(packageId)
            syncEpgUseCase(channelPackage, forceRefresh = true)
        }
    }

    fun selectBundle(bundleId: String) {
        viewModelScope.launch {
            userPreferences.setSelectedBundle(bundleId)
        }
    }

    fun setLanguage(langCode: String, activity: Activity?) {
        viewModelScope.launch {
            userPreferences.setAppLanguage(langCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList.forLanguageTags(langCode)
            } else {
                val locale = Locale(langCode)
                Locale.setDefault(locale)
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
                val packageId = userPreferences.selectedPackage.first()
                val channelPackage = ChannelPackage.fromId(packageId)
                syncEpgUseCase(channelPackage, forceRefresh = true)
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
            var totalBytes = context.imageLoader.diskCache?.size ?: 0L
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                totalBytes += getDirSize(cacheDir)
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
