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

    private data class ChannelPreferences(
        val bundleId: String,
        val hiddenChannels: Set<String>,
        val favoriteChannels: Set<String>,
        val channelSortOrder: String
    )

    private val channelPrefsFlow = combine(
        userPreferences.selectedBundle,
        userPreferences.hiddenChannels,
        userPreferences.favoriteChannels,
        userPreferences.channelSortOrder
    ) { bundleId, hidden, favorites, sortOrder ->
        ChannelPreferences(bundleId, hidden, favorites, sortOrder)
    }

    val filteredChannels: StateFlow<List<Channel>> = combine(
        rawChannels,
        _channelSearchQuery,
        channelPrefsFlow
    ) { list, query, prefs ->
        val bundle = ChannelBundle.fromId(prefs.bundleId)
        val filtered = list.filter { channel ->
            val notHidden = !prefs.hiddenChannels.contains(channel.id)
            val matchesQuery = query.isBlank() ||
                channel.displayName.contains(query, ignoreCase = true) ||
                (channel.channelNumber > 0 && channel.channelNumber.toString() == query.trim())
            val matchesBundle = com.music4music.tvguide.data.local.ChannelCatalog.isChannelInBundle(channel.id, bundle)
            notHidden && matchesQuery && matchesBundle
        }.map { channel ->
            val isFav = prefs.favoriteChannels.contains(channel.id) || channel.isFavorite
            if (channel.isFavorite != isFav) channel.copy(isFavorite = isFav) else channel
        }

        when (prefs.channelSortOrder) {
            "favorites" -> filtered.sortedWith(
                compareByDescending<Channel> { it.isFavorite }
                    .thenBy { if (it.channelNumber > 0) 0 else 1 }
                    .thenBy { it.channelNumber }
                    .thenBy { it.displayName }
            )
            "name" -> filtered.sortedWith(compareBy { it.displayName })
            else -> filtered.sortedWith(
                compareBy<Channel> { if (it.channelNumber > 0) 0 else 1 }
                    .thenBy { it.channelNumber }
                    .thenBy { it.displayName }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
    }.combine(
        combine(_searchQuery, userPreferences.hideReruns) { q, hr -> q to hr }
    ) { list, (query, hideReruns) ->
        list.filter { program ->
            val matchesRerun = if (hideReruns) program.isUnreleased else true
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                program.title.contains(query, ignoreCase = true) ||
                    program.description.contains(query, ignoreCase = true) ||
                    program.categories.any { cat -> cat.contains(query, ignoreCase = true) }
            }
            matchesRerun && matchesQuery
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
