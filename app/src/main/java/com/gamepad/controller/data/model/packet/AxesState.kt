// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model.packet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 摇杆轴值状态
 *
 * 所有轴值归一化到 [-1.0, 1.0]，扳机为 [0.0, 1.0]
 */
@Serializable
data class AxesState(
    @SerialName("lx") val lx: Float = 0f,
    @SerialName("ly") val ly: Float = 0f,
    @SerialName("rx") val rx: Float = 0f,
    @SerialName("ry") val ry: Float = 0f,
    @SerialName("lt") val lt: Float = 0f,
    @SerialName("rt") val rt: Float = 0f
) {
    companion object {
        val ZERO = AxesState()
    }
}
