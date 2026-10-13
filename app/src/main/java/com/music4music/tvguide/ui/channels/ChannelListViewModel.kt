package com.music4music.tvguide.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.repository.EpgRepository
import com.music4music.tvguide.domain.usecase.GetChannelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChannelListViewModel @Inject constructor(
    getChannelsUseCase: GetChannelsUseCase,
    private val epgRepository: EpgRepository,
    userPreferences: UserPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allChannels: StateFlow<List<Channel>> = userPreferences.selectedPackage
        .flatMapLatest { packageId ->
            getChannelsUseCase(ChannelPackage.fromId(packageId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredChannels: StateFlow<List<Channel>> = combine(allChannels, _searchQuery) { channels, query ->
        if (query.isBlank()) {
            channels
        } else {
            channels.filter { it.displayName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(channelId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            epgRepository.toggleFavoriteChannel(channelId, !currentFavorite)
        }
    }
}
