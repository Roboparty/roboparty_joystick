// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.domain.service

import com.gamepad.controller.data.model.DeviceIdentity
import com.gamepad.controller.data.model.enums.ConnectionStatus
import com.gamepad.controller.data.model.packet.ControlPacket
import kotlinx.coroutines.flow.StateFlow

/**
 * 控制通道接口 — WSS/JSON 实现（rp_server /ws-json）
 *
 * 职责：
 * - App 配对密钥挑战-响应认证（ECDSA-P256）
 * - 控制租约（lease.acquire/heartbeat）
 * - 高频发送摇杆/速度/按键数据（50Hz）
 * - 心跳保活与断线重连
 */
interface ControlChannel {

    /**
     * 通道连接状态
     */
    val isConnected: StateFlow<Boolean>

    /**
     * 详细连接状态（用于 UI 展示）
     */
    val connectionStatus: StateFlow<ConnectionStatus>

    /**
     * 认证 Token（挑战-响应通过后获取）
     */
    val token: StateFlow<String?>

    /**
     * Token 是否有效（未过期）
     */
    val isTokenValid: StateFlow<Boolean>

    /**
     * 检查 Token 是否有效
     *
     * @return true 如果 token 存在且未过期
     */
    fun checkTokenValidity(): Boolean

    /** 首次连接发现的新机器人身份，等待用户确认绑定。 */
    val pendingPairing: StateFlow<DeviceIdentity?>

    /** 确认绑定当前待确认的机器人。 */
    fun confirmPairing(): Boolean

    /** 取消本次绑定。 */
    fun rejectPairing()

    /**
     * 连接到机器人 /ws-json
     *
     * 1. 读取 /identity 并校验 SN/设备密钥绑定
     * 2. auth_request → challenge → challenge_response（ECDSA-P256）
     * 3. auth_result 保存 JWT
     * 4. lease.acquire 获取控制权
     *
     * @param host 机器人 IP 地址
     * @param port rp_server 端口（默认 9000）
     */
    suspend fun connect(host: String, port: Int)

    /**
     * 断开 WebSocket 连接
     */
    fun disconnect()

    /**
     * 发送控制指令（50Hz）
     *
     * 将 ControlPacket 序列化为 JSON，自动携带认证 Token。
     */
    fun sendControl(packet: ControlPacket)

    /**
     * 发送软件停止并锁存 — 三步确认协议
     *
     * 1. 发送 stop request
     * 2. 等待服务器返回 accepted
     * 3. 发送 stop received
     *
     * @return 是否收到服务器确认
     */
    suspend fun sendEstop(): Boolean

    /**
     * 清除服务器端锁存的软件停止，并重新申请控制权。
     *
     * @return 是否清除并重新获得控制权
     */
    suspend fun clearEstop(): Boolean = false

    /**
     * 发送原始JSON数据
     *
     * 用于发送技能动作等非控制指令
     *
     * @param json JSON字符串
     */
    fun sendJson(json: String)

    /**
     * 释放资源
     */
    fun destroy()
}
