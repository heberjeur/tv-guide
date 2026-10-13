package com.music4music.tvguide.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelBundle
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
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBundle = MutableStateFlow(ChannelBundle.ALL)
    val selectedBundle: StateFlow<ChannelBundle> = _selectedBundle.asStateFlow()

    val isGridMode: StateFlow<Boolean> = userPreferences.isChannelGridMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val allChannels: StateFlow<List<Channel>> = userPreferences.selectedPackage
        .flatMapLatest { packageId ->
            getChannelsUseCase(ChannelPackage.fromId(packageId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            userPreferences.selectedBundle.collect { bundleId ->
                _selectedBundle.value = ChannelBundle.fromId(bundleId)
            }
        }
    }

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
        allChannels,
        _searchQuery,
        channelPrefsFlow
    ) { channels, query, prefs ->
        val bundle = ChannelBundle.fromId(prefs.bundleId)
        val filtered = channels.filter { channel ->
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectBundle(bundle: ChannelBundle) {
        _selectedBundle.value = bundle
        viewModelScope.launch {
            userPreferences.setSelectedBundle(bundle.id)
        }
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
