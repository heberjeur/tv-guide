package com.music4music.tvguide.data.repository

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.music4music.tvguide.data.local.ChannelCatalog
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import com.music4music.tvguide.data.local.prefs.UserPreferences
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
    private val connectivityObserver: ConnectivityObserver,
    private val userPreferences: UserPreferences
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

    override fun getFilteredPrograms(
        category: String,
        channelId: String?,
        dayStart: Long,
        dayEnd: Long
    ): Flow<List<Program>> {
        val now = System.currentTimeMillis() - 2 * 3600 * 1000L
        val fromTime = if (dayStart > 0L) minOf(now, dayStart) else now
        return programDao.getFilteredPrograms(category, channelId, dayStart, dayEnd, fromTime).map { list ->
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
            var cachedCount = programDao.getProgramCount()

            if (cachedCount == 0) {
                _syncProgress.value = SyncProgress(state = SyncState.PARSING)
                preloadFromAssetsIfEmpty(channelPackage)
                cachedCount = programDao.getProgramCount()
            }

            val metadata = syncMetadataDao.getSyncMetadataDirect(channelPackage.id)
            val fourHoursMillis = 4 * 60 * 60 * 1000L
            val isStale = metadata == null || (now - metadata.lastSyncTimestamp) > fourHoursMillis

            if (!forceRefresh && !isStale && cachedCount > 0) {
                _syncProgress.value = SyncProgress(
                    state = SyncState.IDLE,
                    programCount = cachedCount,
                    lastSyncTimestamp = metadata?.lastSyncTimestamp ?: now
                )
                return@withContext Result.success(Unit)
            }

            if (!connectivityObserver.isConnected()) {
                _syncProgress.value = SyncProgress(
                    state = SyncState.IDLE,
                    programCount = cachedCount,
                    lastSyncTimestamp = metadata?.lastSyncTimestamp ?: now
                )
                return@withContext Result.success(Unit)
            }

            _syncProgress.value = SyncProgress(
                state = SyncState.DOWNLOADING
            )

            val candidateUrls = listOf(
                channelPackage.feedUrl,
                "https://raw.githubusercontent.com/heberjeur/tv-guide/gh-pages/epg_tnt.xml.gz"
            )

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
                            programCount = metadata?.programCount ?: cachedCount,
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

                    stream.use { input ->
                        parser.parse(input, channelPackage.id) { channels, programs ->
                            if (channels.isNotEmpty()) {
                                channelDao.insertChannels(channels)
                                channels.forEach { c ->
                                    val icon = ChannelCatalog.getLogoUrl(c.id) ?: c.iconUrl
                                    if (!icon.isNullOrBlank() && !icon.startsWith("data:")) {
                                        imageList.add(icon)
                                    }
                                }
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

                    val retentionDays = userPreferences.retentionDays.first()
                    val zone = ZoneId.of("Europe/Paris")
                    val cutoffTimestamp = LocalDate.now(zone).minusDays(retentionDays.coerceAtLeast(2).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
                    programDao.deleteExpiredPrograms(cutoffTimestamp)

                    _syncProgress.value = SyncProgress(
                        state = SyncState.CACHING_IMAGES,
                        programCount = totalPrograms,
                        lastSyncTimestamp = now
                    )

                    imageList.addAll(ChannelCatalog.getAllLogoUrls())
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
            } else if (cachedCount > 0) {
                _syncProgress.value = SyncProgress(
                    state = SyncState.IDLE,
                    programCount = cachedCount,
                    lastSyncTimestamp = metadata?.lastSyncTimestamp ?: now
                )
                Result.success(Unit)
            } else {
                _syncProgress.value = SyncProgress(
                    state = SyncState.ERROR
                )
                Result.failure(lastException ?: IOException("Failed to sync EPG from all sources"))
            }
        }

    private suspend fun preloadFromAssetsIfEmpty(channelPackage: ChannelPackage) {
        try {
            val assetStream = context.assets.open("epg_tnt.xml.gz")
            var totalPrograms = 0
            val imageList = mutableListOf<String>()

            assetStream.use { input ->
                parser.parse(input, channelPackage.id) { channels, programs ->
                    if (channels.isNotEmpty()) {
                        channelDao.insertChannels(channels)
                        channels.forEach { c ->
                            val icon = ChannelCatalog.getLogoUrl(c.id) ?: c.iconUrl
                            if (!icon.isNullOrBlank() && !icon.startsWith("data:")) {
                                imageList.add(icon)
                            }
                        }
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

            val now = System.currentTimeMillis()
            syncMetadataDao.insertSyncMetadata(
                SyncMetadataEntity(
                    packageId = channelPackage.id,
                    lastSyncTimestamp = now,
                    etag = "bundled_asset",
                    programCount = totalPrograms,
                    isSuccess = true
                )
            )

            imageList.addAll(ChannelCatalog.getAllLogoUrls())
            prefetchImages(imageList)
        } catch (_: Exception) {
        }
    }

    private suspend fun prefetchImages(urls: List<String>) {
        if (urls.isEmpty()) return
        val distinctUrls = urls.filter { it.isNotBlank() && !it.startsWith("data:") }.distinct()
        val loader = context.imageLoader

        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(10)
            coroutineScope {
                distinctUrls.map { url ->
                    launch {
                        semaphore.withPermit {
                            try {
                                val request = ImageRequest.Builder(context)
                                    .data(url)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build()
                                loader.execute(request)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }.joinAll()
            }
        }
    }
}
