package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log

class BleGattClientManager(
    private val context: Context,
    private val onConnectionChanged: (isConnected: Boolean, deviceAddress: String) -> Unit,
    private val onMessageAckReceived: (fromAddress: String, ackText: String) -> Unit
) {

    private val tag = "BleGattClient"
    private var activeGatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val addr = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(tag, "Connected to GATT server on $addr. Requesting MTU and discovering services...")
                onConnectionChanged(true, addr)
                // Request higher MTU for text messages
                gatt.requestMtu(BleConstants.MAX_BLE_PACKET_BYTES)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(tag, "Disconnected from GATT server on $addr")
                onConnectionChanged(false, addr)
                activeGatt?.close()
                activeGatt = null
                messageCharacteristic = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(BleConstants.ASVASA_SERVICE_UUID)
                if (service != null) {
                    messageCharacteristic = service.getCharacteristic(BleConstants.MESSAGE_CHAR_UUID)
                    Log.i(tag, "ASVASA Message characteristic discovered on ${gatt.device.address}")

                    // Enable local notification
                    if (messageCharacteristic != null) {
                        gatt.setCharacteristicNotification(messageCharacteristic, true)
                        val descriptor = messageCharacteristic?.getDescriptor(BleConstants.CCCD_DESCRIPTOR_UUID)
                        if (descriptor != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            } else {
                                @Suppress("DEPRECATION")
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            val success = status == BluetoothGatt.GATT_SUCCESS
            Log.i(tag, "Message write completed to ${gatt.device.address}, success: $success")
            if (success) {
                onMessageAckReceived(gatt.device.address, "ACK: Message delivered via BLE link.")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == BleConstants.MESSAGE_CHAR_UUID) {
                @Suppress("DEPRECATION")
                val text = characteristic.value?.let { String(it, Charsets.UTF_8) } ?: ""
                onMessageAckReceived(gatt.device.address, text)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        disconnect()
        return try {
            activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed connecting to BLE device: ${device.address}", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(text: String): Boolean {
        val gatt = activeGatt ?: return false
        val char = messageCharacteristic ?: return false

        val bytes = text.toByteArray(Charsets.UTF_8)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val res = gatt.writeCharacteristic(
                    char,
                    bytes,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                res == 0 // BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception writing message to characteristic", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {}
        activeGatt = null
        messageCharacteristic = null
    }
}
