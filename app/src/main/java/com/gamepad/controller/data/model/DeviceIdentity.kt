// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 机器人公开身份（来自 rp_server 的 GET /identity，SN 最终由 TUI 读取）。
 */
@Serializable
data class DeviceIdentity(
    @SerialName("serial") val serial: String = "",
    @SerialName("serialSource") val serialSource: String = "",
    @SerialName("machineId") val machineId: String = "",
    @SerialName("model") val model: String = "",
    @SerialName("hostname") val hostname: String = "",
    @SerialName("identitySource") val identitySource: String = "",
    @SerialName("server") val server: String = "",
    @SerialName("protocolVersion") val protocolVersion: Int = 2,
    @SerialName("features") val features: List<String> = emptyList(),
    @SerialName("spkiSha256") val spkiSha256: String? = null,
    @SerialName("devicePublicKey") val devicePublicKey: String? = null,
    @SerialName("deviceKeySource") val deviceKeySource: String? = null,
    @SerialName("tlsSpkiSha256") val tlsSpkiSha256: String? = null,
    @SerialName("tlsEnabled") val tlsEnabled: Boolean? = null
)
