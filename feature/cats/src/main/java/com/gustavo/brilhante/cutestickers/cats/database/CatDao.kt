package com.gustavo.brilhante.cutestickers.cats.database

import androidx.room.Dao
import androidx.room.Query
import com.gustavo.brilhante.cutestickers.database.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface CatDao : BaseDao<CatEntity> {
    @Query("SELECT * FROM cats")
    fun getCats(): Flow<List<CatEntity>>

    @Query("DELETE FROM cats")
    suspend fun clearCats()

    @Query("SELECT COUNT(*) FROM cats")
    suspend fun getCount(): Int
}
