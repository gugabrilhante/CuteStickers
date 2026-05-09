package com.gustavo.brilhante.cutestickers.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface MediaType {
    @Serializable
    data object Static : MediaType
    @Serializable
    data object Animated : MediaType
}
