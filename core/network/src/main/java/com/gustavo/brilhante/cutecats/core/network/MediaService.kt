package com.gustavo.brilhante.cutecats.core.network

import com.gustavo.brilhante.cutecats.core.network.model.NetworkMediaItem
import retrofit2.http.GET
import retrofit2.http.Query

interface MediaService {
    @GET("images/search")
    suspend fun getMedia(
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 0
    ): List<NetworkMediaItem>
}
