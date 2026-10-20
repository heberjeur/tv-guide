package com.music4music.tvguide.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.music4music.tvguide.data.local.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE :currentTimestamp >= startTimestamp AND :currentTimestamp < stopTimestamp ORDER BY channelId ASC")
    fun getNowPlaying(currentTimestamp: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE :currentTimestamp >= startTimestamp AND :currentTimestamp < stopTimestamp ORDER BY channelId ASC")
    suspend fun getNowPlayingDirect(currentTimestamp: Long): List<ProgramEntity>

    @Query("SELECT * FROM programs WHERE channelId = :channelId AND stopTimestamp > :dayStart AND startTimestamp < :dayEnd ORDER BY startTimestamp ASC")
    fun getProgramsForChannel(channelId: String, dayStart: Long, dayEnd: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id LIMIT 1")
    fun getProgramById(id: Long): Flow<ProgramEntity?>

    @Query("SELECT * FROM programs WHERE id = :id LIMIT 1")
    suspend fun getProgramByIdDirect(id: Long): ProgramEntity?

    @Query("SELECT * FROM programs WHERE channelId = :channelId AND startTimestamp = :startTimestamp LIMIT 1")
    suspend fun getProgramByChannelAndStart(channelId: String, startTimestamp: Long): ProgramEntity?

    @Query("SELECT * FROM programs WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR directors LIKE '%' || :query || '%' OR actors LIKE '%' || :query || '%' OR presenters LIKE '%' || :query || '%' OR guests LIKE '%' || :query || '%' ORDER BY startTimestamp ASC LIMIT 100")
    fun searchPrograms(query: String): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE directors LIKE '%' || :personName || '%' OR actors LIKE '%' || :personName || '%' OR presenters LIKE '%' || :personName || '%' OR guests LIKE '%' || :personName || '%' ORDER BY startTimestamp ASC")
    fun getProgramsByPerson(personName: String): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE title = :title AND startTimestamp > :fromTime ORDER BY startTimestamp ASC LIMIT 10")
    fun getUpcomingBroadcasts(title: String, fromTime: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE LOWER(title) = LOWER(:title) ORDER BY seasonNumber ASC, episodeNumber ASC, startTimestamp ASC")
    fun getProgramsByTitle(title: String): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE (:category = '' OR categories LIKE '%' || :category || '%') AND stopTimestamp > :fromTime ORDER BY startTimestamp ASC LIMIT 250")
    fun getProgramsByCategory(category: String, fromTime: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE (:category = '' OR categories LIKE '%' || :category || '%') AND (:channelId IS NULL OR channelId = :channelId) AND (:dayStart = 0 OR (stopTimestamp > :dayStart AND startTimestamp < :dayEnd)) AND stopTimestamp > :fromTime ORDER BY startTimestamp ASC LIMIT 1000")
    fun getFilteredPrograms(category: String, channelId: String?, dayStart: Long, dayEnd: Long, fromTime: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE stopTimestamp > :startTime AND startTimestamp < :endTime ORDER BY channelId ASC, startTimestamp ASC")
    fun getProgramsByTimeRange(startTime: Long, endTime: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE stopTimestamp > :startTime AND startTimestamp < :endTime ORDER BY channelId ASC, startTimestamp ASC")
    suspend fun getProgramsByTimeRangeDirect(startTime: Long, endTime: Long): List<ProgramEntity>

    @Query("SELECT * FROM programs WHERE stopTimestamp > :fromTime ORDER BY startTimestamp ASC")
    fun getAllActivePrograms(fromTime: Long): Flow<List<ProgramEntity>>

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun getProgramCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs WHERE stopTimestamp < :cutoffTimestamp")
    suspend fun deleteExpiredPrograms(cutoffTimestamp: Long)

    @Query("DELETE FROM programs WHERE channelId = :channelId")
    suspend fun deleteProgramsForChannel(channelId: String)

    @Query("DELETE FROM programs")
    suspend fun clearAll()
}
