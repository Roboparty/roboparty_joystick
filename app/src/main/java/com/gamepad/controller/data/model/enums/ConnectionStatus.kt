// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model.enums

/**
 * 机器人连接状态
 *
 * 对应设计文档 5.1 连接状态机 + 心跳保活
 */
enum class ConnectionStatus {
    /** 未连接 */
    DISCONNECTED,

    /** 连接中（握手阶段） */
    CONNECTING,

    /** 已连接，正常通信 */
    CONNECTED,

    /** 重连中（断线后自动重连） */
    RECONNECTING,

    /** 连接已丢失 */
    LOST,

    /** 连接失败（不可恢复） */
    FAILED;

    val isConnected: Boolean get() = this == CONNECTED
    val isAlive: Boolean get() = this == CONNECTED || this == RECONNECTING

    val displayName: String
        get() = when (this) {
            DISCONNECTED -> "未连接"
            CONNECTING -> "连接中..."
            CONNECTED -> "已连接"
            RECONNECTING -> "重连中..."
            LOST -> "连接断开"
            FAILED -> "连接失败"
        }
}
