package com.music4music.tvguide.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.ChannelCatalog
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelBundle
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryExploreViewModel @Inject constructor(
    private val epgRepository: EpgRepository,
    private val userPreferences: UserPreferences,
    getChannelsUseCase: GetChannelsUseCase
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<TvCategory?>(null)
    val selectedCategory: StateFlow<TvCategory?> = _selectedCategory.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val rawChannels: StateFlow<List<Channel>> = userPreferences.selectedPackage
        .flatMapLatest { packageId ->
            getChannelsUseCase(ChannelPackage.fromId(packageId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<Channel>> = combine(
        rawChannels,
        userPreferences.selectedBundle
    ) { list, bundleId ->
        val bundle = ChannelBundle.fromId(bundleId)
        list.filter { channel ->
            ChannelCatalog.isChannelInBundle(channel.id, bundle)
        }.sortedWith(
            compareBy<Channel> { if (it.channelNumber > 0) 0 else 1 }
                .thenBy { it.channelNumber }
                .thenBy { it.displayName }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val programs: StateFlow<List<Program>> = combine(
        _selectedCategory,
        _selectedDate,
        _selectedChannelId,
        _searchQuery,
        userPreferences.selectedBundle
    ) { category, date, channelId, query, bundleId ->
        val bundle = ChannelBundle.fromId(bundleId)
        val keyword = category?.searchKeyword ?: ""
        epgRepository.getProgramsByCategory(keyword).map { list ->
            val zone = ZoneId.of("Europe/Paris")
            val (dayStart, dayEnd) = if (date != null) {
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                start to end
            } else {
                null to null
            }

            list.filter { program ->
                val matchesBundle = ChannelCatalog.isChannelInBundle(program.channelId, bundle)
                val matchesChannel = channelId == null || program.channelId == channelId
                val matchesDate = if (dayStart != null && dayEnd != null) {
                    program.stopTimestamp > dayStart && program.startTimestamp < dayEnd
                } else true
                val matchesQuery = query.isBlank() ||
                        program.title.contains(query, ignoreCase = true) ||
                        program.description.contains(query, ignoreCase = true) ||
                        program.channelId.contains(query, ignoreCase = true)

                matchesBundle && matchesChannel && matchesDate && matchesQuery
            }
        }
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: TvCategory?) {
        _selectedCategory.value = category
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
