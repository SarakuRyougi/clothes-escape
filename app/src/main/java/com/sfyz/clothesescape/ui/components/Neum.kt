package com.sfyz.clothesescape.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.innerShadow
import com.sfyz.clothesescape.ui.theme.softShadow

@Composable
fun NeumSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    color: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val neum = LocalNeum.current
    Box(
        modifier = modifier
            .softShadow(corner = corner, offset = 5.dp)
            .background(color ?: neum.surface, RoundedCornerShape(corner)),
        content = content
    )
}

@Composable
fun PressableNeum(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    color: Color? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val neum = LocalNeum.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.972f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.7f),
        label = "press-scale"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .softShadow(corner = corner, offset = if (pressed) 2.dp else 5.dp)
            .clip(RoundedCornerShape(corner))
            .background(color ?: neum.surface)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun NeumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val neum = LocalNeum.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn")
    val bg = when {
        !enabled -> neum.surfaceAlt
        primary -> neum.accent
        else -> neum.surface
    }
    val fg = when {
        !enabled -> neum.textSoft
        primary -> neum.onAccent
        else -> neum.text
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .softShadow(corner = 18.dp, offset = if (pressed || !enabled) 2.dp else 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, color = fg)
        }
    }
}

@Composable
fun NeumChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "chip")
    val bg = if (selected) neum.accent else neum.surface
    val fg = if (selected) neum.onAccent else neum.text
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (selected) Modifier
                    .softShadow(corner = 14.dp, offset = 3.dp, alpha = 0.4f)
                    .background(bg, RoundedCornerShape(14.dp))
                else
                    Modifier
                        .softShadow(corner = 14.dp, offset = 3.dp, alpha = 0.55f)
                        .background(bg, RoundedCornerShape(14.dp))
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatPill(
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    Row(
        modifier = modifier
            .softShadow(corner = 16.dp, offset = 4.dp)
            .background(neum.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(tint, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(7.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = neum.text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = neum.textSoft)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val neum = LocalNeum.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = neum.text,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    suffix: String = "",
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    Row(
        modifier = modifier
            .softShadow(corner = 16.dp, offset = 3.dp)
            .background(neum.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton("−", enabled = value > min) { onChange((value - 1).coerceAtLeast(min)) }
        Text(
            text = "$value$suffix",
            style = MaterialTheme.typography.titleMedium,
            color = neum.text,
            modifier = Modifier
                .width(56.dp)
                .padding(horizontal = 4.dp)
        )
        StepButton("+", enabled = value < max) { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val neum = LocalNeum.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(neum.surfaceAlt)
            .innerShadow(corner = 10.dp, alpha = 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = if (enabled) neum.text else neum.textSoft)
    }
}

@Composable
fun BottomTabs(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val neum = LocalNeum.current
    val items = listOf(
        "晾晒" to Icons.Filled.Home,
        "记录" to Icons.Filled.List,
        "我的" to Icons.Filled.Settings
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .softShadow(corner = 24.dp, offset = 6.dp)
            .background(neum.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { index, (label, icon) ->
            val isSelected = index == selected
            val fg = if (isSelected) neum.onAccent else neum.textSoft
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (isSelected) Modifier.background(neum.accent, RoundedCornerShape(16.dp))
                        else Modifier
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    val neum = LocalNeum.current
    Box(
        modifier = modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 44.dp, height = 4.dp)
            .background(neum.outline, RoundedCornerShape(2.dp))
    )
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun ColumnSpacer(h: Dp) {
    Spacer(modifier = Modifier.height(h))
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    val neum = LocalNeum.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = neum.textSoft)
    }
}
