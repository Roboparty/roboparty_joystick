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
    val failSafe: FailSafeConfig = FailSafeConfig()
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
