package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Executes prompt on Gemini 3.8 Flash (fast/general) or Gemini 4 Argon with thinkingLevel: HIGH
     */
    suspend fun generateEmergencyResponse(
        userPrompt: String,
        useHighThinking: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineDisasterFallback(userPrompt)
        }

        val modelName = if (useHighThinking) {
            GeminiModels.ARGON_HIGH_THINKING
        } else {
            GeminiModels.FLASH_GENERAL
        }

        try {
            val rootJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", userPrompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val configObj = JSONObject().apply {
                    put("temperature", if (useHighThinking) 0.4 else 0.2)
                    if (useHighThinking) {
                        val thinkingObj = JSONObject().apply {
                            put("thinkingLevel", "high")
                        }
                        put("thinkingConfig", thinkingObj)
                    }
                }
                put("generationConfig", configObj)

                val systemInstruction = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", "You are ASVASA, a specialized offline and field emergency disaster response AI. Provide concise, life-saving emergency advice, first-aid steps, triage recommendations, and tactical survival instructions. Keep answers actionable, authoritative, and direct."))
                    }
                    put("parts", parts)
                }
                put("systemInstruction", systemInstruction)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.e("GeminiRepository", "API Error: ${response.code} -> $responseBody")
                return@withContext getOfflineDisasterFallback(userPrompt)
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCand = candidates.getJSONObject(0)
                val content = firstCand.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    if (text.isNotBlank()) return@withContext text
                }
            }

            return@withContext getOfflineDisasterFallback(userPrompt)
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Failed calling Gemini API", e)
            return@withContext getOfflineDisasterFallback(userPrompt)
        }
    }

    /**
     * Local zero-latency regex & semantic parser for voice commands.
     * Works 100% offline without requiring internet.
     */
    fun parseVoiceCommand(text: String): ParsedVoiceCommand {
        val lower = text.lowercase().trim()
        return when {
            lower.contains("police") || lower.contains("cop") -> {
                ParsedVoiceCommand(
                    EmergencyActionType.CALL_POLICE,
                    "Dispatching call to nearest Police Station (100 / 911).",
                    "100"
                )
            }
            lower.contains("fire") || lower.contains("brigade") || lower.contains("smoke") -> {
                ParsedVoiceCommand(
                    EmergencyActionType.CALL_FIRE,
                    "Calling Emergency Fire & Rescue Services (101 / 911).",
                    "101"
                )
            }
            lower.contains("ambulance") || lower.contains("hospital") || lower.contains("doctor") || lower.contains("medic") -> {
                ParsedVoiceCommand(
                    EmergencyActionType.CALL_AMBULANCE,
                    "Connecting to Emergency Medical Ambulance (102 / 911).",
                    "102"
                )
            }
            lower.contains("sos") || lower.contains("emergency") || lower.contains("help me") || lower.contains("danger") -> {
                ParsedVoiceCommand(
                    EmergencyActionType.BROADCAST_SOS,
                    "Triggering Emergency SOS Distress Broadcast across Bluetooth mesh.",
                    null
                )
            }
            lower.contains("center") || lower.contains("station") || lower.contains("shelter") || lower.contains("nearest") -> {
                ParsedVoiceCommand(
                    EmergencyActionType.FIND_NEAREST_HELP,
                    "Displaying nearest emergency centers, police stations, and shelters.",
                    null
                )
            }
            lower.contains("mesh") || lower.contains("bluetooth") || lower.contains("signal") || lower.contains("probability") -> {
                ParsedVoiceCommand(
                    EmergencyActionType.CHECK_MESH_STATUS,
                    "Checking Bluetooth mesh nodes and communication probability.",
                    null
                )
            }
            else -> {
                ParsedVoiceCommand(
                    EmergencyActionType.GENERAL_QUERY,
                    "Processing query with ASVASA emergency AI.",
                    null
                )
            }
        }
    }

    private fun getOfflineDisasterFallback(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("burn") ->
                "FIRST-AID FOR BURNS:\n1. Cool burn immediately with cool running water for 10-20 min. Do not use ice.\n2. Do NOT pop blisters.\n3. Cover loosely with sterile plastic wrap or clean cloth.\n4. Call emergency services if severe."
            lower.contains("bleed") || lower.contains("blood") ->
                "FIRST-AID FOR SEVERE BLEEDING:\n1. Apply firm, direct pressure with a clean cloth or gauze.\n2. Keep wound elevated above heart level if possible.\n3. If blood soaks through, do not remove cloth; add more layers on top.\n4. Seek immediate emergency assistance."
            lower.contains("quake") || lower.contains("earthquake") ->
                "EARTHQUAKE SAFETY:\n1. DROP, COVER, and HOLD ON.\n2. Protect head and neck under sturdy desk or table.\n3. Stay away from glass, windows, and exterior walls.\n4. If outside, stay in open areas away from power lines."
            lower.contains("flood") ->
                "FLOOD PROTOCOL:\n1. Seek immediate higher ground.\n2. Never walk or drive through moving flood waters (turn around, don't drown).\n3. Keep your battery conserved and monitor emergency mesh relays."
            else ->
                "EMERGENCY PROTOCOL ACTIVE:\n• Mesh communication link active.\n• Keep emergency coordinates ready.\n• Tap quick action buttons below for immediate dispatch to Police, Fire, or Medical."
        }
    }
}
