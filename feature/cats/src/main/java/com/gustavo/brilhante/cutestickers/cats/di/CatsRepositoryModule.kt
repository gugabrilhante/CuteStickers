package com.gustavo.brilhante.cutestickers.cats.di

import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.network.CatsApi
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.data.MediaRepositoryImpl
import com.gustavo.brilhante.cutestickers.data.PaginationSession
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.domain.MediaRepository
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.cats.database.CatLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatsRepositoryModule {

    @Provides
    @Singleton
    @CatsApi
    fun provideCatsRepository(
        @CatsApi mediaService: MediaService,
        catLocalDataSource: CatLocalDataSource,
        @CatsApi cacheMetadataDao: CacheMetadataDao,
        paginationSession: PaginationSession,
        timeProvider: TimeProvider,
        @Dispatcher(CatsDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ): MediaRepository {
        return MediaRepositoryImpl(
            mediaService = mediaService,
            localDataSource = catLocalDataSource,
            cacheMetadataDao = cacheMetadataDao,
            paginationSession = paginationSession,
            timeProvider = timeProvider,
            featureKey = "cats",
            ioDispatcher = ioDispatcher
        )
    }
}
