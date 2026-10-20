package com.music4music.tvguide.widget

import com.music4music.tvguide.R

class TodayScheduleWidgetProvider : BaseWidgetProvider() {
    override val widgetType: Int = WidgetRemoteViewsService.TYPE_TODAY
    override val layoutResId: Int = R.layout.widget_today
}
