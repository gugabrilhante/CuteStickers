package com.gustavo.brilhante.cutecats.core.domain

import com.gustavo.brilhante.cutecats.core.model.AnimalGif
import kotlinx.coroutines.flow.Flow

interface AnimalRepository {
    fun getGifs(): Flow<List<AnimalGif>>
}
