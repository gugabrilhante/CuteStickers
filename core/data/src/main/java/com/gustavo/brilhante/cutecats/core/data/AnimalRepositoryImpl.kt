package com.gustavo.brilhante.cutecats.core.data

import com.gustavo.brilhante.cutecats.core.common.network.CatsDispatchers
import com.gustavo.brilhante.cutecats.core.common.network.Dispatcher
import com.gustavo.brilhante.cutecats.core.domain.AnimalRepository
import com.gustavo.brilhante.cutecats.core.model.AnimalGif
import com.gustavo.brilhante.cutecats.core.network.AnimalService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class AnimalRepositoryImpl @Inject constructor(
    private val animalService: AnimalService,
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : AnimalRepository {
    override fun getGifs(): Flow<List<AnimalGif>> = flow {
        val gifs = animalService.getGifs().map { 
            AnimalGif(id = it.id, url = it.url)
        }
        emit(gifs)
    }.flowOn(ioDispatcher)
}
