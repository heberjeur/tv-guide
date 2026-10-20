package com.music4music.tvguide.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.net.URLDecoder
import javax.inject.Inject

data class PersonProgramsUiState(
    val isLoading: Boolean = true,
    val personName: String = "",
    val programs: List<Program> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonProgramsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val epgRepository: EpgRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val rawPersonName: String = savedStateHandle.get<String>("personName") ?: ""
    val personName: String = try {
        URLDecoder.decode(rawPersonName, "UTF-8")
    } catch (_: Exception) {
        rawPersonName
    }

    private val programsFlow = if (personName.isNotBlank()) {
        epgRepository.getProgramsByPerson(personName.trim())
    } else {
        flowOf(emptyList())
    }

    val uiState: StateFlow<PersonProgramsUiState> = combine(
        programsFlow,
        userPreferences.hideReruns
    ) { programs, hideReruns ->
        val filtered = if (hideReruns) {
            programs.filter { !it.isRerun }
        } else {
            programs
        }
        PersonProgramsUiState(
            isLoading = false,
            personName = personName,
            programs = filtered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonProgramsUiState(isLoading = true, personName = personName)
    )
}
