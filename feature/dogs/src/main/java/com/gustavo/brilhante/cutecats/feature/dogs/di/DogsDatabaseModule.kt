package com.gustavo.brilhante.cutecats.feature.dogs.di

import android.content.Context
import androidx.room.Room
import com.gustavo.brilhante.cutecats.core.common.network.DogsApi
import com.gustavo.brilhante.cutecats.core.database.CacheMetadataDao
import com.gustavo.brilhante.cutecats.feature.dogs.database.DogDao
import com.gustavo.brilhante.cutecats.feature.dogs.database.DogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DogsDatabaseModule {

    @Provides
    @Singleton
    fun provideDogDatabase(@ApplicationContext context: Context): DogDatabase {
        return Room.databaseBuilder(
            context,
            DogDatabase::class.java,
            "dogs_database"
        ).build()
    }

    @Provides
    fun provideDogDao(db: DogDatabase): DogDao = db.dogDao()

    @Provides
    @DogsApi
    fun provideCacheMetadataDao(db: DogDatabase): CacheMetadataDao = db.dogCacheMetadataDao()
}
