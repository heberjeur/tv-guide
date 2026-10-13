package com.music4music.tvguide.di

import com.music4music.tvguide.data.repository.EpgRepositoryImpl
import com.music4music.tvguide.data.repository.ReminderRepositoryImpl
import com.music4music.tvguide.domain.repository.EpgRepository
import com.music4music.tvguide.domain.repository.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
