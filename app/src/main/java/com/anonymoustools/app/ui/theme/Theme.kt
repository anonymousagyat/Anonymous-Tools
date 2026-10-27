package com.anonymoustools.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PastelColorScheme = lightColorScheme(
    primary = TextBlack,
    onPrimary = CardWhite,
    primaryContainer = PastelMintGreen,
    onPrimaryContainer = TextBlack,
    secondary = PastelPeach,
    onSecondary = TextBlack,
    tertiary = PastelLavender,
    onTertiary = TextBlack,
    background = CreamBackground,
    onBackground = TextBlack,
    surface = CardWhite,
    onSurface = TextBlack,
    surfaceVariant = CreamBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun AnonymousToolsTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PastelColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = true
            insets.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
