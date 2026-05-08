package com.gustavo.brilhante.cutestickers.common.di

import com.gustavo.brilhante.cutestickers.common.DefaultTimeProvider
import com.gustavo.brilhante.cutestickers.common.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CommonModule {
    @Binds
    @Singleton
    fun bindTimeProvider(defaultTimeProvider: DefaultTimeProvider): TimeProvider
}
