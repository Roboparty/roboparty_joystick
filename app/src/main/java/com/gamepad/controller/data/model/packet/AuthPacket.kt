package com.gamepad.controller.data.model.packet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 认证相关数据包 — 三步认证流程
 *
 * 流程：
 * 1. 客户端 → 服务器：AuthRequest（设备ID）
 * 2. 服务器 → 客户端：Challenge（挑战码）
 * 3. 客户端 → 服务器：ChallengeResponse（HMAC-SHA256 签名）
 * 4. 服务器 → 客户端：AuthResult（Token）
 */

/** ① 认证请求 — 客户端 → 服务器 */
@Serializable
data class AuthRequest(
    @SerialName("type") val type: String = "auth_request",
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String = "Android Gamepad",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/** ② 挑战码 — 服务器 → 客户端 */
@Serializable
data class Challenge(
    @SerialName("type") val type: String = "challenge",
    @SerialName("challenge_code") val challengeCode: String,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/** ③ 挑战响应 — 客户端 → 服务器 */
@Serializable
data class ChallengeResponse(
    @SerialName("type") val type: String = "challenge_response",
    @SerialName("device_id") val deviceId: String,
    @SerialName("signature") val signature: String
)

/** ④ 认证结果 — 服务器 → 客户端 */
@Serializable
data class AuthResult(
    @SerialName("type") val type: String = "auth_result",
    @SerialName("ok") val ok: Boolean,
    @SerialName("token") val token: String = "",
    @SerialName("expires_at") val expiresAt: Long = 0,
    @SerialName("reason") val reason: String = ""
)

/**
 * 服务器消息统一解析
 *
 * 用于解析服务器发送的所有类型消息
 */
@Serializable
data class ServerMessage(
    @SerialName("type") val type: String,
    // challenge 字段
    @SerialName("challenge_code") val challengeCode: String? = null,
    // auth_result 字段
    @SerialName("ok") val ok: Boolean? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("expires_at") val expiresAt: Long? = null,
    @SerialName("reason") val reason: String? = null,
    // heartbeat/通用字段
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("sequence") val sequence: Long? = null,
    // battery 字段
    @SerialName("voltage") val voltage: Float? = null,
    @SerialName("current") val current: Float? = null,
    @SerialName("soc") val soc: Float? = null,
    @SerialName("temp") val temp: Float? = null,
    // skill_action_ack 字段
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("execution_time_ms") val executionTimeMs: Long? = null,
    // OTA 版本检查响应字段
    @SerialName("has_update") val hasUpdate: Boolean? = null,
    @SerialName("latest_version") val latestVersion: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("md5") val md5: String? = null,
    @SerialName("changelog") val changelog: String? = null
)
