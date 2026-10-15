package com.music4music.tvguide.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "programs",
    indices = [
        Index(value = ["channelId", "startTimestamp"], unique = true),
        Index(value = ["channelId", "stopTimestamp"]),
        Index(value = ["startTimestamp", "stopTimestamp"]),
        Index(value = ["title"])
    ]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: String,
    val startTimestamp: Long,
    val stopTimestamp: Long,
    val title: String,
    val subTitle: String?,
    val description: String,
    val categories: List<String>,
    val iconUrl: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val episodeDisplay: String?,
    val presenters: List<String>,
    val actors: List<String>,
    val guests: List<String>,
    val directors: List<String>,
    val producers: List<String>,
    val writers: List<String>,
    val contentRating: String?,
    val starRating: Float?,
    val isUnreleased: Boolean,
    val hasSubtitles: Boolean,
    val isHd: Boolean,
    val durationMinutes: Int,
    val productionYear: Int? = null,
    val country: String? = null
)
