package com.gustavo.brilhante.cutecats.core.data

import com.gustavo.brilhante.cutecats.core.common.TimeProvider
import com.gustavo.brilhante.cutecats.core.database.CacheMetadataDao
import com.gustavo.brilhante.cutecats.core.database.CacheMetadataEntity
import com.gustavo.brilhante.cutecats.core.domain.MediaRepository
import com.gustavo.brilhante.cutecats.core.model.MediaItem
import com.gustavo.brilhante.cutecats.core.network.MediaService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class MediaRepositoryImpl(
    private val mediaService: MediaService,
    private val localDataSource: MediaLocalDataSource,
    private val cacheMetadataDao: CacheMetadataDao,
    private val paginationSession: PaginationSession,
    private val timeProvider: TimeProvider,
    private val featureKey: String,
    private val ioDispatcher: CoroutineDispatcher
) : MediaRepository {

    private val CACHE_EXPIRATION_MS = 10 * 60 * 1000L
    private val PAGINATION_LIMIT = 4

    override fun getMedia(): Flow<List<MediaItem>> = localDataSource.getMedia()
        .onStart {
            if (shouldRefreshCache()) {
                refresh()
            }
        }

    override suspend fun refresh(): Unit = withContext(ioDispatcher) {
        val remoteItems = mediaService.getMedia(limit = 20)
        val mediaItems = remoteItems.map { MediaItem(id = it.id, url = it.url) }
        
        localDataSource.clear()
        localDataSource.insertAll(mediaItems)
        
        cacheMetadataDao.insertMetadata(
            CacheMetadataEntity(
                featureKey = featureKey,
                lastUpdated = timeProvider.getCurrentTimeMillis(),
                nextPage = 2
            )
        )
        paginationSession.resetSession(featureKey)
    }

    override suspend fun loadNextPage(): Unit = withContext(ioDispatcher) {
        if (!paginationSession.canLoadMore(featureKey, PAGINATION_LIMIT)) return@withContext
        
        val metadata = cacheMetadataDao.getMetadata(featureKey) ?: return@withContext
        val nextPage = metadata.nextPage
        
        // Note: The current MediaService doesn't have a page parameter in the prompt's initial file, 
        // but we'll assume it supports it or we'll update it.
        // Actually, the requirements say "Use same endpoint with page parameter".
        // I'll update MediaService to include page.
        
        val remoteItems = mediaService.getMedia(limit = 20, page = nextPage)
        val mediaItems = remoteItems.map { MediaItem(id = it.id, url = it.url) }
        
        localDataSource.insertAll(mediaItems)
        
        cacheMetadataDao.insertMetadata(
            metadata.copy(
                nextPage = nextPage + 1
            )
        )
        paginationSession.incrementPageCount(featureKey)
    }

    private suspend fun shouldRefreshCache(): Boolean {
        val metadata = cacheMetadataDao.getMetadata(featureKey) ?: return true
        val currentTime = timeProvider.getCurrentTimeMillis()
        return (currentTime - metadata.lastUpdated) >= CACHE_EXPIRATION_MS
    }
}
