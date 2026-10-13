package com.music4music.tvguide.domain.usecase

import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.repository.EpgRepository
import javax.inject.Inject

class SyncEpgUseCase @Inject constructor(
    private val repository: EpgRepository
) {
    suspend operator fun invoke(channelPackage: ChannelPackage, forceRefresh: Boolean = false): Result<Unit> {
        return repository.syncEpg(channelPackage, forceRefresh)
    }
}
