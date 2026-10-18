package com.music4music.tvguide.ui.detail

import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.music4music.tvguide.R
import com.music4music.tvguide.util.ProgramDescriptionUtils
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgramDetailScreen(
    onBackClick: () -> Unit,
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val program by viewModel.program.collectAsStateWithLifecycle()
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()
    val upcomingBroadcasts by viewModel.upcomingBroadcasts.collectAsStateWithLifecycle()
    val defaultLeadTime by viewModel.defaultLeadTimeMinutes.collectAsStateWithLifecycle()
    val defaultRecurrence by viewModel.defaultReminderRecurrence.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedLeadTime by remember(defaultLeadTime) { mutableIntStateOf(defaultLeadTime) }
    var selectedRecurrence by remember(defaultRecurrence) { mutableStateOf(defaultRecurrence) }
    var showLeadTimeOptions by remember { mutableStateOf(false) }
    var isImageEnlarged by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = program?.title ?: "",
                        color = TextPrimary,
                        maxLines = 1
                    )
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
            val zone = com.music4music.tvguide.ui.theme.LocalDisplayZone.current
            val startTime = Instant.ofEpochMilli(currentProgram.startTimestamp).atZone(zone).format(timeFormatter)
            val stopTime = Instant.ofEpochMilli(currentProgram.stopTimestamp).atZone(zone).format(timeFormatter)
            val broadcastDate = Instant.ofEpochMilli(currentProgram.startTimestamp).atZone(zone).format(dateFormatter)

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
                            .clickable { isImageEnlarged = true }
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
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clickable { isImageEnlarged = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }

                val isSeriesProgram = currentProgram.seasonNumber != null ||
                    currentProgram.seriesYear != null ||
                    currentProgram.categories.any { it.equals("Série", ignoreCase = true) }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${currentProgram.channelId}  •  ${broadcastDate.replaceFirstChar { it.uppercase() }}  •  $startTime - $stopTime  •  ${currentProgram.formattedDuration}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val displaySubtitle = when {
                        currentProgram.subTitle.isNullOrEmpty() -> {
                            if (currentProgram.episodeNumber != null) {
                                stringResource(R.string.label_episode_value, currentProgram.episodeNumber)
                            } else null
                        }
                        currentProgram.episodeNumber != null && !currentProgram.subTitle.contains("épisode", ignoreCase = true) && !currentProgram.subTitle.contains("ep.", ignoreCase = true) -> {
                            stringResource(
                                R.string.label_episode_with_title,
                                currentProgram.episodeNumber,
                                currentProgram.subTitle
                            )
                        }
                        else -> currentProgram.subTitle
                    }

                    SelectionContainer {
                        Column {
                            Text(
                                text = currentProgram.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            if (!displaySubtitle.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displaySubtitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentProgram.seasonNumber != null && currentProgram.episodeNumber != null) {
                            BadgeItem(
                                text = stringResource(
                                    R.string.label_season_episode_badge,
                                    currentProgram.seasonNumber,
                                    currentProgram.episodeNumber
                                ),
                                color = TextPrimary
                            )
                        } else if (!currentProgram.episodeDisplay.isNullOrBlank()) {
                            BadgeItem(text = currentProgram.episodeDisplay, color = TextPrimary)
                        }
                        if (currentProgram.isUnreleased) {
                            BadgeItem(text = stringResource(R.string.label_unreleased), color = IndicatorGreen)
                        }
                        if (currentProgram.isHd) {
                            BadgeItem(text = stringResource(R.string.label_hd), color = AccentRed)
                        }
                        val highlightAccessibility = com.music4music.tvguide.ui.theme.LocalHighlightAccessibility.current
                        if (highlightAccessibility) {
                            if (currentProgram.hasSubtitlesResolved) {
                                BadgeItem(text = stringResource(R.string.badge_subtitles_long), color = TextSecondary)
                            }
                            if (currentProgram.hasAudioDescription) {
                                BadgeItem(text = stringResource(R.string.badge_ad_long), color = TextSecondary)
                            }
                        }
                        if (currentProgram.contentRating != null) {
                            BadgeItem(text = currentProgram.contentRating, color = TextSecondary)
                        }
                        if (currentProgram.starRating != null) {
                            BadgeItem(
                                text = "★ ${"%.1f".format(Locale.US, currentProgram.starRating)}/5",
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.section_information),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (currentProgram.seasonNumber != null) {
                                val epText = if (currentProgram.episodeNumber != null) {
                                    stringResource(R.string.label_season_episode_short, currentProgram.seasonNumber, currentProgram.episodeNumber)
                                } else {
                                    stringResource(R.string.label_season_value, currentProgram.seasonNumber)
                                }
                                InfoRow(
                                    label = stringResource(R.string.label_season),
                                    value = epText
                                )
                            } else if (currentProgram.episodeNumber != null) {
                                InfoRow(
                                    label = stringResource(R.string.label_episode),
                                    value = stringResource(R.string.label_episode_value, currentProgram.episodeNumber)
                                )
                            }

                            val formatValue = currentProgram.categories.firstOrNull {
                                it.equals("Culture Infos", ignoreCase = true) ||
                                it.equals("Série", ignoreCase = true) ||
                                it.equals("Film", ignoreCase = true) ||
                                it.equals("Documentaire", ignoreCase = true) ||
                                it.equals("Magazine", ignoreCase = true) ||
                                it.equals("Téléfilm", ignoreCase = true) ||
                                it.equals("Divertissement", ignoreCase = true) ||
                                it.equals("Journal", ignoreCase = true) ||
                                it.equals("Sport", ignoreCase = true)
                            } ?: currentProgram.categories.firstOrNull()

                            val genreValue = currentProgram.categories.firstOrNull { it != formatValue && it != "General" }
                                ?: if (currentProgram.categories.size > 1) {
                                    currentProgram.categories.drop(1).joinToString(", ")
                                } else null

                            if (!formatValue.isNullOrBlank()) {
                                InfoRow(
                                    label = stringResource(R.string.label_format),
                                    value = formatValue
                                )
                            }
                            if (!genreValue.isNullOrBlank()) {
                                InfoRow(
                                    label = stringResource(R.string.label_category),
                                    value = genreValue
                                )
                            }

                            val broadcastType = when {
                                currentProgram.isLiveBroadcast || currentProgram.isNowPlaying -> stringResource(R.string.status_live)
                                currentProgram.isUnreleasedResolved -> stringResource(R.string.status_unreleased)
                                currentProgram.isRerun -> stringResource(R.string.status_rerun)
                                else -> null
                            }
                            if (broadcastType != null) {
                                InfoRow(
                                    label = stringResource(R.string.label_broadcast_type),
                                    value = broadcastType
                                )
                            }

                            if (currentProgram.credit.directors.isNotEmpty()) {
                                InfoRow(
                                    label = if (currentProgram.credit.directors.size > 1) {
                                        stringResource(R.string.label_directors)
                                    } else {
                                        stringResource(R.string.label_director)
                                    },
                                    value = currentProgram.credit.directors.joinToString(", ")
                                )
                            }

                            if (currentProgram.credit.presenters.isNotEmpty()) {
                                InfoRow(
                                    label = stringResource(R.string.label_presented_by),
                                    value = currentProgram.credit.presenters.joinToString(", ")
                                )
                            }

                            if (currentProgram.credit.guests.isNotEmpty()) {
                                InfoRow(
                                    label = stringResource(R.string.label_guests),
                                    value = currentProgram.credit.guests.joinToString(", ")
                                )
                            }

                            val accessibilityText = when {
                                currentProgram.hasSubtitlesResolved && currentProgram.hasAudioDescription -> {
                                    stringResource(R.string.accessibility_subtitles_and_ad)
                                }
                                currentProgram.hasSubtitlesResolved -> {
                                    stringResource(R.string.badge_subtitles_long)
                                }
                                currentProgram.hasAudioDescription -> {
                                    stringResource(R.string.badge_ad_long)
                                }
                                else -> null
                            }
                            if (accessibilityText != null) {
                                InfoRow(
                                    label = stringResource(R.string.label_accessibility),
                                    value = accessibilityText
                                )
                            }

                            if (!currentProgram.contentRating.isNullOrBlank()) {
                                val ratingClean = currentProgram.contentRating.trim()
                                val ratingDisplay = when (ratingClean) {
                                    "-10", "10" -> stringResource(R.string.csa_under_10)
                                    "-12", "12", "13" -> stringResource(R.string.csa_under_12)
                                    "-16", "16" -> stringResource(R.string.csa_under_16)
                                    "-18", "18" -> stringResource(R.string.csa_under_18)
                                    "Tout public", "TP", "G", "Tous publics" -> stringResource(R.string.csa_all_audiences)
                                    else -> ratingClean
                                }
                                InfoRow(
                                    label = stringResource(R.string.label_parental_rating),
                                    value = ratingDisplay
                                )
                            }

                            if (currentProgram.isHd) {
                                InfoRow(
                                    label = stringResource(R.string.label_video_format),
                                    value = stringResource(R.string.video_format_hd)
                                )
                            }

                            if (isSeriesProgram) {
                                if (currentProgram.productionYear != null) {
                                    InfoRow(
                                        label = stringResource(R.string.label_episode_release_year),
                                        value = currentProgram.productionYear.toString()
                                    )
                                }
                                if (currentProgram.seriesYear != null) {
                                    InfoRow(
                                        label = stringResource(R.string.label_series_release_year),
                                        value = currentProgram.seriesYear.toString()
                                    )
                                }
                            } else {
                                val releaseYear = currentProgram.productionYear ?: currentProgram.seriesYear
                                if (releaseYear != null) {
                                    InfoRow(
                                        label = stringResource(R.string.label_release_year),
                                        value = releaseYear.toString()
                                    )
                                }
                            }

                            val countryText = currentProgram.country
                                ?: if (currentProgram.channelId.endsWith(".fr")) {
                                    stringResource(R.string.default_country)
                                } else null

                            if (!countryText.isNullOrBlank()) {
                                InfoRow(
                                    label = stringResource(R.string.label_country),
                                    value = countryText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (reminder != null) {
                                    viewModel.toggleReminder()
                                } else {
                                    showLeadTimeOptions = !showLeadTimeOptions
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reminder != null) DarkSurfaceVariant else AccentRed
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (reminder != null) Icons.Default.AlarmOn else Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val reminderButtonText = if (reminder != null) {
                                val recurrenceText = when (reminder?.recurrence) {
                                    "daily" -> " (${stringResource(R.string.reminder_recurrence_daily)})"
                                    "weekly" -> " (${stringResource(R.string.reminder_recurrence_weekly)})"
                                    "monthly" -> " (${stringResource(R.string.reminder_recurrence_monthly)})"
                                    else -> ""
                                }
                                stringResource(R.string.reminder_active) + recurrenceText
                            } else {
                                stringResource(R.string.reminder_set)
                            }
                            Text(text = reminderButtonText)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = viewModel.createExternalCalendarIntent(currentProgram)
                                try {
                                    context.startActivity(intent)
                                } catch (_: android.content.ActivityNotFoundException) {
                                    val fallbackIntent = Intent(Intent.ACTION_EDIT).apply {
                                        setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.item/event")
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, currentProgram.startTimestamp)
                                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, currentProgram.stopTimestamp)
                                        putExtra(CalendarContract.Events.TITLE, currentProgram.title)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(fallbackIntent)
                                    } catch (_: android.content.ActivityNotFoundException) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.no_calendar_app_installed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_external_calendar),
                                color = TextPrimary
                            )
                        }
                    }

                    if (showLeadTimeOptions && reminder == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_reminder_lead_time),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0, 30, 60).forEach { mins ->
                                        val isChosen = selectedLeadTime == mins
                                        SuggestionChip(
                                            onClick = { selectedLeadTime = mins },
                                            label = {
                                                Text(
                                                    text = if (mins == 0) {
                                                        stringResource(R.string.reminder_exact_time)
                                                    } else {
                                                        stringResource(R.string.minutes_short, mins)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isChosen) AccentRed else TextPrimary
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (isChosen) DarkSurfaceVariant else Color.Transparent
                                            ),
                                            border = null
                                        )
                                    }

                                    SuggestionChip(
                                        onClick = {
                                            val cal = java.util.Calendar.getInstance().apply {
                                                timeInMillis = currentProgram.startTimestamp
                                            }
                                            android.app.DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    android.app.TimePickerDialog(
                                                        context,
                                                        { _, hour, minute ->
                                                            val picked = java.util.Calendar.getInstance().apply {
                                                                set(year, month, day, hour, minute, 0)
                                                            }
                                                            viewModel.setCustomDateTimeReminder(picked.timeInMillis, selectedRecurrence)
                                                            showLeadTimeOptions = false
                                                        },
                                                        cal.get(java.util.Calendar.HOUR_OF_DAY),
                                                        cal.get(java.util.Calendar.MINUTE),
                                                        true
                                                    ).show()
                                                },
                                                cal.get(java.util.Calendar.YEAR),
                                                cal.get(java.util.Calendar.MONTH),
                                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(R.string.action_pick_datetime),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AccentRed
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = DarkSurfaceVariant
                                        ),
                                        border = null
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.settings_reminder_recurrence),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "once" to stringResource(R.string.reminder_recurrence_once),
                                        "daily" to stringResource(R.string.reminder_recurrence_daily),
                                        "weekly" to stringResource(R.string.reminder_recurrence_weekly),
                                        "monthly" to stringResource(R.string.reminder_recurrence_monthly)
                                    ).forEach { (recurrenceKey, label) ->
                                        val isChosen = selectedRecurrence == recurrenceKey
                                        SuggestionChip(
                                            onClick = { selectedRecurrence = recurrenceKey },
                                            label = {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isChosen) AccentRed else TextPrimary
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (isChosen) DarkSurfaceVariant else Color.Transparent
                                            ),
                                            border = null
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        showLeadTimeOptions = false
                                        viewModel.toggleReminder(selectedLeadTime, selectedRecurrence)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = stringResource(R.string.action_confirm), color = Color.White)
                                }
                            }
                        }
                    }

                    val cleanedDescription = remember(currentProgram.description, currentProgram.title) {
                        ProgramDescriptionUtils.cleanWithTitlePrefix(currentProgram.description, currentProgram.title)
                    }

                    val cleanedSecondaryDescription = remember(currentProgram.secondaryDescription, currentProgram.title, cleanedDescription) {
                        val secondary = ProgramDescriptionUtils.cleanWithTitlePrefix(currentProgram.secondaryDescription, currentProgram.title)
                        if (secondary != null && !ProgramDescriptionUtils.isRedundant(cleanedDescription, secondary)) {
                            secondary
                        } else {
                            null
                        }
                    }

                    val primaryDescriptionHeader = when {
                        isSeriesProgram && !cleanedSecondaryDescription.isNullOrBlank() -> stringResource(R.string.section_episode_description)
                        isSeriesProgram && currentProgram.seasonNumber == null && currentProgram.episodeNumber == null -> stringResource(R.string.section_series_description)
                        else -> stringResource(R.string.section_description)
                    }

                    val secondaryDescriptionHeader = if (isSeriesProgram) {
                        stringResource(R.string.section_series_description)
                    } else {
                        stringResource(R.string.section_more_details)
                    }

                    if (!cleanedDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = primaryDescriptionHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SelectionContainer {
                            Text(
                                text = cleanedDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                            )
                        }
                    }

                    if (!cleanedSecondaryDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = secondaryDescriptionHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        SelectionContainer {
                            Text(
                                text = cleanedSecondaryDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                            )
                        }
                    }

                    if (upcomingBroadcasts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.section_upcoming_broadcasts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        upcomingBroadcasts.take(5).forEach { upcoming ->
                            val upStart = Instant.ofEpochMilli(upcoming.startTimestamp).atZone(zone).format(timeFormatter)
                            val upDate = Instant.ofEpochMilli(upcoming.startTimestamp).atZone(zone).format(dateFormatter)
                            val fullTitle = if (upcoming.seasonNumber != null) {
                                stringResource(
                                    R.string.label_season_with_title,
                                    upcoming.seasonNumber,
                                    upcoming.subTitle ?: upcoming.title
                                )
                            } else {
                                upcoming.subTitle ?: upcoming.title
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = upcoming.channelId,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.broadcast_at_time, upDate, upStart),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fullTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    val currentImageUrl = program?.iconUrl
    if (isImageEnlarged && !currentImageUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { isImageEnlarged = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentImageUrl,
                        contentDescription = program?.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = { isImageEnlarged = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newScale = (scale - 0.5f).coerceAtLeast(1f)
                            scale = newScale
                            if (newScale == 1f) offset = Offset.Zero
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                    IconButton(
                        onClick = {
                            scale = (scale + 0.5f).coerceAtMost(5f)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    if (scale > 1f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
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

