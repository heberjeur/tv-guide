package com.music4music.tvguide.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun addEventToCalendar(
        title: String,
        description: String,
        startTimestamp: Long,
        stopTimestamp: Long,
        leadTimeMinutes: Int
    ): Long? {
        val calendarId = getPrimaryCalendarId() ?: return null

        val eventValues = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTimestamp)
            put(CalendarContract.Events.DTEND, stopTimestamp)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        val eventUri = try {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues)
        } catch (_: SecurityException) {
            null
        } ?: return null

        val eventId = eventUri.lastPathSegment?.toLongOrNull() ?: return null

        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.MINUTES, leadTimeMinutes)
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }

        try {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        } catch (_: SecurityException) {
        }

        return eventId
    }

    fun removeEventFromCalendar(eventId: Long): Boolean {
        val deleteUri: Uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return try {
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (_: SecurityException) {
            false
        }
    }

    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"

        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                } else {
                    null
                }
            }
        } catch (_: SecurityException) {
            null
        }
    }
}
