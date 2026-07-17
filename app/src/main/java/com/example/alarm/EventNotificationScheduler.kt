package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.ScheduleEvent
import com.example.receiver.EventNotificationReceiver
import java.util.*

object EventNotificationScheduler {

    fun scheduleEventNotification(context: Context, event: ScheduleEvent, hoursBefore: Int) {
        val triggerTime = calculateNextTriggerTime(event, hoursBefore) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventNotificationReceiver::class.java).apply {
            putExtra("EVENT_ID", event.id)
            putExtra("EVENT_TITLE", event.title)
            putExtra("EVENT_START_TIME", event.startTime)
            putExtra("HOURS_BEFORE", hoursBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id + 3000000, // Offset to avoid conflicts with custom alarms
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                )
            }
            Log.d("EventNotificationSch", "Scheduled pre-reminder for event ${event.id} (${event.title}) for ${Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.e("EventNotificationSch", "SecurityException scheduling exact event notification: ${e.message}")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelEventNotification(context: Context, event: ScheduleEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id + 3000000,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("EventNotificationSch", "Cancelled pre-reminder for event ${event.id}")
        }
    }

    fun rescheduleAllEventNotifications(context: Context, events: List<ScheduleEvent>, hoursBefore: Int) {
        for (event in events) {
            cancelEventNotification(context, event)
            scheduleEventNotification(context, event, hoursBefore)
        }
    }

    private fun calculateNextTriggerTime(event: ScheduleEvent, hoursBefore: Int): Long? {
        val parts = event.startTime.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        val now = Calendar.getInstance()

        if (!event.specificDate.isNullOrBlank()) {
            val dateParts = event.specificDate.split("-")
            if (dateParts.size == 3) {
                val year = dateParts[0].toIntOrNull() ?: return null
                val month = (dateParts[1].toIntOrNull() ?: return null) - 1
                val day = dateParts[2].toIntOrNull() ?: return null

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val triggerTime = calendar.timeInMillis - (hoursBefore * 60 * 60 * 1000L)
                return if (triggerTime > now.timeInMillis) triggerTime else null
            }
            return null
        }

        val dayMap = mapOf(
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )
        val targetDay = dayMap[event.dayOfWeek.lowercase()] ?: return null

        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) == targetDay) {
                val triggerTime = candidate.timeInMillis - (hoursBefore * 60 * 60 * 1000L)
                if (triggerTime > now.timeInMillis) {
                    return triggerTime
                }
                if (i == 0) {
                    val nextWeekCandidate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 7)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val triggerNextWeek = nextWeekCandidate.timeInMillis - (hoursBefore * 60 * 60 * 1000L)
                    if (triggerNextWeek > now.timeInMillis) {
                        return triggerNextWeek
                    }
                }
            }
        }
        return null
    }
}
