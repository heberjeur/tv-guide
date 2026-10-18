package com.music4music.tvguide.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.music4music.tvguide.domain.repository.ReminderRepository
import com.music4music.tvguide.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val programId = intent.getLongExtra("extra_program_id", -1L)
        if (programId == -1L) return

        val title = intent.getStringExtra("extra_title") ?: ""
        val channelId = intent.getStringExtra("extra_channel_id") ?: ""
        val leadTime = intent.getIntExtra("extra_lead_time", 10)
        val soundUri = intent.getStringExtra("extra_sound_uri")
        val vibrationEnabled = intent.getBooleanExtra("extra_vibration_enabled", true)

        notificationHelper.showReminderNotification(
            programId = programId,
            title = title,
            channelId = channelId,
            leadTimeMinutes = leadTime,
            customSoundUri = soundUri,
            vibrationEnabled = vibrationEnabled
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = reminderRepository.getReminder(programId) ?: return@launch
                if (reminder.recurrence == "once") return@launch

                val zone = ZoneId.systemDefault()
                val currentStart = Instant.ofEpochMilli(reminder.startTimestamp).atZone(zone)
                val nextStart = when (reminder.recurrence) {
                    "daily" -> currentStart.plusDays(1)
                    "weekly" -> currentStart.plusWeeks(1)
                    "monthly" -> currentStart.plusMonths(1)
                    else -> null
                } ?: return@launch

                val delta = nextStart.toInstant().toEpochMilli() - reminder.startTimestamp
                val updatedReminder = reminder.copy(
                    startTimestamp = reminder.startTimestamp + delta,
                    triggerTimestamp = reminder.triggerTimestamp + delta
                )
                reminderRepository.saveReminder(updatedReminder)

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val nextIntent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("extra_program_id", updatedReminder.programId)
                    putExtra("extra_title", updatedReminder.title)
                    putExtra("extra_channel_id", updatedReminder.channelId)
                    putExtra("extra_lead_time", updatedReminder.leadTimeMinutes)
                    putExtra("extra_sound_uri", updatedReminder.soundUri)
                    putExtra("extra_vibration_enabled", vibrationEnabled)
                }

                val nextPendingIntent = PendingIntent.getBroadcast(
                    context,
                    updatedReminder.programId.toInt(),
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        updatedReminder.triggerTimestamp,
                        nextPendingIntent
                    )
                } catch (_: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        updatedReminder.triggerTimestamp,
                        nextPendingIntent
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
