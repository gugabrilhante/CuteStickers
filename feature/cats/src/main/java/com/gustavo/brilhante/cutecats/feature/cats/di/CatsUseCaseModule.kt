package com.gustavo.brilhante.cutecats.feature.cats.di

import com.gustavo.brilhante.cutecats.core.common.network.CatsApi
import com.gustavo.brilhante.cutecats.core.domain.MediaRepository
import com.gustavo.brilhante.cutecats.core.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutecats.core.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutecats.core.domain.usecase.RefreshMediaUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object CatsUseCaseModule {
    @Provides
    @CatsApi
    fun provideGetCachedMediaUseCase(@CatsApi repository: MediaRepository) = GetCachedMediaUseCase(repository)

    @Provides
    @CatsApi
    fun provideRefreshMediaUseCase(@CatsApi repository: MediaRepository) = RefreshMediaUseCase(repository)

    @Provides
    @CatsApi
    fun provideLoadNextPageUseCase(@CatsApi repository: MediaRepository) = LoadNextPageUseCase(repository)
}
