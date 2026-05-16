package com.gustavo.brilhante.cutestickers.mystickers.di

import android.content.Context
import androidx.room.Room
import com.gustavo.brilhante.cutestickers.mystickers.AutoCutProcessor
import com.gustavo.brilhante.cutestickers.mystickers.CropImageProcessor
import com.gustavo.brilhante.cutestickers.mystickers.data.AutoCutProcessorImpl
import com.gustavo.brilhante.cutestickers.mystickers.data.CropImageProcessorImpl
import com.gustavo.brilhante.cutestickers.mystickers.data.MyStickerDao
import com.gustavo.brilhante.cutestickers.mystickers.data.MyStickerDatabase
import com.gustavo.brilhante.cutestickers.mystickers.data.MyStickersRepositoryImpl
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MyStickersModule {

    @Binds
    @Singleton
    abstract fun bindMyStickersRepository(impl: MyStickersRepositoryImpl): MyStickersRepository

    @Binds
    abstract fun bindCropImageProcessor(impl: CropImageProcessorImpl): CropImageProcessor

    @Binds
    abstract fun bindAutoCutProcessor(impl: AutoCutProcessorImpl): AutoCutProcessor

    companion object {
        @Provides
        @Singleton
        fun provideMyStickersDatabase(@ApplicationContext context: Context): MyStickerDatabase =
            Room.databaseBuilder(context, MyStickerDatabase::class.java, "my_stickers_database")
                .fallbackToDestructiveMigration(true)
                .build()

        @Provides
        fun provideMyStickerDao(db: MyStickerDatabase): MyStickerDao = db.myStickerDao()
    }
}
