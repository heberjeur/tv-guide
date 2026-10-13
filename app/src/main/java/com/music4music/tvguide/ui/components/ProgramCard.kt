package com.music4music.tvguide.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.music4music.tvguide.R
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.ui.theme.AccentRed
import com.music4music.tvguide.ui.theme.DarkSurface
import com.music4music.tvguide.ui.theme.DarkSurfaceVariant
import com.music4music.tvguide.ui.theme.IndicatorGreen
import com.music4music.tvguide.ui.theme.TextPrimary
import com.music4music.tvguide.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ProgramCard(
    program: Program,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    channelName: String? = null
) {
    val zone = ZoneId.systemDefault()
    val startTime = Instant.ofEpochMilli(program.startTimestamp).atZone(zone).format(timeFormatter)
    val stopTime = Instant.ofEpochMilli(program.stopTimestamp).atZone(zone).format(timeFormatter)
    val channelLabel = channelName ?: program.channelId.removeSuffix(".fr")

    val episodeLabel = when {
        program.seasonNumber != null && program.episodeNumber != null -> stringResource(
            R.string.label_season_episode_short,
            program.seasonNumber,
            program.episodeNumber
        )
        !program.episodeDisplay.isNullOrBlank() -> program.episodeDisplay
        else -> null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!program.iconUrl.isNullOrBlank() && !program.iconUrl.startsWith("data:")) {
                    AsyncImage(
                        model = program.iconUrl,
                        contentDescription = program.title,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channelLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "  •  $startTime - $stopTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (program.isNowPlaying) IndicatorGreen else TextSecondary,
                                fontWeight = if (program.isNowPlaying) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (program.durationMinutes > 0) {
                            Text(
                                text = stringResource(R.string.label_duration_minutes, program.durationMinutes),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!program.subTitle.isNullOrEmpty() || episodeLabel != null) {
                        val subText = buildString {
                            if (episodeLabel != null) append(episodeLabel)
                            if (!program.subTitle.isNullOrEmpty()) {
                                if (isNotEmpty()) append(" - ")
                                append(program.subTitle)
                            }
                        }
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentRed.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (program.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = program.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (program.isNowPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { program.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentRed,
                    trackColor = DarkSurfaceVariant
                )
            }

            if (program.categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    program.categories.take(2).forEach { category ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = DarkSurfaceVariant
                            ),
                            border = null,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }
        }
    }
}
