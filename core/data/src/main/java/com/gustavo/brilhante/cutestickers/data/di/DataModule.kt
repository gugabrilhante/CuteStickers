package com.gustavo.brilhante.cutestickers.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    // Repositories are now provided in feature modules to avoid hardcoding features in core.
}
