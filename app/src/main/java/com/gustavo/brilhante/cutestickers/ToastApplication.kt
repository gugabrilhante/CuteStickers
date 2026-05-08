package com.gustavo.brilhante.cutestickers

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ToastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
