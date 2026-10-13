package com.music4music.tvguide.ui.now

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music4music.tvguide.R
import com.music4music.tvguide.domain.model.ChannelBundle
import com.music4music.tvguide.domain.model.SyncState
import com.music4music.tvguide.ui.components.OfflineBanner
import com.music4music.tvguide.ui.components.ProgramCard
import com.music4music.tvguide.ui.theme.AccentRed
import com.music4music.tvguide.ui.theme.DarkBackground
import com.music4music.tvguide.ui.theme.DarkSurface
import com.music4music.tvguide.ui.theme.DarkSurfaceVariant
import com.music4music.tvguide.ui.theme.IndicatorGreen
import com.music4music.tvguide.ui.theme.SurfaceBorder
import com.music4music.tvguide.ui.theme.TextPrimary
import com.music4music.tvguide.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onProgramClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedEveningPart by viewModel.selectedEveningPart.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }

    val syncMessage = when (syncProgress.state) {
        SyncState.DOWNLOADING -> stringResource(R.string.sync_state_downloading)
        SyncState.PARSING -> stringResource(R.string.sync_state_parsing)
        SyncState.CACHING_IMAGES -> stringResource(R.string.sync_state_caching_images)
        SyncState.SUCCESS -> stringResource(R.string.sync_state_success, syncProgress.programCount)
        SyncState.ERROR -> stringResource(R.string.sync_state_error)
        SyncState.IDLE -> ""
    }

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
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = SurfaceBorder
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear_search), tint = TextSecondary)
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.app_name),
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
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                                tint = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refresh(force = true) },
                            enabled = !uiState.isRefreshing
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AccentRed,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.action_refresh),
                                    tint = TextPrimary
                                )
                            }
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
            OfflineBanner(isOffline = !isOnline)

            AnimatedVisibility(visible = syncProgress.state != SyncState.IDLE && syncMessage.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (syncProgress.state == SyncState.SUCCESS) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IndicatorGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (syncProgress.state == SyncState.ERROR) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentRed,
                                strokeWidth = 2.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::dismissSyncBanner,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            PrimaryTabRow(
                selectedTabIndex = selectedPeriod.ordinal,
                containerColor = DarkBackground,
                contentColor = AccentRed
            ) {
                Tab(
                    selected = selectedPeriod == BroadcastPeriod.NOW,
                    onClick = { viewModel.selectPeriod(BroadcastPeriod.NOW) },
                    text = {
                        Text(
                            text = stringResource(R.string.period_now),
                            color = if (selectedPeriod == BroadcastPeriod.NOW) AccentRed else TextSecondary,
                            fontWeight = if (selectedPeriod == BroadcastPeriod.NOW) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedPeriod == BroadcastPeriod.TONIGHT,
                    onClick = { viewModel.selectPeriod(BroadcastPeriod.TONIGHT) },
                    text = {
                        Text(
                            text = stringResource(R.string.period_tonight),
                            color = if (selectedPeriod == BroadcastPeriod.TONIGHT) AccentRed else TextSecondary,
                            fontWeight = if (selectedPeriod == BroadcastPeriod.TONIGHT) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedPeriod == BroadcastPeriod.YESTERDAY,
                    onClick = { viewModel.selectPeriod(BroadcastPeriod.YESTERDAY) },
                    text = {
                        Text(
                            text = stringResource(R.string.period_yesterday),
                            color = if (selectedPeriod == BroadcastPeriod.YESTERDAY) AccentRed else TextSecondary,
                            fontWeight = if (selectedPeriod == BroadcastPeriod.YESTERDAY) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            if (selectedPeriod != BroadcastPeriod.NOW) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedEveningPart == EveningPart.PART_1,
                        onClick = { viewModel.selectEveningPart(EveningPart.PART_1) },
                        label = {
                            Text(
                                text = stringResource(
                                    if (selectedPeriod == BroadcastPeriod.TONIGHT) R.string.period_tonight_part_1 else R.string.period_yesterday_part_1
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRed,
                            selectedLabelColor = TextPrimary,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = null
                    )
                    FilterChip(
                        selected = selectedEveningPart == EveningPart.PART_2,
                        onClick = { viewModel.selectEveningPart(EveningPart.PART_2) },
                        label = {
                            Text(
                                text = stringResource(
                                    if (selectedPeriod == BroadcastPeriod.TONIGHT) R.string.period_tonight_part_2 else R.string.period_yesterday_part_2
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRed,
                            selectedLabelColor = TextPrimary,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = null
                    )
                }
            }



            if (programs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isRefreshing) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = AccentRed)
                            Text(
                                text = stringResource(R.string.sync_state_downloading),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = when {
                                    !isOnline -> stringResource(R.string.offline_banner_no_data)
                                    searchQuery.isNotEmpty() -> stringResource(R.string.search_empty)
                                    else -> uiState.errorMessage ?: stringResource(R.string.no_programs_period)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.refresh(force = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                            ) {
                                Text(text = stringResource(R.string.action_refresh))
                            }
                        }
                    }
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
                            channelName = program.channelId.removeSuffix(".fr")
                        )
                    }
                }
            }
        }
    }
}
