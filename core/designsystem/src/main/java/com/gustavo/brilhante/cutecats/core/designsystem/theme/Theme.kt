package com.gustavo.brilhante.cutecats.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrightMint,
    onPrimary = OnBrightMint,
    primaryContainer = BrightMintContainer,
    onPrimaryContainer = OnBrightMintContainer,
    secondary = BrightLime,
    onSecondary = OnBrightLime,
    secondaryContainer = BrightLimeContainer,
    onSecondaryContainer = OnBrightLimeContainer,
    tertiary = DarkPastelGreen,
    onTertiary = OnDarkPastelGreen,
    tertiaryContainer = DarkPastelGreenContainer,
    onTertiaryContainer = OnDarkPastelGreenContainer,
    background = DarkBackground,
    onBackground = OnMintBackground,
    surface = DarkSurface,
    onSurface = OnMintSurface,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = MintGreen,
    onPrimary = OnMintGreen,
    primaryContainer = MintGreenContainer,
    onPrimaryContainer = OnMintGreenContainer,
    secondary = SoftLime,
    onSecondary = OnSoftLime,
    secondaryContainer = SoftLimeContainer,
    onSecondaryContainer = OnSoftLimeContainer,
    tertiary = PastelGreen,
    onTertiary = OnPastelGreen,
    tertiaryContainer = PastelGreenContainer,
    onTertiaryContainer = OnPastelGreenContainer,
    background = MintBackground,
    onBackground = OnMintBackground,
    surface = MintSurface,
    onSurface = OnMintSurface,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun CuteStickersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize our brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
