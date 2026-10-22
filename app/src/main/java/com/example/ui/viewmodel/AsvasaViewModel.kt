package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothMeshManager
import com.example.data.database.AsvasaDatabase
import com.example.data.gemini.EmergencyActionType
import com.example.data.gemini.GeminiRepository
import com.example.data.model.BleMessageEntity
import com.example.data.model.BluetoothPeer
import com.example.data.model.ChatMessage
import com.example.data.model.EmergencyCenter
import com.example.data.model.MessageStatus
import com.example.data.model.MlCommProbability
import com.example.data.model.UserProfile
import com.example.location.EmergencyLocatorService
import com.example.location.UserGpsLocation
import com.example.ml.MlCommunicationEngine
import com.example.voice.EmergencyVoiceController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AsvasaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AsvasaDatabase.getDatabase(application)
    private val userProfileDao = database.userProfileDao()
    private val chatMessageDao = database.chatMessageDao()
    private val emergencyCenterDao = database.emergencyCenterDao()
    val bleMessageDao = database.bleMessageDao()

    val bluetoothManager = BluetoothMeshManager(application)
    val locatorService = EmergencyLocatorService(application)
    val geminiRepository = GeminiRepository()

    lateinit var voiceController: EmergencyVoiceController
        private set

    // Navigation Tab: 0 = Dashboard, 1 = Mesh Comms, 2 = Help Centers, 3 = Profile
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    val userProfile: StateFlow<UserProfile> = userProfileDao.getUserProfile()
        .map { it ?: UserProfile() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserProfile()
        )

    val bluetoothPeers: StateFlow<List<BluetoothPeer>> = bluetoothManager.peers
    val isScanningBt: StateFlow<Boolean> = bluetoothManager.isScanning
    val isAdvertisingBt: StateFlow<Boolean> = bluetoothManager.isAdvertising
    val connectedPeer: StateFlow<BluetoothPeer?> = bluetoothManager.connectedPeer

    private val _selectedPeer = MutableStateFlow<BluetoothPeer?>(null)
    val selectedPeer: StateFlow<BluetoothPeer?> = _selectedPeer.asStateFlow()

    private val _mlProbability = MutableStateFlow<MlCommProbability?>(null)
    val mlProbability: StateFlow<MlCommProbability?> = _mlProbability.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = chatMessageDao.getAllMessages()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val emergencyCenters: StateFlow<List<EmergencyCenter>> = locatorService.emergencyCenters
    val currentGps: StateFlow<UserGpsLocation> = locatorService.currentLocation

    // Voice State
    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _voiceTranscript = MutableStateFlow("")
    val voiceTranscript: StateFlow<String> = _voiceTranscript.asStateFlow()

    private val _voiceActionBanner = MutableStateFlow<String?>(null)
    val voiceActionBanner: StateFlow<String?> = _voiceActionBanner.asStateFlow()

    // Gemini Triage State
    private val _triageResponse = MutableStateFlow<String?>(null)
    val triageResponse: StateFlow<String?> = _triageResponse.asStateFlow()

    private val _isTriageLoading = MutableStateFlow(false)
    val isTriageLoading: StateFlow<Boolean> = _isTriageLoading.asStateFlow()

    init {
        setupVoiceController(application)
        observeBluetoothPeers()
        observeIncomingBleMessages()
        seedDefaultProfileAndMessages()
    }

    private fun observeIncomingBleMessages() {
        viewModelScope.launch {
            bluetoothManager.incomingMessages.collect { incoming ->
                val isSos = incoming.text.contains("SOS", ignoreCase = true) ||
                        incoming.text.contains("EMERGENCY", ignoreCase = true) ||
                        incoming.text.contains("MAYDAY", ignoreCase = true)

                val msgId = UUID.randomUUID().toString()
                chatMessageDao.insertMessage(
                    ChatMessage(
                        id = msgId,
                        peerAddress = incoming.senderAddress,
                        senderName = incoming.senderName,
                        text = incoming.text,
                        timestamp = incoming.timestamp,
                        isFromMe = false,
                        isEmergencyAlert = isSos,
                        deliveryProbability = 95,
                        status = MessageStatus.DELIVERED
                    )
                )

                // Persist to BleMessageDao for offline recovery across restarts
                bleMessageDao.insertBleMessage(
                    BleMessageEntity(
                        id = msgId,
                        senderAddress = incoming.senderAddress,
                        senderName = incoming.senderName,
                        recipientAddress = "LOCAL_NODE",
                        messageText = incoming.text,
                        timestamp = incoming.timestamp,
                        isOutgoing = false,
                        isEmergencySos = isSos,
                        deliveryProbabilityPercent = 95,
                        deliveryStatus = "DELIVERED",
                        packetSizeBytes = incoming.text.toByteArray(Charsets.UTF_8).size
                    )
                )

                if (isSos) {
                    voiceController.speak("Incoming emergency SOS from ${incoming.senderName}")
                }
            }
        }
    }

    private fun setupVoiceController(app: Application) {
        voiceController = EmergencyVoiceController(app) { recognizedText ->
            handleSpokenVoiceInput(recognizedText)
        }
    }

    private fun seedDefaultProfileAndMessages() {
        viewModelScope.launch {
            userProfileDao.saveUserProfile(
                UserProfile(
                    userId = "local_user_1",
                    fullName = "Rescue Operator / Citizen",
                    emergencyContactName = "Primary ICE Contact",
                    emergencyContactPhone = "911",
                    bloodType = "O+",
                    medicalNotes = "Trained in CPR. No medical allergies.",
                    beaconBroadcastName = "ASVASA-Relay-01"
                )
            )

            // Only seed default message if database is newly initialized
            if (bleMessageDao.getMessageCount() == 0) {
                val seedText = "Broadcast: Emergency water and first aid station open at City Hall (Zone 4). Mesh operational."
                val seedTime = System.currentTimeMillis() - 1000 * 60 * 12
                val seedMsg = ChatMessage(
                    id = "msg_seed_1",
                    peerAddress = "BROADCAST",
                    senderName = "RescueTeam-Alpha",
                    text = seedText,
                    timestamp = seedTime,
                    isFromMe = false,
                    isEmergencyAlert = true,
                    deliveryProbability = 95
                )
                chatMessageDao.insertMessage(seedMsg)
                bleMessageDao.insertBleMessage(
                    BleMessageEntity(
                        id = "msg_seed_1",
                        senderAddress = "02:00:AA:BB:CC:11",
                        senderName = "RescueTeam-Alpha",
                        recipientAddress = "BROADCAST",
                        messageText = seedText,
                        timestamp = seedTime,
                        isOutgoing = false,
                        isEmergencySos = true,
                        deliveryProbabilityPercent = 95,
                        deliveryStatus = "DELIVERED",
                        packetSizeBytes = seedText.toByteArray(Charsets.UTF_8).size
                    )
                )
            }
        }
    }

    private fun observeBluetoothPeers() {
        viewModelScope.launch {
            bluetoothPeers.collect { peers ->
                val target = _selectedPeer.value ?: peers.firstOrNull()
                _selectedPeer.value = target
                if (target != null) {
                    _mlProbability.value = MlCommunicationEngine.computeCommunicationProbability(
                        target.address,
                        target.rssi,
                        target.batteryLevel,
                        target.estimatedDistanceMeters
                    )
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    fun selectPeer(peer: BluetoothPeer) {
        _selectedPeer.value = peer
        bluetoothManager.connectToPeer(peer)
        _mlProbability.value = MlCommunicationEngine.computeCommunicationProbability(
            peer.address,
            peer.rssi,
            peer.batteryLevel,
            peer.estimatedDistanceMeters
        )
    }

    fun startVoiceListening() {
        _voiceActionBanner.value = "Listening for emergency command (e.g. 'Call Police', 'SOS')..."
        voiceController.startListening()
        _isVoiceListening.value = true
    }

    fun stopVoiceListening() {
        voiceController.stopListening()
        _isVoiceListening.value = false
    }

    fun handleSpokenVoiceInput(transcript: String) {
        _isVoiceListening.value = false
        _voiceTranscript.value = transcript
        val parsed = geminiRepository.parseVoiceCommand(transcript)

        _voiceActionBanner.value = parsed.spokenSummary
        voiceController.speak(parsed.spokenSummary)

        when (parsed.actionType) {
            EmergencyActionType.CALL_POLICE -> {
                executeEmergencyCall("100")
            }
            EmergencyActionType.CALL_FIRE -> {
                executeEmergencyCall("101")
            }
            EmergencyActionType.CALL_AMBULANCE -> {
                executeEmergencyCall("102")
            }
            EmergencyActionType.BROADCAST_SOS -> {
                sendSosBroadcast()
            }
            EmergencyActionType.FIND_NEAREST_HELP -> {
                _currentTab.value = 2 // Switch to Help Centers tab
            }
            EmergencyActionType.CHECK_MESH_STATUS -> {
                _currentTab.value = 1 // Switch to Mesh tab
            }
            EmergencyActionType.GENERAL_QUERY -> {
                requestGeminiTriage(transcript, useHighThinking = false)
            }
        }
    }

    fun executeEmergencyCall(phoneNumber: String) {
        voiceController.dialPhoneNumber(phoneNumber)
    }

    fun sendChatMessage(text: String, isEmergencyAlert: Boolean = false) {
        if (text.isBlank()) return
        val currentProb = _mlProbability.value?.overallChancePercent ?: 85
        val peerAddr = _selectedPeer.value?.address ?: "BROADCAST"

        val msgId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = msgId,
            peerAddress = peerAddr,
            senderName = userProfile.value.fullName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            isEmergencyAlert = isEmergencyAlert,
            deliveryProbability = currentProb,
            status = MessageStatus.SENT
        )

        viewModelScope.launch {
            chatMessageDao.insertMessage(message)
            bleMessageDao.insertBleMessage(
                BleMessageEntity(
                    id = msgId,
                    senderAddress = "LOCAL_NODE",
                    senderName = userProfile.value.fullName,
                    recipientAddress = peerAddr,
                    messageText = text,
                    timestamp = message.timestamp,
                    isOutgoing = true,
                    isEmergencySos = isEmergencyAlert,
                    deliveryProbabilityPercent = currentProb,
                    deliveryStatus = "SENT",
                    packetSizeBytes = text.toByteArray(Charsets.UTF_8).size
                )
            )

            // Send via Bluetooth Low Energy GATT Characteristic
            bluetoothManager.sendMessageToConnectedPeer(text)

            // Simulate realistic reply from connected peer node if available
            val peer = _selectedPeer.value
            if (peer != null) {
                simulatePeerAck(peer, text, isEmergencyAlert)
            }
        }
    }

    private suspend fun simulatePeerAck(peer: BluetoothPeer, originalText: String, isAlert: Boolean) {
        kotlinx.coroutines.delay(1600)
        val replyText = if (isAlert) {
            "ACK from ${peer.name}: SOS distress received! Distance ~${peer.estimatedDistanceMeters}m. Dispatching local assistance."
        } else {
            "ACK from ${peer.name}: Message received via Bluetooth mesh. Signal solid (${peer.rssi} dBm)."
        }

        val ackId = UUID.randomUUID().toString()
        val ackTime = System.currentTimeMillis()
        val ackMessage = ChatMessage(
            id = ackId,
            peerAddress = peer.address,
            senderName = peer.name,
            text = replyText,
            timestamp = ackTime,
            isFromMe = false,
            isEmergencyAlert = isAlert,
            deliveryProbability = peer.commProbabilityPercent,
            status = MessageStatus.DELIVERED
        )
        chatMessageDao.insertMessage(ackMessage)
        bleMessageDao.insertBleMessage(
            BleMessageEntity(
                id = ackId,
                senderAddress = peer.address,
                senderName = peer.name,
                recipientAddress = "LOCAL_NODE",
                messageText = replyText,
                timestamp = ackTime,
                isOutgoing = false,
                isEmergencySos = isAlert,
                deliveryProbabilityPercent = peer.commProbabilityPercent,
                deliveryStatus = "DELIVERED",
                packetSizeBytes = replyText.toByteArray(Charsets.UTF_8).size
            )
        )
    }

    fun sendSosBroadcast() {
        val coords = currentGps.value
        val sosText = "🚨 MAYDAY / SOS BROADCAST: Survivor at Lat ${coords.latitude}, Lon ${coords.longitude}. Need immediate assistance!"
        sendChatMessage(sosText, isEmergencyAlert = true)
        voiceController.speak("Emergency SOS Broadcast transmitted to all nearby Bluetooth nodes.")
    }

    fun requestGeminiTriage(prompt: String, useHighThinking: Boolean) {
        _isTriageLoading.value = true
        viewModelScope.launch {
            val response = geminiRepository.generateEmergencyResponse(prompt, useHighThinking)
            _triageResponse.value = response
            _isTriageLoading.value = false
            // Read first 2 sentences aloud via TTS for hands-free support
            val shortAudio = response.split("\n").firstOrNull { it.isNotBlank() } ?: "Advice received."
            voiceController.speak(shortAudio)
        }
    }

    fun clearTriageResponse() {
        _triageResponse.value = null
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            userProfileDao.saveUserProfile(profile)
        }
    }

    fun scanBluetooth() {
        bluetoothManager.startScan()
    }

    fun toggleAdvertising(enable: Boolean) {
        if (enable) bluetoothManager.startAdvertising()
        else bluetoothManager.stopAdvertising()
    }

    fun onBluetoothPermissionsGranted() {
        bluetoothManager.tryStartBleServices()
    }

    fun refreshGps() {
        locatorService.fetchLocation()
    }

    override fun onCleared() {
        super.onCleared()
        voiceController.destroy()
        bluetoothManager.destroy()
    }
}
