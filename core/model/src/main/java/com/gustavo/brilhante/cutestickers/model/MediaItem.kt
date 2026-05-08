package com.gustavo.brilhante.cutestickers.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    val url: String
)
