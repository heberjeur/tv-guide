package com.music4music.tvguide.ui.guide

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.music4music.tvguide.ui.components.ProgramCard
import com.music4music.tvguide.ui.theme.AccentRed
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("EEE dd MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onProgramClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    initialChannelId: String? = null,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val selectedChannelId by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(initialChannelId) {
        if (!initialChannelId.isNullOrBlank()) {
            viewModel.selectChannel(initialChannelId)
        }
    }

    val channelListState = rememberLazyListState()
    LaunchedEffect(selectedChannelId, channels) {
        val index = channels.indexOfFirst { it.id == selectedChannelId }
        if (index >= 0) {
            channelListState.animateScrollToItem(index)
        }
    }

    val availableDates = remember {
        val zone = ZoneId.of("Europe/Paris")
        (0..13).map { LocalDate.now(zone).plusDays(it.toLong()) }
    }

    val currentChannel = channels.find { it.id == selectedChannelId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text(stringResource(R.string.search_programs_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.action_clear_search),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
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
                    if (!isSearchActive) {
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
            ScrollableTabRow(
                selectedTabIndex = availableDates.indexOf(selectedDate).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    val index = availableDates.indexOf(selectedDate).coerceAtLeast(0)
                    if (index < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                            color = AccentRed
                        )
                    }
                },
                edgePadding = 16.dp
            ) {
                availableDates.forEach { date ->
                    Tab(
                        selected = selectedDate == date,
                        onClick = { viewModel.selectDate(date) },
                        text = {
                            Text(
                                text = date.format(dayFormatter),
                                color = if (selectedDate == date) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedDate == date) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                state = channelListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = channels,
                    key = { it.id }
                ) { channel ->
                    FilterChip(
                        selected = selectedChannelId == channel.id,
                        onClick = { viewModel.selectChannel(channel.id) },
                        label = { Text(text = channel.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = AccentRed,
                            selectedLabelColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (programs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = programs,
                        key = { it.id }
                    ) { program ->
                        ProgramCard(
                            program = program,
                            onClick = { onProgramClick(program.id) },
                            channelName = currentChannel?.displayName ?: program.channelId.removeSuffix(".fr")
                        )
                    }
                }
            }
        }
    }
}
