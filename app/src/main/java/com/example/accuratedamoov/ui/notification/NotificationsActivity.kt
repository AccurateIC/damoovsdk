package com.example.accuratedamoov.ui.notification

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.accuratedamoov.R
import com.example.accuratedamoov.database.DatabaseHelper
import com.example.accuratedamoov.model.TripNotification
import com.example.accuratedamoov.ui.notification.adapter.TripNotificationAdapter
import com.example.accuratedamoov.ui.notification.model.NotificationListItem
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class NotificationsActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val recyclerView: RecyclerView = findViewById(R.id.notificationsRecyclerView)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get DB Instance
        val dbHelper = DatabaseHelper.getInstance(this)

        // Read all notifications
        val rawList = dbHelper.getTripNotifications()

        // Convert into header + items
        val finalList = buildNotificationList(rawList)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TripNotificationAdapter(finalList)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // go back when back button is pressed
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildNotificationList(data: List<TripNotification>): List<NotificationListItem> {
        val list = mutableListOf<NotificationListItem>()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Group notifications by correct date (timestamp is UNIX seconds → convert to ms)
        val grouped = data.groupBy { notification ->
            Instant.ofEpochMilli(notification.timestamp * 1000L)   // FIXED ✔️
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        // Sort groups by date (newest first)
        grouped.toSortedMap(compareByDescending { it }).forEach { (date, notifications) ->

            val headerText = when (date) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            }

            // Add date header
            list.add(NotificationListItem.DateHeader(headerText))

            // Add notifications under this date
            notifications.sortedByDescending { it.timestamp }.forEach { notif ->
                list.add(NotificationListItem.NotificationItem(notif))
            }
        }

        return list
    }


    override fun onResume() {
        super.onResume()
        val dbHelper = DatabaseHelper.getInstance(applicationContext)
        dbHelper.markAllNotificationsAsRead()
    }

}
