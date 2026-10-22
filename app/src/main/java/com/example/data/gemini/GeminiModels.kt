package com.example.data.gemini

object GeminiModels {
    const val FLASH_GENERAL = "gemini-3.8-flash"
    const val FLASH_LITE_FAST = "gemini-3.1-flash-lite-preview"
    const val ARGON_HIGH_THINKING = "gemini-4-argon"
    const val FLASH_TTS = "gemini-3.8-flash-tts"
}

enum class EmergencyActionType {
    CALL_POLICE,
    CALL_FIRE,
    CALL_AMBULANCE,
    BROADCAST_SOS,
    FIND_NEAREST_HELP,
    CHECK_MESH_STATUS,
    GENERAL_QUERY
}

data class ParsedVoiceCommand(
    val actionType: EmergencyActionType,
    val spokenSummary: String,
    val immediatePhoneToDial: String? = null
)
