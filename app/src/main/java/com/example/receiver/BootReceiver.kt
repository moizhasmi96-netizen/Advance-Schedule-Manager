package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.alarm.EventNotificationScheduler
import com.example.data.PrefsManager
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Reboot detected, restoring alarms and event notifications...")
            val db = AppDatabase.getDatabase(context)
            val prefs = PrefsManager(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Restore custom alarms
                    val alarms = db.alarmDao().getAllAlarms()
                    for (alarm in alarms) {
                        if (alarm.isEnabled) {
                            AlarmScheduler.scheduleNextAlarm(context, alarm)
                        }
                    }
                    Log.d("BootReceiver", "Successfully restored ${alarms.size} alarms")

                    // Restore event pre-notifications
                    val events = db.scheduleEventDao().getAllEvents()
                    val preNotifyHours = prefs.activityPreNotifyHours
                    for (event in events) {
                        EventNotificationScheduler.scheduleEventNotification(context, event, preNotifyHours)
                    }
                    Log.d("BootReceiver", "Successfully restored ${events.size} activity pre-notifications")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring alarms or event notifications: ${e.message}")
                }
            }
        }
    }
}
