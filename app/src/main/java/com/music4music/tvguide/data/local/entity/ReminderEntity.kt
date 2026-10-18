package com.music4music.tvguide.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["programId"], unique = true),
        Index(value = ["triggerTimestamp"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programId: Long,
    val channelId: String,
    val title: String,
    val startTimestamp: Long,
    val triggerTimestamp: Long,
    val leadTimeMinutes: Int,
    val isCalendarSyncEnabled: Boolean,
    val calendarEventId: Long?,
    val isNotificationEnabled: Boolean,
    val soundUri: String?,
    @androidx.room.ColumnInfo(defaultValue = "'once'")
    val recurrence: String = "once"
)
