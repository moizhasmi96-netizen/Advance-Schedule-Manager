package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean = true,
    val days: String, // Comma-separated days of week, e.g., "Mon,Tue,Wed"
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val remindBeforeMinutes: Int = 0
)
