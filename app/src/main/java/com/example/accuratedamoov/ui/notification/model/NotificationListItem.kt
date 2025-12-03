package com.example.accuratedamoov.ui.notification.model

import com.example.accuratedamoov.model.TripNotification

sealed class NotificationListItem {
    data class DateHeader(val title: String) : NotificationListItem()
    data class NotificationItem(val data: TripNotification) : NotificationListItem()
}

