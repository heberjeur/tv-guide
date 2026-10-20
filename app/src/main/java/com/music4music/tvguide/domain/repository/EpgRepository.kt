package com.music4music.tvguide.domain.repository

import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.model.Program
import kotlinx.coroutines.flow.Flow

interface EpgRepository {
    fun getChannels(channelPackage: ChannelPackage): Flow<List<Channel>>
    fun getFavoriteChannels(channelPackage: ChannelPackage): Flow<List<Channel>>
    fun getNowPlaying(): Flow<List<Program>>
    fun getProgramsForChannel(channelId: String, dayStart: Long, dayEnd: Long): Flow<List<Program>>
    fun getProgramById(programId: Long): Flow<Program?>
    fun searchPrograms(query: String): Flow<List<Program>>
    fun getUpcomingBroadcasts(title: String): Flow<List<Program>>
    fun getProgramsByTitle(title: String): Flow<List<Program>>
    fun getProgramsByCategory(category: String): Flow<List<Program>>
    fun getFilteredPrograms(category: String, channelId: String?, dayStart: Long, dayEnd: Long): Flow<List<Program>>
    fun getProgramsByTimeRange(startTime: Long, endTime: Long): Flow<List<Program>>
    fun getAllActivePrograms(): Flow<List<Program>>
    fun getProgramsByPerson(personName: String): Flow<List<Program>>
    fun getSyncProgress(): kotlinx.coroutines.flow.StateFlow<com.music4music.tvguide.domain.model.SyncProgress>
    suspend fun toggleFavoriteChannel(channelId: String, isFavorite: Boolean)
    suspend fun syncEpg(channelPackage: ChannelPackage, forceRefresh: Boolean = false): Result<Unit>
    fun getLastSyncTimestamp(channelPackage: ChannelPackage): Flow<Long?>
    fun resetSyncProgress()
}
