package com.music4music.tvguide.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.TvCategory
import com.music4music.tvguide.domain.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryExploreViewModel @Inject constructor(
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(TvCategory.MOVIES)
    val selectedCategory: StateFlow<TvCategory> = _selectedCategory.asStateFlow()

    val programs: StateFlow<List<Program>> = _selectedCategory
        .flatMapLatest { category ->
            epgRepository.getProgramsByCategory(category.searchKeyword)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: TvCategory) {
        _selectedCategory.value = category
    }
}
