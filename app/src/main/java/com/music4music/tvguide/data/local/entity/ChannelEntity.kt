package com.music4music.tvguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val iconUrl: String?,
    val packageId: String,
    val channelNumber: Int = 0,
    val isFavorite: Boolean = false
)
