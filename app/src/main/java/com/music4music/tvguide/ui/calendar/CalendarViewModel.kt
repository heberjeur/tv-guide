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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now(ZoneId.of("Europe/Paris")))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allReminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredReminders: StateFlow<List<Reminder>> = combine(allReminders, _selectedDate, _searchQuery) { reminders, date, query ->
        val zone = ZoneId.of("Europe/Paris")
        reminders.filter { reminder ->
            val reminderDate = Instant.ofEpochMilli(reminder.startTimestamp).atZone(zone).toLocalDate()
            val matchesDate = reminderDate == date
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
