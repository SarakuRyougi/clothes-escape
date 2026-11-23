package com.sfyz.clothesescape.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Scales, lifts and fades its content in on first composition. Used for record
 * cards so a freshly created entry pops in instead of appearing instantly.
 */
@Composable
fun PopIn(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val anim = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            val v = anim.value
            alpha = v.coerceIn(0f, 1f)
            val scale = 0.82f + 0.18f * v
            scaleX = scale
            scaleY = scale
            translationY = (1f - v) * 46f
        }
    ) {
        content()
    }
}
