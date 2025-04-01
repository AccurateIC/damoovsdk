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

class DatabaseHelper(context: Context) {
    val dbPath = "/data/data/com.example.accuratedamoov/databases/com.telematicssdk.tracking.database"

    // 🔹 Check if the database file exists
    private fun databaseExists(): Boolean {
        return File(dbPath).exists()
    }

    fun getRowCount(tableName: String): Int {
        if (!databaseExists()) return 0

        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        var count = 0

        try {
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
            cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)

            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }
        return count
    }

    fun getTableData(tableName: String): JSONArray {
        val jsonArray = JSONArray()

        if (!databaseExists()) return jsonArray

        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null

        try {
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
            cursor = db.rawQuery("SELECT * FROM $tableName", null)

            if (cursor.moveToFirst()) {
                do {
                    val jsonObject = JSONObject()
                    for (i in 0 until cursor.columnCount) {
                        jsonObject.put(cursor.getColumnName(i), cursor.getString(i))
                    }
                    jsonArray.put(jsonObject)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }

        return jsonArray
    }

    fun addSyncedColumnIfNotExists(tableName: String) {
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

        try {
            // Check if "synced" column exists
            val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            var columnExists = false

            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (columnName == "synced") {
                    columnExists = true
                    break
                }
            }
            cursor.close()

            // If "synced" column does not exist, add it
            if (!columnExists) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN synced INTEGER DEFAULT 0")
                Log.d("DatabaseHelper", "✅ Added 'synced' column to $tableName")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "❌ Failed to add 'synced' column to $tableName: ${e.message}")
        }
    }


    fun getUnsyncedTableData(tableName: String): JSONArray {
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        val cursor = db.rawQuery("SELECT * FROM $tableName WHERE synced = 0", null)
        return cursorToJson(cursor)
    }




    fun cursorToJson(cursor: Cursor): JSONArray {
        val jsonArray = JSONArray()

        if (cursor.moveToFirst()) {
            do {
                val jsonObject = JSONObject()
                for (i in 0 until cursor.columnCount) {
                    jsonObject.put(cursor.getColumnName(i), cursor.getString(i))
                }
                jsonArray.put(jsonObject)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return jsonArray
    }

    fun addDeviceIdColumnIfNotExists(tableName: String,context: Context) {
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)

        try {
            // Check if "device_id" column exists
            val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            var columnExists = false

            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (columnName == "device_id") {
                    columnExists = true
                    break
                }
            }
            cursor.close()
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val deviceId = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

            // If "device_id" column does not exist, add it
            if (!columnExists) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN device_id TEXT DEFAULT '$deviceId'")
                Log.d("DatabaseHelper", "✅ Added 'device_id' column to $tableName")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "❌ Failed to add 'device_id' column to $tableName: ${e.message}")
        } finally {
            db.close()
        }
    }



}

