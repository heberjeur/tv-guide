package com.music4music.tvguide.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.ChannelCatalog
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.TvCategory
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GuideViewModel @Inject constructor(
    private val epgRepository: EpgRepository,
    private val userPreferences: UserPreferences,
    getChannelsUseCase: GetChannelsUseCase
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<TvCategory?>(null)
    val selectedCategory: StateFlow<TvCategory?> = _selectedCategory.asStateFlow()

    private val _selectedSubCategory = MutableStateFlow<String?>(null)
    val selectedSubCategory: StateFlow<String?> = _selectedSubCategory.asStateFlow()

    val availableSubCategories: StateFlow<List<String>> = _selectedCategory.flatMapLatest { cat ->
        if (cat == null) flowOf(emptyList())
        else epgRepository.getFilteredPrograms(cat.searchKeyword, null, 0L, 0L).map { list ->
            list.flatMap { it.categories }
                .filter { it.isNotBlank() && !it.equals(cat.searchKeyword, ignoreCase = true) && !it.equals("General", ignoreCase = true) }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .map { it.first }
                .take(15)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val rawChannels: StateFlow<List<Channel>> = getChannelsUseCase(ChannelPackage.COMMUNITY_ENRICHED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<Channel>> = combine(
        rawChannels,
        userPreferences.hiddenChannels,
        userPreferences.favoriteChannels,
        userPreferences.channelSortOrder
    ) { list, hidden, favorites, sortOrder ->
        val filtered = list.filter { channel ->
            !hidden.contains(channel.id)
        }.map { channel ->
            val isFav = ChannelCatalog.isFavorite(channel.id, favorites) || channel.isFavorite
            if (channel.isFavorite != isFav) channel.copy(isFavorite = isFav) else channel
        }
        when (sortOrder) {
            "popular" -> filtered.sortedWith(
                compareBy<Channel> { ChannelCatalog.getPopularityRank(it.id) }
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

    private data class FilterPreferences(
        val hiddenChannels: Set<String>,
        val favoriteChannels: Set<String>,
        val channelSortOrder: String,
        val hideReruns: Boolean
    )

    private val filterPrefsFlow = combine(
        userPreferences.hiddenChannels,
        userPreferences.favoriteChannels,
        userPreferences.channelSortOrder,
        userPreferences.hideReruns
    ) { hidden, favorites, sortOrder, hideReruns ->
        FilterPreferences(hidden, favorites, sortOrder, hideReruns)
    }

    private data class GuideFilter(
        val category: TvCategory?,
        val subCategory: String?,
        val date: LocalDate?,
        val channelId: String?
    )

    private val guideFilterFlow = combine(
        _selectedCategory,
        _selectedSubCategory,
        _selectedDate,
        _selectedChannelId
    ) { category, subCategory, date, channelId ->
        GuideFilter(category, subCategory, date, channelId)
    }

    val programs: StateFlow<List<Program>> = guideFilterFlow.flatMapLatest { filter ->
        val zone = ZoneId.of("Europe/Paris")
        val (dayStart, dayEnd) = if (filter.date != null) {
            val start = filter.date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = filter.date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            start to end
        } else {
            0L to 0L
        }
        val keyword = filter.subCategory ?: filter.category?.searchKeyword ?: ""
        epgRepository.getFilteredPrograms(keyword, filter.channelId, dayStart, dayEnd)
    }.combine(
        combine(filterPrefsFlow, _searchQuery) { prefs, query -> prefs to query }
    ) { list, (prefs, query) ->
        val filtered = list.filter { program ->
            val notHidden = !prefs.hiddenChannels.any {
                ChannelCatalog.normalizeId(it) == ChannelCatalog.normalizeId(program.channelId)
            }
            val notRerun = if (prefs.hideReruns) program.isUnreleased else true
            val matchesQuery = query.isBlank() ||
                    program.title.contains(query, ignoreCase = true) ||
                    program.description.contains(query, ignoreCase = true) ||
                    ChannelCatalog.getDisplayName(program.channelId).contains(query, ignoreCase = true)
            notHidden && notRerun && matchesQuery
        }
        when (prefs.channelSortOrder) {
            "popular" -> filtered.sortedWith(
                compareBy<Program> { ChannelCatalog.getPopularityRank(it.channelId) }
                    .thenBy { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
                    .thenBy { it.startTimestamp }
            )
            "favorites", "favorites_first" -> filtered.sortedWith(
                compareByDescending<Program> { ChannelCatalog.isFavorite(it.channelId, prefs.favoriteChannels) }
                    .thenBy { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
                    .thenBy { it.startTimestamp }
            )
            "name" -> filtered.sortedWith(
                compareBy<Program> { ChannelCatalog.getDisplayName(it.channelId) }
                    .thenBy { it.startTimestamp }
            )
            else -> filtered.sortedWith(
                compareBy<Program> { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
                    .thenBy { it.startTimestamp }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: TvCategory?) {
        _selectedCategory.value = category
        _selectedSubCategory.value = null
    }

    fun selectSubCategory(subCategory: String?) {
        _selectedSubCategory.value = subCategory
    }

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun selectChannel(channelId: String?) {
        _selectedChannelId.value = channelId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
