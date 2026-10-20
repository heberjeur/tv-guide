package com.music4music.tvguide.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.repository.EpgRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val epgRepository: EpgRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncResult = epgRepository.syncEpg(ChannelPackage.COMMUNITY_ENRICHED, forceRefresh = false)
        return if (syncResult.isSuccess) {
            com.music4music.tvguide.widget.WidgetUpdateHelper.updateAllWidgets(applicationContext)
            Result.success()
        } else {
            Result.retry()
        }
    }
}
