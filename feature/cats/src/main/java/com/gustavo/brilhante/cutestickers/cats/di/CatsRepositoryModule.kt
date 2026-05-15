package com.gustavo.brilhante.cutestickers.cats.di

import android.content.Context
import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.ToastManager
import com.gustavo.brilhante.cutestickers.common.network.CatsApi
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.common.network.NetworkMonitor
import com.gustavo.brilhante.cutestickers.data.MediaRepositoryImpl
import com.gustavo.brilhante.cutestickers.data.PaginationSession
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.domain.MediaRepository
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.cats.database.CatLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
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
        networkMonitor: NetworkMonitor,
        toastManager: ToastManager,
        @ApplicationContext context: Context,
        json: Json,
        @Dispatcher(CatsDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ): MediaRepository {
        return MediaRepositoryImpl(
            mediaService = mediaService,
            localDataSource = catLocalDataSource,
            cacheMetadataDao = cacheMetadataDao,
            paginationSession = paginationSession,
            timeProvider = timeProvider,
            networkMonitor = networkMonitor,
            toastManager = toastManager,
            assetManager = context.assets,
            json = json,
            featureKey = "cats",
            seedFiles = listOf("cats_seed_1.json", "cats_seed_2.json", "cats_seed_3.json"),
            ioDispatcher = ioDispatcher
        )
    }
}
