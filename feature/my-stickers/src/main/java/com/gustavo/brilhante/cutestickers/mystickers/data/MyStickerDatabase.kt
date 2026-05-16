package com.gustavo.brilhante.cutestickers.mystickers.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MyStickerEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MyStickerDatabase : RoomDatabase() {
    abstract fun myStickerDao(): MyStickerDao
}
