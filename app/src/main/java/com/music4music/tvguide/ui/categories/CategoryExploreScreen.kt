package com.music4music.tvguide.ui.categories

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music4music.tvguide.R
import com.music4music.tvguide.domain.model.TvCategory
import com.music4music.tvguide.ui.components.ProgramCard
import com.music4music.tvguide.ui.theme.AccentRed
import com.music4music.tvguide.ui.theme.DarkBackground
import com.music4music.tvguide.ui.theme.DarkSurface
import com.music4music.tvguide.ui.theme.DarkSurfaceVariant
import com.music4music.tvguide.ui.theme.TextPrimary
import com.music4music.tvguide.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayChipFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExploreScreen(
    onProgramClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: CategoryExploreViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedChannelId by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }

    val availableDates = remember {
        (0..13).map { LocalDate.now().plusDays(it.toLong()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text(text = stringResource(R.string.search_programs_placeholder), color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tab_categories),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_clear_search),
                                    tint = TextPrimary
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                                tint = TextPrimary
                            )
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.tab_settings),
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text(text = stringResource(R.string.category_all)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentRed,
                        selectedLabelColor = TextPrimary,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = null,
                    shape = RoundedCornerShape(8.dp)
                )

                TvCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(text = stringResource(category.nameRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRed,
                            selectedLabelColor = TextPrimary,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDate == null,
                        onClick = { viewModel.selectDate(null) },
                        label = { Text(text = stringResource(R.string.filter_all_days)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkSurfaceVariant,
                            selectedLabelColor = AccentRed,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                items(
                    items = availableDates,
                    key = { it.toString() }
                ) { date ->
                    FilterChip(
                        selected = selectedDate == date,
                        onClick = { viewModel.selectDate(date) },
                        label = { Text(text = date.format(dayChipFormatter)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkSurfaceVariant,
                            selectedLabelColor = AccentRed,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (channels.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedChannelId == null,
                            onClick = { viewModel.selectChannel(null) },
                            label = { Text(text = stringResource(R.string.filter_all_channels)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkSurfaceVariant,
                                selectedLabelColor = AccentRed,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(
                        items = channels,
                        key = { it.id }
                    ) { channel ->
                        FilterChip(
                            selected = selectedChannelId == channel.id,
                            onClick = { viewModel.selectChannel(channel.id) },
                            label = { Text(text = channel.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkSurfaceVariant,
                                selectedLabelColor = AccentRed,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (programs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.category_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = programs,
                        key = { it.id }
                    ) { program ->
                        ProgramCard(
                            program = program,
                            onClick = { onProgramClick(program.id) }
                        )
                    }
                }
            }
        }
    }
}
