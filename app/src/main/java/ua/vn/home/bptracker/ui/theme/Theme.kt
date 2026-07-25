package ua.vn.home.bptracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBgPrimary,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    secondary = DarkNavBar,
    onSecondary = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextMuted,
    tertiary = ColorPulse
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBgPrimary,
    surface = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    secondary = LightSurface,
    onSecondary = LightTextPrimary,
    surfaceVariant = LightBgPrimary,
    onSurfaceVariant = LightTextMuted,
    tertiary = ColorPulse
)

@Composable
fun BPTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color as per design plan (Brand theme, not Material You)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic color is explicitly disabled in the design plan to maintain brand consistency
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
