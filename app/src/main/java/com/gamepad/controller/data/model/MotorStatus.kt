// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 电机状态数据
 *
 * 服务器 → 客户端，频率 10Hz
 *
 * JSON格式（23个电机）：
 * {
 *   "type": "motors",
 *   "timestamp": 1787212448470,
 *   "id": [1, 2, 3, ...],
 *   "position": [0.0231, -0.0429, ...],
 *   "speed": [0.0061, 0.0061, ...],
 *   "torque": [-0.0488, -0.0488, ...],
 *   "temperature": [30.0, 30.0, ...],
 *   "error": [0, 0, ...],
 *   "mode": [1, 1, ...],
 *   "response_count": [0, 0, ...]
 * }
 */
@Serializable
data class MotorStatusResponse(
    @SerialName("type") val type: String = "motors",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerialName("id") val id: List<Int> = emptyList(),
    @SerialName("position") val position: List<Float> = emptyList(),
    @SerialName("speed") val speed: List<Float> = emptyList(),
    @SerialName("torque") val torque: List<Float> = emptyList(),
    @SerialName("temperature") val temperature: List<Float> = emptyList(),
    @SerialName("error") val error: List<Int> = emptyList(),
    @SerialName("mode") val mode: List<Int> = emptyList(),
    @SerialName("response_count") val responseCount: List<Int> = emptyList()
) {
    /** 电机数量 */
    val motorCount: Int get() = id.size

    /** 转换为 MotorStatus 列表用于 UI 展示 */
    fun toMotorStatusList(): List<MotorStatus> {
        return id.indices.map { i ->
            MotorStatus(
                id = id.getOrElse(i) { 0 },
                position = position.getOrElse(i) { 0f },
                speed = speed.getOrElse(i) { 0f },
                torque = torque.getOrElse(i) { 0f },
                temperature = temperature.getOrElse(i) { 0f },
                errorCode = error.getOrElse(i) { 0 },
                mode = mode.getOrElse(i) { 0 },
                responseCount = responseCount.getOrElse(i) { 0 }
            )
        }
    }
}

/**
 * 单个电机状态（用于 UI 展示）
 */
data class MotorStatus(
    val id: Int = 0,
    val position: Float = 0f,
    val speed: Float = 0f,
    val torque: Float = 0f,
    val temperature: Float = 0f,
    val errorCode: Int = 0,
    val mode: Int = 0,
    val responseCount: Int = 0
) {
    /** 获取状态类型 */
    val statusType: MotorStatusType
        get() = when {
            errorCode != 0 -> MotorStatusType.ERROR
            temperature > 70 -> MotorStatusType.WARNING
            else -> MotorStatusType.NORMAL
        }

    /** 获取模式描述 */
    val modeDescription: String
        get() = when (mode) {
            0 -> "待机"
            1 -> "运行"
            2 -> "校准"
            3 -> "错误"
            else -> "未知($mode)"
        }
}

/**
 * 电机状态类型
 */
enum class MotorStatusType {
    NORMAL,
    WARNING,
    ERROR,
    DISABLED;

    val displayName: String
        get() = when (this) {
            NORMAL -> "正常"
            WARNING -> "警告"
            ERROR -> "错误"
            DISABLED -> "禁用"
        }
}

/**
 * 电机错误码定义
 */
object MotorErrorCode {
    const val NO_ERROR = 0
    const val OVER_TEMPERATURE = 1
    const val OVER_CURRENT = 2
    const val OVER_VOLTAGE = 3
    const val UNDER_VOLTAGE = 4
    const val STALL = 5
    const val ENCODER_FAULT = 6
    const val COMMUNICATION_TIMEOUT = 7
    const val DRIVER_FAULT = 8

    /** 获取错误码描述 */
    fun getDescription(code: Int): String = when (code) {
        NO_ERROR -> "无错误"
        OVER_TEMPERATURE -> "过温保护"
        OVER_CURRENT -> "过流保护"
        OVER_VOLTAGE -> "过压保护"
        UNDER_VOLTAGE -> "欠压保护"
        STALL -> "堵转保护"
        ENCODER_FAULT -> "编码器故障"
        COMMUNICATION_TIMEOUT -> "通信超时"
        DRIVER_FAULT -> "驱动器故障"
        else -> "未知错误 ($code)"
    }

    /** 获取错误码十六进制表示 */
    fun toHexString(code: Int): String = "0x%04X".format(code)

    /** 获取处理建议 */
    fun getSuggestion(code: Int): String = when (code) {
        NO_ERROR -> ""
        OVER_TEMPERATURE -> "降低负载，等待冷却"
        OVER_CURRENT -> "检查负载，重启电机"
        OVER_VOLTAGE -> "检查电源电压"
        UNDER_VOLTAGE -> "检查电池电量"
        STALL -> "清除障碍物，重启电机"
        ENCODER_FAULT -> "检查编码器连接"
        COMMUNICATION_TIMEOUT -> "检查通信线缆"
        DRIVER_FAULT -> "更换驱动器"
        else -> "请联系技术支持"
    }
}
