package com.music4music.tvguide.di

import android.content.Context
import androidx.room.Room
import com.music4music.tvguide.data.local.db.ChannelDao
import com.music4music.tvguide.data.local.db.ProgramDao
import com.music4music.tvguide.data.local.db.ReminderDao
import com.music4music.tvguide.data.local.db.SyncMetadataDao
import com.music4music.tvguide.data.local.db.TvGuideDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTvGuideDatabase(@ApplicationContext context: Context): TvGuideDatabase {
        return Room.databaseBuilder(
            context,
            TvGuideDatabase::class.java,
            "tv_guide.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChannelDao(database: TvGuideDatabase): ChannelDao = database.channelDao()

    @Provides
    fun provideProgramDao(database: TvGuideDatabase): ProgramDao = database.programDao()

    @Provides
    fun provideReminderDao(database: TvGuideDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideSyncMetadataDao(database: TvGuideDatabase): SyncMetadataDao = database.syncMetadataDao()
}
