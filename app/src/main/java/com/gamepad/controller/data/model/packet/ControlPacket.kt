package com.gamepad.controller.data.model.packet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 握手请求包 — 客户端 → 服务器
 *
 * 建立一一对应关系：每个手柄设备有唯一 ID，机器人只接受配对设备
 */
@Serializable
data class HandshakeRequest(
    @SerialName("type") val type: String = "handshake",
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String = "Android Gamepad",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * 握手响应包 — 服务器 → 客户端
 *
 * @param ok 握手是否成功
 * @param session_id 会话 ID（后续控制包需携带）
 * @param reason 失败原因（如"device already paired"）
 */
@Serializable
data class HandshakeResponse(
    @SerialName("type") val type: String = "handshake_response",
    @SerialName("ok") val ok: Boolean,
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("reason") val reason: String = ""
)

/**
 * 控制指令包 — 匹配目标 UDP 服务器 JSON 格式
 *
 * 字段说明：
 * - timestamp: 毫秒时间戳
 * - sequence: 单调递增序列号
 * - left_stick_x/y: 左摇杆归一化 [-1, 1]
 * - right_stick_x/y: 右摇杆归一化 [-1, 1]
 * - btn_a/b/x/y: 按键布尔状态
 * - dpad_*: 方向键布尔状态
 */
@Serializable
data class ControlPacket(
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("sequence") val sequence: Long,
    @SerialName("left_stick_x") val leftStickX: Float = 0f,
    @SerialName("left_stick_y") val leftStickY: Float = 0f,
    @SerialName("right_stick_x") val rightStickX: Float = 0f,
    @SerialName("right_stick_y") val rightStickY: Float = 0f,
    @SerialName("btn_a") val btnA: Boolean = false,
    @SerialName("btn_b") val btnB: Boolean = false,
    @SerialName("btn_x") val btnX: Boolean = false,
    @SerialName("btn_y") val btnY: Boolean = false,
    @SerialName("dpad_up") val dpadUp: Boolean = false,
    @SerialName("dpad_down") val dpadDown: Boolean = false,
    @SerialName("dpad_left") val dpadLeft: Boolean = false,
    @SerialName("dpad_right") val dpadRight: Boolean = false
)
