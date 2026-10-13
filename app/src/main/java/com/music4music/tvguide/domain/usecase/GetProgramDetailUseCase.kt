package com.music4music.tvguide.domain.usecase

import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProgramDetailUseCase @Inject constructor(
    private val repository: EpgRepository
) {
    operator fun invoke(programId: Long): Flow<Program?> {
        return repository.getProgramById(programId)
    }
}
