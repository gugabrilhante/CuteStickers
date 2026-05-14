package com.gustavo.brilhante.cutestickers.dogs.di

import android.content.Context
import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.ToastManager
import com.gustavo.brilhante.cutestickers.common.network.DogsApi
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.common.network.NetworkMonitor
import com.gustavo.brilhante.cutestickers.data.MediaRepositoryImpl
import com.gustavo.brilhante.cutestickers.data.PaginationSession
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.domain.MediaRepository
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.dogs.database.DogLocalDataSource
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
        networkMonitor: NetworkMonitor,
        toastManager: ToastManager,
        @ApplicationContext context: Context,
        json: Json,
        @Dispatcher(CatsDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ): MediaRepository {
        return MediaRepositoryImpl(
            mediaService = mediaService,
            localDataSource = dogLocalDataSource,
            cacheMetadataDao = cacheMetadataDao,
            paginationSession = paginationSession,
            timeProvider = timeProvider,
            networkMonitor = networkMonitor,
            toastManager = toastManager,
            assetManager = context.assets,
            json = json,
            featureKey = "dogs",
            seedFiles = listOf("dogs_seed_1.json", "dogs_seed_2.json", "dogs_seed_3.json"),
            ioDispatcher = ioDispatcher
        )
    }
}
