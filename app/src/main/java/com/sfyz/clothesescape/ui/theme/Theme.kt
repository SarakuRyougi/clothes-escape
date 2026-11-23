package com.sfyz.clothesescape.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Typeface

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val LocalNeum = staticCompositionLocalOf { WarmNeum }
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun ClothesEscapeTheme(
    mode: ThemeMode,
    reduceMotion: Boolean = false,
    fontFamily: FontFamily = FontFamily.Default,
    typeface: Typeface? = null,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val neum = if (dark) DarkNeum else WarmNeum

    val scheme = if (dark) {
        darkColorScheme(
            primary = neum.accent,
            onPrimary = neum.onAccent,
            primaryContainer = neum.accentSoft,
            onPrimaryContainer = neum.text,
            background = neum.bg,
            onBackground = neum.text,
            surface = neum.surface,
            onSurface = neum.text,
            surfaceVariant = neum.surfaceAlt,
            onSurfaceVariant = neum.textSoft,
            outline = neum.outline,
            error = neum.danger,
            scrim = neum.shadow
        )
    } else {
        lightColorScheme(
            primary = neum.accent,
            onPrimary = neum.onAccent,
            primaryContainer = neum.accentSoft,
            onPrimaryContainer = neum.text,
            background = neum.bg,
            onBackground = neum.text,
            surface = neum.surface,
            onSurface = neum.text,
            surfaceVariant = neum.surfaceAlt,
            onSurfaceVariant = neum.textSoft,
            outline = neum.outline,
            error = neum.danger,
            scrim = neum.shadow
        )
    }

    CompositionLocalProvider(
        LocalNeum provides neum,
        LocalReduceMotion provides reduceMotion,
        LocalAppTypeface provides typeface
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = appTypography(fontFamily),
            shapes = AppShapes,
            content = content
        )
    }
}
