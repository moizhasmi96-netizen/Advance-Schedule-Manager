package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppDatabase
import com.example.service.AlarmRingingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d("AlarmDismissReceiver", "Dismissing alarm ID: $alarmId")

        // Stop ringing sound and vibration
        val stopServiceIntent = Intent(context, AlarmRingingService::class.java)
        context.stopService(stopServiceIntent)

        if (alarmId != -1) {
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                val alarm = db.alarmDao().getAlarmById(alarmId)
                if (alarm != null) {
                    // Reschedule next repeating trigger
                    AlarmScheduler.scheduleNextAlarm(context, alarm)
                }
            }
        }
    }
}
