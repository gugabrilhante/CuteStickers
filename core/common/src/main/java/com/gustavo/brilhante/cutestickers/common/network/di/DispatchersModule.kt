package com.gustavo.brilhante.cutestickers.common.network.di

import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers.Default
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers.IO
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Dispatcher(IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
