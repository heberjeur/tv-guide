package com.music4music.tvguide.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music4music.tvguide.R
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

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgramDetailScreen(
    onBackClick: () -> Unit,
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val program by viewModel.program.collectAsStateWithLifecycle()
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = program?.title ?: "", color = TextPrimary) },
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
    ) { padding ->
        val currentProgram = program
        if (currentProgram == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            val zone = ZoneId.systemDefault()
            val startTime = Instant.ofEpochMilli(currentProgram.startTimestamp).atZone(zone).format(timeFormatter)
            val stopTime = Instant.ofEpochMilli(currentProgram.stopTimestamp).atZone(zone).format(timeFormatter)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!currentProgram.iconUrl.isNullOrBlank() && !currentProgram.iconUrl.startsWith("data:")) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        AsyncImage(
                            model = currentProgram.iconUrl,
                            contentDescription = currentProgram.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, DarkBackground),
                                        startY = 100f
                                    )
                                )
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${currentProgram.channelId}  •  $startTime - $stopTime  •  ${currentProgram.durationMinutes} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentProgram.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (!currentProgram.subTitle.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentProgram.subTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentProgram.isUnreleased) {
                            BadgeItem(text = stringResource(R.string.label_unreleased), color = IndicatorGreen)
                        }
                        if (currentProgram.isHd) {
                            BadgeItem(text = stringResource(R.string.label_hd), color = AccentRed)
                        }
                        if (currentProgram.hasSubtitles) {
                            BadgeItem(text = stringResource(R.string.label_subtitles), color = TextSecondary)
                        }
                        if (currentProgram.contentRating != null) {
                            BadgeItem(
                                text = stringResource(R.string.label_content_rating, currentProgram.contentRating),
                                color = TextSecondary
                            )
                        }
                        if (currentProgram.starRating != null) {
                            BadgeItem(
                                text = "★ ${"%.1f".format(currentProgram.starRating)}/5",
                                color = Color(0xFFFFD54F)
                            )
                        }
                        if (currentProgram.seasonNumber != null && currentProgram.episodeNumber != null) {
                            BadgeItem(
                                text = stringResource(
                                    R.string.label_season_episode,
                                    currentProgram.seasonNumber,
                                    currentProgram.episodeNumber
                                ),
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.toggleReminder() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (reminder != null) DarkSurfaceVariant else AccentRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (reminder != null) Icons.Default.AlarmOn else Icons.Default.Alarm,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = if (reminder != null) {
                                stringResource(R.string.action_remove_reminder)
                            } else {
                                stringResource(R.string.action_set_reminder)
                            }
                        )
                    }

                    if (currentProgram.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentProgram.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                        )
                    }

                    if (currentProgram.categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentProgram.categories.forEach { category ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(text = category, color = TextSecondary) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = DarkSurface
                                    ),
                                    border = null
                                )
                            }
                        }
                    }

                    val credits = currentProgram.credit
                    if (credits.presenters.isNotEmpty() || credits.guests.isNotEmpty() || credits.directors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        if (credits.presenters.isNotEmpty()) {
                            CreditSection(
                                label = stringResource(R.string.label_presented_by),
                                names = credits.presenters
                            )
                        }

                        if (credits.guests.isNotEmpty()) {
                            CreditSection(
                                label = stringResource(R.string.label_guests),
                                names = credits.guests
                            )
                        }

                        if (credits.directors.isNotEmpty()) {
                            CreditSection(
                                label = stringResource(R.string.label_director),
                                names = credits.directors
                            )
                        }

                        if (credits.producers.isNotEmpty()) {
                            CreditSection(
                                label = stringResource(R.string.label_production),
                                names = credits.producers
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeItem(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CreditSection(label: String, names: List<String>) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = names.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
