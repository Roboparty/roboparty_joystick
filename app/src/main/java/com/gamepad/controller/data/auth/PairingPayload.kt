// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.auth

import android.util.Base64
import org.json.JSONObject

/**
 * 解析 `rp-server-pair code` 输出的 `roboparty://pair?data=...`。
 *
 * data 是服务端配对负载的 base64url(JSON)。URI 由机器人侧可信入口（二维码/
 * 标签/屏幕）传递，作为首次配对的信任根。
 */
data class PairingPayload(
    val version: Int,
    val code: String,
    val expiresAt: Long,
    val tlsSpkiSha256: String,
    val deviceSpkiSha256: String,
    val serial: String,
    val machineId: String,
    val signature: String
)

object PairingPayloadParser {
    private const val PREFIX = "roboparty://pair?data="

    fun parse(uri: String): PairingPayload {
        val trimmed = uri.trim()
        require(trimmed.startsWith(PREFIX)) { "不是有效的 roboparty 配对 URI" }
        val encoded = trimmed.removePrefix(PREFIX)
        val raw = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val obj = JSONObject(String(raw, Charsets.UTF_8))
        return PairingPayload(
            version = obj.optInt("version", 0),
            code = obj.optString("code", ""),
            expiresAt = obj.optLong("expiresAt", 0L),
            tlsSpkiSha256 = obj.optString("tlsSpkiSha256", ""),
            deviceSpkiSha256 = obj.optString("deviceSpkiSha256", ""),
            serial = obj.optString("serial", ""),
            machineId = obj.optString("machineId", ""),
            signature = obj.optString("signature", "")
        ).also {
            require(it.version == 1) { "不支持的配对负载版本: ${it.version}" }
            require(it.code.length == 6) { "配对码无效" }
            require(it.tlsSpkiSha256.isNotBlank()) { "配对 URI 未包含 TLS SPKI 指纹" }
        }
    }
}
