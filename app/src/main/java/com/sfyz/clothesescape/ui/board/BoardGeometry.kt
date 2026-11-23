package com.sfyz.clothesescape.ui.board

import kotlin.math.abs

/** Zero-based cell on the board: row 0..rows-1 (rack 1..15), col 0..cols-1 (slot 1..N). */
data class Cell(val row: Int, val col: Int)

/** Inclusive rectangle of cells, always normalised so r0 <= r1 and c0 <= c1. */
data class Region(val r0: Int, val c0: Int, val r1: Int, val c1: Int) {

    val rows: Int get() = r1 - r0 + 1
    val cols: Int get() = c1 - c0 + 1
    val size: Int get() = rows * cols

    fun cells(): List<Cell> {
        val out = ArrayList<Cell>(size)
        for (r in r0..r1) for (c in c0..c1) out.add(Cell(r, c))
        return out
    }

    fun contains(cell: Cell): Boolean =
        cell.row in r0..r1 && cell.col in c0..c1
}

fun regionOf(a: Cell, b: Cell): Region = Region(
    r0 = minOf(a.row, b.row),
    c0 = minOf(a.col, b.col),
    r1 = maxOf(a.row, b.row),
    c1 = maxOf(a.col, b.col)
)

/** Rack numbers (1-based) touched by the region, top to bottom. */
fun Region.racks(): IntRange = (r0 + 1)..(r1 + 1)

/** Slot numbers (1-based) touched by the region, left to right. */
fun Region.slots(): IntRange = (c0 + 1)..(c1 + 1)

/**
 * Geometry of the board canvas. Pure data so it can be unit-tested on the JVM;
 * all values are pixels.
 *
 * The board is a top-down map of the drying room: the left and right gutters are
 * the side walls (window, mirrors 1-4), the top strip is the far wall (water
 * dispenser / washing machine) and the bottom strip is the entrance.
 */
data class BoardMetrics(
    val rows: Int,
    val cols: Int,
    val leftGutter: Float,
    val rightGutter: Float,
    val topStrip: Float,
    val bottomStrip: Float,
    val cellW: Float,
    val cellH: Float,
    val gapX: Float,
    val gapY: Float,
    val top: Float
) {
    fun cellLeft(col: Int): Float = leftGutter + col * (cellW + gapX)
    fun cellTop(row: Int): Float = top + row * (cellH + gapY)

    fun gridLeft(): Float = cellLeft(0)
    fun gridRight(): Float = cellLeft(cols - 1) + cellW
    fun gridTop(): Float = cellTop(0)
    fun gridBottom(): Float = cellTop(rows - 1) + cellH

    /** Returns null when the point lands in a gutter/gap or outside the grid. */
    fun cellAt(x: Float, y: Float): Cell? {
        if (cols <= 0 || rows <= 0) return null
        if (x < leftGutter || y < top) return null
        val strideX = cellW + gapX
        val strideY = cellH + gapY
        val col = ((x - leftGutter) / strideX).toInt()
        val row = ((y - top) / strideY).toInt()
        if (col !in 0 until cols || row !in 0 until rows) return null
        val left = cellLeft(col)
        val topY = cellTop(row)
        if (x > left + cellW || y > topY + cellH) return null
        return Cell(row, col)
    }

    fun centerOf(cell: Cell): Pair<Float, Float> =
        (cellLeft(cell.col) + cellW / 2f) to (cellTop(cell.row) + cellH / 2f)
}

/**
 * Lays out [rows] x [cols] cells inside the given pixel box, keeping room for the
 * side walls and the far/near strips.
 */
fun computeMetrics(
    width: Float,
    height: Float,
    rows: Int,
    cols: Int,
    leftGutter: Float,
    rightGutter: Float,
    gapX: Float,
    gapY: Float,
    maxCellH: Float,
    topStrip: Float = 0f,
    bottomStrip: Float = 0f
): BoardMetrics {
    val safeCols = cols.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val usableW = (width - leftGutter - rightGutter - gapX * (safeCols - 1)).coerceAtLeast(1f)
    val cellW = (usableW / safeCols).coerceAtLeast(1f)
    val usableH = (height - topStrip - bottomStrip).coerceAtLeast(1f)
    var cellH = ((usableH - gapY * (safeRows - 1)) / safeRows).coerceAtLeast(1f)
    if (cellH > maxCellH) cellH = maxCellH
    val totalH = cellH * safeRows + gapY * (safeRows - 1)
    val top = topStrip + ((usableH - totalH) / 2f).coerceAtLeast(0f)
    return BoardMetrics(
        rows = safeRows,
        cols = safeCols,
        leftGutter = leftGutter,
        rightGutter = rightGutter,
        topStrip = topStrip,
        bottomStrip = bottomStrip,
        cellW = cellW,
        cellH = cellH,
        gapX = gapX,
        gapY = gapY,
        top = top
    )
}

/** Occupied cells inside [region], used for the "skipped" feedback. */
fun Region.conflicts(occupied: Set<Cell>): List<Cell> = cells().filter { it in occupied }

fun Region.freeCells(occupied: Set<Cell>): List<Cell> = cells().filter { it !in occupied }

/** Manhattan distance in cells, used for the staggered highlight. */
fun Cell.distanceTo(other: Cell): Int = abs(row - other.row) + abs(col - other.col)
