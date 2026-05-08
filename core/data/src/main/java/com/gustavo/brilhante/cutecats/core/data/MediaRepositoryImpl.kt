package com.gustavo.brilhante.cutecats.core.data

import com.gustavo.brilhante.cutecats.core.common.network.CatsDispatchers
import com.gustavo.brilhante.cutecats.core.common.network.Dispatcher
import com.gustavo.brilhante.cutecats.core.domain.MediaRepository
import com.gustavo.brilhante.cutecats.core.model.MediaItem
import com.gustavo.brilhante.cutecats.core.network.MediaService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val mediaService: MediaService,
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MediaRepository {
    override fun getMedia(): Flow<List<MediaItem>> = flow {
        val media = mediaService.getMedia().map { 
            MediaItem(id = it.id, url = it.url)
        }
        emit(media)
    }.flowOn(ioDispatcher)
}
