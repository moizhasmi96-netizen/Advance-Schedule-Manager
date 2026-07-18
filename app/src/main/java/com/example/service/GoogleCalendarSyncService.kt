package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.PrefsManager
import com.example.data.model.ScheduleEvent
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

object GoogleCalendarSyncService {
    private val client = OkHttpClient()
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    private suspend fun getFreshAccessToken(context: Context): String? {
        val prefs = PrefsManager(context)
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (account == null || account.email == null) {
            Log.d("CalendarSync", "No Google account signed in.")
            return null
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val scopeStr = "oauth2:https://www.googleapis.com/auth/calendar.events"
            val currentToken = prefs.googleAccessToken
            try {
                val emailAccount = account.account ?: android.accounts.Account(account.email!!, "com.google")
                if (!currentToken.isNullOrEmpty()) {
                    try {
                        com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, currentToken)
                    } catch (e: Exception) {
                        Log.w("CalendarSync", "Error clearing old token: ${e.message}")
                    }
                }
                val freshToken = com.google.android.gms.auth.GoogleAuthUtil.getToken(context, emailAccount, scopeStr)
                prefs.googleAccessToken = freshToken
                Log.d("CalendarSync", "Successfully obtained fresh token.")
                freshToken
            } catch (e: Exception) {
                Log.e("CalendarSync", "Failed to get fresh token: ${e.message}")
                currentToken
            }
        }
    }

    fun formatRfc3339(dateStr: String, timeStr: String): String {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val formattedTime = String.format("%02d:%02d:00", h, m)
        
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz)
        val offsetMillis = tz.getOffset(cal.timeInMillis)
        val offsetHours = Math.abs(offsetMillis / 3600000)
        val offsetMinutes = Math.abs((offsetMillis / 60000) % 60)
        val sign = if (offsetMillis >= 0) "+" else "-"
        val offsetStr = String.format("%s%02d:%02d", sign, offsetHours, offsetMinutes)
        
        return "${dateStr.trim()}T$formattedTime$offsetStr"
    }

    suspend fun syncEvent(context: Context, event: ScheduleEvent): String? {
        val prefs = PrefsManager(context)
        if (!prefs.isCalendarSyncEnabled) {
            Log.d("CalendarSync", "Sync skipped. Calendar sync is disabled in settings.")
            return null
        }

        val token = getFreshAccessToken(context)
        if (token.isNullOrEmpty()) {
            Log.d("CalendarSync", "Sync skipped. OAuth token not available.")
            return null
        }

        val dateStr = if (!event.specificDate.isNullOrBlank()) {
            event.specificDate
        } else {
            getNextDateForDayOfWeek(event.dayOfWeek)
        }

        val startDateTime = formatRfc3339(dateStr, event.startTime)
        val endDateTime = formatRfc3339(dateStr, event.endTime)
        val localTimeZoneId = TimeZone.getDefault().id

        val bodyJson = JSONObject().apply {
            put("summary", event.title)
            if (!event.location.isNullOrEmpty()) {
                put("location", event.location)
            }
            put("start", JSONObject().apply {
                put("dateTime", startDateTime)
                put("timeZone", localTimeZoneId)
            })
            put("end", JSONObject().apply {
                put("dateTime", endDateTime)
                put("timeZone", localTimeZoneId)
            })
            if (event.specificDate == null) {
                // Repeating weekly event
                put("recurrence", JSONArray().put("RRULE:FREQ=WEEKLY"))
            }
        }

        val request = Request.Builder()
            .url("https://www.googleapis.com/calendar/v3/calendars/primary/events")
            .post(bodyJson.toString().toRequestBody(mediaTypeJson))
            .addHeader("Authorization", "Bearer $token")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val respJson = JSONObject(responseBody)
                val eventId = respJson.optString("id")
                Log.d("CalendarSync", "Successfully synced event: $eventId")
                eventId
            } else {
                Log.e("CalendarSync", "Error syncing event: ${response.code} ${response.message}")
                Log.e("CalendarSync", "Response body: $responseBody")
                null
            }
        } catch (e: Exception) {
            Log.e("CalendarSync", "Exception during sync: ${e.message}")
            null
        }
    }

    suspend fun deleteEvent(context: Context, googleEventId: String) {
        val prefs = PrefsManager(context)
        val token = getFreshAccessToken(context) ?: prefs.googleAccessToken
        if (token.isNullOrEmpty()) return

        val request = Request.Builder()
            .url("https://www.googleapis.com/calendar/v3/calendars/primary/events/$googleEventId")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("CalendarSync", "Deleted event $googleEventId from Google Calendar")
            } else {
                Log.e("CalendarSync", "Failed to delete calendar event: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("CalendarSync", "Error deleting event: ${e.message}")
        }
    }

    fun getNextDateForDayOfWeek(dayOfWeek: String): String {
        val dayMap = mapOf(
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )
        val targetDay = dayMap[dayOfWeek.lowercase().trim()] ?: Calendar.MONDAY
        val calendar = Calendar.getInstance()
        var safety = 0
        while (calendar.get(Calendar.DAY_OF_WEEK) != targetDay && safety < 14) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            safety++
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }
}
