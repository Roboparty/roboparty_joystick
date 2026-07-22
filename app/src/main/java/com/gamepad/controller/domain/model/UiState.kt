package com.gamepad.controller.domain.model

import com.gamepad.controller.data.model.enums.ConnectionStatus

/**
 * 姿态模式
 */
enum class PostureMode(val label: String) {
    STAND("站起"),
    SIT("坐下"),
    IDLE("待机")
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
    // 姿态
    val posture: PostureMode = PostureMode.STAND,
    // 左摇杆实时位置（-1~1）
    val stickX: Float = 0f,
    val stickY: Float = 0f,
    // 右摇杆实时位置（-1~1）
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f
) {
    val isConnected: Boolean get() = connectionStatus.isConnected
}
