package com.example.accuratedamoov.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import androidx.core.database.sqlite.transaction

class DatabaseHelper private constructor(context: Context) {

    private val dbPath = "/data/data/com.example.accuratedamoov/databases/com.telematicssdk.tracking.database"
    private var database: SQLiteDatabase? = null

    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun databaseExists(): Boolean {
        return File(dbPath).exists()
    }

    fun openDatabase(): SQLiteDatabase? {
        if (database == null || !database!!.isOpen) {
            database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        }
        return database
    }

    fun closeDatabase() {
        database?.close()
        database = null
    }

    fun getRowCount(tableName: String): Int {
        if (!databaseExists()) return 0
        var count = 0
        val db = openDatabase() ?: return 0

        db.rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
        }
        return count
    }

    fun getTableData(tableName: String): JSONArray {
        val jsonArray = JSONArray()
        if (!databaseExists()) return jsonArray

        val db = openDatabase() ?: return jsonArray
        db.rawQuery("SELECT * FROM $tableName", null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val jsonObject = JSONObject()
                    for (i in 0 until cursor.columnCount) {
                        jsonObject.put(cursor.getColumnName(i), cursor.getString(i))
                    }
                    jsonArray.put(jsonObject)
                } while (cursor.moveToNext())
            }
        }
        return jsonArray
    }

    fun addSyncedColumnIfNotExists(tableName: String) {
        val db = openDatabase() ?: return

        try {
            db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                var columnExists = false

                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "synced") {
                        columnExists = true
                        break
                    }
                }

                if (!columnExists) {
                    db.execSQL("ALTER TABLE $tableName ADD COLUMN synced INTEGER DEFAULT 0")
                    Log.d("DatabaseHelper", "✅ Added 'synced' column to $tableName")
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "❌ Failed to add 'synced' column to $tableName: ${e.message}")
        }
    }

    fun getUnsyncedTableData(tableName: String): JSONArray {
        val jsonArray = JSONArray()
        val db = openDatabase() ?: return jsonArray

        db.rawQuery("SELECT * FROM $tableName WHERE synced = 0", null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val jsonObject = JSONObject()
                    for (i in 0 until cursor.columnCount) {
                        jsonObject.put(cursor.getColumnName(i), cursor.getString(i))
                    }
                    jsonArray.put(jsonObject)
                } while (cursor.moveToNext())
            }
        }
        return jsonArray
    }

    fun addDeviceIdColumnIfNotExists(tableName: String, context: Context) {
        val db = openDatabase() ?: return

        try {
            db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                var columnExists = false

                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "device_id") {
                        columnExists = true
                        break
                    }
                }

                if (!columnExists) {
                    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
                    db.execSQL("ALTER TABLE $tableName ADD COLUMN device_id TEXT DEFAULT '$deviceId'")
                    Log.d("DatabaseHelper", "✅ Added 'device_id' column to $tableName")
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "❌ Failed to add 'device_id' column to $tableName: ${e.message}")
        }
    }

    fun markAsSynced(tableName: String, ids: List<Int>) {
        if (ids.isEmpty()) return

        val db = database
        db?.beginTransaction()
        try {
            val placeholders = ids.joinToString(",") { "?" }
            val sql = "UPDATE $tableName SET synced = 1 WHERE id IN ($placeholders)"
            val statement = db?.compileStatement(sql)

            ids.forEachIndexed { index, id ->
                statement?.bindLong(index + 1, id.toLong())
            }

            val rowsUpdated = statement?.executeUpdateDelete()
            Log.d("DatabaseHelper", "✅ Updated $rowsUpdated rows in $tableName as synced")

            db?.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "❌ Error marking as synced in $tableName: ${e.message}")
        } finally {
            db?.endTransaction()
        }
    }


    fun addEndDateColumnIfNotExists() {
        val db = openDatabase() ?: return
        try {
            val cursor = db.rawQuery("PRAGMA table_info(TrackTable)", null)
            var columnExists = false

            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (columnName.equals("end_date", ignoreCase = true)) {
                    columnExists = true
                    break
                }
            }

            if (!columnExists) {
                db.execSQL("ALTER TABLE TrackTable ADD COLUMN end_date INTEGER")
                Log.d("DB", "✅ Added 'end_date' column to TrackTable")
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("DB", "❌ Failed to add 'end_date' column", e)
        }
    }


}
