package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MlCommProbability
import com.example.ui.theme.EmergencyGlow
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDark
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    isListening: Boolean,
    voiceTranscript: String,
    voiceActionBanner: String?,
    mlProbability: MlCommProbability?,
    triageResponse: String?,
    isTriageLoading: Boolean,
    onStartVoiceListening: () -> Unit,
    onStopVoiceListening: () -> Unit,
    onSimulateVoiceCommand: (String) -> Unit,
    onCallEmergency: (String) -> Unit,
    onSendSos: () -> Unit,
    onRequestTriage: (String, Boolean) -> Unit,
    onClearTriage: () -> Unit,
    onSpeakAudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var customQuery by remember { mutableStateOf("") }
    var useHighThinking by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Voice Emergency Dispatcher Card
        VoiceEmergencyHeroCard(
            isListening = isListening,
            voiceTranscript = voiceTranscript,
            voiceActionBanner = voiceActionBanner,
            onStartListening = onStartVoiceListening,
            onStopListening = onStopVoiceListening,
            onQuickCommand = onSimulateVoiceCommand
        )

        // 2. High-Priority Emergency Call Grid
        EmergencyDialerSection(
            onCallEmergency = onCallEmergency,
            onSendSos = onSendSos
        )

        // 3. ML Communication Probability Engine
        MlProbabilityDashboardCard(mlProbability = mlProbability)

        // 4. Gemini AI Emergency Triage (Flash 3.8 / Argon High-Thinking)
        GeminiTriageCard(
            customQuery = customQuery,
            onQueryChange = { customQuery = it },
            useHighThinking = useHighThinking,
            onToggleHighThinking = { useHighThinking = it },
            triageResponse = triageResponse,
            isTriageLoading = isTriageLoading,
            onSubmitQuery = {
                if (customQuery.isNotBlank()) {
                    onRequestTriage(customQuery, useHighThinking)
                    customQuery = ""
                }
            },
            onQuickPrompt = { prompt ->
                onRequestTriage(prompt, useHighThinking)
            },
            onClearTriage = onClearTriage,
            onSpeakAudio = onSpeakAudio
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceEmergencyHeroCard(
    isListening: Boolean,
    voiceTranscript: String,
    voiceActionBanner: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onQuickCommand: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(SignalCyan, SafetyAmber))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_emergency_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VOICE EMERGENCY CONTROL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SignalCyan,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = if (isListening) "LISTENING TO VOICE..." else "HANDS-FREE DISPATCH",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) EmergencyRed else TextLight
                    )
                }

                // Interactive Mic Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .scale(micScale)
                        .clip(CircleShape)
                        .background(if (isListening) EmergencyRed else SignalCyan)
                        .clickable {
                            if (isListening) onStopListening() else onStartListening()
                        }
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Voice Dispatch Microphone",
                        tint = TacticalDark,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Transcript or prompt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp)
            ) {
                Text(
                    text = when {
                        isListening -> "Listening... Say 'Call Police', 'Call Firestation', or 'SOS'"
                        voiceTranscript.isNotBlank() -> "Heard: \"$voiceTranscript\""
                        else -> "Tap microphone or say 'Call Police' or 'SOS' for immediate dispatch."
                    },
                    color = if (isListening) SignalCyan else TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Feedback Banner
            AnimatedVisibility(visible = voiceActionBanner != null) {
                voiceActionBanner?.let { banner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SafetyAmber.copy(alpha = 0.15f))
                            .border(1.dp, SafetyAmber, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Voice Action",
                                tint = SafetyAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = banner,
                                color = SafetyAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Voice Command Fast Triggers (Simulate speech input)
            Text(
                text = "VOICE COMMAND PRESETS:",
                fontSize = 10.sp,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceCommandChip("Call Police", "voice_cmd_police") { onQuickCommand("Call police") }
                VoiceCommandChip("Call Firestation", "voice_cmd_fire") { onQuickCommand("Call firestation") }
                VoiceCommandChip("Call Ambulance", "voice_cmd_ambulance") { onQuickCommand("Call ambulance") }
                VoiceCommandChip("Broadcast SOS", "voice_cmd_sos") { onQuickCommand("SOS emergency help") }
            }
        }
    }
}

@Composable
private fun VoiceCommandChip(label: String, testTag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TacticalSurfaceVariant)
            .border(1.dp, TacticalBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = SignalCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = TextLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmergencyDialerSection(
    onCallEmergency: (String) -> Unit,
    onSendSos: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "ONE-TOUCH EMERGENCY DISPATCH",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EmergencyDialCard(
                title = "POLICE",
                number = "100 / 911",
                icon = Icons.Default.LocalPolice,
                accentColor = SignalCyan,
                modifier = Modifier.weight(1f),
                testTag = "call_police_button",
                onClick = { onCallEmergency("100") }
            )
            EmergencyDialCard(
                title = "FIRE & RESCUE",
                number = "101 / 911",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = SafetyAmber,
                modifier = Modifier.weight(1f),
                testTag = "call_fire_button",
                onClick = { onCallEmergency("101") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EmergencyDialCard(
                title = "AMBULANCE",
                number = "102 / 911",
                icon = Icons.Default.LocalHospital,
                accentColor = SafeGreen,
                modifier = Modifier.weight(1f),
                testTag = "call_ambulance_button",
                onClick = { onCallEmergency("102") }
            )
            EmergencyDialCard(
                title = "DISASTER SOS",
                number = "MESH BROADCAST",
                icon = Icons.Default.Warning,
                accentColor = EmergencyRed,
                modifier = Modifier.weight(1f),
                testTag = "call_disaster_button",
                onClick = onSendSos
            )
        }
    }
}

@Composable
private fun EmergencyDialCard(
    title: String,
    number: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(accentColor, TacticalBorder))),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Dial",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
        }
    }
}

@Composable
private fun MlProbabilityDashboardCard(mlProbability: MlCommProbability?) {
    val prob = mlProbability?.overallChancePercent ?: 85
    val probColor = when {
        prob >= 75 -> SafeGreen
        prob >= 50 -> SafetyAmber
        else -> EmergencyRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(TacticalBorder, probColor))),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ml_probability_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "ML Radio Telemetry",
                        tint = SignalCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ML COMMUNICATION CHANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextLight,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "$prob%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = probColor
                )
            }

            // Probability Progress Bar
            LinearProgressIndicator(
                progress = { (prob / 100f).coerceIn(0f, 1f) },
                color = probColor,
                trackColor = Color(0xFF0F172A),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Diagnostic Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryStat("Distance", "${mlProbability?.distanceMeters ?: 4.2}m")
                TelemetryStat("RF RSSI", "${mlProbability?.rssiFactorPercent ?: 78}%")
                TelemetryStat("Bitrate", "${mlProbability?.estimatedBitrateKbps ?: 48.0} kbps")
                TelemetryStat("Loss Risk", "${mlProbability?.packetLossRiskPercent ?: 15}%")
            }

            // Advisory text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(10.dp)
            ) {
                Text(
                    text = "ML ADVICE: ${mlProbability?.adviceNotice ?: "Signal solid. Emergency mesh packet delivery rate optimal."}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TelemetryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Text(text = value, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiTriageCard(
    customQuery: String,
    onQueryChange: (String) -> Unit,
    useHighThinking: Boolean,
    onToggleHighThinking: (Boolean) -> Unit,
    triageResponse: String?,
    isTriageLoading: Boolean,
    onSubmitQuery: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onClearTriage: () -> Unit,
    onSpeakAudio: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(TacticalBorder, SignalCyan))),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gemini_triage_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI",
                        tint = SignalCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EMERGENCY TRIAGE AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextLight,
                        letterSpacing = 1.sp
                    )
                }

                // Thinking Mode Toggle (Gemini 4 Argon with High Thinking)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "High Thinking Mode",
                        tint = if (useHighThinking) SafetyAmber else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (useHighThinking) "HIGH THINK" else "FAST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (useHighThinking) SafetyAmber else TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = useHighThinking,
                        onCheckedChange = onToggleHighThinking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafetyAmber,
                            checkedTrackColor = TacticalSurfaceVariant
                        ),
                        modifier = Modifier.scale(0.7f).testTag("thinking_mode_switch")
                    )
                }
            }

            // Quick First Aid Presets
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TriageQuickChip("Severe Bleeding") { onQuickPrompt("How to stop severe arterial bleeding immediately?") }
                TriageQuickChip("Burn Treatment") { onQuickPrompt("First aid steps for second-degree thermal burns?") }
                TriageQuickChip("Earthquake Safety") { onQuickPrompt("Immediate actions during violent earthquake shaking?") }
            }

            // Input field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Ask triage AI (e.g. CPR steps)...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("triage_query_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SignalCyan,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    singleLine = true
                )

                Button(
                    onClick = onSubmitQuery,
                    enabled = !isTriageLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = SignalCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("ask_triage_button")
                ) {
                    if (isTriageLoading) {
                        CircularProgressIndicator(color = TacticalDark, modifier = Modifier.size(18.dp))
                    } else {
                        Text("ASK", color = TacticalDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Response Box with TTS playback
            AnimatedVisibility(visible = triageResponse != null) {
                triageResponse?.let { resp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, SignalCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = SignalCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (useHighThinking) "GEMINI 4 ARGON (HIGH THINKING)" else "GEMINI 3.8 FLASH",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SignalCyan
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { onSpeakAudio(resp) },
                                        modifier = Modifier.size(28.dp).testTag("tts_speak_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Read Aloud with TTS",
                                            tint = SafeGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onClearTriage,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = resp,
                                color = TextLight,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriageQuickChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TacticalSurfaceVariant)
            .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = text, color = SignalCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
