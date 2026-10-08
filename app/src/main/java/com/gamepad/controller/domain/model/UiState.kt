// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.domain.model

import com.gamepad.controller.data.model.BatteryData
import com.gamepad.controller.data.model.ImuData
import com.gamepad.controller.data.model.MotorStatus
import com.gamepad.controller.data.model.MotorStatusResponse
import com.gamepad.controller.data.model.enums.ConnectionStatus
import com.gamepad.controller.domain.model.OtaState

/**
 * 姿态模式
 */
enum class PostureMode(val label: String) {
    STAND("站起"),
    SIT("坐下"),
    IDLE("待机")
}

/**
 * 急停确认状态
 */
enum class EstopStatus(val label: String, val description: String) {
    IDLE("待命", "急停系统正常"),
    SENDING("发送中", "正在发送急停请求..."),
    WAITING("等待确认", "等待服务器确认..."),
    CONFIRMED("已确认", "服务器已确认急停"),
    FAILED("确认失败", "服务器未响应")
}

/**
 * UI 唯一可信状态（MVI 架构）
 */
data class UiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val activeDeviceId: Int = -1,
    val deviceName: String = "",
    val buttons: Int = 0,
    val isEstopping: Boolean = false,
    val estopStatus: EstopStatus = EstopStatus.IDLE,
    val isTokenValid: Boolean = false,  // Token 是否有效
    // 服务器配置
    val serverHost: String = "10.43.21.32",
    val serverPort: Int = 9000,
    // 姿态
    val posture: PostureMode = PostureMode.STAND,
    // 左摇杆实时位置（-1~1）
    val stickX: Float = 0f,
    val stickY: Float = 0f,
    // 右摇杆实时位置（-1~1）
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    // 电机状态
    val motors: List<MotorStatus> = emptyList(),
    val motorCount: Int = 0,
    // IMU 数据
    val imuData: ImuData? = null,
    // BMS 电池数据
    val batteryData: BatteryData? = null,
    // OTA 更新状态
    val otaState: OtaState = OtaState()
) {
    val isConnected: Boolean get() = connectionStatus.isConnected
}
