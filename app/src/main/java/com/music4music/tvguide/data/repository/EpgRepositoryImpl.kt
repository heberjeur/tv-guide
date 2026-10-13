package com.music4music.tvguide.data.repository

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
import com.music4music.tvguide.domain.repository.EpgRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val programDao: ProgramDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val downloader: XmltvDownloader,
    private val parser: XmltvParser,
    private val connectivityObserver: ConnectivityObserver
) : EpgRepository {

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

    override suspend fun toggleFavoriteChannel(channelId: String, isFavorite: Boolean) {
        channelDao.updateFavoriteStatus(channelId, isFavorite)
    }

    override fun getLastSyncTimestamp(channelPackage: ChannelPackage): Flow<Long?> {
        return syncMetadataDao.getSyncMetadata(channelPackage.id).map { it?.lastSyncTimestamp }
    }

    override suspend fun syncEpg(channelPackage: ChannelPackage, forceRefresh: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val metadata = syncMetadataDao.getSyncMetadataDirect(channelPackage.id)

            val fourHoursMillis = 4 * 60 * 60 * 1000L
            val isStale = metadata == null || (now - metadata.lastSyncTimestamp) > fourHoursMillis

            if (!forceRefresh && !isStale) {
                return@withContext Result.success(Unit)
            }

            if (!connectivityObserver.isConnected()) {
                return@withContext if (metadata != null) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("No network connection available to perform initial sync"))
                }
            }

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
                        return@withContext Result.success(Unit)
                    }

                    val stream = downloadResult.inputStream
                        ?: throw IOException("Empty stream received from $url")

                    var totalPrograms = 0
                    stream.use { input ->
                        parser.parse(input, channelPackage.id) { channels, programs ->
                            if (channels.isNotEmpty()) {
                                channelDao.insertChannels(channels)
                            }
                            if (programs.isNotEmpty()) {
                                programDao.insertPrograms(programs)
                                totalPrograms += programs.size
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
                    syncSucceeded = true
                    break
                } catch (exception: Exception) {
                    lastException = exception
                }
            }

            if (syncSucceeded) {
                Result.success(Unit)
            } else {
                Result.failure(lastException ?: IOException("Failed to sync EPG from all sources"))
            }
        }
}
