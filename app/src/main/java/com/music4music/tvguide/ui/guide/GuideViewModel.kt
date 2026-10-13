package com.music4music.tvguide.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelBundle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val _selectedDate = MutableStateFlow(LocalDate.now(ZoneId.of("Europe/Paris")))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    private val _channelSearchQuery = MutableStateFlow("")
    val channelSearchQuery: StateFlow<String> = _channelSearchQuery.asStateFlow()

    private val rawChannels: StateFlow<List<Channel>> = userPreferences.selectedPackage
        .flatMapLatest { packageId ->
            getChannelsUseCase(ChannelPackage.fromId(packageId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredChannels: StateFlow<List<Channel>> = combine(
        rawChannels,
        userPreferences.selectedBundle,
        _channelSearchQuery
    ) { list, bundleId, query ->
        val bundle = ChannelBundle.fromId(bundleId)
        list.filter { channel ->
            val matchesQuery = query.isBlank() || channel.displayName.contains(query, ignoreCase = true)
            val matchesBundle = com.music4music.tvguide.data.local.ChannelCatalog.isChannelInBundle(channel.id, bundle)
            matchesQuery && matchesBundle
        }.sortedWith(
            compareBy<Channel> { if (it.channelNumber > 0) 0 else 1 }
                .thenBy { it.channelNumber }
                .thenBy { it.displayName }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programs: StateFlow<List<Program>> = combine(
        _selectedChannelId,
        _selectedDate
    ) { channelId, date ->
        channelId to date
    }.flatMapLatest { (channelId, date) ->
        if (channelId == null) {
            flowOf(emptyList())
        } else {
            val zone = ZoneId.of("Europe/Paris")
            val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            getProgramsUseCase(channelId, startOfDay, endOfDay)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            filteredChannels.collect { list ->
                if (_selectedChannelId.value == null || list.none { it.id == _selectedChannelId.value }) {
                    _selectedChannelId.value = list.firstOrNull()?.id
                }
            }
        }
    }

    fun selectChannel(channelId: String) {
        _selectedChannelId.value = channelId
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun updateChannelSearch(query: String) {
        _channelSearchQuery.value = query
    }
}
