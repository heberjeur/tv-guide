package com.music4music.tvguide.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music4music.tvguide.R
import com.music4music.tvguide.ui.theme.AccentRed
import com.music4music.tvguide.ui.theme.DarkBackground
import com.music4music.tvguide.ui.theme.DarkSurface
import com.music4music.tvguide.ui.theme.DarkSurfaceVariant
import com.music4music.tvguide.ui.theme.TextPrimary
import com.music4music.tvguide.ui.theme.TextSecondary

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val selectedPackage by viewModel.selectedPackage.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val notificationSound by viewModel.notificationSound.collectAsStateWithLifecycle()
    val reminderLeadTime by viewModel.reminderLeadTime.collectAsStateWithLifecycle()
    val syncFrequencyHours by viewModel.syncFrequencyHours.collectAsStateWithLifecycle()
    val syncWifiOnly by viewModel.syncWifiOnly.collectAsStateWithLifecycle()
    val syncChargingOnly by viewModel.syncChargingOnly.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val cacheSizeText by viewModel.cacheSizeText.collectAsStateWithLifecycle()

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val displayDensity by viewModel.displayDensity.collectAsStateWithLifecycle()
    val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
    val hideReruns by viewModel.hideReruns.collectAsStateWithLifecycle()
    val channelSortOrder by viewModel.channelSortOrder.collectAsStateWithLifecycle()
    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
    val hiddenChannels by viewModel.hiddenChannels.collectAsStateWithLifecycle()
    val retentionDays by viewModel.retentionDays.collectAsStateWithLifecycle()
    val imageDownloadPolicy by viewModel.imageDownloadPolicy.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val highlightAccessibility by viewModel.highlightAccessibility.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()

    var showCustomLeadTimeDialog by remember { mutableStateOf(false) }
    var customLeadTimeInput by remember { mutableStateOf("") }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var showHiddenDialog by remember { mutableStateOf(false) }

    val soundDisplayName = remember(notificationSound) {
        if (notificationSound != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(notificationSound))
                ringtone?.getTitle(context) ?: context.getString(R.string.settings_sound_default)
            } catch (_: Exception) {
                context.getString(R.string.settings_sound_default)
            }
        } else {
            context.getString(R.string.settings_sound_default)
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            viewModel.setNotificationSound(uri?.toString())
        }
    }

    if (showCustomLeadTimeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomLeadTimeDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_custom_lead_time_dialog_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = customLeadTimeInput,
                        onValueChange = { customLeadTimeInput = it.filter { char -> char.isDigit() } },
                        label = { Text(text = stringResource(R.string.settings_custom_lead_time_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentRed,
                            unfocusedLabelColor = TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = customLeadTimeInput.toIntOrNull()
                        if (minutes != null && minutes >= 0) {
                            viewModel.setLeadTime(minutes)
                        }
                        showCustomLeadTimeDialog = false
                    }
                ) {
                    Text(text = stringResource(R.string.action_confirm), color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomLeadTimeDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { showFavoritesDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_select_favorites_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(allChannels, key = { it.id }) { channel ->
                        val isFav = favoriteChannels.contains(channel.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleFavoriteChannel(channel.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isFav,
                                onCheckedChange = { viewModel.toggleFavoriteChannel(channel.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentRed,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channel.displayName,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFavoritesDialog = false }) {
                    Text(text = stringResource(R.string.action_confirm), color = AccentRed)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showHiddenDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_select_hidden_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(allChannels, key = { it.id }) { channel ->
                        val isHidden = hiddenChannels.contains(channel.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleHiddenChannel(channel.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isHidden,
                                onCheckedChange = { viewModel.toggleHiddenChannel(channel.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentRed,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channel.displayName,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenDialog = false }) {
                    Text(text = stringResource(R.string.action_confirm), color = AccentRed)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tab_settings), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.package_selection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_package_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "community_enriched" to stringResource(R.string.package_community_enriched),
                            "terrestrial" to stringResource(R.string.package_terrestrial),
                            "extended" to stringResource(R.string.package_extended),
                            "all_channels" to stringResource(R.string.package_all_channels)
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = selectedPackage == id,
                                onClick = { viewModel.selectPackage(id) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "dark" to stringResource(R.string.theme_dark),
                            "oled" to stringResource(R.string.theme_oled),
                            "light" to stringResource(R.string.theme_light),
                            "system" to stringResource(R.string.theme_system)
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_density_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "comfort" to stringResource(R.string.density_comfort),
                            "compact" to stringResource(R.string.density_compact)
                        ).forEach { (density, label) ->
                            FilterChip(
                                selected = displayDensity == density,
                                onClick = { viewModel.setDisplayDensity(density) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_font_scale_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "normal" to stringResource(R.string.font_scale_normal),
                            "large" to stringResource(R.string.font_scale_large)
                        ).forEach { (scale, label) ->
                            FilterChip(
                                selected = fontSizeScale == scale,
                                onClick = { viewModel.setFontSizeScale(scale) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_hide_reruns),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = hideReruns,
                            onCheckedChange = viewModel::setHideReruns,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_highlight_accessibility),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_highlight_accessibility_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = highlightAccessibility,
                            onCheckedChange = viewModel::setHighlightAccessibility,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentRed
                            )
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_channel_management_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_channel_sort_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "popular" to stringResource(R.string.channel_sort_popular),
                            "number" to stringResource(R.string.channel_sort_number),
                            "name" to stringResource(R.string.channel_sort_name),
                            "favorites_first" to stringResource(R.string.channel_sort_favorites_first)
                        ).forEach { (order, label) ->
                            FilterChip(
                                selected = channelSortOrder == order,
                                onClick = { viewModel.setChannelSortOrder(order) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFavoritesDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_manage_favorites, favoriteChannels.size),
                                color = TextPrimary
                            )
                        }
                        OutlinedButton(
                            onClick = { showHiddenDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_manage_hidden, hiddenChannels.size),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = appLanguage == "fr",
                            onClick = { viewModel.setLanguage("fr", activity) },
                            label = { Text(text = stringResource(R.string.language_french)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRed,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = appLanguage == "en",
                            onClick = { viewModel.setLanguage("en", activity) },
                            label = { Text(text = stringResource(R.string.language_english)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRed,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_notifications_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_notifications_enable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = isNotificationsEnabled,
                            onCheckedChange = viewModel::toggleNotifications,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentRed
                            )
                        )
                    }

                    if (isNotificationsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_notification_sound),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = soundDisplayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                        notificationSound?.let {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                        }
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_vibration_enable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = viewModel::setVibrationEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = AccentRed
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.settings_reminder_lead_time),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val presetLeadTimes = listOf(0, 5, 10, 15, 30)
                        val isCustomLeadTime = !presetLeadTimes.contains(reminderLeadTime)

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetLeadTimes.forEach { minutes ->
                                val label = if (minutes == 0) {
                                    stringResource(R.string.reminder_exact_time)
                                } else {
                                    stringResource(R.string.minutes_short, minutes)
                                }
                                FilterChip(
                                    selected = reminderLeadTime == minutes,
                                    onClick = { viewModel.setLeadTime(minutes) },
                                    label = { Text(text = label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentRed,
                                        selectedLabelColor = TextPrimary,
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = TextSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            val customChipLabel = if (isCustomLeadTime) {
                                stringResource(R.string.label_custom_format, reminderLeadTime)
                            } else {
                                stringResource(R.string.label_custom)
                            }
                            FilterChip(
                                selected = isCustomLeadTime,
                                onClick = {
                                    customLeadTimeInput = if (isCustomLeadTime) reminderLeadTime.toString() else "7"
                                    showCustomLeadTimeDialog = true
                                },
                                label = { Text(text = customChipLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_sync_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_sync_frequency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            6 to stringResource(R.string.sync_freq_6h),
                            12 to stringResource(R.string.sync_freq_12h),
                            24 to stringResource(R.string.sync_freq_24h),
                            0 to stringResource(R.string.sync_freq_manual)
                        ).forEach { (hours, label) ->
                            FilterChip(
                                selected = syncFrequencyHours == hours,
                                onClick = { viewModel.setSyncFrequency(hours) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_sync_auto_background),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = viewModel::setAutoSync,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_sync_wifi_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = syncWifiOnly,
                            onCheckedChange = viewModel::setSyncWifiOnly,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_sync_charging_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Switch(
                            checked = syncChargingOnly,
                            onCheckedChange = viewModel::setSyncChargingOnly,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentRed
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_retention_days_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            3 to stringResource(R.string.retention_3_days),
                            7 to stringResource(R.string.retention_7_days),
                            14 to stringResource(R.string.retention_14_days)
                        ).forEach { (days, label) ->
                            FilterChip(
                                selected = retentionDays == days,
                                onClick = { viewModel.setRetentionDays(days) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_image_download_policy),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "always" to stringResource(R.string.image_policy_always),
                            "wifi_only" to stringResource(R.string.image_policy_wifi_only),
                            "never" to stringResource(R.string.image_policy_never)
                        ).forEach { (policy, label) ->
                            FilterChip(
                                selected = imageDownloadPolicy == policy,
                                onClick = { viewModel.setImageDownloadPolicy(policy) },
                                label = { Text(text = label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentRed,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.triggerSyncNow() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.action_sync_now), color = TextPrimary)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_storage_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_cache_size, cacheSizeText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.clearCache() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = stringResource(R.string.settings_clear_cache), color = TextPrimary)
                    }
                }
            }
        }
    }
}
