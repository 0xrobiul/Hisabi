package me.robiul.hisabi.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = Color(0xFF000000),
    primaryContainer = ChampagneContainerDark,
    onPrimaryContainer = ChampagneGoldSoft,
    secondary = AccentGreen,
    onSecondary = Color(0xFF000000),
    secondaryContainer = AccentGreenContainerDark,
    onSecondaryContainer = Color(0xFF8BE5C3),
    tertiary = SemanticTransfer,
    onTertiary = Color(0xFF000000),
    background = DarkBgPrimary,
    onBackground = TextPrimaryDark,
    surface = DarkBgPrimary,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = DarkBgPrimary,
    surfaceContainerLow = DarkBgSecondary,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkSurfaceHighest,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = SemanticExpense,
    onError = Color(0xFF1A0406),
)

private val LightColorScheme = lightColorScheme(
    primary = ChampagneGoldDeep,
    onPrimary = Color.White,
    primaryContainer = ChampagneContainerLight,
    onPrimaryContainer = Color(0xFF3E2D0A),
    secondary = AccentGreenDeep,
    onSecondary = Color.White,
    secondaryContainer = AccentGreenContainerLight,
    onSecondaryContainer = Color(0xFF08422C),
    tertiary = Color(0xFF3B6EC4),
    onTertiary = Color.White,
    background = LightBgPrimary,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightSurfaceHighest,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = SemanticExpenseDeep,
    onError = Color.White,
)

val CardVaultShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CardVaultTheme(
    darkThemeOverride: Boolean? = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = darkThemeOverride ?: isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CardVaultTypography,
        shapes = CardVaultShapes,
        content = content,
    )
}
