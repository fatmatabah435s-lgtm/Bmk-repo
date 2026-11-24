package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ZikrSanctuaryColors(
    val beadCompleted: Color,
    val beadUpcoming: Color,
    val beadHighlight: Color,
    val orbGlowStart: Color,
    val orbGlowEnd: Color,
    val pausedTint: Color,
    val goldAccent: Color,
    val goldContainer: Color,
    val onGoldContainer: Color
)

private val LightSanctuaryColors = ZikrSanctuaryColors(
    beadCompleted = SanctuaryEmerald,
    beadUpcoming = Color(0xFFDCD3C2),
    beadHighlight = BurnishedGold,
    orbGlowStart = Color(0xFFE7F3EE),
    orbGlowEnd = Color(0xFFF6EFE3),
    pausedTint = BurnishedGold,
    goldAccent = BurnishedGold,
    goldContainer = BurnishedGoldContainer,
    onGoldContainer = OnBurnishedGoldContainer
)

private val DarkSanctuaryColors = ZikrSanctuaryColors(
    beadCompleted = DarkCeladon,
    beadUpcoming = Color(0xFF283A35),
    beadHighlight = DarkGold,
    orbGlowStart = Color(0xFF173129),
    orbGlowEnd = Color(0xFF121D1A),
    pausedTint = DarkGold,
    goldAccent = DarkGold,
    goldContainer = DarkGoldContainer,
    onGoldContainer = DarkOnGoldContainer
)

val LocalSanctuaryColors = staticCompositionLocalOf { LightSanctuaryColors }

private val DarkColorScheme = darkColorScheme(
    primary = DarkCeladon,
    onPrimary = DarkOnCeladon,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkGold,
    onSecondary = DarkOnGold,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = DarkOnGoldContainer,
    tertiary = DarkSage,
    onTertiary = DarkOnSage,
    tertiaryContainer = DarkSageContainer,
    onTertiaryContainer = DarkOnSageContainer,
    background = MidnightBackground,
    onBackground = AlabasterInk,
    surface = MidnightSurface,
    onSurface = AlabasterInk,
    surfaceVariant = MidnightSurfaceContainer,
    onSurfaceVariant = AlabasterMuted,
    surfaceContainer = MidnightSurfaceContainer,
    surfaceContainerHigh = MidnightSurfaceContainerHigh,
    outline = MidnightOutline,
    outlineVariant = Color(0xFF23332E)
)

private val LightColorScheme = lightColorScheme(
    primary = SanctuaryEmerald,
    onPrimary = SanctuaryOnEmerald,
    primaryContainer = SanctuaryEmeraldContainer,
    onPrimaryContainer = SanctuaryOnEmeraldContainer,
    secondary = BurnishedGold,
    onSecondary = OnBurnishedGold,
    secondaryContainer = BurnishedGoldContainer,
    onSecondaryContainer = OnBurnishedGoldContainer,
    tertiary = SageMuted,
    onTertiary = OnSageMuted,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = OnSageContainer,
    background = IvoryBackground,
    onBackground = InkCharcoal,
    surface = IvorySurface,
    onSurface = InkCharcoal,
    surfaceVariant = LinenSurfaceContainer,
    onSurfaceVariant = InkMuted,
    surfaceContainer = LinenSurfaceContainer,
    surfaceContainerHigh = LinenSurfaceContainerHigh,
    outline = WarmOutline,
    outlineVariant = Color(0xFFE5DDD0)
)

@Composable
fun ZikrTallyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val sanctuaryColors = if (darkTheme) DarkSanctuaryColors else LightSanctuaryColors

    CompositionLocalProvider(LocalSanctuaryColors provides sanctuaryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
