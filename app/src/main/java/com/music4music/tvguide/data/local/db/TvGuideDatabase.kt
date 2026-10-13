package com.music4music.tvguide.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.music4music.tvguide.data.local.entity.ChannelEntity
import com.music4music.tvguide.data.local.entity.ProgramEntity
import com.music4music.tvguide.data.local.entity.ReminderEntity
import com.music4music.tvguide.data.local.entity.SyncMetadataEntity

@Database(
    entities = [
        ChannelEntity::class,
        ProgramEntity::class,
        ReminderEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TvGuideDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun programDao(): ProgramDao
    abstract fun reminderDao(): ReminderDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
