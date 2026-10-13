package com.music4music.tvguide.domain.usecase

import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNowPlayingUseCase @Inject constructor(
    private val repository: EpgRepository
) {
    operator fun invoke(): Flow<List<Program>> {
        return repository.getNowPlaying()
    }
}
