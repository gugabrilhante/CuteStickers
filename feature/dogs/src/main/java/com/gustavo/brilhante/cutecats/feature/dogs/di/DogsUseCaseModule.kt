package com.gustavo.brilhante.cutecats.feature.dogs.di

import com.gustavo.brilhante.cutecats.core.common.network.DogsApi
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
object DogsUseCaseModule {
    @Provides
    @DogsApi
    fun provideGetCachedMediaUseCase(@DogsApi repository: MediaRepository) = GetCachedMediaUseCase(repository)

    @Provides
    @DogsApi
    fun provideRefreshMediaUseCase(@DogsApi repository: MediaRepository) = RefreshMediaUseCase(repository)

    @Provides
    @DogsApi
    fun provideLoadNextPageUseCase(@DogsApi repository: MediaRepository) = LoadNextPageUseCase(repository)
}
