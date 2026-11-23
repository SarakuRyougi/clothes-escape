package com.sfyz.clothesescape.data

import com.sfyz.clothesescape.ui.theme.ThemeMode

const val STATUS_DRYING = "DRYING"
const val STATUS_COLLECTED = "COLLECTED"
const val STATUS_PARTIAL_LOST = "PARTIAL_LOST"
const val STATUS_ALL_LOST = "ALL_LOST"
const val STATUS_CANCELLED = "CANCELLED"

const val ITEM_DRYING = "DRYING"
const val ITEM_COLLECTED = "COLLECTED"
const val ITEM_LOST = "LOST"

const val GARMENT_IN_CLOSET = "IN_CLOSET"
const val GARMENT_DRYING = "DRYING"
const val GARMENT_WASHING = "WASHING"
const val GARMENT_LOST = "LOST"
const val GARMENT_ARCHIVED = "ARCHIVED"

const val RACK_COUNT = 15
const val SLOT_MIN = 4
const val SLOT_MAX = 12

/** A single hanging position: rack 1..15 (top to bottom in the room), slot 1..N (left to right). */
data class Slot(val rack: Int, val slot: Int)

data class ItemUi(
    val id: Long,
    val garmentId: Long,
    val name: String,
    val state: String
)

data class RecordUi(
    val id: Long,
    val photoPath: String?,
    val note: String,
    val hungAt: Long,
    val dueAt: Long,
    val reminderAt: Long,
    val status: String,
    val accentIndex: Int,
    val collectedAt: Long?,
    val slots: List<Slot>,
    val items: List<ItemUi>
) {
    val active: Boolean get() = status == STATUS_DRYING
    val hasLoss: Boolean get() = status == STATUS_PARTIAL_LOST || status == STATUS_ALL_LOST

    fun isOverdue(now: Long): Boolean = active && now > dueAt

    val names: String get() = items.joinToString("、") { it.name }.ifBlank { "未记录衣物" }

    val remainingItems: Int get() = items.count { it.state == ITEM_DRYING }

    val regionText: String get() = regionLabel(slots)
}

data class Settings(
    val slotCount: Int = 6,
    val defaultDays: Int = 2,
    val defaultHour: Int = 20,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reduceMotion: Boolean = false
)

data class Stats(
    val total: Int = 0,
    val activeCount: Int = 0,
    val lostRecords: Int = 0,
    val lostItems: Int = 0,
    val monthCount: Int = 0,
    val avgHours: Int = 0,
    val worstRack: Int? = null
)

fun regionLabel(slots: List<Slot>): String {
    if (slots.isEmpty()) return "未指定位置"
    val racks = slots.map { it.rack }.distinct().sorted()
    val cols = slots.map { it.slot }.distinct().sorted()
    val rackPart = if (racks.size == 1) "${racks.first()} 号架" else "${racks.first()}–${racks.last()} 号架"
    val colPart = if (cols.size == 1) "第 ${cols.first()} 位" else "第 ${cols.first()}–${cols.last()} 位"
    return "$rackPart $colPart"
}

fun regionSpanText(slots: List<Slot>): String {
    if (slots.isEmpty()) return ""
    val rows = slots.map { it.rack }.distinct().size
    val cols = slots.map { it.slot }.distinct().size
    return "$rows × $cols"
}
