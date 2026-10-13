package com.music4music.tvguide.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.music4music.tvguide.data.local.prefs.UserPreferences
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.repository.EpgRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val epgRepository: EpgRepository,
    private val preferences: UserPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val packageId = preferences.selectedPackage.first()
        val channelPackage = ChannelPackage.fromId(packageId)

        val syncResult = epgRepository.syncEpg(channelPackage, forceRefresh = false)
        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
