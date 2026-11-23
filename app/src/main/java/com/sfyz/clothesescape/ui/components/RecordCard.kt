package com.sfyz.clothesescape.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sfyz.clothesescape.data.RecordUi
import com.sfyz.clothesescape.data.STATUS_ALL_LOST
import com.sfyz.clothesescape.data.STATUS_CANCELLED
import com.sfyz.clothesescape.data.STATUS_COLLECTED
import com.sfyz.clothesescape.data.STATUS_PARTIAL_LOST
import com.sfyz.clothesescape.data.TimeUtils
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.softShadow
import java.io.File

/** Tiny t-shirt glyph used as the app's in-app mark. */
@Composable
fun ShirtMark(
    modifier: Modifier = Modifier,
    color: Color? = null,
    stroke: Boolean = false
) {
    val neum = LocalNeum.current
    val tint = color ?: neum.accent
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.30f)
            lineTo(w * 0.38f, h * 0.18f)
            lineTo(w * 0.50f, h * 0.28f)
            lineTo(w * 0.62f, h * 0.18f)
            lineTo(w * 0.76f, h * 0.30f)
            lineTo(w * 0.68f, h * 0.42f)
            lineTo(w * 0.68f, h * 0.80f)
            lineTo(w * 0.32f, h * 0.80f)
            lineTo(w * 0.32f, h * 0.42f)
            close()
        }
        if (stroke) {
            drawPath(path, color = tint, style = Stroke(width = w * 0.07f))
        } else {
            drawPath(path, color = tint)
        }
        // little running legs
        drawLine(
            color = tint,
            start = Offset(w * 0.40f, h * 0.80f),
            end = Offset(w * 0.36f, h * 0.94f),
            strokeWidth = w * 0.06f
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.58f, h * 0.80f),
            end = Offset(w * 0.63f, h * 0.94f),
            strokeWidth = w * 0.06f
        )
    }
}

@Composable
fun ShirtBadge(modifier: Modifier = Modifier, size: Dp = 38.dp) {
    val neum = LocalNeum.current
    Box(
        modifier = modifier
            .size(size)
            .softShadow(corner = (size / 2), offset = 4.dp)
            .background(neum.accent, RoundedCornerShape(size / 2)),
        contentAlignment = Alignment.Center
    ) {
        ShirtMark(
            modifier = Modifier.size(size * 0.55f),
            color = neum.onAccent
        )
    }
}

fun statusLabel(record: RecordUi, now: Long): String = when {
    record.status == STATUS_COLLECTED -> "已收"
    record.status == STATUS_PARTIAL_LOST -> "有丢失"
    record.status == STATUS_ALL_LOST -> "全丢失"
    record.status == STATUS_CANCELLED -> "已取消"
    record.isOverdue(now) -> "逾期"
    else -> "晾晒中"
}

@Composable
fun statusColor(record: RecordUi, now: Long): Color {
    val neum = LocalNeum.current
    return when {
        record.status == STATUS_COLLECTED -> neum.success
        record.status == STATUS_PARTIAL_LOST || record.status == STATUS_ALL_LOST -> neum.danger
        record.status == STATUS_CANCELLED -> neum.textSoft
        record.isOverdue(now) -> neum.danger
        else -> neum.accent
    }
}

@Composable
fun RecordCard(
    record: RecordUi,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val accent = statusColor(record, now)
    PressableNeum(
        onClick = onClick,
        modifier = modifier.width(172.dp).height(126.dp),
        corner = 22.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 16.dp, height = 3.dp)
                        .background(accent, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = record.regionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = neum.textSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = record.names,
                    style = MaterialTheme.typography.titleMedium,
                    color = neum.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val photo = record.photoPath
                if (photo != null && File(photo).exists()) {
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(
                        model = File(photo),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (record.active) TimeUtils.remainingText(now, record.dueAt) else statusLabel(record, now),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                if (record.active) {
                    Text(
                        text = "${record.items.size} 件",
                        style = MaterialTheme.typography.labelMedium,
                        color = neum.textSoft
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryRow(
    record: RecordUi,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val accent = statusColor(record, now)
    PressableNeum(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        corner = 20.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 34.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = record.names,
                    style = MaterialTheme.typography.titleMedium,
                    color = neum.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.regionText + " · " + TimeUtils.weekday(record.hungAt) +
                        " " + TimeUtils.time(record.hungAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = neum.textSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel(record, now),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    val neum = LocalNeum.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = neum.textSoft)
    }
}
