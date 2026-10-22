package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.BleConstants
import com.example.data.model.BluetoothPeer
import com.example.data.model.ChatMessage
import com.example.data.model.MlCommProbability
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BluetoothCommsScreen(
    peers: List<BluetoothPeer>,
    isScanning: Boolean,
    isAdvertising: Boolean,
    selectedPeer: BluetoothPeer?,
    messages: List<ChatMessage>,
    mlProbability: MlCommProbability?,
    onScan: () -> Unit,
    onToggleAdvertising: (Boolean) -> Unit,
    onSelectPeer: (BluetoothPeer) -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onSendSosBroadcast: () -> Unit,
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Radar Discovery Banner
        BluetoothRadarBanner(
            peerCount = peers.size,
            isScanning = isScanning,
            onScan = onScan
        )

        // 2. BLE Beacon Advertising Control Card
        BleAdvertisingBanner(
            isAdvertising = isAdvertising,
            onToggleAdvertising = onToggleAdvertising
        )

        // 3. Discovered Nodes Carousel
        Text(
            text = "DISCOVERED PEER NODES (${peers.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(peers) { peer ->
                PeerNodeCard(
                    peer = peer,
                    isSelected = peer.address == selectedPeer?.address,
                    onSelect = { onSelectPeer(peer) }
                )
            }
        }

        // 4. Active Chat with Selected Node
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SignalCyan, TacticalBorder))),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("mesh_chat_container")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Chat header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedPeer?.name ?: "Broadcast Channel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${selectedPeer?.estimatedDistanceMeters ?: 0.0}m • ${selectedPeer?.rssi ?: -60} dBm",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    // ML Probability Pill
                    val prob = mlProbability?.overallChancePercent ?: 88
                    val probColor = if (prob >= 75) SafeGreen else if (prob >= 50) SafetyAmber else EmergencyRed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(probColor.copy(alpha = 0.2f))
                            .border(1.dp, probColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$prob% ML CHANCE",
                            color = probColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Messages Stream
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.reversed()) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }

                // Quick emergency broadcast triggers
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    QuickChatPill("I am safe") { onSendMessage("Status: I am safe and secured.", false) }
                    QuickChatPill("Need Medical") { onSendMessage("SOS: Medical assistance needed at my location!", true) }
                    QuickChatPill("Need Water") { onSendMessage("Status: Potable water and food supplies required.", false) }
                }

                // Text Input & Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Offline mesh text...", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SignalCyan,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                onSendMessage(messageInput, false)
                                messageInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SignalCyan)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Mesh Message",
                            tint = TacticalDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onSendSosBroadcast,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EmergencyRed)
                            .testTag("sos_broadcast_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Broadcast SOS",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BleAdvertisingBanner(
    isAdvertising: Boolean,
    onToggleAdvertising: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurfaceVariant),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(if (isAdvertising) SafeGreen else TacticalBorder, TacticalBorder)
            )
        ),
        modifier = Modifier.fillMaxWidth().testTag("ble_advertising_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = "BLE Advertising",
                    tint = if (isAdvertising) SafeGreen else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAdvertising) "BLE ADVERTISER: ACTIVE" else "BLE ADVERTISER: PAUSED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdvertising) SafeGreen else TextMuted
                        )
                    }
                    Text(
                        text = "UUID: ${BleConstants.ASVASA_SERVICE_UUID.toString().take(18)}...",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Switch(
                checked = isAdvertising,
                onCheckedChange = onToggleAdvertising,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SafeGreen,
                    checkedTrackColor = TacticalSurface
                ),
                modifier = Modifier.testTag("toggle_ble_advertising")
            )
        }
    }
}

@Composable
private fun BluetoothRadarBanner(
    peerCount: Int,
    isScanning: Boolean,
    onScan: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotate"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(1.dp, SignalCyan.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Radar",
                        tint = if (isScanning) SafetyAmber else SignalCyan,
                        modifier = Modifier
                            .size(22.dp)
                            .then(if (isScanning) Modifier.rotate(rotation) else Modifier)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isScanning) "BLE SCANNING MESH..." else "BLE MESH DISCOVERY ACTIVE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$peerCount nodes connected in range",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Button(
                onClick = onScan,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = SignalCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("bt_scan_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = TacticalDark, modifier = Modifier.size(16.dp))
                } else {
                    Text("SCAN BLE", color = TacticalDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PeerNodeCard(
    peer: BluetoothPeer,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val prob = peer.commProbabilityPercent
    val probColor = if (prob >= 75) SafeGreen else if (prob >= 50) SafetyAmber else EmergencyRed

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TacticalSurfaceVariant else TacticalSurface
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(if (isSelected) SignalCyan else TacticalBorder, probColor)
            )
        ),
        modifier = Modifier
            .width(190.dp)
            .clickable(onClick = onSelect)
            .testTag("peer_card_${peer.address.takeLast(4)}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = peer.name.take(16),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = SignalCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = "MAC: ${peer.address.takeLast(8)}",
                fontSize = 10.sp,
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${peer.rssi} dBm",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SignalCyan
                )
                Text(
                    text = "~${peer.estimatedDistanceMeters}m",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextLight
                )
            }

            // ML Comm Probability Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(probColor.copy(alpha = 0.15f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$prob% COMM CHANCE",
                    color = probColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isMe = message.isFromMe
    val isAlert = message.isEmergencyAlert
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isMe) 14.dp else 2.dp,
                        bottomEnd = if (isMe) 2.dp else 14.dp
                    )
                )
                .background(
                    when {
                        isAlert -> EmergencyRed
                        isMe -> SignalCyan
                        else -> TacticalSurfaceVariant
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafetyAmber
                    )
                }
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = if (isAlert || !isMe) Color.White else TacticalDark,
                    fontWeight = if (isAlert) FontWeight.Bold else FontWeight.Normal
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = if (isAlert || !isMe) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Delivered",
                            tint = if (isAlert) Color.White else TacticalDark,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickChatPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = SignalCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
