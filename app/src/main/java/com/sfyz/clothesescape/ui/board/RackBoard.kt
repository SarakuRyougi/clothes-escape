package com.sfyz.clothesescape.ui.board

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfyz.clothesescape.data.RACK_COUNT
import com.sfyz.clothesescape.data.Slot
import com.sfyz.clothesescape.ui.theme.LocalAppTypeface
import com.sfyz.clothesescape.ui.theme.LocalNeum
import com.sfyz.clothesescape.ui.theme.LocalReduceMotion
import com.sfyz.clothesescape.ui.theme.NeumColors
import com.sfyz.clothesescape.ui.theme.accentOf
import com.sfyz.clothesescape.ui.theme.innerShadow
import com.sfyz.clothesescape.ui.theme.softShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** What sits on one hanging position, denormalised for drawing. */
data class BoardOccupant(
    val recordId: Long,
    val accentIndex: Int,
    val count: Int,
    val overdue: Boolean
)

/**
 * The drying-room board: 15 rack rows x N slots, drawn on a single canvas so a
 * press-and-drag can sweep out a rectangular region.
 *
 * The appliance and shelf footprints carry no slots and no decoration at all —
 * the tray background simply shows through, with a quiet text label. Every slot
 * and every wall landmark is a raised neumorphic plate built from a soft
 * highlight/shadow pair, matching the app's buttons.
 */
@Composable
fun RackBoard(
    occupied: Map<Slot, BoardOccupant>,
    cols: Int,
    onTapCell: (Cell) -> Unit,
    onCreateRegion: (List<Cell>, skipped: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val neum = LocalNeum.current
    val reduceMotion = LocalReduceMotion.current
    val density = LocalDensity.current
    val appTypeface = LocalAppTypeface.current
    val haptics = LocalHapticFeedback.current

    var selection by remember { mutableStateOf<Region?>(null) }
    var anchor by remember { mutableStateOf<Cell?>(null) }

    val progress = remember { Animatable(0f) }
    var selectionActive by remember { mutableStateOf(false) }

    LaunchedEffect(selectionActive) {
        progress.snapTo(0f)
        if (selectionActive) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 62,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    // One global clock drives every shaking record; per-record phase offsets keep
    // them from marching in lockstep. Read only inside the draw pass, so a calm
    // board does not redraw on every frame.
    val clock = rememberInfiniteTransition(label = "alarm")
    val alarmPhase = clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alarm-phase"
    )

    val leftGutterPx = with(density) { 62.dp.toPx() }
    val rightGutterPx = with(density) { 50.dp.toPx() }
    val topStripPx = with(density) { 10.dp.toPx() }
    val bottomStripPx = with(density) { 34.dp.toPx() }
    val gapPx = with(density) { 3.dp.toPx() }
    val maxCellH = with(density) { 40.dp.toPx() }
    val shakeAmp = with(density) { 4.2.dp.toPx() }

    val rackPaint = remember(neum, density, appTypeface) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = appTypeface ?: Typeface.SANS_SERIF
            color = neum.textSoft.toArgb()
            textSize = with(density) { 11.sp.toPx() }
        }
    }
    val countPaint = remember(density, appTypeface) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = appTypeface ?: Typeface.SANS_SERIF
            color = Color.White.toArgb()
            textSize = with(density) { 11.sp.toPx() }
        }
    }
    val labelPaint = remember(density, appTypeface) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = appTypeface ?: Typeface.SANS_SERIF
            color = Color.White.toArgb()
            textSize = with(density) { 13.sp.toPx() }
        }
    }
    val zonePaint = remember(neum, density, appTypeface) {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = appTypeface ?: Typeface.SANS_SERIF
            color = neum.textSoft.copy(alpha = if (neum.isDark) 1f else 0.8f).toArgb()
            textSize = with(density) { 12.sp.toPx() }
        }
    }

    Box(
        modifier = modifier
            .softShadow(corner = 30.dp, offset = 5.dp, alpha = 0.26f)
            .clip(RoundedCornerShape(30.dp))
            // Recessed tray: a shade darker than the plates sitting in it.
            .background(neum.surfaceAlt)
            .innerShadow(corner = 30.dp, width = 2.dp, alpha = 0.6f)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val metrics = remember(widthPx, heightPx, cols) {
                computeMetrics(
                    width = widthPx,
                    height = heightPx,
                    rows = RACK_COUNT,
                    cols = cols,
                    leftGutter = leftGutterPx,
                    rightGutter = rightGutterPx,
                    gapX = gapPx,
                    gapY = gapPx,
                    maxCellH = maxCellH,
                    topStrip = topStripPx,
                    bottomStrip = bottomStripPx
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(metrics, occupied) {
                        // One gesture loop handles both a quick tap (single slot)
                        // and a press-then-drag (rectangular sweep).
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startCell = metrics.cellAt(down.position.x, down.position.y)
                            val blockedStart = startCell == null ||
                                isBlocked(startCell.row, startCell.col, metrics.cols)

                            // Null means the finger lifted before the long-press
                            // timeout, i.e. a tap.
                            val longPress = awaitLongPressOrCancellation(down.id)
                            if (longPress == null) {
                                if (!blockedStart && startCell != null) onTapCell(startCell)
                                return@awaitEachGesture
                            }
                            if (blockedStart || startCell == null) return@awaitEachGesture

                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            anchor = startCell
                            selection = regionOf(startCell, startCell)
                            selectionActive = true

                            var cursor = startCell
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val cell = metrics.cellAt(change.position.x, change.position.y)
                                if (cell != null && cell != cursor) {
                                    cursor = cell
                                    selection = regionOf(startCell, cell)
                                }
                                change.consume()
                            }

                            val region = selection
                            anchor = null
                            selection = null
                            selectionActive = false
                            if (region != null) {
                                val free = region.cells().filter {
                                    val slot = Slot(it.row + 1, it.col + 1)
                                    !isBlocked(it.row, it.col, metrics.cols) && slot !in occupied.keys
                                }
                                val skipped = region.size - free.size
                                if (free.isEmpty()) {
                                    // nothing usable in this sweep
                                } else if (free.size == 1) {
                                    onTapCell(free.first())
                                } else {
                                    onCreateRegion(free, skipped)
                                }
                            }
                        }
                    }
            ) {
                drawRoom(
                    metrics = metrics,
                    occupied = occupied,
                    selection = selection,
                    progress = progress.value,
                    alarmPhase = alarmPhase,
                    animateAlarm = !reduceMotion,
                    shakeAmp = shakeAmp,
                    colors = neum,
                    rackPaint = rackPaint,
                    countPaint = countPaint,
                    labelPaint = labelPaint,
                    zonePaint = zonePaint
                )
            }
        }
    }
}

/**
 * Neumorphic plate: stacked offset highlight/shadow pairs behind a solid face.
 * Mirrors [com.sfyz.clothesescape.ui.theme.softShadow] so canvas-drawn elements
 * read exactly like the app's buttons and cards.
 */
private fun DrawScope.drawRaisedPlate(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    corner: Float,
    face: Color,
    colors: NeumColors,
    offset: Float,
    layers: Int = 4,
    shadowAlpha: Float = 0.45f
) {
    for (i in layers downTo 1) {
        val f = i.toFloat() / layers
        val d = offset * f
        val a = shadowAlpha * (1f - f * 0.7f)
        drawRoundRect(
            color = colors.light.copy(alpha = a * 0.85f),
            topLeft = Offset(left - d, top - d),
            size = Size(width + d * 2f, height + d * 2f),
            cornerRadius = CornerRadius(corner + d)
        )
        drawRoundRect(
            color = colors.shadow.copy(alpha = a),
            topLeft = Offset(left + d, top + d),
            size = Size(width, height),
            cornerRadius = CornerRadius(corner)
        )
    }
    drawRoundRect(
        color = face,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(corner)
    )
}

private fun DrawScope.drawRoom(
    metrics: BoardMetrics,
    occupied: Map<Slot, BoardOccupant>,
    selection: Region?,
    progress: Float,
    alarmPhase: State<Float>,
    animateAlarm: Boolean,
    shakeAmp: Float,
    colors: NeumColors,
    rackPaint: Paint,
    countPaint: Paint,
    labelPaint: Paint,
    zonePaint: Paint
) {
    drawWallBlocks(metrics, colors, labelPaint)
    drawGrid(
        metrics, occupied, selection, progress, alarmPhase,
        animateAlarm, shakeAmp, colors, rackPaint, countPaint
    )
    drawZoneLabels(metrics, zonePaint)
    drawEntrance(metrics, colors, labelPaint)
}

/** Window and mirrors: flat single-colour plates in the side gutters. */
private fun DrawScope.drawWallBlocks(
    metrics: BoardMetrics,
    colors: NeumColors,
    labelPaint: Paint
) {
    val blockW = 20.dp.toPx()
    val corner = 9.dp.toPx()
    val shadow = 2.2.dp.toPx()

    RoomLandmarks.filter {
        it.side == LandmarkSide.LEFT_WALL || it.side == LandmarkSide.RIGHT_WALL
    }.forEach { mark ->
        val isLeft = mark.side == LandmarkSide.LEFT_WALL
        val top = metrics.cellTop(mark.rackFrom - 1)
        val bottom = metrics.cellTop(mark.rackTo - 1) + metrics.cellH
        val left = if (isLeft) {
            metrics.gridLeft() - blockW - 15.dp.toPx()
        } else {
            metrics.gridRight() + 15.dp.toPx()
        }.coerceAtLeast(1.dp.toPx())

        drawRaisedPlate(
            left = left,
            top = top,
            width = blockW,
            height = bottom - top,
            corner = corner,
            face = mark.color(colors.isDark),
            colors = colors,
            offset = shadow,
            layers = 4
        )

        if (mark.showLabel) {
            labelPaint.color = Color.White.toArgb()
            val chars = mark.label.toCharArray()
            val lineH = labelPaint.textSize * 1.1f
            val startY = (top + bottom) / 2f - (chars.size - 1) * lineH / 2f -
                (labelPaint.ascent() + labelPaint.descent()) / 2f
            chars.forEachIndexed { i, c ->
                drawContext.canvas.nativeCanvas.drawText(
                    c.toString(), left + blockW / 2f, startY + i * lineH, labelPaint
                )
            }
        }
    }
}

/**
 * Labels for the blanked-out appliance and shelf footprints. No plate, no fill —
 * the tray background shows straight through.
 */
private fun DrawScope.drawZoneLabels(metrics: BoardMetrics, zonePaint: Paint) {
    RoomLandmarks.filter { it.isBlockedArea }.forEach { mark ->
        val range = mark.colRange(metrics.cols)
        val left = metrics.cellLeft(range.first)
        val right = metrics.cellLeft(range.last) + metrics.cellW
        val top = metrics.cellTop(mark.rackFrom - 1)
        val bottom = metrics.cellTop(mark.rackTo - 1) + metrics.cellH

        val cy = (top + bottom) / 2f - (zonePaint.ascent() + zonePaint.descent()) / 2f
        drawContext.canvas.nativeCanvas.drawText(mark.label, (left + right) / 2f, cy, zonePaint)
    }
}

/** Entrance: a flat single-colour plate under the last rack. */
private fun DrawScope.drawEntrance(
    metrics: BoardMetrics,
    colors: NeumColors,
    labelPaint: Paint
) {
    val mark = RoomLandmarks.firstOrNull { it.side == LandmarkSide.NEAR_WALL } ?: return
    val blockW = (metrics.gridRight() - metrics.gridLeft()) * 0.22f
    val blockH = metrics.bottomStrip - 8.dp.toPx()
    val left = (metrics.gridLeft() + metrics.gridRight()) / 2f - blockW / 2f
    val top = metrics.gridBottom() + 4.dp.toPx()

    drawRaisedPlate(
        left = left,
        top = top,
        width = blockW,
        height = blockH,
        corner = 10.dp.toPx(),
        face = mark.color(colors.isDark),
        colors = colors,
        offset = 2.2.dp.toPx(),
        layers = 4
    )

    labelPaint.color = Color.White.toArgb()
    val cy = top + blockH / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2f
    drawContext.canvas.nativeCanvas.drawText(mark.label, left + blockW / 2f, cy, labelPaint)
}

private fun DrawScope.drawGrid(
    metrics: BoardMetrics,
    occupied: Map<Slot, BoardOccupant>,
    selection: Region?,
    progress: Float,
    alarmPhase: State<Float>,
    animateAlarm: Boolean,
    shakeAmp: Float,
    colors: NeumColors,
    rackPaint: Paint,
    countPaint: Paint
) {
    val radius = min(metrics.cellW, metrics.cellH) * 0.24f
    val plateOffset = 2.4.dp.toPx()
    val selectionCells = selection?.cells().orEmpty()
    val selectionSize = selectionCells.size.coerceAtLeast(1)

    for (row in 0 until metrics.rows) {
        for (col in 0 until metrics.cols) {
            val left = metrics.cellLeft(col)
            val top = metrics.cellTop(row)
            val slot = Slot(row + 1, col + 1)
            val occupant = occupied[slot]

            // Appliance / shelf footprints stay completely blank. An occupied
            // cell still renders, so older records never vanish.
            if (isBlocked(row, col, metrics.cols) && occupant == null) continue

            if (occupant == null) {
                drawRaisedPlate(
                    left = left,
                    top = top,
                    width = metrics.cellW,
                    height = metrics.cellH,
                    corner = radius,
                    face = colors.surface,
                    colors = colors,
                    offset = plateOffset,
                    layers = 4,
                    shadowAlpha = 0.62f
                )
            } else {
                var dx = 0f
                var dy = 0f
                var beat = 0f
                if (occupant.overdue) {
                    if (animateAlarm) {
                        val p = alarmPhase.value
                        val phase = (p * 6f) + (occupant.recordId % 5L).toFloat() * 0.23f
                        dx = sin(phase * 2f * PI.toFloat()) * shakeAmp
                        dy = cos(phase * PI.toFloat()) * shakeAmp * 0.3f
                        beat = (sin(phase * 2f * PI.toFloat()) + 1f) / 2f
                    } else {
                        beat = 0.6f
                    }
                }
                val x = left + dx
                val y = top + dy
                val accent = accentOf(occupant.accentIndex)

                if (occupant.overdue) {
                    drawRoundRect(
                        color = colors.danger.copy(alpha = 0.10f + 0.16f * beat),
                        topLeft = Offset(x - 3f, y - 3f),
                        size = Size(metrics.cellW + 6f, metrics.cellH + 6f),
                        cornerRadius = CornerRadius(radius + 3f)
                    )
                }

                drawRaisedPlate(
                    left = x,
                    top = y,
                    width = metrics.cellW,
                    height = metrics.cellH,
                    corner = radius,
                    face = accent,
                    colors = colors,
                    offset = plateOffset,
                    layers = 4
                )

                if (occupant.count > 1 && metrics.cellW > 22f) {
                    val cx = x + metrics.cellW / 2f
                    val cy = y + metrics.cellH / 2f - (countPaint.ascent() + countPaint.descent()) / 2f
                    drawContext.canvas.nativeCanvas.drawText(occupant.count.toString(), cx, cy, countPaint)
                }

                if (occupant.overdue) {
                    drawRoundRect(
                        color = colors.danger.copy(alpha = 0.55f + 0.45f * beat),
                        topLeft = Offset(x, y),
                        size = Size(metrics.cellW, metrics.cellH),
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = colors.danger,
                        radius = min(metrics.cellW, metrics.cellH) * 0.13f,
                        center = Offset(x + metrics.cellW - radius * 0.55f, y + radius * 0.55f)
                    )
                }
            }
        }
        // rack number in the left gutter, pinned to the outer edge so the
        // mirror/window labels never collide with it
        val cy = metrics.cellTop(row) + metrics.cellH / 2f -
            (rackPaint.ascent() + rackPaint.descent()) / 2f
        drawContext.canvas.nativeCanvas.drawText(
            (row + 1).toString(),
            13.dp.toPx(),
            cy,
            rackPaint
        )
    }

    // selection sweep
    if (selection != null) {
        selectionCells.forEachIndexed { index, cell ->
            if (isBlocked(cell.row, cell.col, metrics.cols)) return@forEachIndexed
            val enter = ((progress * selectionSize) - index).coerceIn(0f, 1f)
            val left = metrics.cellLeft(cell.col)
            val top = metrics.cellTop(cell.row)
            val isConflict = occupied.containsKey(Slot(cell.row + 1, cell.col + 1))
            val tint = if (isConflict) colors.danger else colors.accent
            drawRoundRect(
                color = tint.copy(alpha = 0.10f + 0.22f * enter),
                topLeft = Offset(left, top),
                size = Size(metrics.cellW, metrics.cellH),
                cornerRadius = CornerRadius(radius)
            )
            drawRoundRect(
                color = tint.copy(alpha = 0.35f + 0.5f * enter),
                topLeft = Offset(left, top),
                size = Size(metrics.cellW, metrics.cellH),
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = 1.6.dp.toPx())
            )
        }
        val r = selection
        val bandLeft = metrics.cellLeft(r.c0) - 2f
        val bandTop = metrics.cellTop(r.r0) - 2f
        val bandWidth = metrics.cellW * r.cols + metrics.gapX * (r.cols - 1) + 4f
        val bandHeight = metrics.cellH * r.rows + metrics.gapY * (r.rows - 1) + 4f
        drawRoundRect(
            color = colors.accent.copy(alpha = 0.85f * progress),
            topLeft = Offset(bandLeft, bandTop),
            size = Size(bandWidth, bandHeight),
            cornerRadius = CornerRadius(radius + 3f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
