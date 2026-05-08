package com.gustavo.brilhante.cutestickers.stickers.data.di

import com.gustavo.brilhante.cutestickers.stickers.data.StickerRepositoryImpl
import com.gustavo.brilhante.cutestickers.stickers.data.WhatsAppStickerExporter
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerExporter
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class StickersModule {

    @Binds
    @Singleton
    abstract fun bindStickerRepository(impl: StickerRepositoryImpl): StickerRepository

    @Binds
    @Singleton
    abstract fun bindStickerExporter(impl: WhatsAppStickerExporter): StickerExporter
}
