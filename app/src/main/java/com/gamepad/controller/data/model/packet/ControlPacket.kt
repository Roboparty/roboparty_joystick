package com.gamepad.controller.data.model.packet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 控制指令包 — 匹配目标 UDP 服务器 JSON 格式
 *
 * 字段说明：
 * - type: 数据包类型标识
 * - token: 认证令牌（三步认证后获取）
 * - timestamp: 毫秒时间戳
 * - sequence: 单调递增序列号
 * - left_stick_x/y: 左摇杆归一化 [-1, 1]
 * - right_stick_x/y: 右摇杆归一化 [-1, 1]
 * - btn_a/b/x/y: 按键布尔状态
 * - dpad_*: 方向键布尔状态
 */
@Serializable
data class ControlPacket(
    @SerialName("type") val type: String = "control",
    @SerialName("token") val token: String = "",
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
    @SerialName("btn_lb") val btnLb: Boolean = false,
    @SerialName("btn_rb") val btnRb: Boolean = false,
    @SerialName("btn_lt") val btnLt: Boolean = false,
    @SerialName("btn_rt") val btnRt: Boolean = false,
    @SerialName("dpad_up") val dpadUp: Boolean = false,
    @SerialName("dpad_down") val dpadDown: Boolean = false,
    @SerialName("dpad_left") val dpadLeft: Boolean = false,
    @SerialName("dpad_right") val dpadRight: Boolean = false
)
