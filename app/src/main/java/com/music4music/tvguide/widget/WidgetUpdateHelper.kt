package com.music4music.tvguide.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.music4music.tvguide.R

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val nowPlayingComponent = ComponentName(context, NowPlayingWidgetProvider::class.java)
        val nowPlayingIds = appWidgetManager.getAppWidgetIds(nowPlayingComponent)
        if (nowPlayingIds.isNotEmpty()) {
            NowPlayingWidgetProvider().updateWidgets(context, appWidgetManager, nowPlayingIds)
            appWidgetManager.notifyAppWidgetViewDataChanged(nowPlayingIds, R.id.widget_list)
        }

        val favoritesComponent = ComponentName(context, FavoritesWidgetProvider::class.java)
        val favoritesIds = appWidgetManager.getAppWidgetIds(favoritesComponent)
        if (favoritesIds.isNotEmpty()) {
            FavoritesWidgetProvider().updateWidgets(context, appWidgetManager, favoritesIds)
            appWidgetManager.notifyAppWidgetViewDataChanged(favoritesIds, R.id.widget_list)
        }

        val todayComponent = ComponentName(context, TodayScheduleWidgetProvider::class.java)
        val todayIds = appWidgetManager.getAppWidgetIds(todayComponent)
        if (todayIds.isNotEmpty()) {
            TodayScheduleWidgetProvider().updateWidgets(context, appWidgetManager, todayIds)
            appWidgetManager.notifyAppWidgetViewDataChanged(todayIds, R.id.widget_list)
        }
    }
}
