package com.music4music.tvguide.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.connectivity.ConnectivityObserver
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.usecase.GetNowPlayingUseCase
import com.music4music.tvguide.domain.usecase.SyncEpgUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NowPlayingUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    getNowPlayingUseCase: GetNowPlayingUseCase,
    private val syncEpgUseCase: SyncEpgUseCase,
    private val userPreferences: UserPreferences,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val programs: StateFlow<List<Program>> = getNowPlayingUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnline: StateFlow<Boolean> = connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), connectivityObserver.isConnected())

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    init {
        refresh(force = false)
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
