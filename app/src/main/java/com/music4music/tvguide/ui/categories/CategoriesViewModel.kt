package com.music4music.tvguide.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.TvCategory
import com.music4music.tvguide.domain.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class CategoriesDisplayMode {
    GRID_HUB,
    ACCORDION,
    SECTIONS
}

data class SubCategoryItem(
    val name: String,
    val count: Int
)

data class CategoryTabItem(
    val category: TvCategory?,
    val count: Int
)

data class CategorySectionData(
    val category: TvCategory,
    val totalProgramsCount: Int,
    val subCategories: List<SubCategoryItem>,
    val previewPrograms: List<Program>
)

data class CategoriesUiState(
    val displayMode: CategoriesDisplayMode = CategoriesDisplayMode.GRID_HUB,
    val categories: List<CategoryTabItem> = emptyList(),
    val sections: List<CategorySectionData> = emptyList(),
    val selectedCategory: TvCategory? = null,
    val subCategories: List<SubCategoryItem> = emptyList(),
    val selectedSubCategory: String? = null,
    val searchQuery: String = "",
    val programs: List<Program> = emptyList(),
    val totalProgramsCount: Int = 0,
    val expandedCategory: TvCategory? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    epgRepository: EpgRepository
) : ViewModel() {

    private val metaKeywords = setOf(
        "rediffusion", "rediff", "direct", "en direct", "inédit", "inedit", "inédite", "inedite",
        "audiodescription", "sous-titres", "general", "autre", "divers", "programme indéterminé"
    )

    private val allActivePrograms: StateFlow<List<Program>> = epgRepository.getAllActivePrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _displayMode = MutableStateFlow(CategoriesDisplayMode.GRID_HUB)
    val displayMode: StateFlow<CategoriesDisplayMode> = _displayMode.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TvCategory?>(null)
    val selectedCategory: StateFlow<TvCategory?> = _selectedCategory.asStateFlow()

    private val _expandedCategory = MutableStateFlow<TvCategory?>(TvCategory.MOVIES)
    val expandedCategory: StateFlow<TvCategory?> = _expandedCategory.asStateFlow()

    private val _selectedSubCategory = MutableStateFlow<String?>(null)
    val selectedSubCategory: StateFlow<String?> = _selectedSubCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private data class FilterParams(
        val mode: CategoriesDisplayMode,
        val selectedCat: TvCategory?,
        val expandedCat: TvCategory?,
        val selectedSub: String?,
        val query: String
    )

    private val filterParams = combine(
        _displayMode,
        _selectedCategory,
        _expandedCategory,
        _selectedSubCategory,
        _searchQuery
    ) { mode, selectedCat, expandedCat, selectedSub, query ->
        FilterParams(mode, selectedCat, expandedCat, selectedSub, query)
    }

    val uiState: StateFlow<CategoriesUiState> = combine(
        allActivePrograms,
        filterParams
    ) { programs, filter ->
        val (mode, selectedCat, expandedCat, selectedSub, query) = filter
        if (programs.isEmpty()) {
            return@combine CategoriesUiState(displayMode = mode, isLoading = true)
        }

        val programParentMap = programs.associateWith { resolveParentCategory(it) }

        val categoryCounts = mutableMapOf<TvCategory, Int>()
        for (cat in TvCategory.entries) {
            categoryCounts[cat] = 0
        }
        for (prog in programs) {
            val parent = programParentMap[prog] ?: TvCategory.OTHER
            categoryCounts[parent] = (categoryCounts[parent] ?: 0) + 1
        }

        val categoryTabs = mutableListOf<CategoryTabItem>()
        categoryTabs.add(CategoryTabItem(category = null, count = programs.size))
        for (cat in TvCategory.entries) {
            val count = categoryCounts[cat] ?: 0
            if (count > 0 || cat != TvCategory.OTHER) {
                categoryTabs.add(CategoryTabItem(category = cat, count = count))
            }
        }

        val sectionList = mutableListOf<CategorySectionData>()
        for (cat in TvCategory.entries) {
            val catProgs = programs.filter { programParentMap[it] == cat }
            if (catProgs.isNotEmpty()) {
                val catSubCounts = mutableMapOf<String, Int>()
                for (prog in catProgs) {
                    val subs = extractSubCategories(prog, cat)
                    for (s in subs) {
                        catSubCounts[s] = (catSubCounts[s] ?: 0) + 1
                    }
                }
                val subItems = catSubCounts.entries
                    .map { SubCategoryItem(name = it.key, count = it.value) }
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    .sortedByDescending { it.count }
                    .toList()

                val preview = catProgs.take(12)
                sectionList.add(
                    CategorySectionData(
                        category = cat,
                        totalProgramsCount = catProgs.size,
                        subCategories = subItems,
                        previewPrograms = preview
                    )
                )
            }
        }

        val programsInParent = if (selectedCat == null) {
            programs
        } else {
            programs.filter { programParentMap[it] == selectedCat }
        }

        val subCatCounts = mutableMapOf<String, Int>()
        for (prog in programsInParent) {
            val parent = programParentMap[prog] ?: TvCategory.OTHER
            val subs = extractSubCategories(prog, parent)
            for (sub in subs) {
                subCatCounts[sub] = (subCatCounts[sub] ?: 0) + 1
            }
        }

        val sortedSubCategories = subCatCounts.entries
            .asSequence()
            .map { SubCategoryItem(name = it.key, count = it.value) }
            .filter { item ->
                query.isBlank() || item.name.contains(query, ignoreCase = true)
            }
            .sortedByDescending { it.count }
            .toList()

        val filteredPrograms = programsInParent.filter { prog ->
            val matchesSub = selectedSub == null || prog.categories.any { it.equals(selectedSub, ignoreCase = true) }
            val matchesQuery = query.isBlank() ||
                prog.title.contains(query, ignoreCase = true) ||
                (prog.subTitle?.contains(query, ignoreCase = true) == true) ||
                prog.description.contains(query, ignoreCase = true) ||
                prog.categories.any { it.contains(query, ignoreCase = true) }

            matchesSub && matchesQuery
        }

        CategoriesUiState(
            displayMode = mode,
            categories = categoryTabs,
            sections = sectionList,
            selectedCategory = selectedCat,
            subCategories = sortedSubCategories,
            selectedSubCategory = selectedSub,
            searchQuery = query,
            programs = filteredPrograms,
            totalProgramsCount = programsInParent.size,
            expandedCategory = expandedCat,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CategoriesUiState(isLoading = true)
    )

    fun setDisplayMode(mode: CategoriesDisplayMode) {
        _displayMode.value = mode
    }

    fun selectCategory(category: TvCategory?) {
        _selectedCategory.value = category
        _selectedSubCategory.value = null
    }

    fun toggleExpandedCategory(category: TvCategory) {
        _expandedCategory.value = if (_expandedCategory.value == category) null else category
        _selectedSubCategory.value = null
    }

    fun selectSubCategory(subCategory: String?) {
        _selectedSubCategory.value = if (_selectedSubCategory.value == subCategory) null else subCategory
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    private fun resolveParentCategory(program: Program): TvCategory {
        val first = program.categories.firstOrNull()?.trim()?.lowercase()
        if (first != null) {
            when {
                first.contains("film") || first.contains("cinéma") || first.contains("cinema") -> return TvCategory.MOVIES
                first.contains("série") || first.contains("serie") || first.contains("feuilleton") -> return TvCategory.SERIES
                first.contains("divertissement") || first.contains("jeu") -> return TvCategory.ENTERTAINMENT
                first.contains("sport") -> return TvCategory.SPORT
                first.contains("info") || first.contains("journal") || first.contains("actualité") -> return TvCategory.NEWS
                first.contains("documentaire") || first.contains("docu") || first.contains("découverte") -> return TvCategory.DOCUMENTARY
                first.contains("jeunesse") || first.contains("animation") -> return TvCategory.KIDS
                first.contains("musique") || first.contains("concert") -> return TvCategory.MUSIC
            }
        }

        val allCatText = program.categories.joinToString(" ").lowercase()
        return when {
            allCatText.contains("film") || allCatText.contains("cinéma") || allCatText.contains("téléfilm") -> TvCategory.MOVIES
            allCatText.contains("série") || allCatText.contains("feuilleton") || program.seasonNumber != null || program.episodeNumber != null -> TvCategory.SERIES
            allCatText.contains("divertissement") || allCatText.contains("jeu") || allCatText.contains("téléréalité") || allCatText.contains("humour") -> TvCategory.ENTERTAINMENT
            allCatText.contains("sport") || allCatText.contains("football") || allCatText.contains("rugby") || allCatText.contains("tennis") || allCatText.contains("cyclisme") -> TvCategory.SPORT
            allCatText.contains("documentaire") || allCatText.contains("découverte") || allCatText.contains("animalier") || allCatText.contains("nature") || allCatText.contains("histoire") -> TvCategory.DOCUMENTARY
            allCatText.contains("info") || allCatText.contains("journal") || allCatText.contains("actualité") || allCatText.contains("météo") || allCatText.contains("débat") || allCatText.contains("société") -> TvCategory.NEWS
            allCatText.contains("jeunesse") || allCatText.contains("dessin animé") || allCatText.contains("animation") -> TvCategory.KIDS
            allCatText.contains("musique") || allCatText.contains("concert") || allCatText.contains("clip") || allCatText.contains("opéra") -> TvCategory.MUSIC
            else -> TvCategory.OTHER
        }
    }

    private fun extractSubCategories(program: Program, parent: TvCategory): List<String> {
        val parentSearch = parent.searchKeyword.lowercase()
        return program.categories.filter { cat ->
            val c = cat.trim().lowercase()
            c.isNotEmpty() &&
                !metaKeywords.contains(c) &&
                c != parentSearch &&
                c != "autre"
        }
    }
}
