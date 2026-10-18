package com.music4music.tvguide.ui.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music4music.tvguide.R
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.TvCategory
import com.music4music.tvguide.ui.components.ProgramCard
import com.music4music.tvguide.ui.theme.AccentRed
import com.music4music.tvguide.ui.theme.DarkSurface
import com.music4music.tvguide.ui.theme.DarkSurfaceVariant
import com.music4music.tvguide.ui.theme.TextPrimary
import com.music4music.tvguide.ui.theme.TextSecondary

@Composable
fun CategoriesScreen(
    onProgramClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tab_categories),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.tab_settings),
                    tint = TextSecondary
                )
            }
        }

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_categories_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentRed,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val modes = listOf(
                CategoriesDisplayMode.GRID_HUB to stringResource(R.string.mode_grid),
                CategoriesDisplayMode.ACCORDION to stringResource(R.string.mode_accordion),
                CategoriesDisplayMode.SECTIONS to stringResource(R.string.mode_sections)
            )

            modes.forEach { (mode, title) ->
                val isSelected = uiState.displayMode == mode
                Surface(
                    onClick = { viewModel.setDisplayMode(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) AccentRed else DarkSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            when (uiState.displayMode) {
                CategoriesDisplayMode.GRID_HUB -> {
                    if (uiState.selectedCategory != null) {
                        CategoryDetailSubView(
                            uiState = uiState,
                            onBackClick = { viewModel.selectCategory(null) },
                            onSubCategoryClick = { viewModel.selectSubCategory(it) },
                            onProgramClick = onProgramClick
                        )
                    } else {
                        CategoryGridHubView(
                            sections = uiState.sections,
                            onCategoryClick = { viewModel.selectCategory(it) },
                            onSubCategoryClick = { cat, sub ->
                                viewModel.selectCategory(cat)
                                viewModel.selectSubCategory(sub)
                            }
                        )
                    }
                }
                CategoriesDisplayMode.ACCORDION -> {
                    CategoryAccordionView(
                        sections = uiState.sections,
                        expandedCategory = uiState.expandedCategory,
                        selectedSubCategory = uiState.selectedSubCategory,
                        programs = uiState.programs,
                        onToggleCategory = { viewModel.toggleExpandedCategory(it) },
                        onSubCategoryClick = { viewModel.selectSubCategory(it) },
                        onProgramClick = onProgramClick
                    )
                }
                CategoriesDisplayMode.SECTIONS -> {
                    CategorySectionsView(
                        sections = uiState.sections,
                        onSeeAllClick = {
                            viewModel.setDisplayMode(CategoriesDisplayMode.GRID_HUB)
                            viewModel.selectCategory(it)
                        },
                        onSubCategoryClick = { cat, sub ->
                            viewModel.setDisplayMode(CategoriesDisplayMode.GRID_HUB)
                            viewModel.selectCategory(cat)
                            viewModel.selectSubCategory(sub)
                        },
                        onProgramClick = onProgramClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGridHubView(
    sections: List<CategorySectionData>,
    onCategoryClick: (TvCategory) -> Unit,
    onSubCategoryClick: (TvCategory, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(sections, key = { it.category.id }) { section ->
            val icon = getCategoryIcon(section.category)
            val title = stringResource(section.category.nameRes)

            Surface(
                onClick = { onCategoryClick(section.category) },
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.programs_found_count, section.totalProgramsCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Text(
                        text = stringResource(R.string.subcategories_count, section.subCategories.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentRed
                    )

                    if (section.subCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            maxItemsInEachRow = 2
                        ) {
                            section.subCategories.take(4).forEach { sub ->
                                Surface(
                                    onClick = { onSubCategoryClick(section.category, sub.name) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkSurfaceVariant
                                ) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDetailSubView(
    uiState: CategoriesUiState,
    onBackClick: () -> Unit,
    onSubCategoryClick: (String?) -> Unit,
    onProgramClick: (Long) -> Unit
) {
    val category = uiState.selectedCategory ?: return
    val categoryTitle = stringResource(category.nameRes)
    val icon = getCategoryIcon(category)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackClick() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_categories),
                    tint = AccentRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.back_to_categories),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentRed
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${stringResource(R.string.programs_found_count, uiState.totalProgramsCount)} • ${stringResource(R.string.subcategories_count, uiState.subCategories.size)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        if (uiState.subCategories.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.subcategories_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isAllSelected = uiState.selectedSubCategory == null
                        Surface(
                            onClick = { onSubCategoryClick(null) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAllSelected) AccentRed else DarkSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isAllSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = "${stringResource(R.string.filter_all_channels_short)} (${uiState.totalProgramsCount})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        uiState.subCategories.forEach { subItem ->
                            val isSelected = uiState.selectedSubCategory == subItem.name
                            Surface(
                                onClick = { onSubCategoryClick(subItem.name) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AccentRed else DarkSurfaceVariant,
                                border = BorderStroke(
                                1.dp,
                                    if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Text(
                                    text = "${subItem.name} (${subItem.count})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.programs_found_count, uiState.programs.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentRed
                )
                if (uiState.selectedSubCategory != null) {
                    Surface(
                        onClick = { onSubCategoryClick(null) },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = uiState.selectedSubCategory,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (uiState.programs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_programs_in_category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(uiState.programs, key = { it.id }) { program ->
                ProgramCard(
                    program = program,
                    onClick = { onProgramClick(program.id) },
                    onCategoryClick = { onSubCategoryClick(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryAccordionView(
    sections: List<CategorySectionData>,
    expandedCategory: TvCategory?,
    selectedSubCategory: String?,
    programs: List<Program>,
    onToggleCategory: (TvCategory) -> Unit,
    onSubCategoryClick: (String?) -> Unit,
    onProgramClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sections, key = { it.category.id }) { section ->
            val isExpanded = expandedCategory == section.category
            val icon = getCategoryIcon(section.category)
            val title = stringResource(section.category.nameRes)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                border = BorderStroke(
                    1.dp,
                    if (isExpanded) AccentRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCategory(section.category) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isExpanded) AccentRed else AccentRed.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isExpanded) Color.White else AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${stringResource(R.string.programs_found_count, section.totalProgramsCount)} • ${stringResource(R.string.subcategories_count, section.subCategories.size)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isExpanded) AccentRed else TextSecondary
                        )
                    }

                    if (isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.subcategories_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val isAllSelected = selectedSubCategory == null
                                Surface(
                                    onClick = { onSubCategoryClick(null) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAllSelected) AccentRed else DarkSurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isAllSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.filter_all_channels_short)} (${section.totalProgramsCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) Color.White else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                section.subCategories.forEach { subItem ->
                                    val isSelected = selectedSubCategory == subItem.name
                                    Surface(
                                        onClick = { onSubCategoryClick(subItem.name) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) AccentRed else DarkSurfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Text(
                                            text = "${subItem.name} (${subItem.count})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val displayPrograms = if (selectedSubCategory == null) {
                                section.previewPrograms
                            } else {
                                programs
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                displayPrograms.take(8).forEach { prog ->
                                    ProgramCard(
                                        program = prog,
                                        onClick = { onProgramClick(prog.id) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionsView(
    sections: List<CategorySectionData>,
    onSeeAllClick: (TvCategory) -> Unit,
    onSubCategoryClick: (TvCategory, String) -> Unit,
    onProgramClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(sections, key = { it.category.id }) { section ->
            val icon = getCategoryIcon(section.category)
            val title = stringResource(section.category.nameRes)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(DarkSurfaceVariant, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = section.totalProgramsCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }

                    TextButton(onClick = { onSeeAllClick(section.category) }) {
                        Text(
                            text = stringResource(R.string.action_see_all),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentRed
                        )
                    }
                }

                if (section.subCategories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(section.subCategories) { sub ->
                            Surface(
                                onClick = { onSubCategoryClick(section.category, sub.name) },
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceVariant
                            ) {
                                Text(
                                    text = "${sub.name} (${sub.count})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(section.previewPrograms, key = { it.id }) { prog ->
                        Box(modifier = Modifier.width(280.dp)) {
                            ProgramCard(
                                program = prog,
                                onClick = { onProgramClick(prog.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: TvCategory?): ImageVector {
    return when (category) {
        null -> Icons.Default.GridView
        TvCategory.MOVIES -> Icons.Default.Movie
        TvCategory.SERIES -> Icons.Default.Tv
        TvCategory.ENTERTAINMENT -> Icons.Default.Celebration
        TvCategory.SPORT -> Icons.Default.SportsSoccer
        TvCategory.NEWS -> Icons.Default.Newspaper
        TvCategory.DOCUMENTARY -> Icons.Default.Public
        TvCategory.KIDS -> Icons.Default.ChildCare
        TvCategory.MUSIC -> Icons.Default.MusicNote
        TvCategory.OTHER -> Icons.Default.MoreHoriz
    }
}
