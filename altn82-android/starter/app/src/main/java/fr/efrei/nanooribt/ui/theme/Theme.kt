package fr.efrei.nanooribt.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val SpaceColorScheme = darkColorScheme(
    primary = SpaceWhite,
    onPrimary = SpaceBlack,
    primaryContainer = Surface2,
    onPrimaryContainer = TextPrimary,

    secondary = TextSecondary,
    onSecondary = SpaceBlack,
    secondaryContainer = Surface2,
    onSecondaryContainer = TextSecondary,

    tertiary = AccentBlue,
    onTertiary = SpaceBlack,
    tertiaryContainer = AccentBlueDim,
    onTertiaryContainer = AccentBlue,

    background = Surface0,
    onBackground = TextPrimary,

    surface = Surface0,
    onSurface = TextPrimary,
    surfaceVariant = Surface1,
    onSurfaceVariant = TextSecondary,

    surfaceTint = SpaceWhite,

    error = StatusFailed,
    onError = SpaceWhite,
    errorContainer = Surface2,
    onErrorContainer = StatusFailed,

    outline = BorderMedium,
    outlineVariant = BorderSubtle,

    inverseSurface = SpaceWhite,
    inverseOnSurface = SpaceBlack,
    inversePrimary = SpaceBlack
)

private val SpaceShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun NanoOribtTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpaceBlack.toArgb()
            window.navigationBarColor = SpaceBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = SpaceColorScheme,
        typography = SpaceTypography,
        shapes = SpaceShapes,
        content = content
    )
}
