package com.music4music.tvguide.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.music4music.tvguide.R
import com.music4music.tvguide.data.local.ChannelCatalog
import com.music4music.tvguide.data.local.entity.ProgramEntity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WidgetRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetType = intent.getIntExtra(EXTRA_WIDGET_TYPE, TYPE_NOW_PLAYING)
        return WidgetItemFactory(applicationContext, widgetType)
    }

    companion object {
        const val EXTRA_WIDGET_TYPE = "extra_widget_type"
        const val TYPE_NOW_PLAYING = 1
        const val TYPE_FAVORITES = 2
        const val TYPE_TODAY = 3
    }
}

private class WidgetItemFactory(
    private val context: Context,
    private val widgetType: Int
) : RemoteViewsService.RemoteViewsFactory {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    private var programs: List<ProgramEntity> = emptyList()
    private var widgetTheme: String = "dark"
    private var showProgress: Boolean = true

    override fun onCreate() {}

    override fun onDataSetChanged() {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                WidgetEntryPoint::class.java
            )
            val dao = entryPoint.programDao()
            val prefs = entryPoint.userPreferences()

            runBlocking {
                widgetTheme = prefs.widgetTheme.first()
                showProgress = prefs.widgetShowProgress.first()
                val now = System.currentTimeMillis()

                when (widgetType) {
                    WidgetRemoteViewsService.TYPE_FAVORITES -> {
                        val favoriteOrder = prefs.favoriteChannelsOrder.first()
                        val allNow = dao.getNowPlayingDirect(now)
                        programs = if (favoriteOrder.isEmpty()) {
                            emptyList()
                        } else {
                            allNow.filter { p ->
                                ChannelCatalog.isFavorite(p.channelId, favoriteOrder.toSet())
                            }.sortedBy { p ->
                                val idx = favoriteOrder.indexOfFirst {
                                    ChannelCatalog.normalizeId(it) == ChannelCatalog.normalizeId(p.channelId)
                                }
                                if (idx >= 0) idx else Int.MAX_VALUE
                            }
                        }
                    }
                    WidgetRemoteViewsService.TYPE_TODAY -> {
                        val zone = ZoneId.systemDefault()
                        val today = LocalDate.now(zone)
                        val primeStart = today.atTime(20, 30).atZone(zone).toInstant().toEpochMilli()
                        val primeEnd = today.atTime(23, 45).atZone(zone).toInstant().toEpochMilli()
                        val queryStart = if (now > primeEnd) {
                            now
                        } else {
                            primeStart
                        }
                        val queryEnd = if (now > primeEnd) {
                            today.plusDays(1).atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
                        } else {
                            primeEnd
                        }
                        val list = dao.getProgramsByTimeRangeDirect(queryStart, queryEnd)
                        programs = list.sortedBy { p ->
                            val num = ChannelCatalog.getChannelNumber(p.channelId)
                            if (num > 0) num else 999
                        }
                    }
                    else -> {
                        val list = dao.getNowPlayingDirect(now)
                        programs = list.sortedBy { p ->
                            val num = ChannelCatalog.getChannelNumber(p.channelId)
                            if (num > 0) num else 999
                        }
                    }
                }
            }
        } catch (e: Exception) {
            programs = emptyList()
        }
    }

    override fun onDestroy() {
        programs = emptyList()
    }

    override fun getCount(): Int = programs.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= programs.size) return null
        val program = programs[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item_program)

        WidgetThemeHelper.applyItemTheme(views, widgetTheme)

        val channelName = ChannelCatalog.getDisplayName(program.channelId)
        views.setTextViewText(R.id.widget_item_channel, channelName)

        val startStr = timeFormatter.format(Instant.ofEpochMilli(program.startTimestamp))
        val stopStr = timeFormatter.format(Instant.ofEpochMilli(program.stopTimestamp))
        views.setTextViewText(R.id.widget_item_time, "$startStr - $stopStr")

        views.setTextViewText(R.id.widget_item_title, program.title)

        val category = program.categories.firstOrNull() ?: ""
        if (category.isNotBlank()) {
            views.setViewVisibility(R.id.widget_item_category, View.VISIBLE)
            views.setTextViewText(R.id.widget_item_category, category)
        } else {
            views.setViewVisibility(R.id.widget_item_category, View.GONE)
        }

        val now = System.currentTimeMillis()
        if (showProgress && now in program.startTimestamp until program.stopTimestamp) {
            val duration = (program.stopTimestamp - program.startTimestamp).coerceAtLeast(1)
            val elapsed = (now - program.startTimestamp).coerceIn(0, duration)
            val progressPercent = ((elapsed * 100) / duration).toInt()
            views.setViewVisibility(R.id.widget_item_progress, View.VISIBLE)
            views.setProgressBar(R.id.widget_item_progress, 100, progressPercent, false)
        } else {
            views.setViewVisibility(R.id.widget_item_progress, View.GONE)
        }

        val fillInIntent = Intent().apply {
            putExtra("program_id", program.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
