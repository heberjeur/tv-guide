package com.music4music.tvguide.data.mapper

import com.music4music.tvguide.data.local.entity.ProgramEntity
import com.music4music.tvguide.domain.model.Credit
import com.music4music.tvguide.domain.model.Program

fun ProgramEntity.toDomain(): Program {
    return Program(
        id = id,
        channelId = channelId,
        startTimestamp = startTimestamp,
        stopTimestamp = stopTimestamp,
        title = title,
        subTitle = subTitle,
        description = description,
        categories = categories,
        iconUrl = iconUrl,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        episodeDisplay = episodeDisplay,
        credit = Credit(
            presenters = presenters,
            actors = actors,
            guests = guests,
            directors = directors,
            producers = producers,
            writers = writers
        ),
        contentRating = contentRating,
        starRating = starRating,
        isUnreleased = isUnreleased,
        hasSubtitles = hasSubtitles,
        isHd = isHd,
        durationMinutes = durationMinutes,
        productionYear = productionYear,
        country = country
    )
}

fun Program.toEntity(): ProgramEntity {
    return ProgramEntity(
        id = id,
        channelId = channelId,
        startTimestamp = startTimestamp,
        stopTimestamp = stopTimestamp,
        title = title,
        subTitle = subTitle,
        description = description,
        categories = categories,
        iconUrl = iconUrl,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        episodeDisplay = episodeDisplay,
        presenters = credit.presenters,
        actors = credit.actors,
        guests = credit.guests,
        directors = credit.directors,
        producers = credit.producers,
        writers = credit.writers,
        contentRating = contentRating,
        starRating = starRating,
        isUnreleased = isUnreleased,
        hasSubtitles = hasSubtitles,
        isHd = isHd,
        durationMinutes = durationMinutes,
        productionYear = productionYear,
        country = country
    )
}
