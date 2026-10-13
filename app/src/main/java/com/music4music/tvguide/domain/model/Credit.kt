package com.music4music.tvguide.domain.model

data class Credit(
    val presenters: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    val guests: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val producers: List<String> = emptyList(),
    val writers: List<String> = emptyList()
)
