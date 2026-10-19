package com.music4music.tvguide.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music4music.tvguide.R
import com.music4music.tvguide.data.local.ChannelCatalog
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.ui.theme.AccentRed
import com.music4music.tvguide.ui.theme.DarkBackground
import com.music4music.tvguide.ui.theme.DarkSurface
import com.music4music.tvguide.ui.theme.DarkSurfaceVariant
import com.music4music.tvguide.ui.theme.IndicatorGreen
import com.music4music.tvguide.ui.theme.TextPrimary
import com.music4music.tvguide.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesEpisodesScreen(
    onBackClick: () -> Unit,
    onProgramClick: (Long) -> Unit,
    viewModel: SeriesEpisodesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.seriesTitle.ifBlank { stringResource(R.string.title_season_episodes) },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.title_season_episodes),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val seasons = uiState.availableSeasons
                if (seasons.size > 1 || (seasons.isNotEmpty() && uiState.hasOtherEpisodes)) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(seasons) { seasonNumber ->
                            val isSelected = uiState.selectedSeason == seasonNumber
                            SeasonFilterChip(
                                text = stringResource(R.string.label_season_value, seasonNumber),
                                isSelected = isSelected,
                                onClick = { viewModel.selectSeason(seasonNumber) }
                            )
                        }

                        if (uiState.hasOtherEpisodes) {
                            val isOtherSelected = uiState.selectedSeason == -1
                            item {
                                SeasonFilterChip(
                                    text = stringResource(R.string.label_other_episodes),
                                    isSelected = isOtherSelected,
                                    onClick = { viewModel.selectSeason(-1) }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val seasonLabel = when {
                        uiState.selectedSeason != null && uiState.selectedSeason!! > 0 -> {
                            stringResource(R.string.label_season_value, uiState.selectedSeason!!)
                        }
                        uiState.selectedSeason == -1 -> {
                            stringResource(R.string.label_other_episodes)
                        }
                        else -> {
                            stringResource(R.string.title_season_episodes)
                        }
                    }
                    Text(
                        text = seasonLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    val count = uiState.episodes.size
                    val countText = if (count <= 1) {
                        stringResource(R.string.episodes_count_single_format, count)
                    } else {
                        stringResource(R.string.episodes_count_format, count)
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                if (uiState.episodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_season_episodes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.episodes, key = { it.id }) { episode ->
                            EpisodeCard(
                                episode = episode,
                                onClick = { onProgramClick(episode.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) AccentRed else DarkSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EpisodeCard(
    episode: Program,
    onClick: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val startTime = Instant.ofEpochMilli(episode.startTimestamp).atZone(zone).format(timeFormatter)
    val stopTime = Instant.ofEpochMilli(episode.stopTimestamp).atZone(zone).format(timeFormatter)
    val dateText = Instant.ofEpochMilli(episode.startTimestamp).atZone(zone).format(dateFormatter)
    val channelDisplayName = ChannelCatalog.getDisplayName(episode.channelId)
    val channelLogoUrl = ChannelCatalog.getLogoUrl(episode.channelId)

    val parsedEp = remember(episode.subTitle) {
        Regex("""(?:[ÉéE]pisode|[ÉéE]p\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(episode.subTitle ?: "")?.groupValues?.get(1)?.toIntOrNull()
    }
    val effectiveEpisodeNumber = parsedEp ?: episode.episodeNumber

    val cleanEpisodeTitle = episode.subTitle?.trim()
        ?.replace(Regex("""^(?:(?:Saison|Season|S)\s*\d+\s*[-:•]?\s*|(?:[ÉéE]pisode|[ÉéE]p\.?|E)\s*\d+\s*[-:•]?\s*)+""", RegexOption.IGNORE_CASE), "")
        ?.replace(Regex("""\s*[-:•(]?\s*(?:(?:Saison|Season|S)\s*\d+|(?:[ÉéE]pisode|[ÉéE]p\.?|E)\s*\d+)\s*\)?$""", RegexOption.IGNORE_CASE), "")
        ?.trim()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        if (!channelLogoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = channelLogoUrl,
                                contentDescription = channelDisplayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channelDisplayName.take(2).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = channelDisplayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed
                    )
                }

                Text(
                    text = "${dateText.replaceFirstChar { it.uppercase() }} • $startTime - $stopTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (effectiveEpisodeNumber != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_episode_value, effectiveEpisodeNumber),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (!cleanEpisodeTitle.isNullOrBlank() && !cleanEpisodeTitle.equals(episode.title, ignoreCase = true)) {
                            Text(
                                text = cleanEpisodeTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!cleanEpisodeTitle.isNullOrBlank() && cleanEpisodeTitle.equals(episode.title, ignoreCase = true) && effectiveEpisodeNumber == null) {
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(14.dp)
                )
            }

            if (!episode.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = episode.description.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (episode.isLiveBroadcast) {
                    IndicatorChip(text = stringResource(R.string.status_live), color = AccentRed)
                } else if (episode.isRerun) {
                    IndicatorChip(text = stringResource(R.string.status_rerun), color = TextSecondary)
                } else if (episode.isUnreleasedResolved) {
                    IndicatorChip(text = stringResource(R.string.status_unreleased), color = IndicatorGreen)
                }

                if (episode.isHd) {
                    IndicatorChip(text = stringResource(R.string.label_hd), color = AccentRed)
                }

                if (episode.hasSubtitlesResolved) {
                    IndicatorChip(text = stringResource(R.string.badge_subtitles), color = TextSecondary)
                }
                if (episode.hasAudioDescription) {
                    IndicatorChip(text = stringResource(R.string.badge_audio_description), color = TextSecondary)
                }

                val ratingText = when (episode.contentRating?.trim()) {
                    "-10", "10" -> stringResource(R.string.csa_under_10)
                    "-12", "12", "13" -> stringResource(R.string.csa_under_12)
                    "-16", "16" -> stringResource(R.string.csa_under_16)
                    "-18", "18" -> stringResource(R.string.csa_under_18)
                    "Tout public", "TP", "G", "Tous publics" -> stringResource(R.string.csa_all_audiences)
                    null, "" -> null
                    else -> episode.contentRating.trim()
                }
                if (!ratingText.isNullOrBlank()) {
                    IndicatorChip(text = ratingText, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun IndicatorChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
