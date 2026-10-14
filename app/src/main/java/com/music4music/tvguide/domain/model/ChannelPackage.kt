package com.music4music.tvguide.domain.model

enum class ChannelPackage(
    val id: String,
    val feedFileName: String,
    val feedUrl: String
) {
    COMMUNITY_ENRICHED(
        id = "community_enriched",
        feedFileName = "epg_tnt.xml.gz",
        feedUrl = "https://heberjeur.github.io/tv-guide/epg_tnt.xml.gz"
    );

    companion object {
        fun fromId(id: String): ChannelPackage = COMMUNITY_ENRICHED
    }
}
