package com.music4music.tvguide.domain.model

data class Channel(
    val id: String,
    val displayName: String,
    val iconUrl: String?,
    val packageId: String,
    val channelNumber: Int = 0,
    val isFavorite: Boolean = false
)
