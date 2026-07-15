package com.example.data

import com.example.data.model.ScheduleEvent
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParserHelper {
    fun parseCsv(inputStream: InputStream): List<ScheduleEvent> {
        val events = mutableListOf<ScheduleEvent>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        try {
            val headerLine = reader.readLine() ?: return emptyList()
            val headers = headerLine.split(",").map { it.trim().lowercase() }

            val dayIdx = headers.indexOfFirst { it.contains("day") }
            val subjectIdx = headers.indexOfFirst { it.contains("subject") || it.contains("title") }
            val startIdx = headers.indexOfFirst { it.contains("start") }
            val endIdx = headers.indexOfFirst { it.contains("end") }
            val typeIdx = headers.indexOfFirst { it.contains("type") }

            if (dayIdx == -1 || subjectIdx == -1 || startIdx == -1 || endIdx == -1) {
                // Fallback to absolute indices if names don't match
                return parseWithFallbackIndices(reader)
            }

            var line = reader.readLine()
            while (line != null) {
                if (line.trim().isEmpty()) {
                    line = reader.readLine()
                    continue
                }
                val tokens = line.split(",").map { it.trim() }
                if (tokens.size > maxOf(dayIdx, subjectIdx, startIdx, endIdx)) {
                    val dayRaw = tokens[dayIdx]
                    val subject = tokens[subjectIdx]
                    val start = formatTime(tokens[startIdx])
                    val end = formatTime(tokens[endIdx])
                    val typeRaw = if (typeIdx != -1 && typeIdx < tokens.size) tokens[typeIdx].uppercase() else "OTHER"

                    val dayOfWeek = formatDayOfWeek(dayRaw)

                    events.add(
                        ScheduleEvent(
                            title = subject,
                            dayOfWeek = dayOfWeek,
                            startTime = start,
                            endTime = end,
                            eventType = typeRaw,
                            isSynced = false
                        )
                    )
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { reader.close() } catch (ignored: Exception) {}
        }
        return events
    }

    private fun parseWithFallbackIndices(reader: BufferedReader): List<ScheduleEvent> {
        val events = mutableListOf<ScheduleEvent>()
        var line = reader.readLine()
        while (line != null) {
            if (line.trim().isEmpty()) {
                line = reader.readLine()
                continue
            }
            val tokens = line.split(",").map { it.trim() }
            if (tokens.size >= 4) {
                val dayRaw = tokens[0]
                val subject = tokens[1]
                val start = formatTime(tokens[2])
                val end = formatTime(tokens[3])
                val typeRaw = if (tokens.size >= 5) tokens[4].uppercase() else "OTHER"

                val dayOfWeek = formatDayOfWeek(dayRaw)

                events.add(
                    ScheduleEvent(
                        title = subject,
                        dayOfWeek = dayOfWeek,
                        startTime = start,
                        endTime = end,
                        eventType = typeRaw,
                        isSynced = false
                    )
                )
            }
            line = reader.readLine()
        }
        return events
    }

    private fun formatTime(timeStr: String): String {
        // Clean and ensure HH:MM format
        val clean = timeStr.trim().replace(" ", "")
        if (clean.contains(":")) {
            val parts = clean.split(":")
            if (parts.size >= 2) {
                val hh = parts[0].padStart(2, '0')
                val mm = parts[1].padStart(2, '0').take(2)
                return "$hh:$mm"
            }
        }
        return "12:00"
    }

    private fun formatDayOfWeek(dayRaw: String): String {
        val clean = dayRaw.trim().lowercase()
        return when {
            clean.startsWith("mon") -> "Monday"
            clean.startsWith("tue") -> "Tuesday"
            clean.startsWith("wed") -> "Wednesday"
            clean.startsWith("thu") -> "Thursday"
            clean.startsWith("fri") -> "Friday"
            clean.startsWith("sat") -> "Saturday"
            clean.startsWith("sun") -> "Sunday"
            else -> "Monday"
        }
    }
}
