package com.sfyz.clothesescape.util

import android.content.Context
import android.net.Uri
import com.sfyz.clothesescape.data.LaundryDb
import org.json.JSONArray
import org.json.JSONObject

/** Plain-JSON backup: every table is stored flat, ids preserved. */
class BackupManager(context: Context) {

    private val helper = LaundryDb(context.applicationContext)

    fun exportTo(context: Context, uri: Uri): Int {
        val db = helper.readableDatabase
        val root = JSONObject().apply {
            put("app", "clothes-escape")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
        }
        val garments = JSONArray()
        db.rawQuery("SELECT id, name, state, created_at, updated_at FROM garment", null).use { c ->
            while (c.moveToNext()) {
                garments.put(
                    JSONObject().apply {
                        put("id", c.getLong(0))
                        put("name", c.getString(1) ?: "")
                        put("state", c.getString(2) ?: "")
                        put("created_at", c.getLong(3))
                        put("updated_at", c.getLong(4))
                    }
                )
            }
        }
        val records = JSONArray()
        db.rawQuery(
            "SELECT id, photo_path, note, hung_at, due_at, reminder_at, status, accent_index, collected_at, created_at FROM drying_record",
            null
        ).use { c ->
            while (c.moveToNext()) {
                records.put(
                    JSONObject().apply {
                        put("id", c.getLong(0))
                        put("photo_path", if (c.isNull(1)) JSONObject.NULL else c.getString(1))
                        put("note", c.getString(2) ?: "")
                        put("hung_at", c.getLong(3))
                        put("due_at", c.getLong(4))
                        put("reminder_at", c.getLong(5))
                        put("status", c.getString(6) ?: "")
                        put("accent_index", c.getInt(7))
                        put("collected_at", if (c.isNull(8)) JSONObject.NULL else c.getLong(8))
                        put("created_at", c.getLong(9))
                    }
                )
            }
        }
        val slots = JSONArray()
        db.rawQuery("SELECT id, record_id, rack_no, slot_no FROM drying_slot", null).use { c ->
            while (c.moveToNext()) {
                slots.put(
                    JSONObject().apply {
                        put("id", c.getLong(0))
                        put("record_id", c.getLong(1))
                        put("rack_no", c.getInt(2))
                        put("slot_no", c.getInt(3))
                    }
                )
            }
        }
        val items = JSONArray()
        db.rawQuery(
            "SELECT id, record_id, garment_id, item_state, snapshot_name FROM drying_item",
            null
        ).use { c ->
            while (c.moveToNext()) {
                items.put(
                    JSONObject().apply {
                        put("id", c.getLong(0))
                        put("record_id", c.getLong(1))
                        put("garment_id", c.getLong(2))
                        put("item_state", c.getString(3) ?: "")
                        put("snapshot_name", c.getString(4) ?: "")
                    }
                )
            }
        }
        root.put("garments", garments)
        root.put("records", records)
        root.put("slots", slots)
        root.put("items", items)

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
        } ?: return 0
        return records.length()
    }

    fun importFrom(context: Context, uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return 0
        val root = JSONObject(text)
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM drying_item")
            db.execSQL("DELETE FROM drying_slot")
            db.execSQL("DELETE FROM drying_record")
            db.execSQL("DELETE FROM garment")

            val garments = root.optJSONArray("garments") ?: JSONArray()
            for (i in 0 until garments.length()) {
                val o = garments.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO garment (id, name, state, created_at, updated_at) VALUES (?,?,?,?,?)",
                    arrayOf(o.getLong("id"), o.optString("name"), o.optString("state"), o.optLong("created_at"), o.optLong("updated_at"))
                )
            }
            val records = root.optJSONArray("records") ?: JSONArray()
            for (i in 0 until records.length()) {
                val o = records.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO drying_record (id, photo_path, note, hung_at, due_at, reminder_at, status, accent_index, collected_at, created_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    arrayOf(
                        o.getLong("id"),
                        if (o.isNull("photo_path")) null else o.getString("photo_path"),
                        o.optString("note"),
                        o.getLong("hung_at"),
                        o.getLong("due_at"),
                        o.getLong("reminder_at"),
                        o.optString("status"),
                        o.optInt("accent_index"),
                        if (o.isNull("collected_at")) null else o.getLong("collected_at"),
                        o.optLong("created_at")
                    )
                )
            }
            val slots = root.optJSONArray("slots") ?: JSONArray()
            for (i in 0 until slots.length()) {
                val o = slots.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO drying_slot (id, record_id, rack_no, slot_no) VALUES (?,?,?,?)",
                    arrayOf(o.getLong("id"), o.getLong("record_id"), o.getInt("rack_no"), o.getInt("slot_no"))
                )
            }
            val items = root.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                db.execSQL(
                    "INSERT INTO drying_item (id, record_id, garment_id, item_state, snapshot_name) VALUES (?,?,?,?,?)",
                    arrayOf(o.getLong("id"), o.getLong("record_id"), o.getLong("garment_id"), o.optString("item_state"), o.optString("snapshot_name"))
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return root.optJSONArray("records")?.length() ?: 0
    }
}
