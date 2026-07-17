package com.example.data

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.alarm.EventNotificationScheduler
import com.example.data.local.AppDatabase
import com.example.data.model.Alarm
import com.example.data.model.ChatMessage
import com.example.data.model.ScheduleEvent
import com.example.service.GoogleCalendarSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ScheduleRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val eventDao = db.scheduleEventDao()
    private val alarmDao = db.alarmDao()
    private val chatDao = db.chatDao()

    // Events
    val allEventsFlow: Flow<List<ScheduleEvent>> = eventDao.getAllEventsFlow()

    suspend fun getAllEvents(): List<ScheduleEvent> {
        return eventDao.getAllEvents()
    }

    suspend fun insertEvent(event: ScheduleEvent) {
        val id = eventDao.insertEvent(event)
        val insertedEvent = event.copy(id = id.toInt())
        
        // Schedule pre-notification for the newly created event
        EventNotificationScheduler.scheduleEventNotification(
            context, 
            insertedEvent, 
            PrefsManager(context).activityPreNotifyHours
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val googleEventId = GoogleCalendarSyncService.syncEvent(context, insertedEvent)
                if (googleEventId != null) {
                    eventDao.updateEvent(insertedEvent.copy(googleEventId = googleEventId, isSynced = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun insertEvents(events: List<ScheduleEvent>) {
        eventDao.insertEvents(events)
        val hoursBefore = PrefsManager(context).activityPreNotifyHours
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val all = eventDao.getAllEvents()
                for (evt in all) {
                    // Schedule pre-notifications for all newly imported/inserted events
                    EventNotificationScheduler.scheduleEventNotification(context, evt, hoursBefore)
                    if (evt.googleEventId == null) {
                        val googleEventId = GoogleCalendarSyncService.syncEvent(context, evt)
                        if (googleEventId != null) {
                            eventDao.updateEvent(evt.copy(googleEventId = googleEventId, isSynced = true))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateEvent(event: ScheduleEvent) {
        eventDao.updateEvent(event)
        
        // Cancel old and schedule new pre-notification
        EventNotificationScheduler.cancelEventNotification(context, event)
        EventNotificationScheduler.scheduleEventNotification(
            context, 
            event, 
            PrefsManager(context).activityPreNotifyHours
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (event.googleEventId != null) {
                    GoogleCalendarSyncService.deleteEvent(context, event.googleEventId)
                }
                val googleEventId = GoogleCalendarSyncService.syncEvent(context, event)
                if (googleEventId != null) {
                    eventDao.updateEvent(event.copy(googleEventId = googleEventId, isSynced = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteEvent(event: ScheduleEvent) {
        eventDao.deleteEvent(event)
        
        // Cancel pre-notification upon event deletion
        EventNotificationScheduler.cancelEventNotification(context, event)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (event.googleEventId != null) {
                    GoogleCalendarSyncService.deleteEvent(context, event.googleEventId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteAllEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val all = eventDao.getAllEvents()
                for (evt in all) {
                    // Cancel pre-notifications for all events
                    EventNotificationScheduler.cancelEventNotification(context, evt)
                    if (evt.googleEventId != null) {
                        GoogleCalendarSyncService.deleteEvent(context, evt.googleEventId)
                    }
                }
                eventDao.deleteAllEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Alarms
    val allAlarmsFlow: Flow<List<Alarm>> = alarmDao.getAllAlarmsFlow()

    suspend fun insertAlarm(alarm: Alarm) {
        val id = alarmDao.insertAlarm(alarm)
        val inserted = alarm.copy(id = id.toInt())
        AlarmScheduler.scheduleNextAlarm(context, inserted)
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
        AlarmScheduler.scheduleNextAlarm(context, alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
        AlarmScheduler.cancelAlarm(context, alarm)
    }

    // Chat Messages
    val allMessagesFlow: Flow<List<ChatMessage>> = chatDao.getAllMessagesFlow()

    suspend fun insertMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChatHistory() {
        chatDao.clearHistory()
    }
}
