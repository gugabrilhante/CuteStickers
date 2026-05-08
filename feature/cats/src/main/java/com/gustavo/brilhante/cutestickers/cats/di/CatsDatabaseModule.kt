package com.gustavo.brilhante.cutestickers.cats.di

import android.content.Context
import androidx.room.Room
import com.gustavo.brilhante.cutestickers.common.network.CatsApi
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.cats.database.CatDao
import com.gustavo.brilhante.cutestickers.cats.database.CatDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatsDatabaseModule {

    @Provides
    @Singleton
    fun provideCatDatabase(@ApplicationContext context: Context): CatDatabase {
        return Room.databaseBuilder(
            context,
            CatDatabase::class.java,
            "cats_database"
        ).build()
    }

    @Provides
    fun provideCatDao(db: CatDatabase): CatDao = db.catDao()

    @Provides
    @CatsApi
    fun provideCacheMetadataDao(db: CatDatabase): CacheMetadataDao = db.catCacheMetadataDao()
}
