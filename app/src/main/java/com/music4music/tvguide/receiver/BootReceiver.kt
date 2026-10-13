package com.music4music.tvguide.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.music4music.tvguide.domain.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val upcoming = reminderRepository.getUpcomingReminders(now)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                for (reminder in upcoming) {
                    val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
                        putExtra("extra_program_id", reminder.programId)
                        putExtra("extra_title", reminder.title)
                        putExtra("extra_channel_id", reminder.channelId)
                        putExtra("extra_lead_time", reminder.leadTimeMinutes)
                        putExtra("extra_sound_uri", reminder.soundUri)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminder.programId.toInt(),
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.triggerTimestamp,
                            pendingIntent
                        )
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.triggerTimestamp,
                            pendingIntent
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
