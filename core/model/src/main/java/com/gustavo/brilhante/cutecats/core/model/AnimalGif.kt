package com.gustavo.brilhante.cutecats.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AnimalGif(
    val id: String,
    val url: String
)
