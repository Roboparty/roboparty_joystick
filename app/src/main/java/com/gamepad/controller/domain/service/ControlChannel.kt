package com.gamepad.controller.domain.service

import com.gamepad.controller.data.model.packet.ControlPacket
import kotlinx.coroutines.flow.StateFlow

/**
 * 控制通道接口 — UDP 实现
 *
 * 对应设计文档 4.3 双通道架构中的控制通道
 *
 * 职责：
 * - 高频发送摇杆/速度/按键数据（30~50Hz）
 * - UDP 语义：丢了就丢，不重传，下一帧覆盖
 * - 发送队列容量=1，新帧覆盖旧帧
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
     * 连接到机器人控制端口
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
     */
    fun sendControl(packet: ControlPacket)

    /**
     * 发送急停指令（最高优先级）
     *
     * @param packet 带 estop=true 的控制包
     */
    fun sendEstop(packet: ControlPacket)

    /**
     * 释放资源
     */
    fun destroy()
}
