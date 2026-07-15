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

    suspend fun syncEvent(context: Context, event: ScheduleEvent): String? {
        val prefs = PrefsManager(context)
        val token = prefs.googleAccessToken
        if (token.isNullOrEmpty() || !prefs.isCalendarSyncEnabled) {
            Log.d("CalendarSync", "Sync skipped. OAuth not connected or sync disabled.")
            return null
        }

        val dateStr = if (event.specificDate != null) {
            event.specificDate
        } else {
            getNextDateForDayOfWeek(event.dayOfWeek)
        }

        val startDateTime = "${dateStr}T${event.startTime}:00"
        val endDateTime = "${dateStr}T${event.endTime}:00"

        val bodyJson = JSONObject().apply {
            put("summary", event.title)
            if (!event.location.isNullOrEmpty()) {
                put("location", event.location)
            }
            put("start", JSONObject().apply {
                put("dateTime", startDateTime)
                put("timeZone", "Asia/Karachi")
            })
            put("end", JSONObject().apply {
                put("dateTime", endDateTime)
                put("timeZone", "Asia/Karachi")
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
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val respJson = JSONObject(respBody)
                val eventId = respJson.optString("id")
                Log.d("CalendarSync", "Successfully synced event: $eventId")
                eventId
            } else {
                Log.e("CalendarSync", "Error syncing event: ${response.code} ${response.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("CalendarSync", "Exception during sync: ${e.message}")
            null
        }
    }

    suspend fun deleteEvent(context: Context, googleEventId: String) {
        val prefs = PrefsManager(context)
        val token = prefs.googleAccessToken
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

    private fun getNextDateForDayOfWeek(dayOfWeek: String): String {
        val dayMap = mapOf(
            "Monday" to Calendar.MONDAY,
            "Tuesday" to Calendar.TUESDAY,
            "Wednesday" to Calendar.WEDNESDAY,
            "Thursday" to Calendar.THURSDAY,
            "Friday" to Calendar.FRIDAY,
            "Saturday" to Calendar.SATURDAY,
            "Sunday" to Calendar.SUNDAY
        )
        val targetDay = dayMap[dayOfWeek] ?: Calendar.MONDAY
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
