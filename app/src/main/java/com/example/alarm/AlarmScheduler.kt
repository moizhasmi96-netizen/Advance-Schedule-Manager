package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Alarm
import com.example.receiver.AlarmReceiver
import java.util.*

object AlarmScheduler {
    fun scheduleNextAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled || alarm.days.isEmpty()) {
            cancelAlarm(context, alarm)
            return
        }

        val triggerTime = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.days) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_HOUR", alarm.hour)
            putExtra("ALARM_MINUTE", alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
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
            Log.d("AlarmScheduler", "Scheduled alarm ${alarm.id} (${alarm.label}) for ${Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm: ${e.message}")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun calculateNextTriggerTime(hour: Int, minute: Int, daysStr: String): Long? {
        val days = daysStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (days.isEmpty()) return null

        val dayMap = mapOf(
            "Mon" to Calendar.MONDAY,
            "Tue" to Calendar.TUESDAY,
            "Wed" to Calendar.WEDNESDAY,
            "Thu" to Calendar.THURSDAY,
            "Fri" to Calendar.FRIDAY,
            "Sat" to Calendar.SATURDAY,
            "Sun" to Calendar.SUNDAY
        )

        val targetDays = days.mapNotNull { dayMap[it] }.sorted()
        if (targetDays.isEmpty()) return null

        val now = Calendar.getInstance()
        var closestTrigger: Calendar? = null

        // Try to find a trigger day starting from today up to next 8 days
        for (i in 0..8) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (candidate.timeInMillis > now.timeInMillis) {
                val candidateDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
                if (targetDays.contains(candidateDayOfWeek)) {
                    closestTrigger = candidate
                    break
                }
            }
        }

        return closestTrigger?.timeInMillis
    }
}
