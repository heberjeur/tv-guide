package com.music4music.tvguide.domain.model

import androidx.annotation.StringRes
import com.music4music.tvguide.R

enum class ChannelBundle(
    val id: String,
    @StringRes val nameRes: Int
) {
    ALL("all", R.string.bundle_all),
    TNT("tnt", R.string.bundle_tnt),
    CANAL_PLUS("canal_plus", R.string.bundle_canal_plus),
    ORANGE("orange", R.string.bundle_orange),
    FREE("free", R.string.bundle_free),
    SFR("sfr", R.string.bundle_sfr),
    BOUYGUES("bouygues", R.string.bundle_bouygues),
    OTHER("other", R.string.bundle_other);

    companion object {
        fun fromId(id: String): ChannelBundle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ALL
        }
    }
}
