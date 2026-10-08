// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * BMS 电池数据
 *
 * 服务器推送的电池状态信息
 */
@Serializable
data class BatteryData(
    @SerialName("type") val type: String = "battery",
    @SerialName("voltage") val voltage: Float = -1f,
    @SerialName("current") val current: Float = -1f,
    @SerialName("soc") val soc: Float = -1f,
    @SerialName("temp") val temp: Float = -1f
)
