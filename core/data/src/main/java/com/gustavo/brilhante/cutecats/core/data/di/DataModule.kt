package com.gustavo.brilhante.cutecats.core.data.di

import com.gustavo.brilhante.cutecats.core.common.network.CatsApi
import com.gustavo.brilhante.cutecats.core.common.network.CatsDispatchers
import com.gustavo.brilhante.cutecats.core.common.network.Dispatcher
import com.gustavo.brilhante.cutecats.core.common.network.DogsApi
import com.gustavo.brilhante.cutecats.core.data.AnimalRepositoryImpl
import com.gustavo.brilhante.cutecats.core.domain.AnimalRepository
import com.gustavo.brilhante.cutecats.core.network.AnimalService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @CatsApi
    fun provideCatsRepository(
        @CatsApi animalService: AnimalService,
        @Dispatcher(CatsDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ): AnimalRepository = AnimalRepositoryImpl(animalService, ioDispatcher)

    @Provides
    @Singleton
    @DogsApi
    fun provideDogsRepository(
        @DogsApi animalService: AnimalService,
        @Dispatcher(CatsDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ): AnimalRepository = AnimalRepositoryImpl(animalService, ioDispatcher)
}
