package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getIntExtra("EVENT_ID", -1)
        val eventTitle = intent.getStringExtra("EVENT_TITLE") ?: "Activity"
        val eventStartTime = intent.getStringExtra("EVENT_START_TIME") ?: ""
        val hoursBefore = intent.getIntExtra("HOURS_BEFORE", 4)

        Log.d("EventNotificationReceiver", "Fired! ID: $eventId, Title: $eventTitle, hours before: $hoursBefore")

        showEventNotification(context, eventId, eventTitle, eventStartTime, hoursBefore)
    }

    private fun showEventNotification(
        context: Context,
        eventId: Int,
        title: String,
        startTime: String,
        hoursBefore: Int
    ) {
        val channelId = "activity_pre_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Upcoming Activity Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you before an activity/schedule event is about to start."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Format nice content text
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upcoming Activity: $title ⏳")
            .setContentText("Your activity starts in $hoursBefore hours at $startTime!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(eventId + 3000000, notification)
    }
}
