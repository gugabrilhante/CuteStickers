package com.gustavo.brilhante.cutestickers.dogs.database

import com.gustavo.brilhante.cutestickers.data.MediaLocalDataSource
import com.gustavo.brilhante.cutestickers.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DogLocalDataSource @Inject constructor(
    private val dogDao: DogDao
) : MediaLocalDataSource {
    override fun getMedia(): Flow<List<MediaItem>> = dogDao.getDogs().map { entities ->
        entities.map { it.asExternalModel() }
    }

    override suspend fun insertAll(items: List<MediaItem>) {
        dogDao.insertAll(items.map { it.asEntity() })
    }

    override suspend fun clear() {
        dogDao.clearDogs()
    }
}
