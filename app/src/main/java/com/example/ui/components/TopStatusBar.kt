package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MlCommProbability
import com.example.location.UserGpsLocation
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalSurface

@Composable
fun TopStatusBar(
    gpsLocation: UserGpsLocation,
    isBtScanning: Boolean,
    mlProbability: MlCommProbability?,
    modifier: Modifier = Modifier
) {
    val probPercent = mlProbability?.overallChancePercent ?: 88
    val targetProbColor = when {
        probPercent >= 75 -> SafeGreen
        probPercent >= 50 -> SafetyAmber
        else -> EmergencyRed
    }
    val probColor by animateColorAsState(
        targetValue = targetProbColor,
        label = "probColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateColor(
        initialValue = SafeGreen.copy(alpha = 0.3f),
        targetValue = SafeGreen,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TacticalSurface)
            .border(1.dp, TacticalBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("top_status_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand & Pulse
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(pulseAlpha)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.asvasa_lotus_logo_1792674497632),
                            contentDescription = "ASVASA Lotus Emblem",
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .border(1.dp, SignalCyan.copy(alpha = 0.8f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ASVASA",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        text = "EMERGENCY MESH",
                        color = SignalCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Status Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // GPS Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS Active",
                        tint = SafeGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "GPS LOCK",
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Bluetooth Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "BT Mesh",
                        tint = if (isBtScanning) SafetyAmber else SignalCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isBtScanning) "SCANNING" else "MESH ON",
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ML Probability Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(probColor.copy(alpha = 0.18f))
                        .border(1.dp, probColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$probPercent% ML COMM",
                        color = probColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
