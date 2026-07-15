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
        prefs.customGeminiKey = key.trim().ifEmpty { null }
    }

    fun getGeminiApiKey(): String {
        return prefs.customGeminiKey ?: BuildConfig.GEMINI_API_KEY
    }

    // Schedule CRUD
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
                        _parsedPreviewEvents.value = parsed
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
            val parsed = GeminiParserService.parseScheduleText(getGeminiApiKey(), text)
            if (parsed.isNotEmpty()) {
                _parsedPreviewEvents.value = parsed
            }
            _isAILoading.value = false
        }
    }

    // Gemini Image Parse
    fun parseImageSchedule(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isAILoading.value = true
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val parsed = GeminiParserService.parseImageSchedule(getGeminiApiKey(), base64)
            if (parsed.isNotEmpty()) {
                _parsedPreviewEvents.value = parsed
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
            val responseText = GeminiParserService.chatWithGemini(getGeminiApiKey(), history, msgText)

            val aiMsg = ChatMessage(sender = "ai", message = responseText)
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

    fun googleSignOut() {
        prefs.googleUserEmail = null
        prefs.googleAccessToken = null
        prefs.isCalendarSyncEnabled = false
        _googleEmail.value = null
        _isCalendarSyncEnabled.value = false
    }
}
