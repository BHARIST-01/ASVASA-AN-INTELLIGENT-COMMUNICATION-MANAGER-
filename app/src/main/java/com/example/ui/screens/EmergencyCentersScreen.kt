package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmergencyCenter
import com.example.data.model.EmergencyCenterType
import com.example.location.UserGpsLocation
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class CenterDisplayMode {
    LIST,
    MAP
}

@Composable
fun EmergencyCentersScreen(
    currentGps: UserGpsLocation,
    emergencyCenters: List<EmergencyCenter>,
    onRefreshGps: () -> Unit,
    onCallCenter: (String) -> Unit,
    onNavigateCenter: (EmergencyCenter) -> Unit,
    onSendSosSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(CenterDisplayMode.LIST) }
    var selectedFilter by remember { mutableStateOf<EmergencyCenterType?>(null) }
    var selectedCenterForHud by remember { mutableStateOf<EmergencyCenter?>(null) }

    val filteredList = remember(emergencyCenters, selectedFilter) {
        if (selectedFilter == null) emergencyCenters
        else emergencyCenters.filter { it.type == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Current GPS Coordinate Telemetry Card
        GpsTelemetryCard(
            gps = currentGps,
            onRefresh = onRefreshGps,
            onSendSosSms = onSendSosSms
        )

        // 2. View Mode Switcher (List vs Map)
        TabRow(
            selectedTabIndex = if (displayMode == CenterDisplayMode.LIST) 0 else 1,
            containerColor = TacticalSurface,
            contentColor = SignalCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (displayMode == CenterDisplayMode.LIST) 0 else 1]),
                    color = SignalCyan
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = displayMode == CenterDisplayMode.LIST,
                onClick = { displayMode = CenterDisplayMode.LIST },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LIST VIEW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("tab_list_view")
            )
            Tab(
                selected = displayMode == CenterDisplayMode.MAP,
                onClick = { displayMode = CenterDisplayMode.MAP },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RADAR MAP VIEW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.testTag("tab_map_view")
            )
        }

        // 3. Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SignalCyan,
                    selectedLabelColor = TacticalDark
                ),
                modifier = Modifier.testTag("filter_all")
            )
            FilterChip(
                selected = selectedFilter == EmergencyCenterType.POLICE,
                onClick = { selectedFilter = EmergencyCenterType.POLICE },
                label = { Text("POLICE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SignalCyan,
                    selectedLabelColor = TacticalDark
                ),
                modifier = Modifier.testTag("filter_police")
            )
            FilterChip(
                selected = selectedFilter == EmergencyCenterType.FIRE,
                onClick = { selectedFilter = EmergencyCenterType.FIRE },
                label = { Text("FIRE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SafetyAmber,
                    selectedLabelColor = TacticalDark
                ),
                modifier = Modifier.testTag("filter_fire")
            )
            FilterChip(
                selected = selectedFilter == EmergencyCenterType.HOSPITAL,
                onClick = { selectedFilter = EmergencyCenterType.HOSPITAL },
                label = { Text("HOSPITAL", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SafeGreen,
                    selectedLabelColor = TacticalDark
                ),
                modifier = Modifier.testTag("filter_hospital")
            )
        }

        // 4. Content Area: List View OR Tactical Radar Map View
        if (displayMode == CenterDisplayMode.LIST) {
            Text(
                text = "NEAREST HELP CENTERS (${filteredList.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { center ->
                    EmergencyCenterCard(
                        center = center,
                        onCall = { onCallCenter(center.phoneNumber) },
                        onNavigate = { onNavigateCenter(center) }
                    )
                }
            }
        } else {
            // Interactive Radar Map View
            TacticalRadarMapView(
                userGps = currentGps,
                centers = filteredList,
                selectedCenter = selectedCenterForHud,
                onSelectCenter = { selectedCenterForHud = it },
                onCallCenter = onCallCenter,
                onNavigateCenter = onNavigateCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun TacticalRadarMapView(
    userGps: UserGpsLocation,
    centers: List<EmergencyCenter>,
    selectedCenter: EmergencyCenter?,
    onSelectCenter: (EmergencyCenter?) -> Unit,
    onCallCenter: (String) -> Unit,
    onNavigateCenter: (EmergencyCenter) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRadius")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SignalCyan, TacticalBorder))),
        modifier = modifier.testTag("tactical_map_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(centers, userGps) {
                        detectTapGestures { tapOffset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val maxRadius = minOf(centerX, centerY) * 0.85f

                            // Max distance range represented on canvas is 5.0 km
                            val maxKm = 5.0

                            var tappedCenter: EmergencyCenter? = null
                            for (center in centers) {
                                val dLat = center.latitude - userGps.latitude
                                val dLon = center.longitude - userGps.longitude
                                val distKm = center.distanceKm

                                val scale = (distKm / maxKm).coerceIn(0.1, 1.0) * maxRadius
                                val angle = kotlin.math.atan2(dLat, dLon)

                                val pinX = centerX + (scale * cos(angle)).toFloat()
                                val pinY = centerY - (scale * sin(angle)).toFloat()

                                val dx = tapOffset.x - pinX
                                val dy = tapOffset.y - pinY
                                if (sqrt(dx * dx + dy * dy) < 45f) {
                                    tappedCenter = center
                                    break
                                }
                            }
                            onSelectCenter(tappedCenter)
                        }
                    }
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val maxRadius = minOf(centerX, centerY) * 0.85f

                // Draw radar concentric rings (1 km, 2.5 km, 5 km)
                val ringStrokes = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // 1 km ring
                drawCircle(
                    color = TacticalBorder.copy(alpha = 0.6f),
                    radius = maxRadius * 0.25f,
                    center = Offset(centerX, centerY),
                    style = ringStrokes
                )

                // 2.5 km ring
                drawCircle(
                    color = TacticalBorder.copy(alpha = 0.6f),
                    radius = maxRadius * 0.55f,
                    center = Offset(centerX, centerY),
                    style = ringStrokes
                )

                // 5.0 km outer ring
                drawCircle(
                    color = SignalCyan.copy(alpha = 0.3f),
                    radius = maxRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Crosshairs
                drawLine(
                    color = TacticalBorder.copy(alpha = 0.4f),
                    start = Offset(centerX, centerY - maxRadius),
                    end = Offset(centerX, centerY + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = TacticalBorder.copy(alpha = 0.4f),
                    start = Offset(centerX - maxRadius, centerY),
                    end = Offset(centerX + maxRadius, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                // Center User Location (pulsing green dot)
                drawCircle(
                    color = SafeGreen.copy(alpha = pulseAlpha),
                    radius = 16.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = SafeGreen,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // Draw Safety Center Markers
                val maxKm = 5.0
                centers.forEach { center ->
                    val dLat = center.latitude - userGps.latitude
                    val dLon = center.longitude - userGps.longitude
                    val distKm = center.distanceKm

                    val scale = (distKm / maxKm).coerceIn(0.1, 1.0) * maxRadius
                    val angle = kotlin.math.atan2(dLat, dLon)

                    val pinX = centerX + (scale * cos(angle)).toFloat()
                    val pinY = centerY - (scale * sin(angle)).toFloat()

                    val isSelected = center.id == selectedCenter?.id

                    val pinColor = when (center.type) {
                        EmergencyCenterType.POLICE -> SignalCyan
                        EmergencyCenterType.FIRE -> SafetyAmber
                        EmergencyCenterType.HOSPITAL -> SafeGreen
                        EmergencyCenterType.DISASTER_RELIEF -> Color(0xFFE040FB)
                    }

                    // Draw pin halo if selected
                    if (isSelected) {
                        drawCircle(
                            color = pinColor.copy(alpha = 0.35f),
                            radius = 20.dp.toPx(),
                            center = Offset(pinX, pinY)
                        )
                    }

                    // Main pin circle
                    drawCircle(
                        color = pinColor,
                        radius = if (isSelected) 10.dp.toPx() else 7.dp.toPx(),
                        center = Offset(pinX, pinY)
                    )
                    drawCircle(
                        color = TacticalDark,
                        radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = Offset(pinX, pinY)
                    )
                }
            }

            // Radar Legend Overlay (Top Left)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text("RADAR RANGE: 5.0 KM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SignalCyan)
                Text("• Green: You (GPS Fix)", fontSize = 9.sp, color = SafeGreen)
                Text("• Cyan: Police • Amber: Fire", fontSize = 9.sp, color = TextMuted)
                Text("• Green Pin: Hospital • Violet: Shelter", fontSize = 9.sp, color = TextMuted)
            }

            // Interactive HUD Pop-up Card when a station pin is tapped
            if (selectedCenter != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    val center = selectedCenter
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalSurfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SignalCyan, TacticalBorder))),
                        modifier = Modifier.fillMaxWidth().testTag("map_station_hud")
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
                                Column {
                                    Text(
                                        text = center.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${center.type.name} • ${center.distanceKm} km away (ETA ~${center.estimatedResponseTimeMinutes}m)",
                                        fontSize = 11.sp,
                                        color = SignalCyan
                                    )
                                }
                                IconButton(
                                    onClick = { onSelectCenter(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", color = TextMuted, fontSize = 14.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onCallCenter(center.phoneNumber) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SignalCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("hud_call_button")
                                ) {
                                    Icon(Icons.Default.Phone, null, tint = TacticalDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CALL ${center.phoneNumber}", color = TacticalDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onNavigateCenter(center) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("hud_nav_button")
                                ) {
                                    Icon(Icons.Default.Directions, null, tint = SignalCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ROUTE MAP", color = SignalCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GpsTelemetryCard(
    gps: UserGpsLocation,
    onRefresh: () -> Unit,
    onSendSosSms: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(SignalCyan, SafeGreen))
        ),
        modifier = Modifier.fillMaxWidth().testTag("gps_telemetry_card")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS Lock",
                        tint = SafeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FUSED LOCATION GPS LOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextLight,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp).testTag("refresh_gps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS",
                        tint = SignalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "LATITUDE", fontSize = 10.sp, color = TextMuted)
                    Text(text = String.format("%.5f° N", gps.latitude), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text(text = "LONGITUDE", fontSize = 10.sp, color = TextMuted)
                    Text(text = String.format("%.5f° W", gps.longitude), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text(text = "ACCURACY", fontSize = 10.sp, color = TextMuted)
                    Text(text = "±${gps.accuracyMeters}m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                }
            }

            // SMS SOS button
            Button(
                onClick = onSendSosSms,
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("send_sms_sos_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SMS DISTRESS COORD BROADCAST",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmergencyCenterCard(
    center: EmergencyCenter,
    onCall: () -> Unit,
    onNavigate: () -> Unit
) {
    val (typeColor, icon) = when (center.type) {
        EmergencyCenterType.POLICE -> Pair(SignalCyan, Icons.Default.LocalPolice)
        EmergencyCenterType.FIRE -> Pair(SafetyAmber, Icons.Default.LocalFireDepartment)
        EmergencyCenterType.HOSPITAL -> Pair(SafeGreen, Icons.Default.LocalHospital)
        EmergencyCenterType.DISASTER_RELIEF -> Pair(Color(0xFFE040FB), Icons.Default.Shield)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(typeColor.copy(alpha = 0.6f), TacticalBorder))
        ),
        modifier = Modifier.fillMaxWidth().testTag("center_card_${center.id}")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeColor.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = center.name,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = center.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = center.type.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = typeColor
                        )
                    }
                }

                // Distance Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${center.distanceKm} km",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "ETA ~${center.estimatedResponseTimeMinutes}m",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = center.address,
                fontSize = 11.sp,
                color = TextMuted
            )

            Text(
                text = "CAPABILITY: ${center.specialCapabilities}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = typeColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("call_${center.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = TacticalDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CALL ${center.phoneNumber}",
                        color = TacticalDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("nav_${center.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Navigate",
                        tint = TextLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MAP ROUTE",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
