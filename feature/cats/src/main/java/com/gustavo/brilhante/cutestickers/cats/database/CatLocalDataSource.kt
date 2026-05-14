package com.gustavo.brilhante.cutestickers.cats.database

import com.gustavo.brilhante.cutestickers.data.MediaLocalDataSource
import com.gustavo.brilhante.cutestickers.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CatLocalDataSource @Inject constructor(
    private val catDao: CatDao
) : MediaLocalDataSource {
    override fun getMedia(): Flow<List<MediaItem>> = catDao.getCats().map { entities ->
        entities.map { it.asExternalModel() }
    }

    override suspend fun insertAll(items: List<MediaItem>) {
        catDao.insertAll(items.map { it.asEntity() })
    }

    override suspend fun clear() {
        catDao.clearCats()
    }

    override suspend fun isEmpty(): Boolean = catDao.getCount() == 0
}
