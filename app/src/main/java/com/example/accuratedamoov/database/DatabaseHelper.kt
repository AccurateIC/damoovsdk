package com.example.accuratedamoov.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

import java.io.File

class DatabaseHelper(context: Context) {
    private val dbPath = "/data/data/com.example.accuratedamoov/databases/raxel_traker_db"

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
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
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
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
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
}

