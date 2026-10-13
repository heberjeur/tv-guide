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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
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

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferences.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderLeadTime: StateFlow<Int> = userPreferences.reminderLeadTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

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
