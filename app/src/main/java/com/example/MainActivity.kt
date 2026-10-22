package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.TopStatusBar
import com.example.ui.screens.BluetoothCommsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmergencyCentersScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalDark
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.AsvasaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AsvasaMainApp()
            }
        }
    }
}

@Composable
fun AsvasaMainApp(viewModel: AsvasaViewModel = viewModel()) {
    val context = LocalContext.current

    // Request essential runtime permissions (Audio for Voice control, Location for Help Centers, Bluetooth for Mesh)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onBluetoothPermissionsGranted()
        viewModel.refreshGps()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            viewModel.onBluetoothPermissionsGranted()
            viewModel.refreshGps()
        }
    }

    val currentTab by viewModel.currentTab.collectAsState()
    val isListening by viewModel.isVoiceListening.collectAsState()
    val voiceTranscript by viewModel.voiceTranscript.collectAsState()
    val voiceActionBanner by viewModel.voiceActionBanner.collectAsState()
    val mlProbability by viewModel.mlProbability.collectAsState()
    val triageResponse by viewModel.triageResponse.collectAsState()
    val isTriageLoading by viewModel.isTriageLoading.collectAsState()

    val peers by viewModel.bluetoothPeers.collectAsState()
    val isScanningBt by viewModel.isScanningBt.collectAsState()
    val isAdvertisingBt by viewModel.isAdvertisingBt.collectAsState()
    val selectedPeer by viewModel.selectedPeer.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()

    val currentGps by viewModel.currentGps.collectAsState()
    val emergencyCenters by viewModel.emergencyCenters.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = TacticalDark,
        bottomBar = {
            NavigationBar(
                containerColor = TacticalSurface,
                modifier = Modifier
                    .background(TacticalSurface)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Emergency, contentDescription = "Dashboard") },
                    label = { Text("Command", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalDark,
                        selectedTextColor = SignalCyan,
                        indicatorColor = SignalCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = "Mesh Comms") },
                    label = { Text("BT Comms", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalDark,
                        selectedTextColor = SignalCyan,
                        indicatorColor = SignalCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_mesh")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.LocalHospital, contentDescription = "Help Centers") },
                    label = { Text("Help Centers", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalDark,
                        selectedTextColor = SignalCyan,
                        indicatorColor = SignalCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_centers")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TacticalDark,
                        selectedTextColor = SignalCyan,
                        indicatorColor = SignalCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TacticalDark)
        ) {
            // Persistent Tactical Status Bar
            TopStatusBar(
                gpsLocation = currentGps,
                isBtScanning = isScanningBt,
                mlProbability = mlProbability,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Animated Screen Content Switcher
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize(),
                label = "screenTransition"
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        isListening = isListening,
                        voiceTranscript = voiceTranscript,
                        voiceActionBanner = voiceActionBanner,
                        mlProbability = mlProbability,
                        triageResponse = triageResponse,
                        isTriageLoading = isTriageLoading,
                        onStartVoiceListening = { viewModel.startVoiceListening() },
                        onStopVoiceListening = { viewModel.stopVoiceListening() },
                        onSimulateVoiceCommand = { viewModel.handleSpokenVoiceInput(it) },
                        onCallEmergency = { viewModel.executeEmergencyCall(it) },
                        onSendSos = { viewModel.sendSosBroadcast() },
                        onRequestTriage = { q, high -> viewModel.requestGeminiTriage(q, high) },
                        onClearTriage = { viewModel.clearTriageResponse() },
                        onSpeakAudio = { viewModel.voiceController.speak(it) }
                    )
                    1 -> BluetoothCommsScreen(
                        peers = peers,
                        isScanning = isScanningBt,
                        isAdvertising = isAdvertisingBt,
                        selectedPeer = selectedPeer,
                        messages = messages,
                        mlProbability = mlProbability,
                        onScan = { viewModel.scanBluetooth() },
                        onToggleAdvertising = { viewModel.toggleAdvertising(it) },
                        onSelectPeer = { viewModel.selectPeer(it) },
                        onSendMessage = { text, isAlert -> viewModel.sendChatMessage(text, isAlert) },
                        onSendSosBroadcast = { viewModel.sendSosBroadcast() }
                    )
                    2 -> EmergencyCentersScreen(
                        currentGps = currentGps,
                        emergencyCenters = emergencyCenters,
                        onRefreshGps = { viewModel.refreshGps() },
                        onCallCenter = { viewModel.executeEmergencyCall(it) },
                        onNavigateCenter = { viewModel.locatorService.openMapsNavigation(it) },
                        onSendSosSms = {
                            val intent = viewModel.locatorService.composeSosSms(
                                emergencyContacts = userProfile.emergencyContactPhone,
                                bloodType = userProfile.bloodType,
                                coordinates = currentGps
                            )
                            context.startActivity(intent)
                        }
                    )
                    3 -> ProfileScreen(
                        userProfile = userProfile,
                        onSaveProfile = { viewModel.saveProfile(it) }
                    )
                }
            }
        }
    }
}
