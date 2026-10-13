package com.music4music.tvguide.domain.usecase

import com.music4music.tvguide.domain.model.Channel
import com.music4music.tvguide.domain.model.ChannelPackage
import com.music4music.tvguide.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val repository: EpgRepository
) {
    operator fun invoke(channelPackage: ChannelPackage): Flow<List<Channel>> {
        return repository.getChannels(channelPackage)
    }
}
