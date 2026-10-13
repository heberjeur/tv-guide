package com.music4music.tvguide.domain.model

data class Reminder(
    val id: Long = 0,
    val programId: Long,
    val channelId: String,
    val title: String,
    val startTimestamp: Long,
    val triggerTimestamp: Long,
    val leadTimeMinutes: Int = 10,
    val isCalendarSyncEnabled: Boolean = false,
    val calendarEventId: Long? = null,
    val isNotificationEnabled: Boolean = true,
    val soundUri: String? = null
)
