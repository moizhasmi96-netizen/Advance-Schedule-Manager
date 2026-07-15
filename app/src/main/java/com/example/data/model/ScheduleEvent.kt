package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_events")
data class ScheduleEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val specificDate: String? = null, // "YYYY-MM-DD" for one-off events
    val startTime: String, // "HH:MM" 24h
    val endTime: String, // "HH:MM" 24h
    val eventType: String, // "CLASS", "COACHING", "ACADEMY", "SELF_STUDY", "TEST", "OTHER"
    val location: String? = null,
    val googleEventId: String? = null,
    val isSynced: Boolean = false
)
