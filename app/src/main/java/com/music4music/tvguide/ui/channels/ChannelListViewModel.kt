package com.music4music.tvguide.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.repository.EpgRepository
import com.music4music.tvguide.domain.usecase.GetChannelsUseCase
import com.music4music.tvguide.domain.usecase.SyncEpgUseCase
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
    private val userPreferences: UserPreferences,
    private val syncEpgUseCase: SyncEpgUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncEpgUseCase(ChannelPackage.COMMUNITY_ENRICHED, forceRefresh = force)
            _isRefreshing.value = false
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isGridMode: StateFlow<Boolean> = userPreferences.isChannelGridMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val allChannels: StateFlow<List<Channel>> = getChannelsUseCase(ChannelPackage.COMMUNITY_ENRICHED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class ChannelPreferences(
        val hiddenChannels: Set<String>,
        val favoriteChannels: Set<String>,
        val channelSortOrder: String
    )

    private val channelPrefsFlow = combine(
        userPreferences.hiddenChannels,
        userPreferences.favoriteChannels,
        userPreferences.channelSortOrder
    ) { hidden, favorites, sortOrder ->
        ChannelPreferences(hidden, favorites, sortOrder)
    }

    val filteredChannels: StateFlow<List<Channel>> = combine(
        allChannels,
        _searchQuery,
        channelPrefsFlow
    ) { channels, query, prefs ->
        val filtered = channels.filter { channel ->
            val notHidden = !prefs.hiddenChannels.contains(channel.id)
            val matchesQuery = query.isBlank() ||
                channel.displayName.contains(query, ignoreCase = true) ||
                (channel.channelNumber > 0 && channel.channelNumber.toString() == query.trim())
            notHidden && matchesQuery
        }.map { channel ->
            val isFav = com.music4music.tvguide.data.local.ChannelCatalog.isFavorite(channel.id, prefs.favoriteChannels) || channel.isFavorite
            if (channel.isFavorite != isFav) channel.copy(isFavorite = isFav) else channel
        }

        when (prefs.channelSortOrder) {
            "popular" -> filtered.sortedWith(
                compareBy<Channel> { com.music4music.tvguide.data.local.ChannelCatalog.getPopularityRank(it.id) }
                    .thenBy { if (it.channelNumber > 0) 0 else 1 }
                    .thenBy { it.channelNumber }
                    .thenBy { it.displayName }
            )
            "favorites", "favorites_first" -> filtered.sortedWith(
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleGridMode() {
        viewModelScope.launch {
            userPreferences.setChannelGridMode(!isGridMode.value)
        }
    }

    fun toggleFavorite(channelId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            userPreferences.toggleFavoriteChannel(channelId)
            epgRepository.toggleFavoriteChannel(channelId, !currentFavorite)
        }
    }
}
