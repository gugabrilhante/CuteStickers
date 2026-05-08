package com.gustavo.brilhante.cutestickers.common

import javax.inject.Inject

interface TimeProvider {
    fun getCurrentTimeMillis(): Long
}

class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
}
