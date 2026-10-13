package com.music4music.tvguide.data.repository

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.music4music.tvguide.data.connectivity.ConnectivityObserver
import com.music4music.tvguide.data.local.db.ChannelDao
import com.music4music.tvguide.data.local.db.ProgramDao
import com.music4music.tvguide.data.local.db.SyncMetadataDao
import com.music4music.tvguide.data.local.entity.SyncMetadataEntity
import com.music4music.tvguide.data.mapper.toDomain
import com.music4music.tvguide.data.remote.XmltvDownloader
import com.music4music.tvguide.data.remote.XmltvParser
import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.model.SyncProgress
import com.music4music.tvguide.domain.model.SyncState
import com.music4music.tvguide.domain.repository.EpgRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channelDao: ChannelDao,
    private val programDao: ProgramDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val downloader: XmltvDownloader,
    private val parser: XmltvParser,
    private val connectivityObserver: ConnectivityObserver
) : EpgRepository {

    private val _syncProgress = MutableStateFlow(SyncProgress())

    override fun getSyncProgress(): StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    override fun getChannels(channelPackage: ChannelPackage): Flow<List<Channel>> {
        return channelDao.getChannelsByPackage(channelPackage.id).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getFavoriteChannels(channelPackage: ChannelPackage): Flow<List<Channel>> {
        return channelDao.getFavoriteChannels(channelPackage.id).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getNowPlaying(): Flow<List<Program>> {
        val now = System.currentTimeMillis()
        return programDao.getNowPlaying(now).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProgramsForChannel(
        channelId: String,
        dayStart: Long,
        dayEnd: Long
    ): Flow<List<Program>> {
        return programDao.getProgramsForChannel(channelId, dayStart, dayEnd).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProgramById(programId: Long): Flow<Program?> {
        return programDao.getProgramById(programId).map { it?.toDomain() }
    }

    override fun searchPrograms(query: String): Flow<List<Program>> {
        return programDao.searchPrograms(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUpcomingBroadcasts(title: String): Flow<List<Program>> {
        val now = System.currentTimeMillis()
        return programDao.getUpcomingBroadcasts(title, now).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProgramsByCategory(category: String): Flow<List<Program>> {
        val fromTime = System.currentTimeMillis() - 2 * 3600 * 1000L
        return programDao.getProgramsByCategory(category, fromTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProgramsByTimeRange(startTime: Long, endTime: Long): Flow<List<Program>> {
        return programDao.getProgramsByTimeRange(startTime, endTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun toggleFavoriteChannel(channelId: String, isFavorite: Boolean) {
        channelDao.updateFavoriteStatus(channelId, isFavorite)
    }

    override fun getLastSyncTimestamp(channelPackage: ChannelPackage): Flow<Long?> {
        return syncMetadataDao.getSyncMetadata(channelPackage.id).map { it?.lastSyncTimestamp }
    }

    override fun resetSyncProgress() {
        _syncProgress.value = SyncProgress(state = SyncState.IDLE)
    }

    override suspend fun syncEpg(channelPackage: ChannelPackage, forceRefresh: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val metadata = syncMetadataDao.getSyncMetadataDirect(channelPackage.id)

            val fourHoursMillis = 4 * 60 * 60 * 1000L
            val isStale = metadata == null || (now - metadata.lastSyncTimestamp) > fourHoursMillis

            if (!forceRefresh && !isStale) {
                _syncProgress.value = SyncProgress(
                    state = SyncState.IDLE,
                    programCount = metadata.programCount,
                    lastSyncTimestamp = metadata.lastSyncTimestamp
                )
                return@withContext Result.success(Unit)
            }

            if (!connectivityObserver.isConnected()) {
                val cachedCount = programDao.getProgramCount()
                if (metadata != null || cachedCount > 0) {
                    _syncProgress.value = SyncProgress(
                        state = SyncState.IDLE,
                        programCount = cachedCount,
                        lastSyncTimestamp = metadata?.lastSyncTimestamp ?: now
                    )
                    return@withContext Result.success(Unit)
                } else {
                    _syncProgress.value = SyncProgress(
                        state = SyncState.ERROR
                    )
                    return@withContext Result.failure(IOException("No network connection available to perform initial sync"))
                }
            }

            _syncProgress.value = SyncProgress(
                state = SyncState.DOWNLOADING
            )

            val candidateUrls = if (channelPackage == ChannelPackage.COMMUNITY_ENRICHED) {
                listOf(
                    channelPackage.feedUrl,
                    "https://raw.githubusercontent.com/heberjeur/tv-guide/gh-pages/epg_tnt.xml.gz",
                    ChannelPackage.TERRESTRIAL.feedUrl
                )
            } else {
                listOf(channelPackage.feedUrl)
            }

            var lastException: Exception? = null
            var syncSucceeded = false
            val imageList = mutableListOf<String>()

            for (url in candidateUrls) {
                try {
                    val currentEtag = if (forceRefresh) null else metadata?.etag
                    val downloadResult = downloader.downloadFeed(url, currentEtag)

                    if (downloadResult.isNotModified) {
                        metadata?.let {
                            syncMetadataDao.insertSyncMetadata(
                                it.copy(lastSyncTimestamp = now, isSuccess = true)
                            )
                        }
                        _syncProgress.value = SyncProgress(
                            state = SyncState.SUCCESS,
                            programCount = metadata?.programCount ?: 0,
                            lastSyncTimestamp = now
                        )
                        return@withContext Result.success(Unit)
                    }

                    val stream = downloadResult.inputStream
                        ?: throw IOException("Empty stream received from $url")

                    _syncProgress.value = SyncProgress(
                        state = SyncState.PARSING
                    )

                    var totalPrograms = 0
                    programDao.clearAll()
                    channelDao.deleteChannelsByPackage(channelPackage.id)

                    stream.use { input ->
                        parser.parse(input, channelPackage.id) { channels, programs ->
                            if (channels.isNotEmpty()) {
                                channelDao.insertChannels(channels)
                            }
                            if (programs.isNotEmpty()) {
                                programDao.insertPrograms(programs)
                                totalPrograms += programs.size
                                programs.forEach { p ->
                                    val icon = p.iconUrl
                                    if (!icon.isNullOrBlank() && !icon.startsWith("data:")) {
                                        imageList.add(icon)
                                    }
                                }
                            }
                        }
                    }

                    syncMetadataDao.insertSyncMetadata(
                        SyncMetadataEntity(
                            packageId = channelPackage.id,
                            lastSyncTimestamp = now,
                            etag = downloadResult.etag,
                            programCount = totalPrograms,
                            isSuccess = true
                        )
                    )

                    val yesterday = now - 24 * 60 * 60 * 1000L
                    programDao.deleteExpiredPrograms(yesterday)

                    _syncProgress.value = SyncProgress(
                        state = SyncState.CACHING_IMAGES,
                        programCount = totalPrograms,
                        lastSyncTimestamp = now
                    )

                    prefetchImages(imageList)

                    _syncProgress.value = SyncProgress(
                        state = SyncState.SUCCESS,
                        programCount = totalPrograms,
                        lastSyncTimestamp = now
                    )

                    syncSucceeded = true
                    break
                } catch (exception: Exception) {
                    lastException = exception
                }
            }

            if (syncSucceeded) {
                Result.success(Unit)
            } else {
                _syncProgress.value = SyncProgress(
                    state = SyncState.ERROR
                )
                Result.failure(lastException ?: IOException("Failed to sync EPG from all sources"))
            }
        }

    private fun prefetchImages(urls: List<String>) {
        val loader = context.imageLoader
        urls.distinct().take(400).forEach { url ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            loader.enqueue(request)
        }
    }
}
