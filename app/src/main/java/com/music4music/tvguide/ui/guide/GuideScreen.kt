package com.music4music.tvguide.ui.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import android.os.Build
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.music4music.tvguide.util.CategoryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onProgramClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    initialChannelId: String? = null,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSubCategory by viewModel.selectedSubCategory.collectAsStateWithLifecycle()
    val availableSubCategories by viewModel.availableSubCategories.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedChannelId by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val currentLocale = remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !configuration.locales.isEmpty) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale ?: Locale.getDefault()
        }
    }
    val dayChipFormatter = remember(currentLocale) { DateTimeFormatter.ofPattern("EEE d MMM", currentLocale) }
    val fullDayFormatter = remember(currentLocale) { DateTimeFormatter.ofPattern("EEEE d MMMM", currentLocale) }

    var isSearchActive by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showChannelSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialChannelId) {
        if (!initialChannelId.isNullOrBlank()) {
            viewModel.selectChannel(initialChannelId)
        }
    }

    val zone = com.music4music.tvguide.ui.theme.LocalDisplayZone.current
    val today = remember(zone) { LocalDate.now(zone) }
    val tomorrow = remember(today) { today.plusDays(1) }
    val availableDates = remember(today) {
        (0..10).map { today.plusDays(it.toLong()) }
    }

    val channelNameMap = remember(channels) {
        channels.associate { it.id to it.displayName }
    }

    val activeChannel = remember(selectedChannelId, channels) {
        channels.find { it.id == selectedChannelId }
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
                        IconButton(
                            onClick = { viewModel.refresh(force = true) },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AccentRed,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.action_refresh),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categoryLabel = when {
                    selectedCategory != null && selectedSubCategory != null -> "${stringResource(selectedCategory!!.nameRes)}: ${CategoryUtils.formatCategory(selectedSubCategory, currentLocale)}"
                    selectedSubCategory != null -> CategoryUtils.formatCategory(selectedSubCategory, currentLocale)
                    selectedCategory != null -> stringResource(selectedCategory!!.nameRes)
                    else -> stringResource(R.string.filter_categories_title)
                }
                FilterPill(
                    isSelected = selectedCategory != null || selectedSubCategory != null,
                    icon = getCategoryIcon(selectedCategory),
                    label = categoryLabel,
                    onClick = { showCategorySheet = true },
                    onClear = { viewModel.clearCategoryFilter() }
                )

                FilterPill(
                    isSelected = activeChannel != null,
                    icon = Icons.Default.LiveTv,
                    channelIconUrl = activeChannel?.iconUrl,
                    label = activeChannel?.displayName ?: stringResource(R.string.filter_channels_title),
                    onClick = { showChannelSheet = true },
                    onClear = { viewModel.selectChannel(null) }
                )

                val dateLabel = when (selectedDate) {
                    null -> stringResource(R.string.filter_dates_title)
                    today -> stringResource(R.string.filter_today)
                    tomorrow -> stringResource(R.string.filter_tomorrow)
                    else -> selectedDate!!.format(dayChipFormatter)
                }
                FilterPill(
                    isSelected = selectedDate != null,
                    icon = if (selectedDate != null) Icons.Default.Today else Icons.Default.CalendarMonth,
                    label = dateLabel,
                    onClick = { showDateSheet = true },
                    onClear = { viewModel.selectDate(null) }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedDate == null) {
                        val grouped = programs.groupBy { prog ->
                            Instant.ofEpochMilli(prog.startTimestamp).atZone(zone).toLocalDate()
                        }
                        grouped.forEach { (date, datePrograms) ->
                            item(key = "header_$date") {
                                val formattedDate = date.format(fullDayFormatter).replaceFirstChar { it.uppercase() }
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
                                    channelName = channelNameMap[program.channelId],
                                    onCategoryClick = { tag -> viewModel.filterByTag(tag) },
                                    onChannelClick = { chId -> viewModel.selectChannel(chId) }
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
                                channelName = channelNameMap[program.channelId],
                                onCategoryClick = { tag -> viewModel.filterByTag(tag) },
                                onChannelClick = { chId -> viewModel.selectChannel(chId) }
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
            selectedSubCategory = selectedSubCategory,
            availableSubCategories = availableSubCategories,
            currentLocale = currentLocale,
            onCategorySelect = viewModel::selectCategory,
            onSubCategorySelect = viewModel::selectSubCategory,
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showChannelSheet) {
        ChannelFilterBottomSheet(
            channels = channels,
            selectedChannelId = selectedChannelId,
            onChannelSelect = viewModel::selectChannel,
            onDismiss = { showChannelSheet = false }
        )
    }

    if (showDateSheet) {
        DateFilterBottomSheet(
            availableDates = availableDates,
            selectedDate = selectedDate,
            today = today,
            tomorrow = tomorrow,
            fullDayFormatter = fullDayFormatter,
            onDateSelect = viewModel::selectDate,
            onDismiss = { showDateSheet = false }
        )
    }
}

@Composable
private fun FilterPill(
    isSelected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    channelIconUrl: String? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) AccentRed else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            if (channelIconUrl != null && !channelIconUrl.startsWith("data:")) {
                AsyncImage(
                    model = channelIconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClear
                        ),
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilterBottomSheet(
    selectedCategory: TvCategory?,
    selectedSubCategory: String?,
    availableSubCategories: List<String>,
    currentLocale: Locale,
    onCategorySelect: (TvCategory?) -> Unit,
    onSubCategorySelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    var categoryQuery by remember { mutableStateOf("") }

    val filteredCategories = remember(categoryQuery) {
        if (categoryQuery.isBlank()) {
            TvCategory.entries
        } else {
            TvCategory.entries.filter { cat ->
                context.getString(cat.nameRes).contains(categoryQuery, ignoreCase = true)
            }
        }
    }

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
                    .padding(bottom = 12.dp),
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
                            onSubCategorySelect(null)
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

            OutlinedTextField(
                value = categoryQuery,
                onValueChange = { categoryQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.categories_search_placeholder),
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
                    if (categoryQuery.isNotEmpty()) {
                        IconButton(onClick = { categoryQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
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
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (categoryQuery.isBlank()) {
                    item(span = { GridItemSpan(2) }) {
                        CategoryItemCard(
                            category = null,
                            isSelected = selectedCategory == null,
                            onClick = {
                                onCategorySelect(null)
                                onSubCategorySelect(null)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            }
                        )
                    }
                }
                items(filteredCategories) { category ->
                    CategoryItemCard(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = {
                            if (selectedCategory == category) {
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            } else {
                                onCategorySelect(category)
                                onSubCategorySelect(null)
                            }
                        }
                    )
                }
            }

            if (selectedCategory != null && availableSubCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.filter_subcategories_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isAllSubSelected = selectedSubCategory == null
                    Surface(
                        onClick = {
                            onSubCategorySelect(null)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAllSubSelected) AccentRed else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isAllSubSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.filter_all_channels_short),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isAllSubSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSubSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    availableSubCategories.forEach { subCat ->
                        val isSubSelected = selectedSubCategory == subCat
                        Surface(
                            onClick = {
                                onSubCategorySelect(subCat)
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSubSelected) AccentRed else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSubSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = CategoryUtils.formatCategory(subCat, currentLocale),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSubSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelFilterBottomSheet(
    channels: List<Channel>,
    selectedChannelId: String?,
    onChannelSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var channelQuery by remember { mutableStateOf("") }

    val filteredChannels = remember(channels, channelQuery) {
        if (channelQuery.isBlank()) channels
        else channels.filter {
            it.displayName.contains(channelQuery, ignoreCase = true) ||
            (it.channelNumber > 0 && it.channelNumber.toString().contains(channelQuery))
        }
    }

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
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.filter_channels_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedChannelId != null) {
                    TextButton(
                        onClick = {
                            onChannelSelect(null)
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

            OutlinedTextField(
                value = channelQuery,
                onValueChange = { channelQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.channels_search_placeholder),
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
                    if (channelQuery.isNotEmpty()) {
                        IconButton(onClick = { channelQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                item(span = { GridItemSpan(3) }) {
                    Surface(
                        onClick = {
                            onChannelSelect(null)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedChannelId == null) AccentRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            if (selectedChannelId == null) 2.dp else 1.dp,
                            if (selectedChannelId == null) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = if (selectedChannelId == null) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.filter_all_channels),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedChannelId == null) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedChannelId == null) AccentRed else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedChannelId == null) {
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

                items(
                    items = filteredChannels,
                    key = { it.id }
                ) { channel ->
                    val isSelected = selectedChannelId == channel.id
                    val borderColor = if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    val containerColor = if (isSelected) AccentRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant

                    Surface(
                        onClick = {
                            onChannelSelect(channel.id)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!channel.iconUrl.isNullOrBlank() && !channel.iconUrl.startsWith("data:")) {
                                    AsyncImage(
                                        model = channel.iconUrl,
                                        contentDescription = channel.displayName,
                                        modifier = Modifier
                                            .size(34.dp)
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = channel.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) AccentRed else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterBottomSheet(
    availableDates: List<LocalDate>,
    selectedDate: LocalDate?,
    today: LocalDate,
    tomorrow: LocalDate,
    fullDayFormatter: DateTimeFormatter,
    onDateSelect: (LocalDate?) -> Unit,
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
                    text = stringResource(R.string.filter_dates_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedDate != null) {
                    TextButton(
                        onClick = {
                            onDateSelect(null)
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

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                item {
                    val isAllSelected = selectedDate == null
                    Surface(
                        onClick = {
                            onDateSelect(null)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAllSelected) AccentRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            if (isAllSelected) 2.dp else 1.dp,
                            if (isAllSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isAllSelected) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.filter_all_days),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) AccentRed else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isAllSelected) {
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

                items(
                    items = availableDates,
                    key = { it.toString() }
                ) { date ->
                    val isSelected = selectedDate == date
                    val label = when (date) {
                        today -> stringResource(R.string.filter_today)
                        tomorrow -> stringResource(R.string.filter_tomorrow)
                        else -> date.format(fullDayFormatter).replaceFirstChar { it.uppercase() }
                    }
                    val icon = when (date) {
                        today -> Icons.Default.Today
                        tomorrow -> Icons.Default.Event
                        else -> Icons.Default.CalendarToday
                    }

                    Surface(
                        onClick = {
                            onDateSelect(date)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AccentRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) AccentRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AccentRed else MaterialTheme.colorScheme.onSurface,
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
        TvCategory.OTHER -> Icons.Default.Category
    }
}
