package com.sfyz.clothesescape.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/**
 * The app's display face: 方正喵呜_GBK, bundled under assets/fonts/.
 *
 * It is loaded lazily and defensively — if the file is missing (for example a
 * build with the font stripped out) every call falls back to the system face
 * instead of crashing during layout.
 */
object AppFonts {

    const val ASSET_PATH = "fonts/fzmiaowu_gbk.ttf"

    fun hasAsset(context: Context): Boolean = runCatching {
        context.assets.open(ASSET_PATH).close()
        true
    }.getOrDefault(false)

    fun composeFamily(context: Context): FontFamily =
        if (hasAsset(context)) {
            runCatching { FontFamily(Font(ASSET_PATH, context.assets)) }
                .getOrDefault(FontFamily.Default)
        } else {
            FontFamily.Default
        }

    fun typeface(context: Context): Typeface? =
        if (hasAsset(context)) {
            runCatching { Typeface.createFromAsset(context.assets, ASSET_PATH) }.getOrNull()
        } else {
            null
        }
}

/** Shared typeface for canvas-drawn labels, so the board matches the UI. */
val LocalAppTypeface = staticCompositionLocalOf<Typeface?> { null }
