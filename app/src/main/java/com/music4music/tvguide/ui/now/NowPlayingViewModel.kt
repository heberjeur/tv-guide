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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class BroadcastPeriod {
    NOW,
    TONIGHT,
    YESTERDAY
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
    val prefs: FilterPrefs
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

    private val filterPrefsFlow = combine(
        userPreferences.hiddenChannels,
        userPreferences.favoriteChannels,
        userPreferences.channelSortOrder,
        userPreferences.hideReruns
    ) { hidden, favs, sort, reruns ->
        FilterPrefs(hidden, favs, sort, reruns)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val programs: StateFlow<List<Program>> = combine(
        _selectedPeriod,
        _selectedEveningPart,
        _searchQuery,
        filterPrefsFlow
    ) { period, part, query, prefs ->
        Params(period, part, query, prefs)
    }.flatMapLatest { (period, part, query, prefs) ->
        val zone = ZoneId.of("Europe/Paris")
        val today = LocalDate.now(zone)

        val flow: Flow<List<Program>> = when (period) {
            BroadcastPeriod.NOW -> getNowPlayingUseCase()
            BroadcastPeriod.TONIGHT -> {
                val (startTime, endTime) = if (part == EveningPart.PART_1) {
                    val s = today.atTime(20, 30).atZone(zone).toInstant().toEpochMilli()
                    val e = today.atTime(22, 30).atZone(zone).toInstant().toEpochMilli()
                    Pair(s, e)
                } else {
                    val s = today.atTime(22, 30).atZone(zone).toInstant().toEpochMilli()
                    val e = today.plusDays(1).atTime(1, 0).atZone(zone).toInstant().toEpochMilli()
                    Pair(s, e)
                }
                epgRepository.getProgramsByTimeRange(startTime, endTime)
            }
            BroadcastPeriod.YESTERDAY -> {
                val yesterday = today.minusDays(1)
                val (startTime, endTime) = if (part == EveningPart.PART_1) {
                    val s = yesterday.atTime(20, 30).atZone(zone).toInstant().toEpochMilli()
                    val e = yesterday.atTime(22, 30).atZone(zone).toInstant().toEpochMilli()
                    Pair(s, e)
                } else {
                    val s = yesterday.atTime(22, 30).atZone(zone).toInstant().toEpochMilli()
                    val e = today.atTime(1, 0).atZone(zone).toInstant().toEpochMilli()
                    Pair(s, e)
                }
                epgRepository.getProgramsByTimeRange(startTime, endTime)
            }
        }

        flow.map { list ->
            val filtered = list.filter { program ->
                val notHidden = !prefs.hiddenChannels.contains(program.channelId)
                val notRerun = if (prefs.hideReruns) program.isUnreleased else true
                val matchesQuery = query.isBlank() ||
                        program.title.contains(query, ignoreCase = true) ||
                        program.categories.any { cat -> cat.contains(query, ignoreCase = true) } ||
                        program.channelId.contains(query, ignoreCase = true)
                notHidden && notRerun && matchesQuery
            }
            val distinctList = if (period == BroadcastPeriod.NOW) {
                filtered.distinctBy { it.channelId }
            } else {
                filtered
            }
            when (prefs.sortOrder) {
                "popular" -> distinctList.sortedWith(
                    compareBy<Program> { ChannelCatalog.getPopularityRank(it.channelId) }
                        .thenBy { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
                        .thenBy { it.startTimestamp }
                )
                "favorites", "favorites_first" -> distinctList.sortedWith(
                    compareByDescending<Program> { prefs.favoriteChannels.contains(it.channelId) }
                        .thenBy { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
                        .thenBy { it.startTimestamp }
                )
                "name" -> distinctList.sortedWith(
                    compareBy<Program> { it.channelId }
                        .thenBy { it.startTimestamp }
                )
                else -> distinctList.sortedWith(
                    compareBy<Program> { ChannelCatalog.getChannelNumber(it.channelId).let { num -> if (num > 0) num else 999 } }
                        .thenBy { it.startTimestamp }
                )
            }
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
        _selectedPeriod.value = period
    }

    fun selectEveningPart(part: EveningPart) {
        _selectedEveningPart.value = part
    }

    fun dismissSyncBanner() {
        epgRepository.resetSyncProgress()
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            val packageId = userPreferences.selectedPackage.first()
            val channelPackage = ChannelPackage.fromId(packageId)
            val result = syncEpgUseCase(channelPackage, forceRefresh = force)
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                errorMessage = result.exceptionOrNull()?.localizedMessage
            )
        }
    }
}
