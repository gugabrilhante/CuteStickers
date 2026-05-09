package com.gustavo.brilhante.cutestickers.stickers.di

import android.content.Context
import com.gustavo.brilhante.cutestickers.stickers.data.GalleryDataSource
import com.gustavo.brilhante.cutestickers.stickers.data.GalleryDataSourceImpl
import com.gustavo.brilhante.cutestickers.stickers.data.StickerDownloader
import com.gustavo.brilhante.cutestickers.stickers.data.StickerDownloaderImpl
import com.gustavo.brilhante.cutestickers.stickers.data.StickerStore
import com.gustavo.brilhante.cutestickers.stickers.data.StickerStoreImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StickersDir

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StickersModule {

    @Binds
    @Singleton
    abstract fun bindStickerStore(impl: StickerStoreImpl): StickerStore

    @Binds
    @Singleton
    abstract fun bindGalleryDataSource(impl: GalleryDataSourceImpl): GalleryDataSource

    @Binds
    @Singleton
    abstract fun bindStickerDownloader(impl: StickerDownloaderImpl): StickerDownloader

    companion object {
        @Provides
        @Singleton
        @StickersDir
        fun provideStickersDir(@ApplicationContext context: Context): File {
            return File(context.filesDir, "stickers")
        }
    }
}
