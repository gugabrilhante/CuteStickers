package com.gustavo.brilhante.cutestickers.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaginationSession @Inject constructor() {
    private val sessionPageCounts = mutableMapOf<String, Int>()
    
    fun getExtraPagesCount(featureKey: String): Int = sessionPageCounts.getOrDefault(featureKey, 0)
    
    fun incrementPageCount(featureKey: String) {
        sessionPageCounts[featureKey] = getExtraPagesCount(featureKey) + 1
    }
    
    fun resetSession(featureKey: String) {
        sessionPageCounts[featureKey] = 0
    }
    
    fun canLoadMore(featureKey: String, limit: Int = 4): Boolean {
        return getExtraPagesCount(featureKey) < limit
    }
}
