package com.music4music.tvguide.widget

import android.graphics.Color
import android.widget.RemoteViews
import com.music4music.tvguide.R

object WidgetThemeHelper {

    fun applyTheme(views: RemoteViews, theme: String) {
        when (theme) {
            "light" -> {
                views.setInt(R.id.widget_container, "setBackgroundColor", Color.parseColor("#F8F9FA"))
                views.setTextColor(R.id.widget_header_title, Color.parseColor("#111827"))
                views.setInt(R.id.widget_divider, "setBackgroundColor", Color.parseColor("#E5E7EB"))
            }
            "transparent" -> {
                views.setInt(R.id.widget_container, "setBackgroundColor", Color.parseColor("#B3121212"))
                views.setTextColor(R.id.widget_header_title, Color.parseColor("#FFFFFF"))
                views.setInt(R.id.widget_divider, "setBackgroundColor", Color.parseColor("#33FFFFFF"))
            }
        }
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
