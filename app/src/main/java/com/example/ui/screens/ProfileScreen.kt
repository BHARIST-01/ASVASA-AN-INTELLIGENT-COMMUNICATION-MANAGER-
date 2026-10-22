package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
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

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var fullName by remember(userProfile) { mutableStateOf(userProfile.fullName) }
    var emergencyContactName by remember(userProfile) { mutableStateOf(userProfile.emergencyContactName) }
    var emergencyContactPhone by remember(userProfile) { mutableStateOf(userProfile.emergencyContactPhone) }
    var bloodType by remember(userProfile) { mutableStateOf(userProfile.bloodType) }
    var medicalNotes by remember(userProfile) { mutableStateOf(userProfile.medicalNotes) }
    var role by remember(userProfile) { mutableStateOf(userProfile.organizationOrRole) }
    var beaconName by remember(userProfile) { mutableStateOf(userProfile.beaconBroadcastName) }
    var shareLocation by remember(userProfile) { mutableStateOf(userProfile.isSharingLocation) }
    var isSavedNotification by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Identity Header
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SignalCyan, SafeGreen))),
            modifier = Modifier.fillMaxWidth().testTag("profile_hero_card")
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.asvasa_lotus_logo_1792674497632),
                    contentDescription = "ASVASA Lotus Emblem",
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, SignalCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "EMERGENCY IDENTITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SignalCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = fullName.ifBlank { "Survivor / Node" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "BEACON ID: $beaconName",
                        fontSize = 11.sp,
                        color = SafeGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Account & Presentation Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(TacticalBorder, SignalCyan))),
            modifier = Modifier.fillMaxWidth().testTag("account_details_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ACCOUNT & RESPONDER CREDENTIALS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextLight,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name / Call-Sign", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = SignalCyan) },
                    modifier = Modifier.fillMaxWidth().testTag("input_fullname"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SignalCyan,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Organization / Role / Presentation", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = SafetyAmber) },
                    modifier = Modifier.fillMaxWidth().testTag("input_role"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafetyAmber,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = beaconName,
                    onValueChange = { beaconName = it },
                    label = { Text("Bluetooth Mesh Node Identifier", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Security, null, tint = SafeGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("input_beacon_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeGreen,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    singleLine = true
                )
            }
        }

        // Medical & Triage Info
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(TacticalBorder, EmergencyRed))),
            modifier = Modifier.fillMaxWidth().testTag("medical_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "TRIAGE & MEDICAL INFORMATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextLight,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = bloodType,
                        onValueChange = { bloodType = it },
                        label = { Text("Blood Type", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Bloodtype, null, tint = EmergencyRed) },
                        modifier = Modifier.weight(1f).testTag("input_blood_type"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmergencyRed,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = { Text("ICE Phone Number", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.ContactPhone, null, tint = SignalCyan) },
                        modifier = Modifier.weight(1.4f).testTag("input_ice_phone"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SignalCyan,
                            unfocusedBorderColor = TacticalBorder,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = emergencyContactName,
                    onValueChange = { emergencyContactName = it },
                    label = { Text("Emergency Contact Name (ICE)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().testTag("input_ice_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SignalCyan,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    label = { Text("Allergies, Medical Conditions & Triage Notes", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.MedicalServices, null, tint = SafetyAmber) },
                    modifier = Modifier.fillMaxWidth().testTag("input_medical_notes"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafetyAmber,
                        unfocusedBorderColor = TacticalBorder,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    maxLines = 3
                )

                // Location broadcast toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Broadcast GPS in SOS packets", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Permits first responders to triangulate coordinates", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = shareLocation,
                        onCheckedChange = { shareLocation = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafeGreen,
                            checkedTrackColor = TacticalSurfaceVariant
                        )
                    )
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                val updated = userProfile.copy(
                    fullName = fullName,
                    emergencyContactName = emergencyContactName,
                    emergencyContactPhone = emergencyContactPhone,
                    bloodType = bloodType,
                    medicalNotes = medicalNotes,
                    organizationOrRole = role,
                    beaconBroadcastName = beaconName,
                    isSharingLocation = shareLocation
                )
                onSaveProfile(updated)
                isSavedNotification = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = SignalCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = TacticalDark)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSavedNotification) "PROFILE SAVED SECURELY!" else "SAVE EMERGENCY PROFILE",
                color = TacticalDark,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
