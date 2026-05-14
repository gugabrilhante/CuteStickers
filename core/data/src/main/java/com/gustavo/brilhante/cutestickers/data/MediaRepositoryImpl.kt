package com.gustavo.brilhante.cutestickers.data

import android.content.res.AssetManager
import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.ToastManager
import com.gustavo.brilhante.cutestickers.common.network.NetworkMonitor
import com.gustavo.brilhante.cutestickers.database.CacheMetadataDao
import com.gustavo.brilhante.cutestickers.database.CacheMetadataEntity
import com.gustavo.brilhante.cutestickers.domain.MediaRepository
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.network.model.NetworkMediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.random.Random

class MediaRepositoryImpl(
    private val mediaService: MediaService,
    private val localDataSource: MediaLocalDataSource,
    private val cacheMetadataDao: CacheMetadataDao,
    private val paginationSession: PaginationSession,
    private val timeProvider: TimeProvider,
    private val networkMonitor: NetworkMonitor,
    private val toastManager: ToastManager,
    private val assetManager: AssetManager,
    private val json: Json,
    private val featureKey: String,
    private val seedFiles: List<String>,
    private val ioDispatcher: CoroutineDispatcher,
    private val cacheExpirationMs: Long = 60 * 60 * 1000L // 1 hour default
) : MediaRepository {

    private val PAGINATION_LIMIT = 4

    override fun getMedia(): Flow<List<MediaItem>> = localDataSource.getMedia()

    override suspend fun refresh(force: Boolean): Boolean = withContext(ioDispatcher) {
        val isOnline = try {
            networkMonitor.isOnline.firstOrNull() ?: false
        } catch (e: Exception) {
            false
        }
        
        if (!isOnline) {
            if (force) toastManager.showToast("Sem internet. Exibindo conteúdo offline.")
            handleFailure()
            return@withContext false
        }

        if (!force && !shouldRefreshCache()) return@withContext false

        try {
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
            true
        } catch (e: Exception) {
            if (force) toastManager.showToast("Falha ao atualizar. Exibindo conteúdo salvo.")
            handleFailure()
            false
        }
    }

    private suspend fun handleFailure() {
        if (localDataSource.isEmpty()) {
            loadFromAssets()
        }
    }

    private suspend fun loadFromAssets() {
        if (seedFiles.isEmpty()) return
        val seedFile = seedFiles.random(Random.Default)
        try {
            val jsonString = assetManager.open(seedFile).bufferedReader().use { it.readText() }
            val networkItems = json.decodeFromString<List<NetworkMediaItem>>(jsonString)
            val mediaItems = networkItems.map { MediaItem(id = it.id, url = it.url) }
            localDataSource.insertAll(mediaItems)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadNextPage(): Unit = withContext(ioDispatcher) {
        if (!paginationSession.canLoadMore(featureKey, PAGINATION_LIMIT)) return@withContext
        
        val isOnline = try {
            networkMonitor.isOnline.firstOrNull() ?: false
        } catch (e: Exception) {
            false
        }
        if (!isOnline) return@withContext

        val metadata = cacheMetadataDao.getMetadata(featureKey) ?: return@withContext
        val nextPage = metadata.nextPage
        
        try {
            val remoteItems = mediaService.getMedia(limit = 20, page = nextPage)
            val mediaItems = remoteItems.map { MediaItem(id = it.id, url = it.url) }
            
            localDataSource.insertAll(mediaItems)
            
            cacheMetadataDao.insertMetadata(
                metadata.copy(
                    nextPage = nextPage + 1
                )
            )
            paginationSession.incrementPageCount(featureKey)
        } catch (e: Exception) {
            // Error handled
        }
    }

    private suspend fun shouldRefreshCache(): Boolean {
        val metadata = cacheMetadataDao.getMetadata(featureKey) ?: return true
        val currentTime = timeProvider.getCurrentTimeMillis()
        return (currentTime - metadata.lastUpdated) >= cacheExpirationMs
    }
}
