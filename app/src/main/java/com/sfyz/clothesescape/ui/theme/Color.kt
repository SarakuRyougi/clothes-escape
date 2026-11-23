package com.sfyz.clothesescape.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Every colour in the app comes from one of these two palettes.
 * Light = warm cream neumorphism, dark = warm charcoal neumorphism.
 */
@Immutable
data class NeumColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val light: Color,
    val shadow: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val text: Color,
    val textSoft: Color,
    val outline: Color,
    val danger: Color,
    val success: Color,
    val isDark: Boolean
)

val WarmNeum = NeumColors(
    bg = Color(0xFFF7F1E8),
    surface = Color(0xFFFBF6EF),
    surfaceAlt = Color(0xFFF1E8DA),
    light = Color(0xFFFFFFFF),
    shadow = Color(0xFFD8C6AF),
    accent = Color(0xFFE0784E),
    accentDeep = Color(0xFFBF5D33),
    accentSoft = Color(0xFFF6C3A6),
    onAccent = Color(0xFFFFFDFA),
    text = Color(0xFF3B3229),
    textSoft = Color(0xFF8B7B69),
    outline = Color(0xFFE6D8C6),
    danger = Color(0xFFC0553F),
    success = Color(0xFF7C9A78),
    isDark = false
)

val DarkNeum = NeumColors(
    bg = Color(0xFF211D19),
    surface = Color(0xFF2A2420),
    surfaceAlt = Color(0xFF1B1714),
    light = Color(0xFF3B332C),
    shadow = Color(0xFF0F0C0A),
    accent = Color(0xFFF08A5D),
    accentDeep = Color(0xFFD0703F),
    accentSoft = Color(0xFF7A4B36),
    onAccent = Color(0xFF2A1A12),
    text = Color(0xFFF2E9DE),
    textSoft = Color(0xFFA99A89),
    outline = Color(0xFF3A322C),
    danger = Color(0xFFE07A62),
    success = Color(0xFF9DBB98),
    isDark = true
)

/** Palette used to tell concurrent drying records apart on the board. */
val RecordAccents = listOf(
    Color(0xFFE0784E),
    Color(0xFFD9A03F),
    Color(0xFF7FA07A),
    Color(0xFF9C7BB0),
    Color(0xFF6E93B5),
    Color(0xFFCC7F87)
)

fun accentOf(index: Int): Color {
    val n = RecordAccents.size
    return RecordAccents[((index % n) + n) % n]
}
