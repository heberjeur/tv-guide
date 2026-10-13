package com.music4music.tvguide.domain.model

enum class SyncState {
    IDLE,
    DOWNLOADING,
    PARSING,
    CACHING_IMAGES,
    SUCCESS,
    ERROR
}

data class SyncProgress(
    val state: SyncState = SyncState.IDLE,
    val programCount: Int = 0,
    val lastSyncTimestamp: Long = 0L
)
