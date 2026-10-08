// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * IMU 数据
 *
 * 服务器 → 客户端，频率 50Hz
 *
 * JSON格式：
 * {
 *   "type": "imu",
 *   "timestamp": 1787212448469,
 *   "quat": [w, x, y, z],        // 四元数姿态
 *   "ang_vel": [x, y, z],        // 角速度 (rad/s)
 *   "lin_acc": [x, y, z],        // 线性加速度 (m/s²)
 *   "temp": 0.0                  // 温度 (°C)
 * }
 */
@Serializable
data class ImuData(
    @SerialName("type") val type: String = "imu",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    // 四元数姿态 [w, x, y, z]
    @SerialName("quat") val quat: List<Float> = listOf(1f, 0f, 0f, 0f),
    // 角速度 (rad/s) [x, y, z]
    @SerialName("ang_vel") val angVel: List<Float> = listOf(0f, 0f, 0f),
    // 线性加速度 (m/s²) [x, y, z]
    @SerialName("lin_acc") val linAcc: List<Float> = listOf(0f, 0f, 0f),
    // 温度 (°C)
    @SerialName("temp") val temp: Float = 0f
) {
    // 四元数分量
    val quatW: Float get() = quat.getOrElse(0) { 1f }
    val quatX: Float get() = quat.getOrElse(1) { 0f }
    val quatY: Float get() = quat.getOrElse(2) { 0f }
    val quatZ: Float get() = quat.getOrElse(3) { 0f }

    // 角速度分量 (rad/s)
    val angVelX: Float get() = angVel.getOrElse(0) { 0f }
    val angVelY: Float get() = angVel.getOrElse(1) { 0f }
    val angVelZ: Float get() = angVel.getOrElse(2) { 0f }

    // 线性加速度分量 (m/s²)
    val linAccX: Float get() = linAcc.getOrElse(0) { 0f }
    val linAccY: Float get() = linAcc.getOrElse(1) { 0f }
    val linAccZ: Float get() = linAcc.getOrElse(2) { 0f }

    /** 将四元数转换为欧拉角（度） */
    val euler: Attitude
        get() {
            // Roll (x-axis rotation)
            val sinrCosp = 2f * (quatW * quatX + quatY * quatZ)
            val cosrCosp = 1f - 2f * (quatX * quatX + quatY * quatY)
            val roll = kotlin.math.atan2(sinrCosp, cosrCosp)

            // Pitch (y-axis rotation)
            val sinp = 2f * (quatW * quatY - quatZ * quatX)
            val pitch = if (kotlin.math.abs(sinp) >= 1)
                Math.copySign(kotlin.math.PI.toFloat() / 2f, sinp)
            else kotlin.math.asin(sinp)

            // Yaw (z-axis rotation)
            val sinyCosp = 2f * (quatW * quatZ + quatX * quatY)
            val cosyCosp = 1f - 2f * (quatY * quatY + quatZ * quatZ)
            val yaw = kotlin.math.atan2(sinyCosp, cosyCosp)

            return Attitude(
                pitch = Math.toDegrees(pitch.toDouble()).toFloat(),
                roll = Math.toDegrees(roll.toDouble()).toFloat(),
                yaw = Math.toDegrees(yaw.toDouble()).toFloat()
            )
        }

    /** 合加速度 (m/s²) */
    val totalAcceleration: Float
        get() = kotlin.math.sqrt(linAccX * linAccX + linAccY * linAccY + linAccZ * linAccZ)

    /** 合角速度 (rad/s) */
    val totalAngularVelocity: Float
        get() = kotlin.math.sqrt(angVelX * angVelX + angVelY * angVelY + angVelZ * angVelZ)

    /** 重力偏差（理想值应为 9.8 m/s²） */
    val gravityDeviation: Float
        get() = totalAcceleration - 9.8f
}

/**
 * 姿态角
 *
 * 单位：度
 */
data class Attitude(
    val pitch: Float,  // 俯仰角 (-90~90)
    val roll: Float,   // 横滚角 (-180~180)
    val yaw: Float     // 偏航角 (0~360)
)

/**
 * 三维向量
 */
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
)
