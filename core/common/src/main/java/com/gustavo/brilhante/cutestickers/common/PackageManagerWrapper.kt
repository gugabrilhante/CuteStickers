package com.gustavo.brilhante.cutestickers.common

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PackageManagerWrapper {
    fun isPackageInstalled(packageName: String): Boolean
}

@Singleton
class AndroidPackageManagerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) : PackageManagerWrapper {
    override fun isPackageInstalled(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
