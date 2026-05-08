package com.gustavo.brilhante.cutestickers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gustavo.brilhante.cutestickers.designsystem.theme.CuteStickersTheme
import com.gustavo.brilhante.cutestickers.ui.CuteStickersApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CuteStickersTheme {
                CuteStickersApp()
            }
        }
    }
}
