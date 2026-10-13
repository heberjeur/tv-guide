package com.music4music.tvguide.data.mapper

import com.music4music.tvguide.data.local.entity.ChannelEntity
import com.music4music.tvguide.domain.model.Channel

fun ChannelEntity.toDomain(): Channel {
    return Channel(
        id = id,
        displayName = displayName,
        iconUrl = iconUrl,
        packageId = packageId,
        channelNumber = channelNumber,
        isFavorite = isFavorite
    )
}

fun Channel.toEntity(): ChannelEntity {
    return ChannelEntity(
        id = id,
        displayName = displayName,
        iconUrl = iconUrl,
        packageId = packageId,
        channelNumber = channelNumber,
        isFavorite = isFavorite
    )
}
