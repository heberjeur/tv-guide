package com.music4music.tvguide.data.local

object ChannelCatalog {

    val tntChannelIds: List<String> = listOf(
        "TF1.fr",
        "France2.fr",
        "France3.fr",
        "France4.fr",
        "France5.fr",
        "M6.fr",
        "Arte.fr",
        "LCP.fr",
        "W9.fr",
        "TMC.fr",
        "TFX.fr",
        "Gulli.fr",
        "BFMTV.fr",
        "CNews.fr",
        "LCI.fr",
        "FranceInfo.fr",
        "CStar.fr",
        "T18.fr",
        "NOVO19.fr",
        "TF1SeriesFilms.fr",
        "LEquipe.fr",
        "6ter.fr",
        "RMCStory.fr",
        "RMCDecouverte.fr",
        "RMCLife.fr",
        "TV5Monde.fr",
        "France24.fr"
    )

    private val tntChannelMap = mapOf(
        "TF1.fr" to 1,
        "France2.fr" to 2,
        "France3.fr" to 3,
        "France4.fr" to 4,
        "France5.fr" to 5,
        "M6.fr" to 6,
        "Arte.fr" to 7,
        "LCP.fr" to 8,
        "W9.fr" to 9,
        "TMC.fr" to 10,
        "TFX.fr" to 11,
        "Gulli.fr" to 12,
        "BFMTV.fr" to 13,
        "CNews.fr" to 14,
        "LCI.fr" to 15,
        "FranceInfo.fr" to 16,
        "CStar.fr" to 17,
        "T18.fr" to 18,
        "NOVO19.fr" to 19,
        "TF1SeriesFilms.fr" to 20,
        "LEquipe.fr" to 21,
        "6ter.fr" to 22,
        "RMCStory.fr" to 23,
        "RMCDecouverte.fr" to 24,
        "RMCLife.fr" to 25,
        "TV5Monde.fr" to 26,
        "France24.fr" to 27
    )

    private val logoMap = mapOf(
        "TF1.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~f2b6de26acc4dc3e.png/480x480/quality/100/tf1.png",
        "France2.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~606394a5b095653a.png/480x480/quality/100/france-2.png",
        "France3.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~61eebbb6e38360ae.png/480x480/quality/100/france-3.png",
        "France4.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~e02946cfb82e5310.png/480x480/quality/100/france-4.png",
        "France5.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~5f611b705ca3f017.png/480x480/quality/100/france-5.png",
        "M6.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~46c2dbade9ac9fed.png/480x480/quality/100/m6.png",
        "Arte.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~d6bb7624abff43ac.png/480x480/quality/100/arte.png",
        "LCP.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~080eb27517dd16c6.png/480x480/quality/100/la-chaine-parlementaire.png",
        "W9.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~15acfc705e2d4dd8.png/480x480/quality/100/w9.png",
        "TMC.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~b2c9618d914140d0.png/480x480/quality/100/tmc.png",
        "TFX.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~71e551462d6114e5.png/480x480/quality/100/tfx.png",
        "Gulli.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~059a8175b83117fd.png/480x480/quality/100/gulli.png",
        "BFMTV.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~9048e02338fb1134.png/480x480/quality/100/bfmtv.png",
        "CNews.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~952ec4ae63b20c1b.png/480x480/quality/100/cnews.png",
        "LCI.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~1314d39cfeea2a5c.png/480x480/quality/100/lci-la-chaine-info.png",
        "FranceInfo.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~367eff2d23ab1357.png/480x480/quality/100/franceinfo.png",
        "CStar.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~9583d8a6fa9d6ade.png/480x480/quality/100/cstar.png",
        "T18.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~3deccc95e7e75463.png/480x480/quality/100/t18.png",
        "NOVO19.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~113b817bb2e5c3a6.png/480x480/quality/100/novo19.png",
        "TF1SeriesFilms.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~3104ccb3015a8669.png/480x480/quality/100/tf1-series-films.png",
        "LEquipe.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~060edf53b072af43.png/480x480/quality/100/l-equipe.png",
        "6ter.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~ceee54072118570a.png/480x480/quality/100/6ter.png",
        "RMCStory.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~56b83775344955b8.png/480x480/quality/100/rmc-story.png",
        "RMCDecouverte.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~a95c9fa646201300.png/480x480/quality/100/rmc-decouverte.png",
        "RMCLife.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~91bf2b7762746d0c.png/480x480/quality/100/rmc-life.png",
        "TV5Monde.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~0231873fab336015.png/480x480/quality/100/tv5monde.png",
        "France24.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~afbbeba076c073d9.png/480x480/quality/100/france-24.png"
    )

    private val popularityRankMap = mapOf(
        "TF1.fr" to 1,
        "France2.fr" to 2,
        "France3.fr" to 3,
        "France4.fr" to 4,
        "France5.fr" to 5,
        "M6.fr" to 6,
        "Arte.fr" to 7,
        "TV5Monde.fr" to 8,
        "TMC.fr" to 9,
        "W9.fr" to 10,
        "TFX.fr" to 11,
        "6ter.fr" to 12,
        "Gulli.fr" to 13,
        "T18.fr" to 14,
        "NOVO19.fr" to 15,
        "CStar.fr" to 16,
        "RMCDecouverte.fr" to 17,
        "RMCStory.fr" to 18,
        "RMCLife.fr" to 19,
        "TF1SeriesFilms.fr" to 20,
        "LEquipe.fr" to 21,
        "France24.fr" to 22,
        "FranceInfo.fr" to 23,
        "CNews.fr" to 24,
        "BFMTV.fr" to 25,
        "LCI.fr" to 26,
        "LCP.fr" to 27
    )

    private val channelNameMap = mapOf(
        "TF1.fr" to "TF1",
        "France2.fr" to "France 2",
        "France3.fr" to "France 3",
        "France4.fr" to "France 4",
        "France5.fr" to "France 5",
        "M6.fr" to "M6",
        "Arte.fr" to "Arte",
        "LCP.fr" to "LCP",
        "W9.fr" to "W9",
        "TMC.fr" to "TMC",
        "TFX.fr" to "TFX",
        "Gulli.fr" to "Gulli",
        "BFMTV.fr" to "BFMTV",
        "CNews.fr" to "CNews",
        "LCI.fr" to "LCI",
        "FranceInfo.fr" to "franceinfo",
        "CStar.fr" to "CStar",
        "T18.fr" to "T18",
        "NOVO19.fr" to "NOVO19",
        "TF1SeriesFilms.fr" to "TF1 Séries Films",
        "LEquipe.fr" to "L'Équipe",
        "6ter.fr" to "6ter",
        "RMCStory.fr" to "RMC Story",
        "RMCDecouverte.fr" to "RMC Découverte",
        "RMCLife.fr" to "RMC Life",
        "TV5Monde.fr" to "TV5 Monde",
        "France24.fr" to "France 24"
    )

    fun getChannelNumber(channelId: String): Int {
        val direct = tntChannelMap[channelId]
        if (direct != null) return direct
        val normalized = normalizeId(channelId)
        return tntChannelMap.entries.firstOrNull {
            normalizeId(it.key) == normalized
        }?.value ?: 0
    }

    fun getPopularityRank(channelId: String): Int {
        val direct = popularityRankMap[channelId]
        if (direct != null) return direct
        val normalized = normalizeId(channelId)
        return popularityRankMap.entries.firstOrNull {
            normalizeId(it.key) == normalized
        }?.value ?: Int.MAX_VALUE
    }

    fun getDisplayName(channelId: String): String {
        val direct = channelNameMap[channelId]
        if (direct != null) return direct
        val normalized = normalizeId(channelId)
        return channelNameMap.entries.firstOrNull {
            normalizeId(it.key) == normalized
        }?.value ?: channelId.removeSuffix(".fr")
    }

    fun getLogoUrl(channelId: String): String? {
        val direct = logoMap[channelId]
        if (direct != null) return direct
        val normalized = normalizeId(channelId)
        return logoMap.entries.firstOrNull {
            normalizeId(it.key) == normalized
        }?.value
    }

    fun getAllLogoUrls(): List<String> {
        return logoMap.values.distinct()
    }

    fun isFavorite(channelId: String, favoriteChannels: Set<String>): Boolean {
        if (favoriteChannels.isEmpty()) return false
        if (favoriteChannels.contains(channelId)) return true
        val normalized = normalizeId(channelId)
        return favoriteChannels.any { normalizeId(it) == normalized }
    }

    fun normalizeId(id: String): String {
        return id.trim().lowercase().removeSuffix(".fr").replace("[^a-z0-9]".toRegex(), "")
    }
}
