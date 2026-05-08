package com.gustavo.brilhante.cutestickers.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkMediaItem(
    val id: String,
    val url: String
)
