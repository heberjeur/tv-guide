package com.music4music.tvguide.ui.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music4music.tvguide.R
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.TvCategory
import com.music4music.tvguide.ui.components.ProgramCard
import com.music4music.tvguide.ui.theme.AccentRed
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayChipFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onProgramClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    initialChannelId: String? = null,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedChannelId by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialChannelId) {
        if (!initialChannelId.isNullOrBlank()) {
            viewModel.selectChannel(initialChannelId)
        }
    }

    val channelListState = rememberLazyListState()
    LaunchedEffect(selectedChannelId, channels) {
        if (selectedChannelId != null) {
            val index = channels.indexOfFirst { it.id == selectedChannelId }
            if (index >= 0) {
                channelListState.animateScrollToItem(index + 1)
            }
        }
    }

    val parisZone = remember { ZoneId.of("Europe/Paris") }
    val today = remember { LocalDate.now(parisZone) }
    val tomorrow = remember { today.plusDays(1) }
    val availableDates = remember {
        (0..10).map { today.plusDays(it.toLong()) }
    }

    val channelNameMap = remember(channels) {
        channels.associate { it.id to it.displayName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_programs_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tab_guide),
                            color = MaterialTheme.colorScheme.onSurface,
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
                                tint = MaterialTheme.colorScheme.onSurface
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
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.tab_settings),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (channels.isNotEmpty()) {
                LazyRow(
                    state = channelListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "all_channels") {
                        ChannelLogoItem(
                            channel = null,
                            isSelected = selectedChannelId == null,
                            onClick = { viewModel.selectChannel(null) }
                        )
                    }
                    items(
                        items = channels,
                        key = { it.id }
                    ) { channel ->
                        ChannelLogoItem(
                            channel = channel,
                            isSelected = selectedChannelId == channel.id,
                            onClick = {
                                if (selectedChannelId == channel.id) {
                                    viewModel.selectChannel(null)
                                } else {
                                    viewModel.selectChannel(channel.id)
                                }
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showCategorySheet = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedCategory != null) AccentRed else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedCategory != null) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(selectedCategory),
                            contentDescription = null,
                            tint = if (selectedCategory != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedCategory == null) {
                                stringResource(R.string.filter_categories_title)
                            } else {
                                stringResource(selectedCategory!!.nameRes)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedCategory != null) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCategory != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (selectedCategory != null) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.selectCategory(null) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_reset_filter),
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )

                Spacer(modifier = Modifier.width(8.dp))

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDate == null,
                            onClick = { viewModel.selectDate(null) },
                            label = { Text(text = stringResource(R.string.filter_all_days), style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRed,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(
                        items = availableDates,
                        key = { it.toString() }
                    ) { date ->
                        val label = when (date) {
                            today -> stringResource(R.string.filter_today)
                            tomorrow -> stringResource(R.string.filter_tomorrow)
                            else -> date.format(dayChipFormatter)
                        }
                        FilterChip(
                            selected = selectedDate == date,
                            onClick = { viewModel.selectDate(date) },
                            label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRed,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                stringResource(R.string.search_empty)
                            } else {
                                stringResource(R.string.no_programs_for_date)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val dayHeaderFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedDate == null) {
                        val grouped = programs.groupBy { prog ->
                            Instant.ofEpochMilli(prog.startTimestamp).atZone(parisZone).toLocalDate()
                        }
                        grouped.forEach { (date, datePrograms) ->
                            item(key = "header_$date") {
                                val formattedDate = date.format(dayHeaderFormatter).replaceFirstChar { it.uppercase() }
                                val headerTitle = when (date) {
                                    today -> stringResource(R.string.filter_today_prefix, formattedDate)
                                    tomorrow -> stringResource(R.string.filter_tomorrow_prefix, formattedDate)
                                    else -> formattedDate
                                }
                                Text(
                                    text = headerTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentRed,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = datePrograms,
                                key = { it.id }
                            ) { program ->
                                ProgramCard(
                                    program = program,
                                    onClick = { onProgramClick(program.id) },
                                    channelName = channelNameMap[program.channelId]
                                )
                            }
                        }
                    } else {
                        items(
                            items = programs,
                            key = { it.id }
                        ) { program ->
                            ProgramCard(
                                program = program,
                                onClick = { onProgramClick(program.id) },
                                channelName = channelNameMap[program.channelId]
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        CategoryFilterBottomSheet(
            selectedCategory = selectedCategory,
            onCategorySelect = viewModel::selectCategory,
            onDismiss = { showCategorySheet = false }
        )
    }
}

@Composable
private fun ChannelLogoItem(
    channel: Channel?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val backgroundColor = if (isSelected) AccentRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(58.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channel == null) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = stringResource(R.string.filter_all_channels),
                    tint = if (isSelected) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            } else if (!channel.iconUrl.isNullOrBlank() && !channel.iconUrl.startsWith("data:")) {
                AsyncImage(
                    model = channel.iconUrl,
                    contentDescription = channel.displayName,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = if (channel.channelNumber > 0) channel.channelNumber.toString() else channel.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AccentRed else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = channel?.displayName ?: stringResource(R.string.filter_all_channels_short),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterBottomSheet(
    selectedCategory: TvCategory?,
    onCategorySelect: (TvCategory?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.filter_categories_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedCategory != null) {
                    TextButton(
                        onClick = {
                            onCategorySelect(null)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_reset_filter),
                            color = AccentRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item(span = { GridItemSpan(2) }) {
                    CategoryItemCard(
                        category = null,
                        isSelected = selectedCategory == null,
                        onClick = {
                            onCategorySelect(null)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    )
                }
                items(TvCategory.entries) { category ->
                    CategoryItemCard(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = {
                            onCategorySelect(category)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItemCard(
    category: TvCategory?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val containerColor = if (isSelected) AccentRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AccentRed else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (category == null) {
                    stringResource(R.string.category_all)
                } else {
                    stringResource(category.nameRes)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AccentRed else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getCategoryIcon(category: TvCategory?): ImageVector {
    return when (category) {
        null -> Icons.Default.GridView
        TvCategory.MOVIES -> Icons.Default.Movie
        TvCategory.SERIES -> Icons.Default.Tv
        TvCategory.ENTERTAINMENT -> Icons.Default.TheaterComedy
        TvCategory.SPORT -> Icons.Default.SportsSoccer
        TvCategory.NEWS -> Icons.Default.Newspaper
        TvCategory.DOCUMENTARY -> Icons.Default.Public
        TvCategory.KIDS -> Icons.Default.ChildCare
        TvCategory.MUSIC -> Icons.Default.MusicNote
    }
}
