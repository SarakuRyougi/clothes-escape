package com.sfyz.clothesescape.ui.board

import androidx.compose.ui.graphics.Color

enum class LandmarkSide { LEFT_WALL, RIGHT_WALL, NEAR_WALL, CENTER }

/**
 * A fixed object in the drying room, taken from the hand-drawn floor plan.
 * Rack numbers are 1-based and inclusive; [colFromFrac]/[colToFrac] place
 * centre-of-room objects across the grid width.
 */
data class Landmark(
    val label: String,
    val side: LandmarkSide,
    val rackFrom: Int,
    val rackTo: Int,
    val tint: Color,
    val tintDark: Color,
    val colFromFrac: Float = 0f,
    val colToFrac: Float = 1f,
    /** Side-wall objects (window, mirrors, entrance) print their name in white. */
    val showLabel: Boolean = false
) {
    fun color(dark: Boolean): Color = if (dark) tintDark else tint

    val isBlockedArea: Boolean get() = side == LandmarkSide.CENTER
}

/**
 * Layout read off 水房平面图.jpg, top to bottom:
 *
 *   racks 1-2   : 饮水机 · 洗衣机 — no slots here, the footprint is left empty
 *   racks 2-4   : 窗 (left wall)
 *   racks 5-8   : 镜子 (left wall) / 镜子 (right wall)
 *   racks 6-12  : 公共置物架 — no slots here either
 *   racks 9-12  : 镜子 (left wall) / 镜子 (right wall)
 *   rack 15     : 入口
 */
val RoomLandmarks: List<Landmark> = listOf(
    Landmark(
        "饮水机 · 洗衣机", LandmarkSide.CENTER, 1, 2,
        Color(0xFFC98A2E), Color(0xFFD89A3C), colFromFrac = 0.22f, colToFrac = 0.80f
    ),
    Landmark(
        "窗", LandmarkSide.LEFT_WALL, 2, 4,
        Color(0xFF4C8B84), Color(0xFF5FA39B), showLabel = true
    ),
    Landmark(
        "镜子", LandmarkSide.LEFT_WALL, 5, 8,
        Color(0xFF5A86AB), Color(0xFF6E9CC2), showLabel = true
    ),
    Landmark(
        "镜子", LandmarkSide.RIGHT_WALL, 5, 8,
        Color(0xFF5A86AB), Color(0xFF6E9CC2), showLabel = true
    ),
    Landmark(
        "公共置物架", LandmarkSide.CENTER, 6, 12,
        Color(0xFFB8912A), Color(0xFFC9A238), colFromFrac = 0.28f, colToFrac = 0.73f
    ),
    Landmark(
        "镜子", LandmarkSide.LEFT_WALL, 9, 12,
        Color(0xFF5A86AB), Color(0xFF6E9CC2), showLabel = true
    ),
    Landmark(
        "镜子", LandmarkSide.RIGHT_WALL, 9, 12,
        Color(0xFF5A86AB), Color(0xFF6E9CC2), showLabel = true
    ),
    Landmark(
        "入口", LandmarkSide.NEAR_WALL, 15, 15,
        Color(0xFF7E6396), Color(0xFF9375AC), showLabel = true
    )
)

/** Columns a centre-of-room block covers, snapped to whole cells. */
fun Landmark.colRange(cols: Int): IntRange {
    if (cols <= 0) return 0..0
    val hit = (0 until cols).filter { col ->
        val frac = (col + 0.5f) / cols.toFloat()
        frac >= colFromFrac && frac <= colToFrac
    }
    return if (hit.isEmpty()) 0..(cols - 1) else hit.first()..hit.last()
}

/**
 * The appliance / shelf footprint covering [cell], or null when the cell is a
 * normal hanging slot. Cells inside these blocks are not drawn and cannot be
 * selected — the area is left blank and labelled instead.
 */
fun blockedZoneFor(row: Int, col: Int, cols: Int): Landmark? {
    val rack = row + 1
    return RoomLandmarks.firstOrNull { mark ->
        mark.isBlockedArea &&
            rack in mark.rackFrom..mark.rackTo &&
            col in mark.colRange(cols)
    }
}

fun isBlocked(row: Int, col: Int, cols: Int): Boolean = blockedZoneFor(row, col, cols) != null
