package com.gamepad.controller.data.service

import android.util.Log
import com.gamepad.controller.data.model.packet.ControlPacket
import com.gamepad.controller.domain.service.ControlChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * UDP 控制通道实现
 *
 * 对应设计文档 4.3 控制通道
 *
 * 设计要点：
 * - 使用 Java DatagramSocket（无连接，低延迟）
 * - 发送队列容量 = 1：AtomicReference 存最新帧，发送线程循环取
 * - 新帧覆盖未发送旧帧，绝不堆积
 * - 每帧序列化为 JSON + 时间戳用于机器人端过期丢弃
 *
 * 为何不用 OkHttp/NIO：
 * - UDP 控制只需要简单的 sendto，不需要连接管理
 * - DatagramSocket 足够轻量，无额外依赖
 */
class UdpControlChannel : ControlChannel {

    companion object {
        private const val TAG = "UdpControl"
        private const val SEND_INTERVAL_MS = 20L // 50Hz 发送间隔
    }

    private val json = Json { encodeDefaults = true }

    // ==================== 连接状态 ====================

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ==================== 发送队列（容量 = 1）====================

    /**
     * 最新待发送帧（AtomicReference 实现覆盖语义）
     * null 表示队列为空
     */
    private val pendingFrame = AtomicReference<String?>(null)

    // ==================== Socket ====================

    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null
    private var targetPort: Int = 0

    /** 发送线程 */
    @Volatile
    private var isSending = false
    private var sendThread: Thread? = null

    // ==================== 连接管理 ====================

    override suspend fun connect(host: String, port: Int): Unit = withContext(Dispatchers.IO) {
        try {
            socket = DatagramSocket()
            targetAddress = InetAddress.getByName(host)
            targetPort = port

            isSending = true
            startSendLoop()

            _isConnected.value = true
            Log.i(TAG, "UDP control channel connected: $host:$port")
        } catch (e: Exception) {
            Log.e(TAG, "UDP connect failed: ${e.message}", e)
            cleanup()
            throw e
        }
    }

    override fun disconnect() {
        isSending = false
        sendThread?.interrupt()
        cleanup()
        _isConnected.value = false
        Log.i(TAG, "UDP control channel disconnected")
    }

    // ==================== 指令发送 ====================

    override fun sendControl(packet: ControlPacket) {
        if (!_isConnected.value) return

        // 序列化为 JSON，存入 pendingFrame（覆盖旧帧）
        val data = json.encodeToString(packet)
        pendingFrame.set(data)
    }

    override fun sendEstop(packet: ControlPacket) {
        if (!_isConnected.value) return

        // 急停帧优先：立即同步发送，不走队列
        val data = json.encodeToString(packet)
        sendUdpImmediate(data)
    }

    // ==================== 发送循环 ====================

    /**
     * 后台发送线程：每 SEND_INTERVAL_MS 从 pendingFrame 取最新帧发送
     * 如果队列为空则跳过（不发送空包）
     */
    private fun startSendLoop() {
        sendThread = Thread({
            while (isSending) {
                try {
                    val frame = pendingFrame.getAndSet(null) // 取出并清空
                    if (frame != null) {
                        sendUdpImmediate(frame)
                    }
                    Thread.sleep(SEND_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "UDP send error: ${e.message}")
                }
            }
        }, "udp-control-sender")
        sendThread?.isDaemon = true
        sendThread?.start()
    }

    /**
     * 立即发送一帧 UDP 数据
     */
    private fun sendUdpImmediate(data: String) {
        try {
            val bytes = data.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(bytes, bytes.size, targetAddress, targetPort)
            socket?.send(packet)
        } catch (e: Exception) {
            Log.w(TAG, "UDP send failed: ${e.message}")
        }
    }

    // ==================== 清理 ====================

    private fun cleanup() {
        pendingFrame.set(null)
        socket?.close()
        socket = null
        targetAddress = null
    }

    override fun destroy() {
        disconnect()
    }
}
