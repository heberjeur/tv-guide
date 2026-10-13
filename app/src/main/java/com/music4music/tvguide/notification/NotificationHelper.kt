package com.music4music.tvguide.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.music4music.tvguide.MainActivity
import com.music4music.tvguide.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID_DEFAULT = "tv_guide_reminders"
        const val CHANNEL_ID_CUSTOM_SOUND = "tv_guide_reminders_custom"
    }

    init {
        createDefaultChannel()
    }

    fun showReminderNotification(
        programId: Long,
        title: String,
        channelId: String,
        leadTimeMinutes: Int,
        customSoundUri: String? = null
    ) {
        val channelIdToUse = if (customSoundUri != null) {
            createCustomSoundChannel(customSoundUri)
            CHANNEL_ID_CUSTOM_SOUND
        } else {
            CHANNEL_ID_DEFAULT
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_program_id", programId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            programId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = context.getString(
            R.string.reminder_starting_soon,
            title,
            leadTimeMinutes,
            channelId
        )

        val notification = NotificationCompat.Builder(context, channelIdToUse)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(programId.toInt(), notification)
    }

    private fun createDefaultChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                context.getString(R.string.reminder_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.settings_notifications_title)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createCustomSoundChannel(soundUriString: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse(soundUriString)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID_CUSTOM_SOUND,
                context.getString(R.string.settings_notification_sound),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
