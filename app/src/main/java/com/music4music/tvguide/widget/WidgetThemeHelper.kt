package com.music4music.tvguide.widget

import android.graphics.Color
import android.widget.RemoteViews
import com.music4music.tvguide.R

object WidgetThemeHelper {

    fun applyTheme(views: RemoteViews, theme: String) {
        val bgRes = when (theme) {
            "light" -> R.drawable.widget_bg_light
            "transparent" -> R.drawable.widget_bg_transparent
            else -> R.drawable.widget_bg_dark
        }
        val titleColor = when (theme) {
            "light" -> Color.parseColor("#111827")
            else -> Color.parseColor("#FFFFFF")
        }
        val dividerColor = when (theme) {
            "light" -> Color.parseColor("#E5E7EB")
            "transparent" -> Color.parseColor("#33FFFFFF")
            else -> Color.parseColor("#2E333D")
        }

        views.setInt(R.id.widget_container, "setBackgroundResource", bgRes)
        views.setTextColor(R.id.widget_header_title, titleColor)
        views.setInt(R.id.widget_divider, "setBackgroundColor", dividerColor)
    }

    fun applyItemTheme(views: RemoteViews, theme: String) {
        val titleColor = when (theme) {
            "light" -> Color.parseColor("#111827")
            else -> Color.parseColor("#FFFFFF")
        }
        val subtitleColor = when (theme) {
            "light" -> Color.parseColor("#6B7280")
            else -> Color.parseColor("#9CA3AF")
        }

        views.setTextColor(R.id.widget_item_title, titleColor)
        views.setTextColor(R.id.widget_item_time, subtitleColor)
        views.setTextColor(R.id.widget_item_category, subtitleColor)
    }
}
