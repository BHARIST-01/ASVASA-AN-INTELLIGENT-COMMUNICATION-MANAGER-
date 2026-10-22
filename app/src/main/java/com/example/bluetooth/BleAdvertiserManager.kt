package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleAdvertiserManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {

    private val tag = "BleAdvertiser"
    private var advertiser: BluetoothLeAdvertiser? = null

    private val _advertisingState = MutableStateFlow(BleAdvertisingState.IDLE)
    val advertisingState: StateFlow<BleAdvertisingState> = _advertisingState.asStateFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.i(tag, "BLE Advertising started successfully for ASVASA mesh node.")
            _advertisingState.value = BleAdvertisingState.ADVERTISING
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d(tag, "BLE Advertising not available or failed with code: $errorCode")
            _advertisingState.value = BleAdvertisingState.FAILED
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(nodeName: String = "ASVASA-Node") {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.d(tag, "Bluetooth is disabled or unavailable; skipping hardware advertising.")
            _advertisingState.value = BleAdvertisingState.UNSUPPORTED
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val advPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
            if (advPerm != PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "BLUETOOTH_ADVERTISE permission not yet granted; deferring advertisement.")
                _advertisingState.value = BleAdvertisingState.IDLE
                return
            }
        }

        try {
            advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (advertiser == null) {
                Log.d(tag, "BLE Advertiser hardware not available in this environment.")
                _advertisingState.value = BleAdvertisingState.UNSUPPORTED
                return
            }

            _advertisingState.value = BleAdvertisingState.STARTING

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0) // Advertise indefinitely until explicitly stopped
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(BleConstants.ASVASA_PARCEL_UUID)
                .build()

            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            Log.d(tag, "Exception starting BLE advertisement: ${e.message}")
            _advertisingState.value = BleAdvertisingState.FAILED
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (_: Exception) {}
        _advertisingState.value = BleAdvertisingState.IDLE
    }
}
