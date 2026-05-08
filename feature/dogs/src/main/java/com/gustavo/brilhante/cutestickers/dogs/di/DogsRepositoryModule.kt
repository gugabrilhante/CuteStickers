package com.gustavo.brilhante.cutestickers.dogs.di

import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.network.DogsApi
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.data.MediaRepositoryImpl
import com.gustavo.brilhante.cutestickers.data.PaginationSession
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.domain.MediaRepository
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.dogs.database.DogLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DogsRepositoryModule {

    @Provides
    @Singleton
    @DogsApi
    fun provideDogsRepository(
        @DogsApi mediaService: MediaService,
        dogLocalDataSource: DogLocalDataSource,
        @DogsApi cacheMetadataDao: CacheMetadataDao,
        paginationSession: PaginationSession,
        timeProvider: TimeProvider,
        @Dispatcher(CatsDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ): MediaRepository {
        return MediaRepositoryImpl(
            mediaService = mediaService,
            localDataSource = dogLocalDataSource,
            cacheMetadataDao = cacheMetadataDao,
            paginationSession = paginationSession,
            timeProvider = timeProvider,
            featureKey = "dogs",
            ioDispatcher = ioDispatcher
        )
    }
}
