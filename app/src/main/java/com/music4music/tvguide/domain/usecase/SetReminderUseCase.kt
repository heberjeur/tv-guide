package com.music4music.tvguide.domain.usecase

import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.Reminder
import com.music4music.tvguide.domain.repository.ReminderRepository
import javax.inject.Inject

class SetReminderUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(
        program: Program,
        leadTimeMinutes: Int = 10,
        syncToCalendar: Boolean = false,
        soundUri: String? = null,
        recurrence: String = "once"
    ): Long {
        val triggerTimestamp = program.startTimestamp - (leadTimeMinutes * 60 * 1000L)
        val reminder = Reminder(
            programId = program.id,
            channelId = program.channelId,
            title = program.title,
            startTimestamp = program.startTimestamp,
            triggerTimestamp = triggerTimestamp,
            leadTimeMinutes = leadTimeMinutes,
            isCalendarSyncEnabled = syncToCalendar,
            soundUri = soundUri,
            recurrence = recurrence
        )
        return repository.saveReminder(reminder)
    }
}
