package com.music4music.tvguide.ui.detail

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.Reminder
import com.music4music.tvguide.domain.repository.EpgRepository
import com.music4music.tvguide.domain.repository.ReminderRepository
import com.music4music.tvguide.domain.usecase.GetProgramDetailUseCase
import com.music4music.tvguide.domain.usecase.SetReminderUseCase
import com.music4music.tvguide.receiver.ReminderReceiver
import com.music4music.tvguide.data.remote.TrailerVideo
import com.music4music.tvguide.data.remote.YouTubeTrailerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getProgramDetailUseCase: GetProgramDetailUseCase,
    private val setReminderUseCase: SetReminderUseCase,
    private val reminderRepository: ReminderRepository,
    private val epgRepository: EpgRepository,
    private val userPreferences: UserPreferences,
    private val youTubeTrailerService: YouTubeTrailerService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val programId: Long = checkNotNull(savedStateHandle["programId"])

    private val _trailers = MutableStateFlow<List<TrailerVideo>>(emptyList())
    val trailers: StateFlow<List<TrailerVideo>> = _trailers.asStateFlow()

    private val _selectedTrailer = MutableStateFlow<TrailerVideo?>(null)
    val selectedTrailer: StateFlow<TrailerVideo?> = _selectedTrailer.asStateFlow()

    private val _isTrailerLoading = MutableStateFlow(false)
    val isTrailerLoading: StateFlow<Boolean> = _isTrailerLoading.asStateFlow()

    private val _isTrailerPlayerVisible = MutableStateFlow(false)
    val isTrailerPlayerVisible: StateFlow<Boolean> = _isTrailerPlayerVisible.asStateFlow()

    private val _trailerLoadError = MutableStateFlow(false)
    val trailerLoadError: StateFlow<Boolean> = _trailerLoadError.asStateFlow()

    val program: StateFlow<Program?> = getProgramDetailUseCase(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reminder: StateFlow<Reminder?> = reminderRepository.getReminderForProgram(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val defaultLeadTimeMinutes: StateFlow<Int> = userPreferences.reminderLeadTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val defaultReminderRecurrence: StateFlow<String> = userPreferences.reminderRecurrence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "once")

    val upcomingBroadcasts: StateFlow<List<Program>> = program.flatMapLatest { prog ->
        if (prog != null && prog.title.isNotBlank()) {
            epgRepository.getUpcomingBroadcasts(prog.title).map { list ->
                com.music4music.tvguide.util.SeriesUtils.normalizeEpisodes(list)
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleReminder(customLeadTime: Int? = null, customRecurrence: String? = null) {
        val currentProgram = program.value ?: return
        val currentReminder = reminder.value

        viewModelScope.launch {
            if (currentReminder != null) {
                cancelAlarm(currentReminder)
                reminderRepository.deleteReminder(programId)
            } else {
                val leadTime = customLeadTime ?: userPreferences.reminderLeadTimeMinutes.first()
                val recurrence = customRecurrence ?: userPreferences.reminderRecurrence.first()
                val soundUri = userPreferences.notificationSound.first()

                setReminderUseCase(
                    program = currentProgram,
                    leadTimeMinutes = leadTime,
                    syncToCalendar = false,
                    soundUri = soundUri,
                    recurrence = recurrence
                )

                scheduleAlarm(
                    program = currentProgram,
                    leadTime = leadTime,
                    soundUri = soundUri
                )
            }
        }
    }

    fun setCustomDateTimeReminder(triggerTimestamp: Long, recurrence: String = "once") {
        val currentProgram = program.value ?: return
        viewModelScope.launch {
            val existingReminder = reminder.value
            if (existingReminder != null) {
                cancelAlarm(existingReminder)
                reminderRepository.deleteReminder(existingReminder.programId)
            }
            val soundUri = userPreferences.notificationSound.first()
            val leadTime = ((currentProgram.startTimestamp - triggerTimestamp) / 60000L).toInt().coerceAtLeast(0)

            setReminderUseCase(
                program = currentProgram,
                leadTimeMinutes = leadTime,
                syncToCalendar = false,
                soundUri = soundUri,
                recurrence = recurrence
            )

            scheduleAlarm(
                program = currentProgram,
                leadTime = leadTime,
                soundUri = soundUri,
                explicitTriggerTime = triggerTimestamp
            )
        }
    }

    fun createExternalCalendarIntent(program: Program): Intent {
        val desc = buildString {
            if (!program.subTitle.isNullOrBlank()) append(program.subTitle).append("\n\n")
            if (program.description.isNotBlank()) append(program.description)
        }
        return Intent(Intent.ACTION_INSERT).apply {
            setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.item/event")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, program.startTimestamp)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, program.stopTimestamp)
            putExtra(CalendarContract.Events.TITLE, program.title)
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
            putExtra(CalendarContract.Events.EVENT_LOCATION, program.channelId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private suspend fun scheduleAlarm(
        program: Program,
        leadTime: Int,
        soundUri: String?,
        explicitTriggerTime: Long? = null
    ) {
        val triggerTime = explicitTriggerTime ?: (program.startTimestamp - (leadTime * 60 * 1000L))
        if (triggerTime <= System.currentTimeMillis()) return

        val vibrationEnabled = userPreferences.vibrationEnabled.first()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("extra_program_id", program.id)
            putExtra("extra_title", program.title)
            putExtra("extra_channel_id", program.channelId)
            putExtra("extra_lead_time", leadTime)
            putExtra("extra_sound_uri", soundUri)
            putExtra("extra_vibration_enabled", vibrationEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            program.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun loadTrailers(onEmpty: () -> Unit = {}) {
        val current = program.value ?: return
        if (_trailers.value.isNotEmpty()) {
            _isTrailerPlayerVisible.value = true
            return
        }
        if (_isTrailerLoading.value) return

        viewModelScope.launch {
            _isTrailerLoading.value = true
            _trailerLoadError.value = false
            try {
                val list = youTubeTrailerService.searchTrailers(current.title, current.productionYear ?: current.seriesYear)
                _trailers.value = list
                if (list.isNotEmpty()) {
                    _selectedTrailer.value = list.first()
                    _isTrailerPlayerVisible.value = true
                } else {
                    _isTrailerPlayerVisible.value = false
                    onEmpty()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProgramDetailViewModel", "Error fetching trailers", e)
                _trailerLoadError.value = true
                _isTrailerPlayerVisible.value = false
                onEmpty()
            } finally {
                _isTrailerLoading.value = false
            }
        }
    }

    fun selectTrailer(trailer: TrailerVideo) {
        _selectedTrailer.value = trailer
        _isTrailerPlayerVisible.value = true
    }

    fun toggleTrailerPlayer() {
        if (_isTrailerPlayerVisible.value) {
            _isTrailerPlayerVisible.value = false
        } else {
            loadTrailers()
        }
    }

    fun closeTrailerPlayer() {
        _isTrailerPlayerVisible.value = false
    }

    private fun cancelAlarm(reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.programId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
