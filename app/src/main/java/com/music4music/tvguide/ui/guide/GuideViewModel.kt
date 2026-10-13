package com.music4music.tvguide.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.usecase.GetChannelsUseCase
import com.music4music.tvguide.domain.usecase.GetProgramsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GuideViewModel @Inject constructor(
    getChannelsUseCase: GetChannelsUseCase,
    private val getProgramsUseCase: GetProgramsUseCase,
    userPreferences: UserPreferences
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    val channels: StateFlow<List<Channel>> = userPreferences.selectedPackage
        .flatMapLatest { packageId ->
            getChannelsUseCase(ChannelPackage.fromId(packageId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programs: StateFlow<List<Program>> = _selectedChannelId
        .flatMapLatest { channelId ->
            if (channelId == null) {
                flowOf(emptyList())
            } else {
                val date = _selectedDate.value
                val zone = ZoneId.systemDefault()
                val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                getProgramsUseCase(channelId, startOfDay, endOfDay)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectChannel(channelId: String) {
        _selectedChannelId.value = channelId
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }
}
