package com.music4music.tvguide.data.repository

import com.music4music.tvguide.data.local.db.ReminderDao
import com.music4music.tvguide.data.mapper.toDomain
import com.music4music.tvguide.data.mapper.toEntity
import com.music4music.tvguide.domain.model.Reminder
import com.music4music.tvguide.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getReminderForProgram(programId: Long): Flow<Reminder?> {
        return reminderDao.getReminderForProgram(programId).map { it?.toDomain() }
    }

    override suspend fun getUpcomingReminders(currentTimestamp: Long): List<Reminder> {
        return reminderDao.getUpcomingReminders(currentTimestamp).map { it.toDomain() }
    }

    override suspend fun saveReminder(reminder: Reminder): Long {
        return reminderDao.insertReminder(reminder.toEntity())
    }

    override suspend fun deleteReminder(programId: Long) {
        reminderDao.deleteReminderByProgramId(programId)
    }

    override suspend fun deleteExpiredReminders(cutoffTimestamp: Long) {
        reminderDao.deleteExpiredReminders(cutoffTimestamp)
    }
}
