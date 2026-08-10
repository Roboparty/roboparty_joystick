package com.gamepad.controller.domain.service

import com.gamepad.controller.data.model.enums.ConnectionStatus
import com.gamepad.controller.data.model.packet.ControlPacket
import kotlinx.coroutines.flow.StateFlow

/**
 * 控制通道接口 — UDP 实现
 *
 * 对应设计文档 4.3 双通道架构中的控制通道
 *
 * 职责：
 * - 三步认证（AuthRequest → Challenge → ChallengeResponse → AuthResult）
 * - 高频发送摇杆/速度/按键数据（30~50Hz）
 * - UDP 语义：丢了就丢，不重传，下一帧覆盖
 * - 发送队列容量=1，新帧覆盖旧帧
 * - 心跳保活
 * - 断线自动重连
 *
 * 为何选 UDP 而非 TCP/WebSocket：
 * - 无队头阻塞（TCP 丢一个包会阻塞后续所有包）
 * - 不重传过期帧（50Hz 摇杆每帧 20ms，100ms 前的旧速度毫无意义）
 * - P99 尾部延迟显著优于 TCP
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
     * 认证 Token（三步认证成功后获取）
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

    /**
     * 连接到机器人控制端口
     *
     * 执行三步认证流程：
     * 1. 发送 AuthRequest
     * 2. 接收 Challenge
     * 3. 计算 HMAC-SHA256 签名并发送 ChallengeResponse
     * 4. 接收 AuthResult，保存 Token
     *
     * @param host 机器人 IP 地址
     * @param port UDP 控制端口（机器人端监听）
     */
    suspend fun connect(host: String, port: Int)

    /**
     * 断开 UDP 连接
     */
    fun disconnect()

    /**
     * 发送控制指令（高频）
     *
     * 实现要点：
     * - 将 ControlPacket 序列化为 JSON 字节
     * - 通过 UDP 发送，不保证到达
     * - 新帧自动覆盖未发送的旧帧（发送队列容量=1）
     * - 自动携带认证 Token
     */
    fun sendControl(packet: ControlPacket)

    /**
     * 发送急停指令（最高优先级）
     *
     * @param packet 带 estop=true 的控制包
     */
    fun sendEstop(packet: ControlPacket)

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
