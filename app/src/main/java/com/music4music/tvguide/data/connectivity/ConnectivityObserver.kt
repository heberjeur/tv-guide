package com.music4music.tvguide.data.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<Boolean>
    fun isConnected(): Boolean
}
