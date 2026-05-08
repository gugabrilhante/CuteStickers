package com.gustavo.brilhante.cutestickers.dogs.di

import com.gustavo.brilhante.cutestickers.common.network.DogsApi
import com.gustavo.brilhante.cutestickers.domain.MediaRepository
import com.gustavo.brilhante.cutestickers.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.RefreshMediaUseCase
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
