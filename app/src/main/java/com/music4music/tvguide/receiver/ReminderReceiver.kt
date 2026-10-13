package com.music4music.tvguide.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.music4music.tvguide.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val programId = intent.getLongExtra("extra_program_id", -1L)
        if (programId == -1L) return

        val title = intent.getStringExtra("extra_title") ?: ""
        val channelId = intent.getStringExtra("extra_channel_id") ?: ""
        val leadTime = intent.getIntExtra("extra_lead_time", 10)
        val soundUri = intent.getStringExtra("extra_sound_uri")

        notificationHelper.showReminderNotification(
            programId = programId,
            title = title,
            channelId = channelId,
            leadTimeMinutes = leadTime,
            customSoundUri = soundUri
        )
    }
}
