package com.gustavo.brilhante.cutestickers.mystickers.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MyStickerDao {
    @Query("SELECT * FROM my_stickers ORDER BY createdAt DESC")
    fun getStickers(): Flow<List<MyStickerEntity>>

    @Query("SELECT * FROM my_stickers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MyStickerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sticker: MyStickerEntity)

    @Query("DELETE FROM my_stickers WHERE id = :id")
    suspend fun deleteById(id: String)
}
