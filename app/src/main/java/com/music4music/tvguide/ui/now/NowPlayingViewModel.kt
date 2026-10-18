package com.music4music.tvguide.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.connectivity.ConnectivityObserver
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.SyncProgress
import com.music4music.tvguide.domain.repository.EpgRepository
import com.music4music.tvguide.domain.usecase.GetNowPlayingUseCase
import com.music4music.tvguide.domain.usecase.SyncEpgUseCase
import com.music4music.tvguide.data.local.ChannelCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class BroadcastPeriod {
    YESTERDAY,
    NOW,
    TONIGHT,
    TOMORROW
}

enum class EveningPart {
    PART_1,
    PART_2
}

private data class FilterPrefs(
    val hiddenChannels: Set<String>,
    val favoriteChannels: Set<String>,
    val sortOrder: String,
    val hideReruns: Boolean
)

private data class Params(
    val period: BroadcastPeriod,
    val part: EveningPart,
    val query: String,
    val selectedTag: String?,
    val prefs: FilterPrefs,
    val zone: ZoneId
)

data class NowPlayingUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val getNowPlayingUseCase: GetNowPlayingUseCase,
    private val epgRepository: EpgRepository,
    private val syncEpgUseCase: SyncEpgUseCase,
    private val userPreferences: UserPreferences,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(BroadcastPeriod.NOW)
    val selectedPeriod: StateFlow<BroadcastPeriod> = _selectedPeriod.asStateFlow()

    private val _selectedEveningPart = MutableStateFlow(EveningPart.PART_1)
    val selectedEveningPart: StateFlow<EveningPart> = _selectedEveningPart.asStateFlow()

    val syncProgress: StateFlow<SyncProgress> = epgRepository.getSyncProgress()

    val isOnline: StateFlow<Boolean> = connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), connectivityObserver.isConnected())

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    fun filterByTag(tag: String) {
        val trimmed = tag.trim()
        if (_selectedTag.value.equals(trimmed, ignoreCase = true)) {
            _selectedTag.value = null
        } else {
            _selectedTag.value = trimmed
        }
    }

    fun clearTagFilter() {
        _selectedTag.value = null
    }

    private val filterPrefsFlow = combine(
        userPreferences.hiddenChannels,
        userPreferences.favoriteChannels,
        userPreferences.channelSortOrder,
        userPreferences.hideReruns
    ) { hidden, favs, sort, reruns ->
        FilterPrefs(hidden, favs, sort, reruns)
    }

    private val currentTimeFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(5000L)
        }
    }

    private val _manualRefreshTrigger = MutableStateFlow(0L)

    private val displayZoneFlow: Flow<ZoneId> = userPreferences.displayTimezone.map {
        com.music4music.tvguide.util.TimezoneUtils.resolveZoneId(it)
    }

    private val rawProgramsFlow = combine(
        _selectedPeriod,
        _selectedEveningPart,
        _manualRefreshTrigger,
        displayZoneFlow
    ) { period, part, _, zone ->
        Triple(period, part, zone)
    }.flatMapLatest { (period, part, zone) ->
        val originZone = com.music4music.tvguide.util.TimezoneUtils.ORIGIN_ZONE
        val today = LocalDate.now(originZone)
        val now = System.currentTimeMillis()

        val (startTime, endTime) = when (period) {
            BroadcastPeriod.NOW -> {
                (now - 4 * 3600 * 1000L) to (now + 8 * 3600 * 1000L)
            }
            BroadcastPeriod.TONIGHT -> {
                if (part == EveningPart.PART_1) {
                    val s = today.atTime(19, 30).atZone(originZone).toInstant().toEpochMilli()
                    val e = today.atTime(23, 45).atZone(originZone).toInstant().toEpochMilli()
                    s to e
                } else {
                    val s = today.atTime(22, 15).atZone(originZone).toInstant().toEpochMilli()
                    val e = today.plusDays(1).atTime(2, 30).atZone(originZone).toInstant().toEpochMilli()
                    s to e
                }
            }
            BroadcastPeriod.YESTERDAY -> {
                val yesterday = today.minusDays(1)
                if (part == EveningPart.PART_1) {
                    val s = yesterday.atTime(19, 30).atZone(originZone).toInstant().toEpochMilli()
                    val e = yesterday.atTime(23, 45).atZone(originZone).toInstant().toEpochMilli()
                    s to e
                } else {
                    val s = yesterday.atTime(22, 15).atZone(originZone).toInstant().toEpochMilli()
                    val e = today.atTime(2, 30).atZone(originZone).toInstant().toEpochMilli()
                    s to e
                }
            }
            BroadcastPeriod.TOMORROW -> {
                val tomorrow = today.plusDays(1)
                if (part == EveningPart.PART_1) {
                    val s = tomorrow.atTime(19, 30).atZone(originZone).toInstant().toEpochMilli()
                    val e = tomorrow.atTime(23, 45).atZone(originZone).toInstant().toEpochMilli()
                    s to e
                } else {
                    val s = tomorrow.atTime(22, 15).atZone(originZone).toInstant().toEpochMilli()
                    val e = tomorrow.plusDays(1).atTime(2, 30).atZone(originZone).toInstant().toEpochMilli()
                    s to e
                }
            }
        }
        epgRepository.getProgramsByTimeRange(startTime, endTime)
    }

    private val paramsFlow = combine(
        _selectedPeriod,
        _selectedEveningPart,
        _searchQuery,
        _selectedTag,
        combine(filterPrefsFlow, displayZoneFlow) { prefs, zone -> prefs to zone }
    ) { period, part, query, tag, (prefs, zone) ->
        Params(period, part, query, tag, prefs, zone)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val programs: StateFlow<List<Program>> = combine(
        rawProgramsFlow,
        currentTimeFlow,
        paramsFlow
    ) { rawList, currentTime, (period, part, query, tag, prefs, zone) ->
        val originZone = com.music4music.tvguide.util.TimezoneUtils.ORIGIN_ZONE
        val today = LocalDate.now(originZone)

        val targetTime = when (period) {
            BroadcastPeriod.NOW -> currentTime
            BroadcastPeriod.TONIGHT -> {
                if (part == EveningPart.PART_1) {
                    today.atTime(21, 10).atZone(originZone).toInstant().toEpochMilli()
                } else {
                    today.atTime(23, 0).atZone(originZone).toInstant().toEpochMilli()
                }
            }
            BroadcastPeriod.YESTERDAY -> {
                val yesterday = today.minusDays(1)
                if (part == EveningPart.PART_1) {
                    yesterday.atTime(21, 10).atZone(originZone).toInstant().toEpochMilli()
                } else {
                    yesterday.atTime(23, 0).atZone(originZone).toInstant().toEpochMilli()
                }
            }
            BroadcastPeriod.TOMORROW -> {
                val tomorrow = today.plusDays(1)
                if (part == EveningPart.PART_1) {
                    tomorrow.atTime(21, 10).atZone(originZone).toInstant().toEpochMilli()
                } else {
                    tomorrow.atTime(23, 0).atZone(originZone).toInstant().toEpochMilli()
                }
            }
        }

        val progsByChannel = rawList.groupBy { it.channelId }
        val selectedPrograms = ChannelCatalog.tntChannelIds.mapNotNull { channelId ->
            val channelProgs = progsByChannel[channelId] ?: progsByChannel.entries.firstOrNull {
                ChannelCatalog.normalizeId(it.key) == ChannelCatalog.normalizeId(channelId)
            }?.value
            if (channelProgs.isNullOrEmpty()) return@mapNotNull null

            val overlapping = channelProgs.filter { targetTime in it.startTimestamp until it.stopTimestamp }
            if (overlapping.isNotEmpty()) {
                val meaningful = if (overlapping.size > 1 && overlapping.any { it.stopTimestamp - it.startTimestamp >= 5 * 60 * 1000L }) {
                    overlapping.filter { it.stopTimestamp - it.startTimestamp >= 5 * 60 * 1000L }
                } else {
                    overlapping
                }
                meaningful.maxByOrNull { it.startTimestamp }
            } else {
                channelProgs.minByOrNull { kotlin.math.abs(it.startTimestamp - targetTime) }
            }
        }

        val filtered = selectedPrograms.filter { program ->
            val notHidden = !prefs.hiddenChannels.any {
                ChannelCatalog.normalizeId(it) == ChannelCatalog.normalizeId(program.channelId)
            }
            val notRerun = if (prefs.hideReruns) !program.isRerun else true
            val matchesQuery = query.isBlank() ||
                    program.title.contains(query, ignoreCase = true) ||
                    program.categories.any { cat -> cat.contains(query, ignoreCase = true) } ||
                    ChannelCatalog.getDisplayName(program.channelId).contains(query, ignoreCase = true)
            val matchesTag = tag.isNullOrBlank() || program.categories.any { cat ->
                cat.contains(tag, ignoreCase = true) || tag.contains(cat, ignoreCase = true)
            }
            notHidden && notRerun && matchesQuery && matchesTag
        }

        when (prefs.sortOrder) {
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        refresh(force = false)
        viewModelScope.launch {
            epgRepository.getSyncProgress().collect { progress ->
                if (progress.state == com.music4music.tvguide.domain.model.SyncState.SUCCESS ||
                    progress.state == com.music4music.tvguide.domain.model.SyncState.ERROR
                ) {
                    kotlinx.coroutines.delay(3500)
                    epgRepository.resetSyncProgress()
                }
            }
        }
    }

    fun selectPeriod(period: BroadcastPeriod) {
        if (_selectedPeriod.value == period) {
            _manualRefreshTrigger.value = System.currentTimeMillis()
        } else {
            _selectedPeriod.value = period
        }
    }

    fun selectEveningPart(part: EveningPart) {
        _selectedEveningPart.value = part
    }

    fun dismissSyncBanner() {
        epgRepository.resetSyncProgress()
    }

    fun refresh(force: Boolean = true) {
        _manualRefreshTrigger.value = System.currentTimeMillis()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            val result = syncEpgUseCase(ChannelPackage.COMMUNITY_ENRICHED, forceRefresh = force)
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }
}
