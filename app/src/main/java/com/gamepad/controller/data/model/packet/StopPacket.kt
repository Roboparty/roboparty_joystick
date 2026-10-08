// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model.packet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 急停指令包 — 三步确认协议
 *
 * 协议流程：
 * 1. 客户端发送: type="stop", status="request"
 * 2. 服务器返回: type="stop", status="accepted"
 * 3. 客户端发送: type="stop", status="received"
 *
 * 字段说明：
 * - type: 固定为 "stop"
 * - token: 认证令牌
 * - timestamp: 毫秒时间戳
 * - sequence: 单调递增序列号
 * - status: 状态标识 (request/accepted/received)
 */
@Serializable
data class StopPacket(
    @SerialName("type") val type: String = "stop",
    @SerialName("token") val token: String = "",
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("sequence") val sequence: Long,
    @SerialName("status") val status: String = "request"
) {
    companion object {
        /** 客户端发起急停请求 */
        fun request(token: String, sequence: Long): StopPacket {
            return StopPacket(
                token = token,
                timestamp = System.currentTimeMillis(),
                sequence = sequence,
                status = "request"
            )
        }

        /** 客户端确认收到服务器响应 */
        fun received(token: String, sequence: Long): StopPacket {
            return StopPacket(
                token = token,
                timestamp = System.currentTimeMillis(),
                sequence = sequence,
                status = "received"
            )
        }
    }
}
