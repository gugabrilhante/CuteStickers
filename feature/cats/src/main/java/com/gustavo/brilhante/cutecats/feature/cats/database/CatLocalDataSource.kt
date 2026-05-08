package com.gustavo.brilhante.cutecats.feature.cats.database

import com.gustavo.brilhante.cutecats.core.data.MediaLocalDataSource
import com.gustavo.brilhante.cutecats.core.model.MediaItem
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
}
