package com.gustavo.brilhante.cutestickers.dogs.database

import androidx.room.Dao
import androidx.room.Query
import com.gustavo.brilhante.cutestickers.database.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface DogDao : BaseDao<DogEntity> {
    @Query("SELECT * FROM dogs")
    fun getDogs(): Flow<List<DogEntity>>

    @Query("DELETE FROM dogs")
    suspend fun clearDogs()

    @Query("SELECT COUNT(*) FROM dogs")
    suspend fun getCount(): Int
}
