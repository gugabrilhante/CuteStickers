package com.gustavo.brilhante.cutestickers.common

import com.gustavo.brilhante.cutestickers.model.MediaType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaMetadataResolver @Inject constructor() {
    fun getMediaType(url: String): MediaType {
        val lower = url.lowercase()
        return if (lower.contains("gif") || lower.contains(".webp")) {
            MediaType.Animated
        } else {
            MediaType.Static
        }
    }
}
