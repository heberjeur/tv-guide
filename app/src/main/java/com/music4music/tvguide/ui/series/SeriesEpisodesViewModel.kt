package com.music4music.tvguide.ui.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SeriesEpisodesUiState(
    val isLoading: Boolean = true,
    val seriesTitle: String = "",
    val availableSeasons: List<Int> = emptyList(),
    val hasOtherEpisodes: Boolean = false,
    val selectedSeason: Int? = null,
    val episodes: List<Program> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesEpisodesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val programId: Long = savedStateHandle.get<Long>("programId") ?: 0L
    private val requestedSeason: Int? = savedStateHandle.get<Int>("season")

    private val _selectedSeason = MutableStateFlow<Int?>(requestedSeason)

    private val programFlow = if (programId > 0L) {
        epgRepository.getProgramById(programId)
    } else {
        flowOf(null)
    }

    private val allEpisodesFlow = programFlow.flatMapLatest { prog ->
        if (prog != null && prog.title.isNotBlank()) {
            epgRepository.getProgramsByTitle(prog.title.trim())
        } else {
            flowOf(emptyList())
        }
    }

    val uiState: StateFlow<SeriesEpisodesUiState> = combine(
        programFlow,
        allEpisodesFlow,
        _selectedSeason
    ) { baseProgram, allEpisodes, currentSeasonSelection ->
        if (baseProgram == null) {
            SeriesEpisodesUiState(isLoading = false)
        } else {
            val epRegex = Regex("""(?:[ÉéE]pisode|[ÉéE]p\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
            val seasonRegex = Regex("""Saisons?\s*(\d+)""", RegexOption.IGNORE_CASE)
            val effectiveEpisodes = allEpisodes.map { p ->
                val epFromSub = epRegex.find(p.subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
                val seasonFromSub = seasonRegex.find(p.subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
                if (epFromSub != null || seasonFromSub != null) {
                    p.copy(
                        episodeNumber = epFromSub ?: p.episodeNumber,
                        seasonNumber = seasonFromSub ?: p.seasonNumber
                    )
                } else {
                    p
                }
            }

            val seasons = effectiveEpisodes.mapNotNull { it.seasonNumber }.distinct().sorted()
            val hasUnnumbered = effectiveEpisodes.any { it.seasonNumber == null }

            val effectiveSeason = when {
                currentSeasonSelection != null && (currentSeasonSelection in seasons || currentSeasonSelection == -1) -> currentSeasonSelection
                baseProgram.seasonNumber != null && baseProgram.seasonNumber in seasons -> baseProgram.seasonNumber
                seasons.isNotEmpty() -> seasons.first()
                else -> null
            }

            val filteredEpisodes = when {
                effectiveSeason != null && effectiveSeason > 0 -> {
                    effectiveEpisodes.filter { it.seasonNumber == effectiveSeason }
                        .sortedWith(compareBy({ it.episodeNumber ?: Int.MAX_VALUE }, { it.startTimestamp }))
                }
                effectiveSeason == -1 || (effectiveSeason == null && hasUnnumbered) -> {
                    effectiveEpisodes.filter { it.seasonNumber == null }
                        .sortedWith(compareBy({ it.episodeNumber ?: Int.MAX_VALUE }, { it.startTimestamp }))
                }
                else -> effectiveEpisodes
            }

            SeriesEpisodesUiState(
                isLoading = false,
                seriesTitle = baseProgram.title,
                availableSeasons = seasons,
                hasOtherEpisodes = hasUnnumbered,
                selectedSeason = effectiveSeason,
                episodes = filteredEpisodes
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SeriesEpisodesUiState(isLoading = true)
    )

    fun selectSeason(season: Int?) {
        _selectedSeason.value = season
    }
}
