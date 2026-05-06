package com.gustavo.brilhante.cutecats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gustavo.brilhante.cutecats.core.designsystem.theme.CuteCatsTheme
import com.gustavo.brilhante.cutecats.ui.CuteCatsApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CuteCatsTheme {
                CuteCatsApp()
            }
        }
    }
}
