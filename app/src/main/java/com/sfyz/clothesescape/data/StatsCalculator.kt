package com.sfyz.clothesescape.data

object StatsCalculator {

    fun compute(records: List<RecordUi>, now: Long): Stats {
        val monthStart = TimeUtils.monthStart(now)
        val collected = records.filter { it.collectedAt != null }
        val hours = collected.mapNotNull { r ->
            val c = r.collectedAt ?: return@mapNotNull null
            ((c - r.hungAt).coerceAtLeast(0L) / 3_600_000.0)
        }
        val lostRecords = records.count { it.hasLoss }
        val lostItems = records.sumOf { r -> r.items.count { it.state == ITEM_LOST } }
        val worstRack = records
            .filter { it.hasLoss }
            .flatMap { it.slots }
            .groupingBy { it.rack }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        return Stats(
            total = records.size,
            activeCount = records.count { it.active },
            lostRecords = lostRecords,
            lostItems = lostItems,
            monthCount = records.count { it.hungAt >= monthStart },
            avgHours = if (hours.isEmpty()) 0 else (hours.average() + 0.5).toInt(),
            worstRack = worstRack
        )
    }
}
