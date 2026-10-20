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
import com.music4music.tvguide.domain.usecase.SyncEpgUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GuideViewModel @Inject constructor(
    private val epgRepository: EpgRepository,
    private val userPreferences: UserPreferences,
    private val syncEpgUseCase: SyncEpgUseCase,
    getChannelsUseCase: GetChannelsUseCase
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

    private val _selectedCategory = MutableStateFlow<TvCategory?>(null)
    val selectedCategory: StateFlow<TvCategory?> = _selectedCategory.asStateFlow()

    private val _selectedSubCategory = MutableStateFlow<String?>(null)
    val selectedSubCategory: StateFlow<String?> = _selectedSubCategory.asStateFlow()

    val availableSubCategories: StateFlow<List<String>> = _selectedCategory.flatMapLatest { cat ->
        if (cat == null) flowOf(emptyList())
        else epgRepository.getFilteredPrograms(cat.searchKeyword, null, 0L, 0L).map { list ->
            val parentNames = TvCategory.entries.map { it.searchKeyword.lowercase() } + listOf("general", "autre", "divers")
            list.flatMap { it.categories }
                .map { it.trim() }
                .filter { tag ->
                    tag.isNotBlank() &&
                        !parentNames.contains(tag.lowercase()) &&
                        !tag.equals(cat.searchKeyword, ignoreCase = true) &&
                        !(cat == TvCategory.DOCUMENTARY && tag.contains("série", ignoreCase = true)) &&
                        !(cat == TvCategory.MOVIES && tag.contains("série", ignoreCase = true)) &&
                        !(cat == TvCategory.KIDS && (tag.equals("sport", ignoreCase = true) || tag.equals("musique", ignoreCase = true) || tag.equals("info", ignoreCase = true)))
                }
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
        userPreferences.favoriteChannelsOrder,
        userPreferences.channelSortOrder
    ) { list, hidden, favorites, favoriteOrder, sortOrder ->
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
                compareBy<Channel> {
                    val idx = favoriteOrder.indexOfFirst { favId ->
                        ChannelCatalog.normalizeId(favId) == ChannelCatalog.normalizeId(it.id)
                    }
                    if (idx >= 0) idx else Int.MAX_VALUE
                }.thenBy { if (it.channelNumber > 0) 0 else 1 }
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
        val favoriteChannelsOrder: List<String>,
        val channelSortOrder: String,
        val hideReruns: Boolean
    )

    private val filterPrefsFlow = combine(
        userPreferences.hiddenChannels,
        userPreferences.favoriteChannels,
        userPreferences.favoriteChannelsOrder,
        userPreferences.channelSortOrder,
        userPreferences.hideReruns
    ) { hidden, favorites, favoriteOrder, sortOrder, hideReruns ->
        FilterPreferences(hidden, favorites, favoriteOrder, sortOrder, hideReruns)
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

    private val currentTimeFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(10_000L)
        }
    }

    private val displayZoneFlow: Flow<ZoneId> = userPreferences.displayTimezone.map {
        com.music4music.tvguide.util.TimezoneUtils.resolveZoneId(it)
    }

    val programs: StateFlow<List<Program>> = combine(guideFilterFlow, displayZoneFlow) { filter, zone ->
        filter to zone
    }.flatMapLatest { (filter, zone) ->
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
        currentTimeFlow
    ) { list, currentTime ->
        list to currentTime
    }.combine(
        combine(filterPrefsFlow, _searchQuery, _selectedDate) { prefs, query, date -> Triple(prefs, query, date) }
    ) { (list, currentTime), (prefs, query, selectedDate) ->
        val filtered = list.filter { program ->
            val notHidden = !prefs.hiddenChannels.any {
                ChannelCatalog.normalizeId(it) == ChannelCatalog.normalizeId(program.channelId)
            }
            val notRerun = if (prefs.hideReruns) !program.isRerun else true
            val matchesQuery = query.isBlank() ||
                    program.title.contains(query, ignoreCase = true) ||
                    program.description.contains(query, ignoreCase = true) ||
                    program.credit.directors.any { it.contains(query, ignoreCase = true) } ||
                    program.credit.actors.any { it.contains(query, ignoreCase = true) } ||
                    program.credit.presenters.any { it.contains(query, ignoreCase = true) } ||
                    program.credit.guests.any { it.contains(query, ignoreCase = true) } ||
                    program.categories.any { cat -> cat.contains(query, ignoreCase = true) } ||
                    ChannelCatalog.getDisplayName(program.channelId).contains(query, ignoreCase = true)
            val isCurrentOrUpcoming = if (selectedDate == null) {
                program.stopTimestamp > currentTime - 5 * 60 * 1000L
            } else {
                true
            }
            notHidden && notRerun && matchesQuery && isCurrentOrUpcoming
        }
        val sorted = when (prefs.channelSortOrder) {
            "popular" -> filtered.sortedWith(
                compareBy<Program> { ChannelCatalog.getPopularityRank(it.channelId) }
                    .thenBy { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
                    .thenBy { it.startTimestamp }
            )
            "favorites", "favorites_first" -> filtered.sortedWith(
                compareBy<Program> {
                    val idx = prefs.favoriteChannelsOrder.indexOfFirst { favId ->
                        ChannelCatalog.normalizeId(favId) == ChannelCatalog.normalizeId(it.channelId)
                    }
                    if (idx >= 0) idx else Int.MAX_VALUE
                }.thenBy { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
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
        if (_selectedChannelId.value != null) {
            val sequential = mutableListOf<Program>()
            for (item in sorted) {
                if (sequential.isEmpty()) {
                    sequential.add(item)
                } else {
                    val last = sequential.last()
                    if (item.startTimestamp >= last.stopTimestamp) {
                        sequential.add(item)
                    } else if (item.startTimestamp > last.startTimestamp) {
                        sequential[sequential.lastIndex] = last.copy(stopTimestamp = item.startTimestamp)
                        sequential.add(item)
                    }
                }
            }
            sequential
        } else {
            sorted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: TvCategory?) {
        _selectedCategory.value = category
        _selectedSubCategory.value = null
    }

    fun selectSubCategory(subCategory: String?) {
        _selectedSubCategory.value = subCategory
    }

    fun filterByTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        val matchingParent = TvCategory.entries.find {
            it.searchKeyword.equals(trimmed, ignoreCase = true) ||
                it.name.equals(trimmed, ignoreCase = true)
        }
        if (matchingParent != null) {
            _selectedCategory.value = matchingParent
            _selectedSubCategory.value = null
        } else {
            val inferredParent = when {
                trimmed.contains("film", ignoreCase = true) || trimmed.contains("cinéma", ignoreCase = true) || trimmed.contains("court-métrage", ignoreCase = true) -> TvCategory.MOVIES
                trimmed.contains("série", ignoreCase = true) || trimmed.contains("feuilleton", ignoreCase = true) || trimmed.contains("sitcom", ignoreCase = true) -> TvCategory.SERIES
                trimmed.contains("journal", ignoreCase = true) || trimmed.contains("météo", ignoreCase = true) || trimmed.contains("débat", ignoreCase = true) || trimmed.contains("politique", ignoreCase = true) -> TvCategory.NEWS
                trimmed.contains("jeu", ignoreCase = true) || trimmed.contains("loto", ignoreCase = true) || trimmed.contains("loterie", ignoreCase = true) || trimmed.contains("humour", ignoreCase = true) -> TvCategory.ENTERTAINMENT
                trimmed.contains("sport", ignoreCase = true) || trimmed.contains("foot", ignoreCase = true) || trimmed.contains("rugby", ignoreCase = true) -> TvCategory.SPORT
                trimmed.contains("documentaire", ignoreCase = true) || trimmed.contains("découverte", ignoreCase = true) || trimmed.contains("reportage", ignoreCase = true) -> TvCategory.DOCUMENTARY
                trimmed.contains("animation", ignoreCase = true) || trimmed.contains("jeunesse", ignoreCase = true) || trimmed.contains("manga", ignoreCase = true) -> TvCategory.KIDS
                trimmed.contains("concert", ignoreCase = true) || trimmed.contains("musique", ignoreCase = true) || trimmed.contains("opéra", ignoreCase = true) -> TvCategory.MUSIC
                else -> _selectedCategory.value
            }
            _selectedCategory.value = inferredParent
            _selectedSubCategory.value = trimmed
        }
    }

    fun clearCategoryFilter() {
        _selectedCategory.value = null
        _selectedSubCategory.value = null
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
