package com.gustavo.brilhante.cutecats.core.network

import com.gustavo.brilhante.cutecats.core.network.model.NetworkAnimalGif
import retrofit2.http.GET
import retrofit2.http.Query

interface AnimalService {
    @GET("images/search")
    suspend fun getGifs(@Query("limit") limit: Int = 20): List<NetworkAnimalGif>
}
