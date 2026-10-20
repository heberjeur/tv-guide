package com.music4music.tvguide.widget

import com.music4music.tvguide.R

class NowPlayingWidgetProvider : BaseWidgetProvider() {
    override val widgetType: Int = WidgetRemoteViewsService.TYPE_NOW_PLAYING
    override val layoutResId: Int = R.layout.widget_now_playing
}
