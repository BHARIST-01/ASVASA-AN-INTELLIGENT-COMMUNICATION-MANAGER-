package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BleScannerManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val onDeviceFound: (device: BluetoothDevice, rssi: Int, isAsvasaService: Boolean) -> Unit
) {

    private val tag = "BleScanner"
    private var scanner: BluetoothLeScanner? = null
    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result == null) return

            val device = result.device
            val rssi = result.rssi
            val uuids = result.scanRecord?.serviceUuids
            val isAsvasaService = uuids?.any { it.uuid == BleConstants.ASVASA_SERVICE_UUID } == true

            onDeviceFound(device, rssi, isAsvasaService)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                val device = result.device
                val isAsvasa = result.scanRecord?.serviceUuids?.any { it.uuid == BleConstants.ASVASA_SERVICE_UUID } == true
                onDeviceFound(device, result.rssi, isAsvasa)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(tag, "BLE Scan not supported or failed with errorCode: $errorCode")
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(timeoutMillis: Long = 10000L): Boolean {
        if (_isScanning.value) return true

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.d(tag, "Bluetooth is disabled, cannot start BLE scan")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            if (scanPerm != PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "BLUETOOTH_SCAN permission not yet granted; skipping hardware scan.")
                return false
            }
        }

        try {
            scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                Log.d(tag, "BluetoothLeScanner not available in this environment")
                return false
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            scanner?.startScan(emptyList(), settings, scanCallback)
            _isScanning.value = true
            Log.i(tag, "BLE Scan started successfully.")

            // Automatically stop after timeout to save battery
            scanJob?.cancel()
            scanJob = scope.launch {
                delay(timeoutMillis)
                stopScan()
            }
            return true
        } catch (e: Exception) {
            Log.d(tag, "Failed to start BLE scan: ${e.message}")
            _isScanning.value = false
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        scanJob?.cancel()
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        _isScanning.value = false
        Log.i(tag, "BLE Scan stopped.")
    }
}
