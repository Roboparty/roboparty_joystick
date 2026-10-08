// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.domain.model

/**
 * 动作请求状态
 *
 * 表示动作从发送到执行完成的各个阶段
 */
sealed class ActionRequestState {
    /** 空闲状态 */
    object Idle : ActionRequestState()

    /** 发送中 - 等待服务器接收确认 */
    object Sending : ActionRequestState()

    /** 执行中 - 服务器已收到，正在执行动作 */
    data class Executing(val estimatedDuration: Long) : ActionRequestState()

    /** 执行成功 */
    data class Success(val executionTime: Long) : ActionRequestState()

    /** 执行失败 */
    data class Failed(val reason: String) : ActionRequestState()

    /** 超时 - 服务器未返回执行结果 */
    data class Timeout(val actionId: String) : ActionRequestState()
}
