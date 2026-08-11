package com.gamepad.controller.data.model.packet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OTA 版本检查请求 — 客户端 → 服务器
 *
 * 通过 UDP 发送到 OTA 服务器，查询是否有新版本可用
 */
@Serializable
data class OtaVersionCheckRequest(
    @SerialName("type") val type: String = "ota_version_check",
    @SerialName("current_version") val currentVersion: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * OTA 版本检查响应 — 服务器 → 客户端
 *
 * 服务器返回最新版本信息和 APK 下载地址
 */
@Serializable
data class OtaVersionCheckResponse(
    @SerialName("type") val type: String = "ota_version_check_response",
    @SerialName("has_update") val hasUpdate: Boolean,
    @SerialName("latest_version") val latestVersion: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("md5") val md5: String? = null,
    @SerialName("changelog") val changelog: String? = null
)
