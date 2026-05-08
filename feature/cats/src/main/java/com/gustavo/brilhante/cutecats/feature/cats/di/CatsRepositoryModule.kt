package com.gustavo.brilhante.cutecats.feature.cats.di

import com.gustavo.brilhante.cutecats.core.common.TimeProvider
import com.gustavo.brilhante.cutecats.core.common.network.CatsApi
import com.gustavo.brilhante.cutecats.core.common.network.CatsDispatchers
import com.gustavo.brilhante.cutecats.core.common.network.Dispatcher
import com.gustavo.brilhante.cutecats.core.data.MediaRepositoryImpl
import com.gustavo.brilhante.cutecats.core.data.PaginationSession
import com.gustavo.brilhante.cutecats.core.database.CacheMetadataDao
import com.gustavo.brilhante.cutecats.core.domain.MediaRepository
import com.gustavo.brilhante.cutecats.core.network.MediaService
import com.gustavo.brilhante.cutecats.feature.cats.database.CatLocalDataSource
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
