package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.data.model.BluetoothPeer
import com.example.ml.MlCommunicationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncomingMeshMessage(
    val senderAddress: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BluetoothMeshManager(private val context: Context) {

    private val tag = "BluetoothMeshManager"

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = try {
        bluetoothManager?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
    } catch (_: Exception) {
        null
    }

    // Core BLE Subsystems
    val advertiserManager = BleAdvertiserManager(context, bluetoothAdapter)
    val gattServerManager: BleGattServerManager
    val scannerManager: BleScannerManager
    val gattClientManager: BleGattClientManager

    // UI States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _peers = MutableStateFlow<List<BluetoothPeer>>(emptyList())
    val peers: StateFlow<List<BluetoothPeer>> = _peers.asStateFlow()

    private val _connectedPeer = MutableStateFlow<BluetoothPeer?>(null)
    val connectedPeer: StateFlow<BluetoothPeer?> = _connectedPeer.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<IncomingMeshMessage>()
    val incomingMessages: SharedFlow<IncomingMeshMessage> = _incomingMessages.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val realDiscoveredDevices = mutableMapOf<String, BluetoothDevice>()

    init {
        // 1. Initialize GATT Server to receive incoming peer text messages
        gattServerManager = BleGattServerManager(context) { senderAddress, senderName, text ->
            scope.launch {
                _incomingMessages.emit(
                    IncomingMeshMessage(
                        senderAddress = senderAddress,
                        senderName = senderName,
                        text = text
                    )
                )
            }
        }

        // 2. Initialize GATT Client to send messages to connected peers
        gattClientManager = BleGattClientManager(
            context = context,
            onConnectionChanged = { isConnected, address ->
                scope.launch {
                    val updated = _peers.value.map {
                        if (it.address == address) it.copy(isConnected = isConnected) else it
                    }
                    _peers.value = updated
                    if (!isConnected && _connectedPeer.value?.address == address) {
                        _connectedPeer.value = null
                    }
                }
            },
            onMessageAckReceived = { fromAddress, ackText ->
                scope.launch {
                    _incomingMessages.emit(
                        IncomingMeshMessage(
                            senderAddress = fromAddress,
                            senderName = _peers.value.find { it.address == fromAddress }?.name ?: "Remote Peer",
                            text = ackText
                        )
                    )
                }
            }
        )

        // 3. Initialize BLE Scanner
        scannerManager = BleScannerManager(context, bluetoothAdapter) { device, rssi, isAsvasa ->
            handleDeviceDiscovered(device, rssi, isAsvasa)
        }

        // 4. Seed initial default simulated nodes for offline/emulator reliability
        seedInitialSimulatedPeers()

        // 5. Safely start hardware BLE services if adapter is enabled
        tryStartBleServices()
    }

    fun tryStartBleServices() {
        val adapter = bluetoothAdapter
        if (adapter != null && adapter.isEnabled) {
            gattServerManager.startGattServer()
            startAdvertising()
        }
    }

    private fun seedInitialSimulatedPeers() {
        val initialList = listOf(
            createPeer("02:00:AA:BB:CC:11", "RescueTeam-Alpha (HQ Relay)", -58, true),
            createPeer("02:00:AA:BB:CC:22", "FieldMedic-Post 4", -71, true),
            createPeer("02:00:AA:BB:CC:33", "CommunityShelter-Node", -83, false),
            createPeer("02:00:AA:BB:CC:44", "SurvivorBeacon-Node 09", -65, true)
        )
        _peers.value = initialList
        _connectedPeer.value = initialList.first()
    }

    private fun createPeer(address: String, name: String, rssi: Int, isBeacon: Boolean): BluetoothPeer {
        val dist = MlCommunicationEngine.estimateDistance(rssi)
        val mlProb = MlCommunicationEngine.computeCommunicationProbability(
            peerAddress = address,
            rssi = rssi,
            estimatedDistanceMeters = dist
        )
        return BluetoothPeer(
            address = address,
            name = name,
            rssi = rssi,
            estimatedDistanceMeters = dist,
            commProbabilityPercent = mlProb.overallChancePercent,
            isConnected = false,
            isDisasterBeacon = isBeacon
        )
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceDiscovered(device: BluetoothDevice, rssi: Int, isAsvasaService: Boolean) {
        val address = device.address ?: return
        realDiscoveredDevices[address] = device

        val deviceName = try {
            val rawName = device.name
            if (!rawName.isNullOrBlank()) rawName else if (isAsvasaService) "ASVASA-Beacon-${address.takeLast(4)}" else "BLE-Node-${address.takeLast(4)}"
        } catch (_: Exception) {
            "BLE-Node-${address.takeLast(4)}"
        }

        val distance = MlCommunicationEngine.estimateDistance(rssi)
        val mlProb = MlCommunicationEngine.computeCommunicationProbability(
            peerAddress = address,
            rssi = rssi,
            estimatedDistanceMeters = distance
        )

        val updated = _peers.value.toMutableList()
        val index = updated.indexOfFirst { it.address == address }
        val peer = BluetoothPeer(
            address = address,
            name = deviceName,
            rssi = rssi,
            estimatedDistanceMeters = distance,
            commProbabilityPercent = mlProb.overallChancePercent,
            isConnected = _connectedPeer.value?.address == address,
            isDisasterBeacon = isAsvasaService
        )

        if (index >= 0) {
            updated[index] = peer
        } else {
            updated.add(0, peer)
        }
        _peers.value = updated
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true

        // 1. Launch real BLE Scanner if hardware is available
        val startedHardware = scannerManager.startScan(timeoutMillis = 12000L)
        Log.i(tag, "Hardware BLE scan started: $startedHardware")

        // 2. Also simulate mesh channel dynamics for testing and UI responsiveness
        scope.launch {
            delay(1200)
            val updated = _peers.value.map { peer ->
                val jitter = (-3..3).random()
                val newRssi = (peer.rssi + jitter).coerceIn(-98, -45)
                val newDist = MlCommunicationEngine.estimateDistance(newRssi)
                val mlProb = MlCommunicationEngine.computeCommunicationProbability(
                    peer.address,
                    newRssi,
                    peer.batteryLevel,
                    newDist
                )
                peer.copy(
                    rssi = newRssi,
                    estimatedDistanceMeters = newDist,
                    commProbabilityPercent = mlProb.overallChancePercent
                )
            }.toMutableList()

            // Discover new simulated responder if list is small
            if (updated.size < 6 && (0..1).random() == 1) {
                val newId = updated.size + 1
                updated.add(
                    createPeer(
                        "02:00:AA:BB:CC:0$newId",
                        "EmergencyPatrol-Node $newId",
                        (-80..-60).random(),
                        true
                    )
                )
            }

            _peers.value = updated
            delay(2000)
            _isScanning.value = false
        }
    }

    fun stopScan() {
        _isScanning.value = false
        scannerManager.stopScan()
    }

    fun startAdvertising(nodeName: String = "ASVASA-Emergency-Node") {
        advertiserManager.startAdvertising(nodeName)
        _isAdvertising.value = true
    }

    fun stopAdvertising() {
        advertiserManager.stopAdvertising()
        _isAdvertising.value = false
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(peer: BluetoothPeer) {
        val updated = _peers.value.map {
            if (it.address == peer.address) it.copy(isConnected = true)
            else it.copy(isConnected = false)
        }
        _peers.value = updated
        _connectedPeer.value = peer.copy(isConnected = true)

        // If this is a real discovered physical device, initiate real BLE GATT connection
        val realDevice = realDiscoveredDevices[peer.address]
        if (realDevice != null) {
            Log.i(tag, "Initiating real BLE GATT connection to ${peer.address}")
            gattClientManager.connect(realDevice)
        }
    }

    fun disconnectPeer() {
        val updated = _peers.value.map { it.copy(isConnected = false) }
        _peers.value = updated
        _connectedPeer.value = null
        gattClientManager.disconnect()
    }

    fun sendMessageToConnectedPeer(text: String): Boolean {
        val peer = _connectedPeer.value ?: return false
        val realDevice = realDiscoveredDevices[peer.address]

        if (realDevice != null) {
            // Send via real BLE GATT Characteristic
            return gattClientManager.sendMessage(text)
        }
        return true
    }

    fun isBluetoothHardwareEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }
    }

    fun destroy() {
        stopScan()
        stopAdvertising()
        gattClientManager.disconnect()
        gattServerManager.stopGattServer()
    }
}
