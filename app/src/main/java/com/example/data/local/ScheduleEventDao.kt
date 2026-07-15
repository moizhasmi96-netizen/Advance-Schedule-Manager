package com.example.data.local

import androidx.room.*
import com.example.data.model.ScheduleEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleEventDao {
    @Query("SELECT * FROM schedule_events ORDER BY startTime ASC")
    fun getAllEventsFlow(): Flow<List<ScheduleEvent>>

    @Query("SELECT * FROM schedule_events")
    suspend fun getAllEvents(): List<ScheduleEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ScheduleEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ScheduleEvent>)

    @Update
    suspend fun updateEvent(event: ScheduleEvent)

    @Delete
    suspend fun deleteEvent(event: ScheduleEvent)

    @Query("DELETE FROM schedule_events")
    suspend fun deleteAllEvents()
}
