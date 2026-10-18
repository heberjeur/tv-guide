package com.music4music.tvguide.ui.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Clear
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
import com.music4music.tvguide.domain.model.TvCategory
import com.music4music.tvguide.ui.components.ProgramCard
import com.music4music.tvguide.ui.theme.AccentRed
import com.music4music.tvguide.ui.theme.DarkSurface
import com.music4music.tvguide.ui.theme.DarkSurfaceVariant
import com.music4music.tvguide.ui.theme.TextPrimary
import com.music4music.tvguide.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
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

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.categories) { tabItem ->
                val isSelected = uiState.selectedCategory == tabItem.category
                val label = if (tabItem.category == null) {
                    stringResource(R.string.category_all)
                } else {
                    stringResource(tabItem.category.nameRes)
                }
                val icon = getCategoryIcon(tabItem.category)

                Surface(
                    onClick = { viewModel.selectCategory(tabItem.category) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) AccentRed else DarkSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else TextSecondary
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.25f) else DarkSurface,
                                    CircleShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tabItem.count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.subCategories.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.subcategories_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.subcategories_count, uiState.subCategories.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val isAllSubSelected = uiState.selectedSubCategory == null
                                Surface(
                                    onClick = { viewModel.selectSubCategory(null) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAllSubSelected) AccentRed else DarkSurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isAllSubSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.filter_all_channels_short)} (${uiState.totalProgramsCount})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isAllSubSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSubSelected) Color.White else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                uiState.subCategories.forEach { subItem ->
                                    val isSubSelected = uiState.selectedSubCategory == subItem.name
                                    Surface(
                                        onClick = { viewModel.selectSubCategory(subItem.name) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSubSelected) AccentRed else DarkSurfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSubSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Text(
                                            text = "${subItem.name} (${subItem.count})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSubSelected) Color.White else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
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
                                onClick = { viewModel.selectSubCategory(null) },
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = uiState.selectedSubCategory ?: "",
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
                                .padding(vertical = 48.dp),
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
                            onCategoryClick = { clickedCat ->
                                viewModel.selectSubCategory(clickedCat)
                            }
                        )
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
