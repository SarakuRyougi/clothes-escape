package com.sfyz.clothesescape.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class LaundryRepository(context: Context) {

    private val helper = LaundryDb(context.applicationContext)

    data class Occupied(val recordId: Long, val accentIndex: Int)

    // ---------------------------------------------------------------- reads

    fun loadAll(): List<RecordUi> {
        val db = helper.readableDatabase
        val slotMap = HashMap<Long, MutableList<Slot>>()
        db.rawQuery("SELECT record_id, rack_no, slot_no FROM drying_slot", null).use { c ->
            while (c.moveToNext()) {
                slotMap.getOrPut(c.getLong(0)) { mutableListOf() }.add(Slot(c.getInt(1), c.getInt(2)))
            }
        }
        val itemMap = HashMap<Long, MutableList<ItemUi>>()
        db.rawQuery(
            "SELECT id, record_id, garment_id, item_state, snapshot_name FROM drying_item",
            null
        ).use { c ->
            while (c.moveToNext()) {
                itemMap.getOrPut(c.getLong(1)) { mutableListOf() }.add(
                    ItemUi(
                        id = c.getLong(0),
                        garmentId = c.getLong(2),
                        name = c.getString(4) ?: "",
                        state = c.getString(3) ?: ITEM_DRYING
                    )
                )
            }
        }
        val list = ArrayList<RecordUi>()
        db.rawQuery(
            "SELECT id, photo_path, note, hung_at, due_at, reminder_at, status, accent_index, collected_at " +
                "FROM drying_record ORDER BY hung_at DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                list.add(
                    RecordUi(
                        id = id,
                        photoPath = if (c.isNull(1)) null else c.getString(1),
                        note = c.getString(2) ?: "",
                        hungAt = c.getLong(3),
                        dueAt = c.getLong(4),
                        reminderAt = c.getLong(5),
                        status = c.getString(6) ?: STATUS_DRYING,
                        accentIndex = c.getInt(7),
                        collectedAt = if (c.isNull(8)) null else c.getLong(8),
                        slots = (slotMap[id] ?: emptyList()).sortedWith(compareBy({ it.rack }, { it.slot })),
                        items = itemMap[id] ?: emptyList()
                    )
                )
            }
        }
        return list
    }

    fun occupiedSlots(): Map<Slot, Occupied> {
        val db = helper.readableDatabase
        val map = HashMap<Slot, Occupied>()
        db.rawQuery(
            "SELECT s.rack_no, s.slot_no, r.id, r.accent_index FROM drying_slot s " +
                "JOIN drying_record r ON r.id = s.record_id WHERE r.status = ?",
            arrayOf(STATUS_DRYING)
        ).use { c ->
            while (c.moveToNext()) {
                map[Slot(c.getInt(0), c.getInt(1))] = Occupied(c.getLong(2), c.getInt(3))
            }
        }
        return map
    }

    fun recentNames(limit: Int = 12): List<String> {
        val db = helper.readableDatabase
        val out = ArrayList<String>()
        db.rawQuery("SELECT name FROM garment ORDER BY updated_at DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) c.getString(0)?.let { out.add(it) }
        }
        return out
    }

    fun loadGarments(): List<Pair<String, String>> {
        val db = helper.readableDatabase
        val out = ArrayList<Pair<String, String>>()
        db.rawQuery("SELECT name, state FROM garment ORDER BY updated_at DESC", null).use { c ->
            while (c.moveToNext()) out.add((c.getString(0) ?: "") to (c.getString(1) ?: GARMENT_IN_CLOSET))
        }
        return out
    }

    // -------------------------------------------------------------- writes

    fun createRecord(
        region: List<Slot>,
        garmentNames: List<String>,
        photoPath: String?,
        note: String,
        hungAt: Long,
        dueAt: Long,
        reminderAt: Long
    ): Long {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            val accent = countRecords(db) % 6
            val rid = db.insertOrThrow(
                "drying_record",
                null,
                ContentValues().apply {
                    put("photo_path", photoPath)
                    put("note", note)
                    put("hung_at", hungAt)
                    put("due_at", dueAt)
                    put("reminder_at", reminderAt)
                    put("status", STATUS_DRYING)
                    put("accent_index", accent)
                    put("created_at", now)
                }
            )
            val blocked = blockedSlotsIn(db)
            region.filter { it !in blocked }.forEach { slot ->
                db.insert(
                    "drying_slot",
                    null,
                    ContentValues().apply {
                        put("record_id", rid)
                        put("rack_no", slot.rack)
                        put("slot_no", slot.slot)
                    }
                )
            }
            garmentNames.map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { name ->
                    val gid = findOrCreateGarment(db, name, now)
                    db.insert(
                        "drying_item",
                        null,
                        ContentValues().apply {
                            put("record_id", rid)
                            put("garment_id", gid)
                            put("item_state", ITEM_DRYING)
                            put("snapshot_name", name)
                        }
                    )
                }
            db.setTransactionSuccessful()
            return rid
        } finally {
            db.endTransaction()
        }
    }

    fun collectAll(recordId: Long) = moveAll(recordId, ITEM_COLLECTED)

    fun lostAll(recordId: Long) = moveAll(recordId, ITEM_LOST)

    private fun moveAll(recordId: Long, target: String) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val pairs = ArrayList<Pair<Long, Long>>()
            db.rawQuery(
                "SELECT id, garment_id FROM drying_item WHERE record_id = ? AND item_state = ?",
                arrayOf(recordId.toString(), ITEM_DRYING)
            ).use { c ->
                while (c.moveToNext()) pairs.add(c.getLong(0) to c.getLong(1))
            }
            val garmentState = StatusRules.garmentStateFor(target)
            val now = System.currentTimeMillis()
            pairs.forEach { (itemId, gid) ->
                db.execSQL("UPDATE drying_item SET item_state = ? WHERE id = ?", arrayOf(target, itemId))
                db.execSQL(
                    "UPDATE garment SET state = ?, updated_at = ? WHERE id = ?",
                    arrayOf(garmentState, now, gid)
                )
            }
            refreshStatus(db, recordId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setItemState(recordId: Long, itemId: Long, state: String) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val gid: Long? = db.rawQuery(
                "SELECT garment_id FROM drying_item WHERE id = ?",
                arrayOf(itemId.toString())
            ).use { if (it.moveToFirst()) it.getLong(0) else null }
            db.execSQL("UPDATE drying_item SET item_state = ? WHERE id = ?", arrayOf(state, itemId))
            if (gid != null) {
                db.execSQL(
                    "UPDATE garment SET state = ?, updated_at = ? WHERE id = ?",
                    arrayOf(StatusRules.garmentStateFor(state), System.currentTimeMillis(), gid)
                )
            }
            refreshStatus(db, recordId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun extend(recordId: Long, days: Int) {
        val delta = days.toLong() * 24L * 60L * 60L * 1000L
        val db = helper.writableDatabase
        db.execSQL(
            "UPDATE drying_record SET due_at = due_at + ?, reminder_at = reminder_at + ? WHERE id = ?",
            arrayOf(delta, delta, recordId)
        )
    }

    fun cancel(recordId: Long) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val gids = ArrayList<Long>()
            db.rawQuery(
                "SELECT garment_id FROM drying_item WHERE record_id = ? AND item_state = ?",
                arrayOf(recordId.toString(), ITEM_DRYING)
            ).use { c -> while (c.moveToNext()) gids.add(c.getLong(0)) }
            val now = System.currentTimeMillis()
            gids.forEach { gid ->
                db.execSQL(
                    "UPDATE garment SET state = ?, updated_at = ? WHERE id = ?",
                    arrayOf(GARMENT_IN_CLOSET, now, gid)
                )
            }
            db.update(
                "drying_record",
                ContentValues().apply {
                    put("status", STATUS_CANCELLED)
                    put("collected_at", now)
                },
                "id = ?",
                arrayOf(recordId.toString())
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteRecord(recordId: Long) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("drying_item", "record_id = ?", arrayOf(recordId.toString()))
            db.delete("drying_slot", "record_id = ?", arrayOf(recordId.toString()))
            db.delete("drying_record", "id = ?", arrayOf(recordId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ------------------------------------------------------------- helpers

    private fun refreshStatus(db: SQLiteDatabase, recordId: Long) {
        val states = ArrayList<String>()
        db.rawQuery(
            "SELECT item_state FROM drying_item WHERE record_id = ?",
            arrayOf(recordId.toString())
        ).use { c -> while (c.moveToNext()) states.add(c.getString(0) ?: ITEM_DRYING) }
        if (states.isEmpty()) return
        val status = StatusRules.recordStatus(states)
        val cv = ContentValues().apply {
            put("status", status)
            if (status != STATUS_DRYING) put("collected_at", System.currentTimeMillis())
        }
        db.update("drying_record", cv, "id = ?", arrayOf(recordId.toString()))
    }

    private fun countRecords(db: SQLiteDatabase): Int =
        db.rawQuery("SELECT COUNT(*) FROM drying_record", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    private fun blockedSlotsIn(db: SQLiteDatabase): Set<Slot> {
        val set = HashSet<Slot>()
        db.rawQuery(
            "SELECT s.rack_no, s.slot_no FROM drying_slot s JOIN drying_record r ON r.id = s.record_id " +
                "WHERE r.status = ?",
            arrayOf(STATUS_DRYING)
        ).use { c -> while (c.moveToNext()) set.add(Slot(c.getInt(0), c.getInt(1))) }
        return set
    }

    private fun findOrCreateGarment(db: SQLiteDatabase, name: String, now: Long): Long {
        db.rawQuery(
            "SELECT id FROM garment WHERE name = ? AND state != ? LIMIT 1",
            arrayOf(name, GARMENT_ARCHIVED)
        ).use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(0)
                db.execSQL(
                    "UPDATE garment SET state = ?, updated_at = ? WHERE id = ?",
                    arrayOf(GARMENT_DRYING, now, id)
                )
                return id
            }
        }
        return db.insertOrThrow(
            "garment",
            null,
            ContentValues().apply {
                put("name", name)
                put("state", GARMENT_DRYING)
                put("created_at", now)
                put("updated_at", now)
            }
        )
    }
}
