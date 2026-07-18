package com.example

import com.example.service.GeminiParserService
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testParseEventsJson_validJson() {
        val json = """
            [
                {
                    "title": "Maths Class",
                    "dayOfWeek": "Monday",
                    "specificDate": null,
                    "startTime": "09:00",
                    "endTime": "10:30",
                    "eventType": "CLASS",
                    "location": "Room 101"
                },
                {
                    "title": "Physics Coaching",
                    "dayOfWeek": "Wednesday",
                    "specificDate": "2026-07-20",
                    "startTime": "15:00",
                    "endTime": "16:30",
                    "eventType": "COACHING",
                    "location": null
                }
            ]
        """.trimIndent()

        val events = GeminiParserService.parseEventsJson(json)
        assertEquals(2, events.size)

        val first = events[0]
        assertEquals("Maths Class", first.title)
        assertEquals("Monday", first.dayOfWeek)
        assertNull(first.specificDate)
        assertEquals("09:00", first.startTime)
        assertEquals("10:30", first.endTime)
        assertEquals("CLASS", first.eventType)
        assertEquals("Room 101", first.location)

        val second = events[1]
        assertEquals("Physics Coaching", second.title)
        assertEquals("Wednesday", second.dayOfWeek)
        assertEquals("2026-07-20", second.specificDate)
        assertEquals("15:00", second.startTime)
        assertEquals("16:30", second.endTime)
        assertEquals("COACHING", second.eventType)
        assertNull(second.location)
    }

    @Test
    fun testParseEventsJson_withMarkdownFormatting() {
        val jsonWithMarkdown = """
            ```json
            [
                {
                    "title": "Chemistry Academy",
                    "dayOfWeek": "Friday",
                    "specificDate": null,
                    "startTime": "14:00",
                    "endTime": "15:30",
                    "eventType": "ACADEMY",
                    "location": "DHA Campus"
                }
            ]
            ```
        """.trimIndent()

        val events = GeminiParserService.parseEventsJson(jsonWithMarkdown)
        assertEquals(1, events.size)
        val event = events[0]
        assertEquals("Chemistry Academy", event.title)
        assertEquals("Friday", event.dayOfWeek)
        assertEquals("ACADEMY", event.eventType)
        assertEquals("DHA Campus", event.location)
    }

    @Test
    fun testParseEventsJson_invalidJson_returnsEmptyList() {
        val invalidJson = "This is not a JSON string!"
        val events = GeminiParserService.parseEventsJson(invalidJson)
        assertTrue(events.isEmpty())
    }
}
