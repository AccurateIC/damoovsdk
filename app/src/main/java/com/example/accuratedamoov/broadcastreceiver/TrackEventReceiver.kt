package com.example.accuratedamoov.broadcastreceiver

import android.content.Context
import android.location.Location
import android.util.Log
import com.telematicssdk.tracking.SpeedViolation
import com.telematicssdk.tracking.TrackingEventsReceiver
import com.telematicssdk.tracking.model.sensor.Event

class TrackEventReceiver : TrackingEventsReceiver() {
    override fun onLocationChanged(
        context: Context,
        location: Location
    ) {
    }

    override fun onStartTracking(context: Context) {
        Log.d("TrackEventReceiver", "Tracking started")
    }

    override fun onStopTracking(context: Context) {
        Log.d("TrackEventReceiver", "Tracking stopped")
    }

    override fun onSpeedViolation(
        context: Context,
        violation: SpeedViolation
    ) {
    }

    override fun onNewEvents(
        context: Context,
        events: Array<Event>
    ) {
    }

    override fun onSdkDeprecated(context: Context) {
    }
}