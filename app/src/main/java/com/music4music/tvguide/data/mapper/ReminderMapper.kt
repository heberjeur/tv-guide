package com.music4music.tvguide.data.mapper

import com.music4music.tvguide.data.local.entity.ReminderEntity
import com.music4music.tvguide.domain.model.Reminder

fun ReminderEntity.toDomain(): Reminder {
    return Reminder(
        id = id,
        programId = programId,
        channelId = channelId,
        title = title,
        startTimestamp = startTimestamp,
        triggerTimestamp = triggerTimestamp,
        leadTimeMinutes = leadTimeMinutes,
        isCalendarSyncEnabled = isCalendarSyncEnabled,
        calendarEventId = calendarEventId,
        isNotificationEnabled = isNotificationEnabled,
        soundUri = soundUri,
        recurrence = recurrence
    )
}

fun Reminder.toEntity(): ReminderEntity {
    return ReminderEntity(
        id = id,
        programId = programId,
        channelId = channelId,
        title = title,
        startTimestamp = startTimestamp,
        triggerTimestamp = triggerTimestamp,
        leadTimeMinutes = leadTimeMinutes,
        isCalendarSyncEnabled = isCalendarSyncEnabled,
        calendarEventId = calendarEventId,
        isNotificationEnabled = isNotificationEnabled,
        soundUri = soundUri,
        recurrence = recurrence
    )
}
