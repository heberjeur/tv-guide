package com.music4music.tvguide.domain.model

import androidx.annotation.StringRes
import com.music4music.tvguide.R

enum class TvCategory(
    val id: String,
    @StringRes val nameRes: Int,
    val searchKeyword: String
) {
    MOVIES("movies", R.string.category_movies, "Film"),
    SERIES("series", R.string.category_series, "Série"),
    ENTERTAINMENT("entertainment", R.string.category_entertainment, "Divertissement"),
    SPORT("sport", R.string.category_sport, "Sport"),
    NEWS("news", R.string.category_news, "Info"),
    DOCUMENTARY("documentary", R.string.category_documentaries, "Documentaire"),
    KIDS("kids", R.string.category_kids, "Jeunesse"),
    MUSIC("music", R.string.category_music, "Musique"),
    OTHER("other", R.string.category_other, "Autre")
}
