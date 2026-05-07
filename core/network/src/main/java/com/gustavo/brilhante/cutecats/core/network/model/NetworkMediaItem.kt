package com.gustavo.brilhante.cutecats.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkMediaItem(
    val id: String,
    val url: String
)
