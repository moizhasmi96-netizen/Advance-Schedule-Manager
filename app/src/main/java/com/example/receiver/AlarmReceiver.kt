package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.service.AlarmRingingService
import com.example.ui.screens.AlarmRingActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val alarmHour = intent.getIntExtra("ALARM_HOUR", 0)
        val alarmMinute = intent.getIntExtra("ALARM_MINUTE", 0)

        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)
        if (isPreReminder) {
            val preMinutes = intent.getIntExtra("PRE_REMINDER_MINUTES", 5)
            Log.d("AlarmReceiver", "Pre-reminder fired! ID: $alarmId, Label: $alarmLabel, minutes before: $preMinutes")
            showPreReminderNotification(context, alarmId, alarmLabel, preMinutes, alarmHour, alarmMinute)
            return
        }

        Log.d("AlarmReceiver", "Alarm fired! ID: $alarmId, Label: $alarmLabel")

        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_HOUR", alarmHour)
            putExtra("ALARM_MINUTE", alarmMinute)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        val activityIntent = Intent(context, AlarmRingActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_HOUR", alarmHour)
            putExtra("ALARM_MINUTE", alarmMinute)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(activityIntent)
    }

    private fun showPreReminderNotification(
        context: Context,
        alarmId: Int,
        label: String,
        minutes: Int,
        hour: Int,
        minute: Int
    ) {
        val channelId = "alarm_pre_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Upcoming Alarm Reminders",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you shortly before an alarm is about to ring."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val timeStr = String.format("%02d:%02d %s", displayHour, minute, amPm)

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Upcoming Alarm: $label")
            .setContentText("Ringing in $minutes minutes at $timeStr")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmId + 2000000, notification)
    }
}
