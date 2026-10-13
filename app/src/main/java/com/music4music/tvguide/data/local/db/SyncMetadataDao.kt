package com.music4music.tvguide.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.music4music.tvguide.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE packageId = :packageId LIMIT 1")
    fun getSyncMetadata(packageId: String): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE packageId = :packageId LIMIT 1")
    suspend fun getSyncMetadataDirect(packageId: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadataEntity)
}
