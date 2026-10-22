package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class BleGattServerManager(
    private val context: Context,
    private val onMessageReceived: (senderAddress: String, senderName: String, text: String) -> Unit
) {

    private val tag = "BleGattServer"
    private var gattServer: BluetoothGattServer? = null
    private val connectedDevices = mutableSetOf<BluetoothDevice>()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            val address = device.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Remote peer connected to GATT Server: $address")
                connectedDevices.add(device)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(tag, "Remote peer disconnected from GATT Server: $address")
                connectedDevices.remove(device)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)

            if (characteristic.uuid == BleConstants.MESSAGE_CHAR_UUID && value != null) {
                val messageText = String(value, Charsets.UTF_8)
                val deviceName = try { device.name ?: "Peer-${device.address.takeLast(4)}" } catch (_: Exception) { "BLE-Peer" }
                Log.i(tag, "Received BLE message from $deviceName: $messageText")

                onMessageReceived(device.address, deviceName, messageText)

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            } else if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startGattServer(): Boolean {
        if (bluetoothManager == null) return false

        val adapter = bluetoothManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.d(tag, "Bluetooth adapter disabled or unavailable; skipping hardware GATT server.")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            if (connectPerm != PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "BLUETOOTH_CONNECT permission not yet granted; deferring GATT server initialization.")
                return false
            }
        }

        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                Log.d(tag, "Hardware GATT Server connection not available in this environment; mesh fallback active.")
                return false
            }

            val service = BluetoothGattService(
                BleConstants.ASVASA_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            val messageCharacteristic = BluetoothGattCharacteristic(
                BleConstants.MESSAGE_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            val cccdDescriptor = BluetoothGattDescriptor(
                BleConstants.CCCD_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            messageCharacteristic.addDescriptor(cccdDescriptor)
            service.addCharacteristic(messageCharacteristic)

            val success = gattServer?.addService(service) == true
            Log.i(tag, "ASVASA GATT Server setup result: $success")
            return success
        } catch (e: Exception) {
            Log.d(tag, "GATT server initialization deferred: ${e.message}")
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopGattServer() {
        try {
            gattServer?.close()
        } catch (_: Exception) {}
        gattServer = null
        connectedDevices.clear()
    }
}
