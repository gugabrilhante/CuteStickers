package com.gustavo.brilhante.cutestickers.common.di

import com.gustavo.brilhante.cutestickers.common.AndroidLogger
import com.gustavo.brilhante.cutestickers.common.AndroidPackageManagerWrapper
import com.gustavo.brilhante.cutestickers.common.DefaultTimeProvider
import com.gustavo.brilhante.cutestickers.common.Logger
import com.gustavo.brilhante.cutestickers.common.PackageManagerWrapper
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

    @Binds
    @Singleton
    fun bindLogger(androidLogger: AndroidLogger): Logger

    @Binds
    @Singleton
    fun bindPackageManagerWrapper(impl: AndroidPackageManagerWrapper): PackageManagerWrapper
}
