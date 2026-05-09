package com.gustavo.brilhante.cutestickers.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    val url: String,
    val type: MediaType = if (url.lowercase().endsWith(".gif")) MediaType.Animated else MediaType.Static
)
