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
    ),
    TERRESTRIAL(
        id = "terrestrial",
        feedFileName = "xmltv_tnt.xml.gz",
        feedUrl = "https://xmltvfr.fr/xmltv/xmltv_tnt.xml.gz"
    ),
    EXTENDED(
        id = "extended",
        feedFileName = "xmltv.xml.gz",
        feedUrl = "https://xmltvfr.fr/xmltv/xmltv.xml.gz"
    ),
    ALL_CHANNELS(
        id = "all_channels",
        feedFileName = "xmltv.xml.gz",
        feedUrl = "https://xmltvfr.fr/xmltv/xmltv.xml.gz"
    );

    companion object {
        fun fromId(id: String): ChannelPackage {
            return entries.firstOrNull { it.id == id } ?: COMMUNITY_ENRICHED
        }
    }
}
