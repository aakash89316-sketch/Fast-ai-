package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FastFightDarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = ArenaNight,
    primaryContainer = CyanDeep,
    onPrimaryContainer = TextPrimary,
    secondary = GoldAccent,
    onSecondary = ArenaNight,
    tertiary = CrimsonEnemy,
    onTertiary = TextPrimary,
    background = ArenaNight,
    onBackground = TextPrimary,
    surface = ArenaHeaderBg,
    onSurface = TextPrimary,
    surfaceVariant = ShadowCardBg,
    onSurfaceVariant = TextSecondary,
    outline = ShadowCardBorder,
    error = CrimsonEnemy,
    onError = Color.White,
)

@Composable
fun FastFight1Theme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FastFightDarkColorScheme,
        typography = FastFightTypography,
        content = content,
    )
}
