package com.gamepad.controller.data.model

import android.bluetooth.BluetoothDevice

/**
 * BLE 设备信息
 *
 * 用于扫描结果展示和连接
 */
data class BleDevice(
    /** 设备名称（可能为空） */
    val name: String,

    /** MAC 地址 */
    val address: String,

    /** 信号强度 (dBm) */
    val rssi: Int,

    /** Android BluetoothDevice 对象 */
    val device: BluetoothDevice
) {
    /** 信号强度等级 (0-4) */
    val signalLevel: Int
        get() = when {
            rssi > -50 -> 4  // 极好
            rssi > -60 -> 3  // 良好
            rssi > -70 -> 2  // 一般
            rssi > -80 -> 1  // 较差
            else -> 0        // 极差
        }

    /** 信号强度描述 */
    val signalDescription: String
        get() = when (signalLevel) {
            4 -> "极好"
            3 -> "良好"
            2 -> "一般"
            1 -> "较差"
            else -> "极差"
        }

    /** 显示名称（优先使用设备名，否则显示 MAC） */
    val displayName: String
        get() = name.ifEmpty { address }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BleDevice) return false
        return address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}
