package com.music4music.tvguide.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music4music.tvguide.domain.model.Reminder
import com.music4music.tvguide.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val userPreferences: com.music4music.tvguide.data.local.prefs.UserPreferences
) : ViewModel() {

    private val displayZoneFlow = userPreferences.displayTimezone.map {
        com.music4music.tvguide.util.TimezoneUtils.resolveZoneId(it)
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now(com.music4music.tvguide.util.TimezoneUtils.ORIGIN_ZONE))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allReminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredReminders: StateFlow<List<Reminder>> = combine(allReminders, _selectedDate, _searchQuery, displayZoneFlow) { reminders, date, query, zone ->
        reminders.filter { reminder ->
            val reminderDate = Instant.ofEpochMilli(reminder.startTimestamp).atZone(zone).toLocalDate()
            val matchesDate = when (reminder.recurrence) {
                "daily" -> date >= reminderDate
                "weekly" -> date >= reminderDate && java.time.temporal.ChronoUnit.DAYS.between(reminderDate, date) % 7 == 0L
                "monthly" -> date >= reminderDate && date.dayOfMonth == reminderDate.dayOfMonth
                else -> reminderDate == date
            }
            val matchesQuery = query.isBlank() ||
                reminder.title.contains(query, ignoreCase = true) ||
                reminder.channelId.contains(query, ignoreCase = true)
            matchesDate && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun removeReminder(programId: Long) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(programId)
        }
    }
}
