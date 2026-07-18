package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmScheduler
import com.example.data.model.Alarm
import com.example.receiver.AlarmDismissReceiver
import com.example.service.AlarmRingingService
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WhitePrimary
import java.util.*

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeDevice()
        enableEdgeToEdge()

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "KRONOS Alarm"
        val alarmHour = intent.getIntExtra("ALARM_HOUR", 7)
        val alarmMinute = intent.getIntExtra("ALARM_MINUTE", 30)

        setContent {
            MyApplicationTheme {
                AlarmRingingUI(
                    alarmLabel = alarmLabel,
                    hour = alarmHour,
                    minute = alarmMinute,
                    onDismiss = {
                        dismissAlarm(alarmId)
                    },
                    onSnooze = {
                        snoozeAlarm(alarmId, alarmLabel, alarmHour, alarmMinute)
                    }
                )
            }
        }
    }

    private fun wakeDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dismissAlarm(alarmId: Int) {
        val intent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        sendBroadcast(intent)
        finish()
    }

    private fun snoozeAlarm(alarmId: Int, label: String, hour: Int, minute: Int) {
        // Stop currently ringing service
        val stopServiceIntent = Intent(this, AlarmRingingService::class.java)
        stopService(stopServiceIntent)

        // Reschedule alarm for 5 minutes in the future
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }

        val snoozeAlarm = Alarm(
            id = alarmId,
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            label = "$label [SNOOZED]",
            isEnabled = true,
            days = "" // Empty days indicates a one-off/immediate alarm, which is perfect for snooze and avoids day-of-week rollover bugs
        )

        AlarmScheduler.scheduleNextAlarm(this, snoozeAlarm)
        finish()
    }

    private fun getTodayDayShort(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> "Mon"
        }
    }
}

@Composable
fun AlarmRingingUI(
    alarmLabel: String,
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "KRONOS ALERT",
                color = LimeAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .background(LimeAccent.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, LimeAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = LimeAccent,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = alarmLabel,
                color = WhitePrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DISCIPLINE TRUMPS MOTIVATION. WAKE UP.",
                color = LimeAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = CarbonBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("dismiss_alarm_btn")
            ) {
                Text("DISMISS ALARM", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("snooze_alarm_btn")
            ) {
                Text("SNOOZE 5 MINUTES", color = LimeAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
