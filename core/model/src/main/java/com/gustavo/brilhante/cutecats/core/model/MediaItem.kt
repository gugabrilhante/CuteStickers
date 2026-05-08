package com.gustavo.brilhante.cutecats.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    val url: String
)
