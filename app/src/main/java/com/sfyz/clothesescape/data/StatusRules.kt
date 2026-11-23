package com.sfyz.clothesescape.data

object StatusRules {

    /** Derives a record status from the state of its items. Empty record stays DRYING. */
    fun recordStatus(itemStates: List<String>): String {
        if (itemStates.isEmpty()) return STATUS_DRYING
        val lost = itemStates.count { it == ITEM_LOST }
        val collected = itemStates.count { it == ITEM_COLLECTED }
        return when {
            lost == itemStates.size -> STATUS_ALL_LOST
            lost > 0 -> STATUS_PARTIAL_LOST
            collected == itemStates.size -> STATUS_COLLECTED
            else -> STATUS_DRYING
        }
    }

    /** Where a garment goes after the given item transition. */
    fun garmentStateFor(itemState: String): String = when (itemState) {
        ITEM_COLLECTED -> GARMENT_IN_CLOSET
        ITEM_LOST -> GARMENT_LOST
        else -> GARMENT_DRYING
    }
}
