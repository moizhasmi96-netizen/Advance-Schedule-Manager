package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.CsvParserHelper
import com.example.data.PrefsManager
import com.example.data.ScheduleRepository
import com.example.data.model.Alarm
import com.example.data.model.ChatMessage
import com.example.data.model.ScheduleEvent
import com.example.service.GeminiParserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScheduleRepository(application)
    val prefs = PrefsManager(application)

    // Flows
    val events: StateFlow<List<ScheduleEvent>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alarms: StateFlow<List<Alarm>> = repository.allAlarmsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _isAILoading = MutableStateFlow(false)
    val isAILoading = _isAILoading.asStateFlow()

    private val _parsedPreviewEvents = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    val parsedPreviewEvents = _parsedPreviewEvents.asStateFlow()

    // Preferences exposed directly
    private val _isCalendarSyncEnabled = MutableStateFlow(prefs.isCalendarSyncEnabled)
    val isCalendarSyncEnabled = _isCalendarSyncEnabled.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(prefs.is24HourFormat)
    val is24HourFormat = _is24HourFormat.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode = _themeMode.asStateFlow()

    private val _googleEmail = MutableStateFlow(prefs.googleUserEmail)
    val googleEmail = _googleEmail.asStateFlow()

    private val _hasShownApiKeyOnboarding = MutableStateFlow(prefs.hasShownApiKeyOnboarding)
    val hasShownApiKeyOnboarding = _hasShownApiKeyOnboarding.asStateFlow()

    private val _customGeminiKey = MutableStateFlow(prefs.customGeminiKey)
    val customGeminiKey = _customGeminiKey.asStateFlow()

    private val _activityPreNotifyHours = MutableStateFlow(prefs.activityPreNotifyHours)
    val activityPreNotifyHours = _activityPreNotifyHours.asStateFlow()

    fun setActivityPreNotifyHours(hours: Int) {
        prefs.activityPreNotifyHours = hours
        _activityPreNotifyHours.value = hours
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val allEvents = repository.getAllEvents()
                com.example.alarm.EventNotificationScheduler.rescheduleAllEventNotifications(
                    getApplication(),
                    allEvents,
                    hours
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCalendarSyncEnabled(enabled: Boolean) {
        prefs.isCalendarSyncEnabled = enabled
        _isCalendarSyncEnabled.value = enabled
    }

    fun set24HourFormat(enabled: Boolean) {
        prefs.is24HourFormat = enabled
        _is24HourFormat.value = enabled
    }

    fun setThemeMode(mode: String) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun saveGeminiApiKey(key: String) {
        val trimmed = key.trim().ifEmpty { null }
        prefs.customGeminiKey = trimmed
        _customGeminiKey.value = trimmed
    }

    fun setHasShownApiKeyOnboarding(shown: Boolean) {
        prefs.hasShownApiKeyOnboarding = shown
        _hasShownApiKeyOnboarding.value = shown
    }

    fun getGeminiApiKey(): String {
        return prefs.customGeminiKey ?: BuildConfig.GEMINI_API_KEY
    }

    // Schedule CRUD
    fun getOverlappingEvents(event: ScheduleEvent): List<ScheduleEvent> {
        val list = events.value
        val targetDay = event.dayOfWeek.trim().lowercase()
        val targetDate = event.specificDate?.trim()

        return list.filter { existing ->
            // Skip checking against itself if we're editing
            if (existing.id != 0 && existing.id == event.id) return@filter false

            val existingDay = existing.dayOfWeek.trim().lowercase()
            val existingDate = existing.specificDate?.trim()

            // Check if they represent the same day
            val isSameDay = (targetDay == existingDay) || 
                            (!targetDate.isNullOrBlank() && !existingDate.isNullOrBlank() && targetDate == existingDate)

            if (!isSameDay) return@filter false

            // Check time overlaps
            val existingStart = timeToMinutes(existing.startTime) ?: return@filter false
            val existingEnd = timeToMinutes(existing.endTime) ?: return@filter false
            val targetStart = timeToMinutes(event.startTime) ?: return@filter false
            val targetEnd = timeToMinutes(event.endTime) ?: return@filter false

            // Overlap check: start1 < end2 and end1 > start2
            targetStart < existingEnd && targetEnd > existingStart
        }
    }

    private fun timeToMinutes(timeStr: String): Int? {
        val parts = timeStr.split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return h * 60 + m
        }
        return null
    }

    fun saveEventOverwritingConflicts(event: ScheduleEvent, conflictsToDelete: List<ScheduleEvent>) {
        viewModelScope.launch {
            conflictsToDelete.forEach { conflict ->
                repository.deleteEvent(conflict)
            }
            if (event.id == 0) {
                repository.insertEvent(event)
            } else {
                repository.updateEvent(event)
            }
        }
    }

    fun addEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            repository.insertEvent(event)
        }
    }

    fun updateEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun deleteEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun clearEvents() {
        viewModelScope.launch {
            repository.deleteAllEvents()
        }
    }

    // Alarms CRUD
    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.insertAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    // CSV Pick and Import
    fun importCsvFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val parsed = CsvParserHelper.parseCsv(inputStream)
                    if (parsed.isNotEmpty()) {
                        val withDates = parsed.map { event ->
                            if (event.specificDate.isNullOrBlank()) {
                                event.copy(specificDate = com.example.service.GoogleCalendarSyncService.getNextDateForDayOfWeek(event.dayOfWeek))
                            } else {
                                event
                            }
                        }
                        _parsedPreviewEvents.value = withDates
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing CSV: ${e.message}")
            }
        }
    }

    fun acceptParsedPreview() {
        viewModelScope.launch {
            val current = _parsedPreviewEvents.value
            if (current.isNotEmpty()) {
                repository.insertEvents(current)
                _parsedPreviewEvents.value = emptyList()
            }
        }
    }

    fun clearParsedPreview() {
        _parsedPreviewEvents.value = emptyList()
    }

    // Gemini Text Parse
    fun parseTextSchedule(text: String) {
        viewModelScope.launch {
            _isAILoading.value = true
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss (EEEE)", java.util.Locale.getDefault())
            val currentTimeStr = sdf.format(java.util.Date())
            val parsed = GeminiParserService.parseScheduleText(getGeminiApiKey(), text, currentTimeStr)
            if (parsed.isNotEmpty()) {
                val withDates = parsed.map { event ->
                    if (event.specificDate.isNullOrBlank()) {
                        event.copy(specificDate = com.example.service.GoogleCalendarSyncService.getNextDateForDayOfWeek(event.dayOfWeek))
                    } else {
                        event
                    }
                }
                _parsedPreviewEvents.value = withDates
            }
            _isAILoading.value = false
        }
    }

    // Gemini Image Parse
    fun parseImageSchedule(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isAILoading.value = true
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss (EEEE)", java.util.Locale.getDefault())
            val currentTimeStr = sdf.format(java.util.Date())
            val parsed = GeminiParserService.parseImageSchedule(getGeminiApiKey(), base64, currentTimeStr)
            if (parsed.isNotEmpty()) {
                val withDates = parsed.map { event ->
                    if (event.specificDate.isNullOrBlank()) {
                        event.copy(specificDate = com.example.service.GoogleCalendarSyncService.getNextDateForDayOfWeek(event.dayOfWeek))
                    } else {
                        event
                    }
                }
                _parsedPreviewEvents.value = withDates
            }
            _isAILoading.value = false
        }
    }

    // Gemini Conversational Chat
    fun sendChatMessage(msgText: String) {
        if (msgText.trim().isEmpty()) return
        viewModelScope.launch {
            val userMsg = ChatMessage(sender = "user", message = msgText)
            repository.insertMessage(userMsg)

            _isAILoading.value = true
            val history = chatMessages.value

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss (EEEE)", java.util.Locale.getDefault())
            val currentTimeStr = sdf.format(java.util.Date())

            val currentEventsText = events.value.joinToString("\n") { 
                "- [ID: ${it.id}] ${it.title} on ${it.dayOfWeek} at ${it.startTime}-${it.endTime} (Type: ${it.eventType}, Date: ${it.specificDate ?: "Weekly"}, Location: ${it.location ?: "N/A"})"
            }.ifEmpty { "None" }

            val currentAlarmsText = alarms.value.joinToString("\n") { 
                "- [ID: ${it.id}] Time: ${String.format("%02d:%02d", it.hour, it.minute)}, Label: ${it.label}, Days: ${it.days.ifEmpty { "One-time" }}, Enabled: ${it.isEnabled}"
            }.ifEmpty { "None" }

            val rawResponse = GeminiParserService.chatWithGemini(
                getGeminiApiKey(),
                history,
                msgText,
                currentEventsText,
                currentAlarmsText,
                currentTimeStr
            )

            var displayResponse = rawResponse
            try {
                var sanitized = rawResponse.trim()
                if (sanitized.startsWith("```")) {
                    sanitized = sanitized.substringAfter("\n").substringBeforeLast("```").trim()
                }
                if (sanitized.startsWith("json")) {
                    sanitized = sanitized.substring(4).trim()
                }

                val jsonObj = org.json.JSONObject(sanitized)
                displayResponse = jsonObj.optString("response", rawResponse)

                val commandsArray = jsonObj.optJSONArray("commands")
                if (commandsArray != null) {
                    for (i in 0 until commandsArray.length()) {
                        val cmd = commandsArray.getJSONObject(i)
                        val action = cmd.optString("action")
                        Log.d("MainViewModel", "Processing chatbot command: $action")
                        when (action) {
                            "ADD_ALARM" -> {
                                val hour = cmd.optInt("hour")
                                val minute = cmd.optInt("minute")
                                val label = cmd.optString("label", "KRONOS Alarm")
                                val days = cmd.optString("days", "")
                                val alarm = Alarm(
                                    hour = hour,
                                    minute = minute,
                                    label = label,
                                    days = days,
                                    isEnabled = true
                                )
                                repository.insertAlarm(alarm)
                            }
                            "DELETE_ALARM" -> {
                                val label = cmd.optString("label", "")
                                val hour = cmd.optInt("hour", -1)
                                val minute = cmd.optInt("minute", -1)
                                alarms.value.forEach { alarm ->
                                    if ((label.isNotEmpty() && alarm.label.equals(label, ignoreCase = true)) || 
                                        (hour != -1 && minute != -1 && alarm.hour == hour && alarm.minute == minute)) {
                                        repository.deleteAlarm(alarm)
                                    }
                                }
                            }
                            "ADD_EVENT" -> {
                                val title = cmd.optString("title", "Untitled Event")
                                val dayOfWeek = cmd.optString("dayOfWeek", "Monday")
                                val specificDate = if (cmd.isNull("specificDate")) null else cmd.optString("specificDate").ifEmpty { null }
                                val startTime = cmd.optString("startTime", "09:00")
                                val endTime = cmd.optString("endTime", "10:00")
                                val eventType = cmd.optString("eventType", "OTHER")
                                val location = if (cmd.isNull("location")) null else cmd.optString("location").ifEmpty { null }
                                
                                val event = ScheduleEvent(
                                    title = title,
                                    dayOfWeek = dayOfWeek,
                                    specificDate = specificDate,
                                    startTime = startTime,
                                    endTime = endTime,
                                    eventType = eventType,
                                    location = location
                                )
                                repository.insertEvent(event)
                            }
                            "DELETE_EVENT" -> {
                                val title = cmd.optString("title", "")
                                if (title.isNotEmpty()) {
                                    events.value.forEach { event ->
                                        if (event.title.equals(title, ignoreCase = true)) {
                                            repository.deleteEvent(event)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to parse chatbot command JSON: ${e.message}. Falling back to raw text.")
            }

            val aiMsg = ChatMessage(sender = "ai", message = displayResponse)
            repository.insertMessage(aiMsg)
            _isAILoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun mockGoogleSignIn(email: String, token: String) {
        prefs.googleUserEmail = email
        prefs.googleAccessToken = token
        _googleEmail.value = email
    }

    fun googleSignIn(email: String, token: String) {
        prefs.googleUserEmail = email
        prefs.googleAccessToken = token
        _googleEmail.value = email
        prefs.isCalendarSyncEnabled = true
        _isCalendarSyncEnabled.value = true
    }

    fun googleSignOut() {
        prefs.googleUserEmail = null
        prefs.googleAccessToken = null
        prefs.isCalendarSyncEnabled = false
        _googleEmail.value = null
        _isCalendarSyncEnabled.value = false
    }
}
