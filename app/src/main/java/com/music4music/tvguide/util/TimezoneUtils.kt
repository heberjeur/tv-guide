package com.music4music.tvguide.util

import java.time.ZoneId

object TimezoneUtils {
    val ORIGIN_ZONE: ZoneId = ZoneId.of("Europe/Paris")

    const val DEFAULT_TIMEZONE_PREF = "system"

    val AVAILABLE_TIMEZONES = listOf(
        "origin",
        "system",
        "GMT-5",
        "GMT-4",
        "GMT-3",
        "GMT+0",
        "GMT+1",
        "GMT+2",
        "GMT+3",
        "GMT+4",
        "GMT+5",
        "GMT+6",
        "GMT+7",
        "GMT+8",
        "GMT+9",
        "GMT+10",
        "GMT+11",
        "GMT+12"
    )

    fun resolveZoneId(pref: String?): ZoneId {
        return when (pref) {
            null, "", "origin", "Europe/Paris" -> ORIGIN_ZONE
            "system" -> ZoneId.systemDefault()
            else -> runCatching { ZoneId.of(pref) }.getOrDefault(ORIGIN_ZONE)
        }
    }
}
