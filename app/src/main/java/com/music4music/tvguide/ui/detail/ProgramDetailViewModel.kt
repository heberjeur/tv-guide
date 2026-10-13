package com.music4music.tvguide.ui.detail

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.calendar.CalendarHelper
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.Reminder
import com.music4music.tvguide.domain.repository.ReminderRepository
import com.music4music.tvguide.domain.usecase.GetProgramDetailUseCase
import com.music4music.tvguide.domain.usecase.SetReminderUseCase
import com.music4music.tvguide.receiver.ReminderReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getProgramDetailUseCase: GetProgramDetailUseCase,
    private val setReminderUseCase: SetReminderUseCase,
    private val reminderRepository: ReminderRepository,
    private val calendarHelper: CalendarHelper,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val programId: Long = checkNotNull(savedStateHandle["programId"])

    val program: StateFlow<Program?> = getProgramDetailUseCase(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reminder: StateFlow<Reminder?> = reminderRepository.getReminderForProgram(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleReminder() {
        val currentProgram = program.value ?: return
        val currentReminder = reminder.value

        viewModelScope.launch {
            if (currentReminder != null) {
                cancelAlarm(currentReminder)
                if (currentReminder.calendarEventId != null) {
                    calendarHelper.removeEventFromCalendar(currentReminder.calendarEventId)
                }
                reminderRepository.deleteReminder(programId)
            } else {
                val leadTime = userPreferences.reminderLeadTimeMinutes.first()
                val syncCalendar = userPreferences.isCalendarSyncEnabled.first()
                val soundUri = userPreferences.notificationSound.first()

                var calendarId: Long? = null
                if (syncCalendar) {
                    calendarId = calendarHelper.addEventToCalendar(
                        title = currentProgram.title,
                        description = currentProgram.description,
                        startTimestamp = currentProgram.startTimestamp,
                        stopTimestamp = currentProgram.stopTimestamp,
                        leadTimeMinutes = leadTime
                    )
                }

                setReminderUseCase(
                    program = currentProgram,
                    leadTimeMinutes = leadTime,
                    syncToCalendar = syncCalendar,
                    soundUri = soundUri
                )

                scheduleAlarm(
                    program = currentProgram,
                    leadTime = leadTime,
                    soundUri = soundUri
                )
            }
        }
    }

    private fun scheduleAlarm(program: Program, leadTime: Int, soundUri: String?) {
        val triggerTime = program.startTimestamp - (leadTime * 60 * 1000L)
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("extra_program_id", program.id)
            putExtra("extra_title", program.title)
            putExtra("extra_channel_id", program.channelId)
            putExtra("extra_lead_time", leadTime)
            putExtra("extra_sound_uri", soundUri)
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
        }
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
