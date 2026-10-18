package com.music4music.tvguide.domain.repository

import com.music4music.tvguide.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getReminderForProgram(programId: Long): Flow<Reminder?>
    suspend fun getReminder(programId: Long): Reminder?
    suspend fun getUpcomingReminders(currentTimestamp: Long): List<Reminder>
    suspend fun saveReminder(reminder: Reminder): Long
    suspend fun deleteReminder(programId: Long)
    suspend fun deleteExpiredReminders(cutoffTimestamp: Long)
}
