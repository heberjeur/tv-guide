package com.music4music.tvguide.widget

import com.music4music.tvguide.R

class FavoritesWidgetProvider : BaseWidgetProvider() {
    override val widgetType: Int = WidgetRemoteViewsService.TYPE_FAVORITES
    override val layoutResId: Int = R.layout.widget_favorites
}
