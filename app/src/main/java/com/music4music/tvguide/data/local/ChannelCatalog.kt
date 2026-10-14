package com.music4music.tvguide.data.local

import com.music4music.tvguide.domain.model.ChannelBundle

object ChannelCatalog {

    private val tntChannelMap = mapOf(
        "TF1.fr" to 1,
        "France2.fr" to 2,
        "France3.fr" to 3,
        "France4.fr" to 4,
        "CanalPlus.fr" to 4,
        "France5.fr" to 5,
        "M6.fr" to 6,
        "Arte.fr" to 7,
        "LCP.fr" to 8,
        "LaChaineParlementaire.fr" to 8,
        "C8.fr" to 8,
        "W9.fr" to 9,
        "TMC.fr" to 10,
        "TFX.fr" to 11,
        "NT1.fr" to 11,
        "Gulli.fr" to 12,
        "NRJ12.fr" to 12,
        "BFMTV.fr" to 13,
        "CNews.fr" to 14,
        "iTele.fr" to 14,
        "LCI.fr" to 15,
        "FranceInfo.fr" to 16,
        "CStar.fr" to 17,
        "D17.fr" to 17,
        "T18.fr" to 18,
        "NOVO19.fr" to 19,
        "TF1SeriesFilms.fr" to 20,
        "HD1.fr" to 20,
        "LEquipe.fr" to 21,
        "LEquipe21.fr" to 21,
        "6ter.fr" to 22,
        "RMCStory.fr" to 23,
        "Numero23.fr" to 23,
        "RMCDecouverte.fr" to 24,
        "RMCLife.fr" to 25,
        "Cherie25.fr" to 25,
        "TV5Monde.fr" to 26,
        "TV5MondeEurope.fr" to 26,
        "France24.fr" to 27,
        "ParisPremiere.fr" to 31,
        "CanalPlusSport.fr" to 32,
        "CanalPlusCinema.fr" to 33,
        "PlanetePlus.fr" to 34
    )

    private val logoMap = mapOf(
        "TF1.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~f2b6de26acc4dc3e.png/480x480/quality/100/tf1.png",
        "France2.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~606394a5b095653a.png/480x480/quality/100/france-2.png",
        "France3.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~61eebbb6e38360ae.png/480x480/quality/100/france-3.png",
        "CanalPlus.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~897223a57e2e8ae5.png/480x480/quality/100/canal.png",
        "France5.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~5f611b705ca3f017.png/480x480/quality/100/france-5.png",
        "M6.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~46c2dbade9ac9fed.png/480x480/quality/100/m6.png",
        "Arte.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~d6bb7624abff43ac.png/480x480/quality/100/arte.png",
        "C8.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~cb818eb4a8c9197c.png/480x480/quality/100/c8.png",
        "W9.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~15acfc705e2d4dd8.png/480x480/quality/100/w9.png",
        "TMC.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~b2c9618d914140d0.png/480x480/quality/100/tmc.png",
        "TFX.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~71e551462d6114e5.png/480x480/quality/100/tfx.png",
        "NT1.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~71e551462d6114e5.png/480x480/quality/100/tfx.png",
        "NRJ12.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~e52eef9e4b6eb6ca.png/480x480/quality/100/nrj-12.png",
        "LCP.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~080eb27517dd16c6.png/480x480/quality/100/la-chaine-parlementaire.png",
        "LaChaineParlementaire.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~080eb27517dd16c6.png/480x480/quality/100/la-chaine-parlementaire.png",
        "France4.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~e02946cfb82e5310.png/480x480/quality/100/france-4.png",
        "BFMTV.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~9048e02338fb1134.png/480x480/quality/100/bfmtv.png",
        "CNews.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~b9ba22fbf5c29ea9.png/480x480/quality/100/cnews.png",
        "iTele.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~b9ba22fbf5c29ea9.png/480x480/quality/100/cnews.png",
        "CStar.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~e9a116f1c4ee8006.png/480x480/quality/100/cstar.png",
        "D17.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~e9a116f1c4ee8006.png/480x480/quality/100/cstar.png",
        "T18.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~3deccc95e7e75463.png/480x480/quality/100/t18.png",
        "NOVO19.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~113b817bb2e5c3a6.png/480x480/quality/100/novo19.png",
        "Gulli.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~f69f88924b209e99.png/480x480/quality/100/gulli.png",
        "TF1SeriesFilms.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~6c1df7712140d3a5.png/480x480/quality/100/tf1-series-films.png",
        "HD1.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~6c1df7712140d3a5.png/480x480/quality/100/tf1-series-films.png",
        "LEquipe.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~3a59d873d63b2ad9.png/480x480/quality/100/l-equipe.png",
        "LEquipe21.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~3a59d873d63b2ad9.png/480x480/quality/100/l-equipe.png",
        "6ter.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~e712a4f488663aa7.png/480x480/quality/100/6ter.png",
        "RMCStory.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~a6375aeef58607aa.png/480x480/quality/100/rmc-story.png",
        "Numero23.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~a6375aeef58607aa.png/480x480/quality/100/rmc-story.png",
        "RMCDecouverte.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~251bbf390f70f69a.png/480x480/quality/100/rmc-decouverte.png",
        "RMCLife.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~91bf2b7762746d0c.png/480x480/quality/100/rmc-life.png",
        "Cherie25.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~91bf2b7762746d0c.png/480x480/quality/100/rmc-life.png",
        "LCI.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~6ce35ff5eef833a6.png/480x480/quality/100/lci.png",
        "FranceInfo.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~b0cb5ea004e03b22.png/480x480/quality/100/franceinfo.png",
        "TV5Monde.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~0231873fab336015.png/480x480/quality/100/tv5monde.png",
        "TV5MondeEurope.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~0231873fab336015.png/480x480/quality/100/tv5monde.png",
        "France24.fr" to "https://www.programme-tv.net/imgre/fit/~2~channel~afbbeba076c073d9.png/480x480/quality/100/france-24.png"
    )

    private val tntIds = tntChannelMap.keys

    fun getChannelNumber(channelId: String): Int {
        val direct = tntChannelMap[channelId]
        if (direct != null) return direct
        val normalized = normalizeId(channelId)
        return tntChannelMap.entries.firstOrNull {
            normalizeId(it.key) == normalized
        }?.value ?: 0
    }

    fun getLogoUrl(channelId: String): String? {
        val direct = logoMap[channelId]
        if (direct != null) return direct
        val normalized = normalizeId(channelId)
        return logoMap.entries.firstOrNull {
            normalizeId(it.key) == normalized
        }?.value
    }

    private fun normalizeId(id: String): String {
        return id.trim().lowercase().removeSuffix(".fr").replace("[^a-z0-9]".toRegex(), "")
    }

    fun isChannelInBundle(channelId: String, bundle: ChannelBundle): Boolean {
        return when (bundle) {
            ChannelBundle.ALL -> true
            ChannelBundle.TNT -> tntIds.contains(channelId) || (getChannelNumber(channelId) in 1..27)
            ChannelBundle.CANAL_PLUS -> {
                tntIds.contains(channelId) || channelId.contains("canal", ignoreCase = true) || (getChannelNumber(channelId) in 31..34)
            }
            ChannelBundle.ORANGE -> {
                tntIds.contains(channelId) || channelId.contains("ocs", ignoreCase = true) || channelId.contains("orange", ignoreCase = true)
            }
            ChannelBundle.FREE -> {
                tntIds.contains(channelId) || channelId.contains("free", ignoreCase = true) || channelId.contains("rtl9", ignoreCase = true)
            }
            ChannelBundle.SFR -> {
                tntIds.contains(channelId) || channelId.contains("sfr", ignoreCase = true) || channelId.contains("rmc", ignoreCase = true)
            }
            ChannelBundle.BOUYGUES -> {
                tntIds.contains(channelId) || channelId.contains("bbox", ignoreCase = true)
            }
            ChannelBundle.OTHER -> !tntIds.contains(channelId)
        }
    }
}
