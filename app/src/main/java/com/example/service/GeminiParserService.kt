package com.example.service

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ScheduleEvent
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiParserService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun parseScheduleText(apiKey: String, text: String): List<ScheduleEvent> {
        val systemPrompt = """
            You are a highly precise schedule parser. Your job is to extract weekly class schedules, coaching times, or exam/test dates from text and convert them into a structured JSON array.
            
            Every event in the output array must strictly contain:
            - "title": String (The subject or name, e.g. "Physics Test", "Maths Coaching", "Chemistry Class")
            - "dayOfWeek": String (The full name of the day: "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            - "specificDate": String or null (If it's a one-off exam or test date, use "YYYY-MM-DD" format. If it's a regular weekly event, set to null)
            - "startTime": String (HH:MM 24h format, e.g., "09:30", "16:00")
            - "endTime": String (HH:MM 24h format, e.g., "11:00", "17:30")
            - "eventType": String (Must be one of: "CLASS", "COACHING", "ACADEMY", "SELF_STUDY", "TEST", "OTHER")
            - "location": String or null (e.g. "Khan Academy", "Sir Naeem's Room")
            
            For Friday prayer times (Jumu'ah 13:00 - 14:00), if the user schedules during this time, add an event labeled "Jumu'ah Break" or adjust coaching times with a polite warning in location or title.
            Return ONLY a raw JSON array of events, without any Markdown backticks, block formatting, or extra characters. Example:
            [{"title": "Maths", "dayOfWeek": "Monday", "specificDate": null, "startTime": "09:00", "endTime": "10:30", "eventType": "CLASS", "location": "School"}]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "Parse the following schedule: $text")))
            ),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.1),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = api.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d("GeminiParserService", "Parsed Raw Response: $rawText")
            parseEventsJson(rawText)
        } catch (e: Exception) {
            Log.e("GeminiParserService", "Error calling Gemini API: ${e.message}")
            emptyList()
        }
    }

    suspend fun parseImageSchedule(apiKey: String, base64Image: String): List<ScheduleEvent> {
        val systemPrompt = """
            You are a highly precise visual schedule parser. Analyze this image of a schedule or timetable and extract all events into a JSON array.
            
            Every event in the output array must strictly contain:
            - "title": String (e.g. "Physics Class", "Math Coaching")
            - "dayOfWeek": String (The full name of the day: "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            - "specificDate": null
            - "startTime": String (HH:MM 24h format, e.g., "09:30", "16:00")
            - "endTime": String (HH:MM 24h format, e.g., "11:00", "17:30")
            - "eventType": String (Must be one of: "CLASS", "COACHING", "ACADEMY", "SELF_STUDY", "TEST", "OTHER")
            - "location": String or null
            
            Return ONLY a raw JSON array of events. No markdown block formatting or backticks.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(
                    GeminiPart(text = "Extract schedule events from this image:"),
                    GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                ))
            ),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.1),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = api.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d("GeminiParserService", "Parsed Image Response: $rawText")
            parseEventsJson(rawText)
        } catch (e: Exception) {
            Log.e("GeminiParserService", "Error parsing image: ${e.message}")
            emptyList()
        }
    }

    suspend fun chatWithGemini(apiKey: String, history: List<ChatMessage>, newMessage: String): String {
        val systemPrompt = """
            You are KRONOS, a personal productivity companion and scheduling assistant. Speak directly, keep answers clear, short and concise.
            You help the user (a student in Karachi) manage their studies (academy, coaching, classes, self-study, and alarms).
            You can understand basic Urdu phrases like "Mera maths ka test hai Saturday ko 5 baje".
            If the user asks you to add an event, respond nicely and inform them they can tap the '+' button or type it in plain text.
        """.trimIndent()

        val chatContents = mutableListOf<GeminiContent>()
        // Map history
        history.takeLast(10).forEach { msg ->
            chatContents.add(
                GeminiContent(
                    role = if (msg.sender == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = msg.message))
                )
            )
        }
        chatContents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = newMessage))))

        val request = GeminiRequest(
            contents = chatContents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.7)
        )

        return try {
            val response = api.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from KRONOS."
        } catch (e: Exception) {
            Log.e("GeminiParserService", "Chat Error: ${e.message}")
            "Error communicating with AI: ${e.message}"
        }
    }

    private fun parseEventsJson(jsonStr: String): List<ScheduleEvent> {
        val events = mutableListOf<ScheduleEvent>()
        try {
            // Remove markdown format if any
            var sanitized = jsonStr.trim()
            if (sanitized.startsWith("```")) {
                sanitized = sanitized.substringAfter("\n").substringBeforeLast("```").trim()
            }
            if (sanitized.startsWith("json")) {
                sanitized = sanitized.substring(4).trim()
            }

            val array = JSONArray(sanitized)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.optString("title", "Untitled Event")
                val day = obj.optString("dayOfWeek", "Monday")
                val specificDate = if (obj.isNull("specificDate")) null else obj.optString("specificDate")
                val startTime = obj.optString("startTime", "08:00")
                val endTime = obj.optString("endTime", "09:00")
                val eventType = obj.optString("eventType", "OTHER")
                val location = if (obj.isNull("location")) null else obj.optString("location")

                events.add(
                    ScheduleEvent(
                        title = title,
                        dayOfWeek = day,
                        specificDate = specificDate,
                        startTime = startTime,
                        endTime = endTime,
                        eventType = eventType,
                        location = location
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiParserService", "JSON Parsing Error: ${e.message}")
        }
        return events
    }
}
