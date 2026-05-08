package com.gustavo.brilhante.cutestickers.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Cats : Screen
    @Serializable
    data object Dogs : Screen

    @Serializable
    data class MediaDetails(val imageUrl: String, val mediaId: String) : Screen
}
