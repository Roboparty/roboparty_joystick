package com.gamepad.controller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * IMU 数据
 *
 * 服务器 → 客户端，频率 50Hz
 */
@Serializable
data class ImuData(
    @SerialName("type") val type: String = "imu",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    // 加速度 (m/s²)
    @SerialName("accel_x") val accelX: Float = 0f,
    @SerialName("accel_y") val accelY: Float = 0f,
    @SerialName("accel_z") val accelZ: Float = 0f,
    // 角速度 (deg/s)
    @SerialName("gyro_x") val gyroX: Float = 0f,
    @SerialName("gyro_y") val gyroY: Float = 0f,
    @SerialName("gyro_z") val gyroZ: Float = 0f,
    // 姿态角 (度)
    @SerialName("roll") val roll: Float = 0f,
    @SerialName("pitch") val pitch: Float = 0f,
    @SerialName("yaw") val yaw: Float = 0f,
    // 磁场 (uT)
    @SerialName("mag_x") val magX: Float = 0f,
    @SerialName("mag_y") val magY: Float = 0f,
    @SerialName("mag_z") val magZ: Float = 0f,
    // 错误码
    @SerialName("imu_error") val imuError: Int = 0
) {
    /** 获取加速度向量 */
    val acceleration: Vector3
        get() = Vector3(accelX, accelY, accelZ)

    /** 获取角速度向量 */
    val angularVelocity: Vector3
        get() = Vector3(gyroX, gyroY, gyroZ)

    /** 获取磁场向量 */
    val magneticField: Vector3
        get() = Vector3(magX, magY, magZ)

    /** 获取姿态角 */
    val attitude: Attitude
        get() = Attitude(pitch, roll, yaw)

    /** 合加速度 */
    val totalAcceleration: Float
        get() = kotlin.math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)

    /** 合角速度 */
    val totalAngularVelocity: Float
        get() = kotlin.math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

    /** 合磁场强度 */
    val totalMagneticField: Float
        get() = kotlin.math.sqrt(magX * magX + magY * magY + magZ * magZ)

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
