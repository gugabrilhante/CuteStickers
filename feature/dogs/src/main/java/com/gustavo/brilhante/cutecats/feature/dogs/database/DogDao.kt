package com.gustavo.brilhante.cutecats.feature.dogs.database

import androidx.room.Dao
import androidx.room.Query
import com.gustavo.brilhante.cutecats.core.database.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface DogDao : BaseDao<DogEntity> {
    @Query("SELECT * FROM dogs")
    fun getDogs(): Flow<List<DogEntity>>

    @Query("DELETE FROM dogs")
    suspend fun clearDogs()
}
