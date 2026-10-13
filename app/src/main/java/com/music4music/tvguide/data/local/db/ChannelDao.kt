package com.music4music.tvguide.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.music4music.tvguide.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE packageId = :packageId ORDER BY channelNumber ASC, displayName ASC")
    fun getChannelsByPackage(packageId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE packageId = :packageId AND isFavorite = 1 ORDER BY channelNumber ASC")
    fun getFavoriteChannels(packageId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun updateFavoriteStatus(channelId: String, isFavorite: Boolean)

    @Query("DELETE FROM channels WHERE packageId = :packageId")
    suspend fun deleteChannelsByPackage(packageId: String)
}
