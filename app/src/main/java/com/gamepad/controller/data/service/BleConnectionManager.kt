// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.gamepad.controller.data.model.BleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.bluetooth.BluetoothGattDescriptor
import android.os.ParcelUuid
import java.util.UUID

/**
 * BLE 连接管理器
 *
 * 职责：
 * - BLE 设备扫描
 * - GATT 连接管理
 * - 服务发现和特征订阅
 * - MTU 协商
 */
class BleConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "BleConnection"

        // BLE 服务 UUID（可配置）
        val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val TX_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        val RX_CHAR_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")

        // 默认 MTU
        const val DEFAULT_MTU = 20
        const val REQUESTED_MTU = 512
    }

    // ==================== 蓝牙适配器 ====================

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null

    // ==================== 扫描状态 ====================

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    // ==================== 连接状态 ====================

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    private val _mtu = MutableStateFlow(DEFAULT_MTU)
    val mtu: StateFlow<Int> = _mtu.asStateFlow()

    // ==================== 特征引用 ====================

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    // ==================== 回调 ====================

    var onDataReceived: ((ByteArray) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onMtuChanged: ((Int) -> Unit)? = null

    // ==================== 扫描 ====================

    @SuppressLint("MissingPermission")
    fun startScan(timeoutMs: Long = 10000) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter not available")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is disabled")
            return
        }

        _discoveredDevices.value = emptyList()
        _isScanning.value = true

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE scanner not available")
            _isScanning.value = false
            return
        }

        // 扫描过滤器：按 Service UUID 过滤
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        Log.i(TAG, "BLE scan started")

        // 超时停止扫描
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopScan()
        }, timeoutMs)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.stopScan(scanCallback)
        _isScanning.value = false
        Log.i(TAG, "BLE scan stopped")
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: ""
            val address = device.address
            val rssi = result.rssi

            val bleDevice = BleDevice(
                name = name,
                address = address,
                rssi = rssi,
                device = device
            )

            // 更新设备列表（去重）
            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }
            if (existingIndex >= 0) {
                currentList[existingIndex] = bleDevice
            } else {
                currentList.add(bleDevice)
            }
            _discoveredDevices.value = currentList
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            _isScanning.value = false
        }
    }

    // ==================== 连接 ====================

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        Log.i(TAG, "Connecting to ${device.address}")
        _connectionState.value = BluetoothProfile.STATE_CONNECTING

        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        txCharacteristic = null
        rxCharacteristic = null
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
        onConnectionStateChanged?.invoke(false)
    }

    // ==================== GATT 回调 ====================

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "Connection state changed: status=$status, newState=$newState")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BluetoothProfile.STATE_CONNECTED
                    onConnectionStateChanged?.invoke(true)

                    // 连接成功后发现服务
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                    onConnectionStateChanged?.invoke(false)

                    // 清理资源
                    txCharacteristic = null
                    rxCharacteristic = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: $status")
                return
            }

            Log.i(TAG, "Services discovered")

            // 查找目标服务
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "Target service not found")
                disconnect()
                return
            }

            // 获取特征
            txCharacteristic = service.getCharacteristic(TX_CHAR_UUID)
            rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)

            if (txCharacteristic == null || rxCharacteristic == null) {
                Log.e(TAG, "Required characteristics not found")
                disconnect()
                return
            }

            // 订阅 RX 通知
            subscribeToNotifications(gatt, rxCharacteristic!!)

            // 协商 MTU
            gatt.requestMtu(REQUESTED_MTU)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == RX_CHAR_UUID) {
                val data = characteristic.value
                if (data != null) {
                    onDataReceived?.invoke(data)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == RX_CHAR_UUID) {
                onDataReceived?.invoke(value)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _mtu.value = mtu
                onMtuChanged?.invoke(mtu)
                Log.i(TAG, "MTU changed to $mtu")
            } else {
                Log.w(TAG, "MTU negotiation failed, using default: $DEFAULT_MTU")
                _mtu.value = DEFAULT_MTU
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Characteristic write failed: $status")
            }
        }
    }

    // ==================== 数据发送 ====================

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun sendData(data: ByteArray): Boolean {
        val gatt = bluetoothGatt
        val characteristic = txCharacteristic

        if (gatt == null || characteristic == null) {
            Log.e(TAG, "GATT or TX characteristic not available")
            return false
        }

        characteristic.value = data
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return gatt.writeCharacteristic(characteristic)
    }

    // ==================== 辅助方法 ====================

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun subscribeToNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        )
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    fun isConnected(): Boolean {
        return _connectionState.value == BluetoothProfile.STATE_CONNECTED
    }

    fun getConnectedDevice(): BluetoothDevice? {
        return bluetoothGatt?.device
    }

    // ==================== 清理 ====================

    @SuppressLint("MissingPermission")
    fun destroy() {
        stopScan()
        disconnect()
    }
}
