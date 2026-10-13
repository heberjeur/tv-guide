package com.music4music.tvguide.domain.usecase

import com.music4music.tvguide.domain.model.Program
import com.music4music.tvguide.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchProgramsUseCase @Inject constructor(
    private val repository: EpgRepository
) {
    operator fun invoke(query: String): Flow<List<Program>> {
        return repository.searchPrograms(query)
    }
}
