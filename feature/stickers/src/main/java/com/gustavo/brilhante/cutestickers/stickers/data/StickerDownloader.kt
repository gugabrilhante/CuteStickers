package com.gustavo.brilhante.cutestickers.stickers.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface StickerDownloader {
    suspend fun download(url: String): Result<ByteArray>
}

@Singleton
class StickerDownloaderImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : StickerDownloader {
    override suspend fun download(url: String): Result<ByteArray> = runCatching {
        if (url.startsWith("/") || url.startsWith("file://")) {
            val path = url.removePrefix("file://")
            return@runCatching File(path).readBytes()
        }
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Failed to download image: ${response.code} ${response.message}")
            response.body?.bytes() ?: error("Empty image response")
        }
    }
}
