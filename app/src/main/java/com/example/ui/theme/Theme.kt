package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.prefs.ThemeMode

private val DzLightColorScheme = lightColorScheme(
    primary = DzRoyalBlue,
    onPrimary = DzWhite,
    primaryContainer = DzRoyalBlueSoft,
    onPrimaryContainer = DzRoyalBlueDark,
    secondary = DzCrimsonRed,
    onSecondary = DzWhite,
    secondaryContainer = DzCrimsonSoft,
    onSecondaryContainer = DzCrimsonRedDark,
    tertiary = EmeraldSuccess,
    onTertiary = DzWhite,
    tertiaryContainer = EmeraldSuccessSoft,
    onTertiaryContainer = EmeraldSuccess,
    background = LightBackground,
    onBackground = DzNearBlack,
    surface = LightBackground,
    onSurface = DzNearBlack,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = DzWhite,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE6ECF5),
    error = DzCrimsonRed,
    onError = DzWhite,
    errorContainer = DzCrimsonSoft,
    onErrorContainer = DzCrimsonRedDark
)

private val DzDarkColorScheme = darkColorScheme(
    primary = DzRoyalBlueBright,
    onPrimary = Color(0xFF041D3D),
    primaryContainer = Color(0xFF143563),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = DzCrimsonBright,
    onSecondary = Color(0xFF3E0004),
    secondaryContainer = Color(0xFF4D1418),
    onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF113521),
    onTertiaryContainer = Color(0xFFBBF7D0),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF222B38),
    error = DzCrimsonBright,
    onError = Color(0xFF3E0004),
    errorContainer = Color(0xFF4D1418),
    onErrorContainer = Color(0xFFFFDAD9)
)

val DzShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DZDailyPlannerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    forceLightSystemBars: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DzDarkColorScheme else DzLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                val lightIcons = forceLightSystemBars || !isDark
                controller.isAppearanceLightStatusBars = lightIcons
                controller.isAppearanceLightNavigationBars = lightIcons
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = DzShapes,
        content = content
    )
}
