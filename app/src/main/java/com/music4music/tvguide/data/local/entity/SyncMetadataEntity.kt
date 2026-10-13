package com.music4music.tvguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val packageId: String,
    val lastSyncTimestamp: Long,
    val etag: String?,
    val programCount: Int,
    val isSuccess: Boolean
)
