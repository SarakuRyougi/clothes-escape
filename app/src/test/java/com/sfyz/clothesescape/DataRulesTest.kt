package com.sfyz.clothesescape

import com.sfyz.clothesescape.data.ITEM_COLLECTED
import com.sfyz.clothesescape.data.ITEM_DRYING
import com.sfyz.clothesescape.data.ITEM_LOST
import com.sfyz.clothesescape.data.ItemUi
import com.sfyz.clothesescape.data.RecordUi
import com.sfyz.clothesescape.data.STATUS_ALL_LOST
import com.sfyz.clothesescape.data.STATUS_COLLECTED
import com.sfyz.clothesescape.data.STATUS_DRYING
import com.sfyz.clothesescape.data.STATUS_PARTIAL_LOST
import com.sfyz.clothesescape.data.Slot
import com.sfyz.clothesescape.data.StatsCalculator
import com.sfyz.clothesescape.data.StatusRules
import com.sfyz.clothesescape.data.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DataRulesTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `due date is days later at the chosen hour`() {
        val now = ZonedDateTime.of(2026, 11, 23, 12, 30, 0, 0, zone).toInstant().toEpochMilli()
        val due = TimeUtils.dueAt(now, 2, 20, zone)
        val expected = ZonedDateTime.of(2026, 11, 25, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, due)
    }

    @Test
    fun `due date crosses month boundary`() {
        val now = ZonedDateTime.of(2026, 11, 30, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val due = TimeUtils.dueAt(now, 3, 7, zone)
        val expected = ZonedDateTime.of(2026, 12, 3, 7, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, due)
    }

    @Test
    fun `remaining text flips to overdue`() {
        val due = 1_000_000_000_000L
        assertTrue(TimeUtils.remainingText(due - 3_600_000, due).startsWith("还有"))
        assertTrue(TimeUtils.remainingText(due + 3_600_000, due).startsWith("超时"))
    }

    @Test
    fun `record status follows item states`() {
        assertEquals(STATUS_DRYING, StatusRules.recordStatus(listOf(ITEM_DRYING, ITEM_DRYING)))
        assertEquals(STATUS_COLLECTED, StatusRules.recordStatus(listOf(ITEM_COLLECTED, ITEM_COLLECTED)))
        assertEquals(STATUS_PARTIAL_LOST, StatusRules.recordStatus(listOf(ITEM_COLLECTED, ITEM_LOST)))
        assertEquals(STATUS_ALL_LOST, StatusRules.recordStatus(listOf(ITEM_LOST, ITEM_LOST)))
        assertEquals(STATUS_DRYING, StatusRules.recordStatus(emptyList()))
    }

    @Test
    fun `garment state mirrors item state`() {
        assertEquals("IN_CLOSET", StatusRules.garmentStateFor(ITEM_COLLECTED))
        assertEquals("LOST", StatusRules.garmentStateFor(ITEM_LOST))
        assertEquals("DRYING", StatusRules.garmentStateFor(ITEM_DRYING))
    }

    @Test
    fun `stats aggregate counts and average`() {
        val now = ZonedDateTime.of(2026, 11, 23, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val recent = now - 10 * 3_600_000L
        val old = now - 40L * 24 * 3_600_000L

        val a = record(
            id = 1, hungAt = recent, dueAt = now + 1000, collectedAt = recent + 6 * 3_600_000L,
            status = STATUS_COLLECTED, items = listOf(item(1, ITEM_COLLECTED)), slots = listOf(Slot(3, 2))
        )
        val b = record(
            id = 2, hungAt = recent, dueAt = now + 1000, collectedAt = recent + 10 * 3_600_000L,
            status = STATUS_PARTIAL_LOST, items = listOf(item(2, ITEM_LOST), item(3, ITEM_COLLECTED)),
            slots = listOf(Slot(3, 3))
        )
        val c = record(
            id = 3, hungAt = old, dueAt = now + 1000, collectedAt = null,
            status = STATUS_DRYING, items = listOf(item(4, ITEM_DRYING)), slots = listOf(Slot(7, 1))
        )

        val stats = StatsCalculator.compute(listOf(a, b, c), now)
        assertEquals(3, stats.total)
        assertEquals(1, stats.activeCount)
        assertEquals(1, stats.lostRecords)
        assertEquals(1, stats.lostItems)
        assertEquals(2, stats.monthCount)
        assertEquals(8, stats.avgHours)
        assertEquals(3, stats.worstRack)
    }

    private fun item(id: Long, state: String) = ItemUi(id = id, garmentId = id, name = "item$id", state = state)

    private fun record(
        id: Long,
        hungAt: Long,
        dueAt: Long,
        collectedAt: Long?,
        status: String,
        items: List<ItemUi>,
        slots: List<Slot>
    ) = RecordUi(
        id = id,
        photoPath = null,
        note = "",
        hungAt = hungAt,
        dueAt = dueAt,
        reminderAt = dueAt,
        status = status,
        accentIndex = 0,
        collectedAt = collectedAt,
        slots = slots,
        items = items
    )
}
