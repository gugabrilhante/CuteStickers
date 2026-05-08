package com.gustavo.brilhante.cutestickers.cats.di

import com.gustavo.brilhante.cutestickers.common.network.CatsApi
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
