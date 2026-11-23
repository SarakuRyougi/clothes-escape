package com.sfyz.clothesescape.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LaundryDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE garment (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT,
                color TEXT,
                photo_path TEXT,
                state TEXT NOT NULL DEFAULT 'IN_CLOSET',
                note TEXT,
                wear_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE drying_record (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                photo_path TEXT,
                note TEXT,
                hung_at INTEGER NOT NULL,
                due_at INTEGER NOT NULL,
                reminder_at INTEGER NOT NULL,
                status TEXT NOT NULL,
                accent_index INTEGER NOT NULL DEFAULT 0,
                collected_at INTEGER,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE drying_slot (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                record_id INTEGER NOT NULL,
                rack_no INTEGER NOT NULL,
                slot_no INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE drying_item (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                record_id INTEGER NOT NULL,
                garment_id INTEGER NOT NULL,
                item_state TEXT NOT NULL,
                snapshot_name TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE app_settings (
                key TEXT PRIMARY KEY,
                value TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_slot_record ON drying_slot(record_id)")
        db.execSQL("CREATE INDEX idx_slot_rack ON drying_slot(rack_no, slot_no)")
        db.execSQL("CREATE INDEX idx_item_record ON drying_item(record_id)")
        db.execSQL("CREATE INDEX idx_record_status ON drying_record(status)")
        db.execSQL("CREATE INDEX idx_garment_state ON garment(state)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v2 will add the wardrobe module tables.
    }

    companion object {
        const val DB_NAME = "clothes_escape.db"
        const val DB_VERSION = 1
    }
}
