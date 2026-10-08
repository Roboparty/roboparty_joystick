// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.config

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * 连接配置 — 从 assets/connection_config.json 加载
 */
@Serializable
data class ConnectionConfig(
    val control: ControlConfig = ControlConfig(),
    val controlParams: ControlParamsConfig = ControlParamsConfig(),
    val failSafe: FailSafeConfig = FailSafeConfig(),
    val auth: AuthConfig = AuthConfig(),
    val ble: BleConfig = BleConfig(),
    val ota: OtaConfig = OtaConfig()
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun load(context: Context): ConnectionConfig {
            return try {
                val jsonStr = context.assets.open("connection_config.json")
                    .bufferedReader()
                    .use { it.readText() }
                json.decodeFromString<ConnectionConfig>(jsonStr)
            } catch (_: Exception) {
                ConnectionConfig()
            }
        }
    }
}

@Serializable
data class ControlConfig(
    @SerialName("host") val host: String = "10.43.21.32",
    @SerialName("port") val port: Int = 9000
)

@Serializable
data class ControlParamsConfig(
    @SerialName("max_linear_velocity") val maxLinearVelocity: Float = 1.5f,
    @SerialName("max_angular_velocity") val maxAngularVelocity: Float = 3.0f,
    @SerialName("send_frequency_hz") val sendFrequencyHz: Long = 50L
)

@Serializable
data class FailSafeConfig(
    @SerialName("gamepad_timeout_ms") val gamepadTimeoutMs: Long = 500L,
    @SerialName("stale_packet_ms") val stalePacketMs: Long = 300L
)

/**
 * 认证配置
 *
 * 控制三步认证、心跳保活、断线重连的参数
 */
@Serializable
data class AuthConfig(
    /** 单步认证超时（毫秒） */
    @SerialName("timeout_ms") val timeoutMs: Long = 5000,
    /** 认证重试次数 */
    @SerialName("retry_count") val retryCount: Int = 3,
    /** 心跳发送间隔（毫秒） */
    @SerialName("heartbeat_interval_ms") val heartbeatIntervalMs: Long = 1000,
    /** 心跳超时判定断线（毫秒） */
    @SerialName("heartbeat_timeout_ms") val heartbeatTimeoutMs: Long = 5000,
    /** 重连等待时间（毫秒） */
    @SerialName("reconnect_delay_ms") val reconnectDelayMs: Long = 2000,
    /** 最大重连次数 */
    @SerialName("reconnect_max_count") val reconnectMaxCount: Int = 5
)

/**
 * BLE 配置
 *
 * 控制 BLE 连接参数
 */
@Serializable
data class BleConfig(
    /** BLE 服务 UUID */
    @SerialName("service_uuid") val serviceUuid: String = "0000fff0-0000-1000-8000-00805f9b34fb",
    /** TX 特征 UUID（客户端写入） */
    @SerialName("tx_characteristic_uuid") val txCharacteristicUuid: String = "0000fff1-0000-1000-8000-00805f9b34fb",
    /** RX 特征 UUID（客户端接收通知） */
    @SerialName("rx_characteristic_uuid") val rxCharacteristicUuid: String = "0000fff2-0000-1000-8000-00805f9b34fb",
    /** 扫描超时（毫秒） */
    @SerialName("scan_timeout_ms") val scanTimeoutMs: Long = 10000,
    /** 请求的 MTU 大小 */
    @SerialName("mtu_size") val mtuSize: Int = 512
)

/**
 * OTA 配置
 *
 * 控制 OTA 版本检查和下载的参数
 * OTA 服务器地址独立配置，与控制服务器解耦
 */
@Serializable
data class OtaConfig(
    /** OTA 版本检查服务器 IP */
    @SerialName("host") val host: String = "10.43.21.32",
    /** OTA 版本检查服务器端口 */
    @SerialName("port") val port: Int = 9001,
    /** 版本检查超时时间（毫秒） */
    @SerialName("timeout_ms") val timeoutMs: Long = 5000,
    /** APK 下载 HTTP 基础地址（空则使用服务器响应的 download_url） */
    @SerialName("http_base_url") val httpBaseUrl: String = ""
)
