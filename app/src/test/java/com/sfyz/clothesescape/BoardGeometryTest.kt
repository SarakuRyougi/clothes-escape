package com.sfyz.clothesescape

import com.sfyz.clothesescape.ui.board.Cell
import com.sfyz.clothesescape.ui.board.LandmarkSide
import com.sfyz.clothesescape.ui.board.Region
import com.sfyz.clothesescape.ui.board.RoomLandmarks
import com.sfyz.clothesescape.ui.board.blockedZoneFor
import com.sfyz.clothesescape.ui.board.colRange
import com.sfyz.clothesescape.ui.board.computeMetrics
import com.sfyz.clothesescape.ui.board.conflicts
import com.sfyz.clothesescape.ui.board.freeCells
import com.sfyz.clothesescape.ui.board.regionOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardGeometryTest {

    private val metrics = computeMetrics(
        width = 320f,
        height = 480f,
        rows = 15,
        cols = 6,
        leftGutter = 20f,
        rightGutter = 20f,
        gapX = 3f,
        gapY = 3f,
        maxCellH = 30f
    )

    @Test
    fun `grid fits the canvas`() {
        val bottom = metrics.cellTop(14) + metrics.cellH
        assertTrue("bottom $bottom should stay inside 480", bottom <= 480.01f)
        val right = metrics.cellLeft(5) + metrics.cellW
        assertTrue("right $right should stay inside 320", right <= 320.01f)
        assertEquals(15, metrics.rows)
        assertEquals(6, metrics.cols)
    }

    @Test
    fun `cell is hit inside its bounds`() {
        val cell = metrics.cellAt(metrics.cellLeft(2) + 4f, metrics.cellTop(3) + 4f)
        assertEquals(Cell(3, 2), cell)
    }

    @Test
    fun `gutter and gaps are not clickable`() {
        assertNull(metrics.cellAt(2f, 40f))
        val gapX = metrics.cellLeft(0) + metrics.cellW + 1f
        assertNull(metrics.cellAt(gapX, metrics.cellTop(0) + 2f))
        val gapY = metrics.cellTop(0) + metrics.cellH + 1f
        assertNull(metrics.cellAt(metrics.cellLeft(0) + 2f, gapY))
    }

    @Test
    fun `outside the grid is ignored`() {
        assertNull(metrics.cellAt(3000f, 40f))
        assertNull(metrics.cellAt(40f, 3000f))
        assertNull(metrics.cellAt(-5f, -5f))
    }

    @Test
    fun `region is normalised when dragged upwards`() {
        val region = regionOf(Cell(8, 4), Cell(5, 2))
        assertEquals(Region(5, 2, 8, 4), region)
        assertEquals(4, region.rows)
        assertEquals(3, region.cols)
        assertEquals(12, region.size)
        assertTrue(region.contains(Cell(6, 3)))
        assertFalse(region.contains(Cell(9, 3)))
    }

    @Test
    fun `single tap region is one cell`() {
        val region = regionOf(Cell(2, 2), Cell(2, 2))
        assertEquals(1, region.size)
        assertEquals(1, region.rows)
        assertEquals(1, region.cols)
    }

    @Test
    fun `conflicts and free cells split the selection`() {
        val region = regionOf(Cell(0, 0), Cell(0, 2))
        val occupied = setOf(Cell(0, 1))
        assertEquals(listOf(Cell(0, 1)), region.conflicts(occupied))
        assertEquals(listOf(Cell(0, 0), Cell(0, 2)), region.freeCells(occupied))
    }

    @Test
    fun `metrics stay sane on a tiny canvas`() {
        val tiny = computeMetrics(
            width = 40f, height = 40f, rows = 15, cols = 6,
            leftGutter = 20f, rightGutter = 20f, gapX = 3f, gapY = 3f, maxCellH = 30f
        )
        assertNotNull(tiny)
        assertTrue(tiny.cellW > 0f)
        assertTrue(tiny.cellH > 0f)
    }

    @Test
    fun `strips reserve room for the room landmarks`() {
        val withStrips = computeMetrics(
            width = 320f, height = 480f, rows = 15, cols = 6,
            leftGutter = 40f, rightGutter = 40f, gapX = 3f, gapY = 3f,
            maxCellH = 40f, topStrip = 26f, bottomStrip = 30f
        )
        assertTrue(withStrips.gridTop() >= 26f)
        assertTrue(withStrips.gridBottom() <= 480f - 30f + 0.01f)
        assertTrue(withStrips.gridLeft() >= 40f)
        assertTrue(withStrips.gridRight() <= 320f - 40f + 0.01f)
    }

    @Test
    fun `landmarks stay inside the rack range`() {
        RoomLandmarks.forEach { mark ->
            assertTrue("${mark.label} rackFrom", mark.rackFrom in 1..15)
            assertTrue("${mark.label} rackTo", mark.rackTo in 1..15)
            assertTrue("${mark.label} order", mark.rackFrom <= mark.rackTo)
        }
    }

    @Test
    fun `four mirrors are placed on the two side walls`() {
        val mirrors = RoomLandmarks.filter { it.label == "镜子" }
        assertEquals(4, mirrors.size)
        assertEquals(2, mirrors.count { it.side == LandmarkSide.LEFT_WALL })
        assertEquals(2, mirrors.count { it.side == LandmarkSide.RIGHT_WALL })
    }

    @Test
    fun `appliances sit over racks one and two`() {
        val appliances = RoomLandmarks.first { it.label.contains("饮水机") }
        assertEquals(1, appliances.rackFrom)
        assertEquals(2, appliances.rackTo)
        assertFalse(appliances.showLabel)
    }

    @Test
    fun `shelf is drawn without a text label`() {
        val shelf = RoomLandmarks.first { it.label == "公共置物架" }
        assertFalse(shelf.showLabel)
        assertEquals(6, shelf.rackFrom)
        assertEquals(12, shelf.rackTo)
    }

    @Test
    fun `only window mirrors and entrance carry text`() {
        val labelled = RoomLandmarks.filter { it.showLabel }.map { it.label }.toSet()
        assertEquals(setOf("窗", "镜子", "入口"), labelled)
    }

    @Test
    fun `zone columns snap to whole cells`() {
        val shelf = RoomLandmarks.first { it.label == "公共置物架" }
        val six = shelf.colRange(6)
        assertTrue(six.first >= 0)
        assertTrue(six.last <= 5)
        // wider grids must still produce a contiguous, in-range span
        listOf(4, 6, 8, 12).forEach { cols ->
            val r = shelf.colRange(cols)
            assertTrue("cols=$cols first", r.first in 0 until cols)
            assertTrue("cols=$cols last", r.last in 0 until cols)
            assertTrue("cols=$cols order", r.first <= r.last)
        }
    }

    @Test
    fun `cells inside the shelf footprint are not selectable`() {
        // rack 6-12 in the middle columns belongs to the shelf block
        val inside = blockedZoneFor(row = 7, col = 2, cols = 6)
        assertNotNull(inside)
        assertEquals("公共置物架", inside?.label)
        // rack 14 is well below the shelf
        assertNull(blockedZoneFor(row = 13, col = 2, cols = 6))
        // first column is outside the shelf's column span
        assertNull(blockedZoneFor(row = 7, col = 0, cols = 6))
    }

    @Test
    fun `appliance footprint sits on racks one and two`() {
        val appliance = blockedZoneFor(row = 0, col = 2, cols = 6)
        assertNotNull(appliance)
        assertEquals("饮水机 · 洗衣机", appliance?.label)
        // rack 3 is below the appliances and stays usable
        assertNull(blockedZoneFor(row = 2, col = 2, cols = 6))
    }

    @Test
    fun `blocked areas never overlap`() {
        var blocked = 0
        for (row in 0 until 15) {
            for (col in 0 until 6) {
                val hits = RoomLandmarks.count {
                    it.isBlockedArea &&
                        (row + 1) in it.rackFrom..it.rackTo &&
                        col in it.colRange(6)
                }
                assertTrue("cell($row,$col) covered by $hits blocks", hits <= 1)
                if (hits == 1) blocked++
            }
        }
        // some slots must survive, otherwise the board is unusable
        assertTrue("blocked=$blocked", blocked in 1..(15 * 6 - 1))
    }
}
