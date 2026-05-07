package com.gustavo.brilhante.cutecats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gustavo.brilhante.cutecats.core.designsystem.theme.CuteStickersTheme
import com.gustavo.brilhante.cutecats.ui.CuteStickersApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.e("Sticker - MainActivity", "CRITICAL: MainActivity.onCreate called!")
        enableEdgeToEdge()
        setContent {
            CuteStickersTheme {
                CuteStickersApp()
            }
        }
    }
}
