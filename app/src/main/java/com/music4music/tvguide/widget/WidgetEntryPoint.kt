package com.music4music.tvguide.widget

import com.music4music.tvguide.data.local.db.ProgramDao
import com.music4music.tvguide.data.local.prefs.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun programDao(): ProgramDao
    fun userPreferences(): UserPreferences
}
