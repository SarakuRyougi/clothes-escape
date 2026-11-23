package com.sfyz.clothesescape.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorphic "soft shadow": a stack of offset rounded rectangles that fakes a
 * blurred dual-tone shadow without per-frame blur effects (cheap on low-end
 * devices and safe on every API level).
 *
 * Apply it *before* the surface background so the shadow sits behind the card.
 */
@Composable
fun Modifier.softShadow(
    corner: Dp = 24.dp,
    offset: Dp = 6.dp,
    layers: Int = 9,
    alpha: Float = 0.5f
): Modifier {
    val neum = LocalNeum.current
    return this.drawWithCache {
        val c = corner.toPx()
        val o = offset.toPx()
        onDrawBehind {
            for (i in layers downTo 1) {
                val f = i.toFloat() / layers
                val d = o * f
                val a = alpha * (1f - f * 0.7f)
                drawRoundRect(
                    color = neum.light.copy(alpha = a * 0.8f),
                    topLeft = Offset(-d, -d),
                    size = Size(size.width + d * 2f, size.height + d * 2f),
                    cornerRadius = CornerRadius(c + d)
                )
                drawRoundRect(
                    color = neum.shadow.copy(alpha = a),
                    topLeft = Offset(d, d),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(c)
                )
            }
        }
    }
}

/**
 * Inset (pressed / sunken) look: a hairline gradient ring that reads as a dent.
 */
@Composable
fun Modifier.innerShadow(
    corner: Dp = 18.dp,
    width: Dp = 2.dp,
    alpha: Float = 0.55f
): Modifier {
    val neum = LocalNeum.current
    return this.drawWithCache {
        val c = corner.toPx()
        val w = width.toPx()
        onDrawWithContent {
            drawContent()
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        neum.shadow.copy(alpha = alpha),
                        neum.light.copy(alpha = alpha * 0.6f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                topLeft = Offset(w / 2f, w / 2f),
                size = Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(c),
                style = Stroke(width = w)
            )
        }
    }
}
