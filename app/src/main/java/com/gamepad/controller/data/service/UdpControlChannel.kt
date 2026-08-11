package com.gamepad.controller.data.service

import android.content.Context
import android.util.Log
import com.gamepad.controller.data.config.ConnectionConfig
import com.gamepad.controller.data.model.BatteryData
import com.gamepad.controller.data.model.enums.ConnectionStatus
import com.gamepad.controller.data.model.packet.AuthRequest
import com.gamepad.controller.data.model.packet.ChallengeResponse
import com.gamepad.controller.data.model.packet.ControlPacket
import com.gamepad.controller.data.model.packet.ServerMessage
import com.gamepad.controller.domain.service.ControlChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * UDP 控制通道实现 — 三步认证 + 双向通信
 *
 * 认证流程：
 * 1. 客户端发送 AuthRequest（设备ID）
 * 2. 服务器下发 Challenge（挑战码）
 * 3. 客户端计算 HMAC-SHA256 签名并发送 ChallengeResponse
 * 4. 服务器验证后返回 AuthResult（Token）
 *
 * 设计要点：
 * - 使用 Java DatagramSocket（无连接，低延迟）
 * - 发送队列容量 = 1：AtomicReference 存最新帧，发送线程循环取
 * - 独立接收线程处理服务器消息
 * - 心跳保活 + 断线自动重连
 */
class UdpControlChannel(
    private val context: Context,
    private val config: ConnectionConfig
) : ControlChannel {

    companion object {
        private const val TAG = "UdpControl"
        private const val SEND_INTERVAL_MS = 20L // 50Hz 发送间隔
    }

    private val jsonParser = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true  // 忽略服务器返回的未知字段
        isLenient = true          // 宽松解析模式
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ==================== 认证状态 ====================

    private enum class AuthState {
        IDLE,               // 未开始
        WAITING_CHALLENGE,  // 已发送 AuthRequest，等待 Challenge
        WAITING_RESULT,     // 已发送 ChallengeResponse，等待 AuthResult
        AUTHENTICATED       // 认证成功
    }

    private var authState = AuthState.IDLE
    private var pendingChallenge: String? = null
    private var authCompletable = CompletableDeferred<Boolean>()

    // ==================== 连接状态 ====================

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    override val token: StateFlow<String?> = _token.asStateFlow()

    // Token 过期时间
    private var tokenExpiresAt: Long = 0L

    // Token 有效性状态
    private val _isTokenValid = MutableStateFlow(false)
    override val isTokenValid: StateFlow<Boolean> = _isTokenValid.asStateFlow()

    /**
     * 检查 Token 是否有效
     */
    override fun checkTokenValidity(): Boolean {
        if (_token.value == null) {
            _isTokenValid.value = false
            return false
        }
        if (tokenExpiresAt > 0 && System.currentTimeMillis() >= tokenExpiresAt) {
            Log.w(TAG, "Token expired at $tokenExpiresAt")
            _token.value = null
            _isTokenValid.value = false
            return false
        }
        _isTokenValid.value = true
        return true
    }

    // ==================== 发送队列（容量 = 1）====================

    private val pendingFrame = AtomicReference<String?>(null)

    // ==================== Socket ====================

    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null
    private var targetPort: Int = 0
    private var targetHost: String = ""

    /** 发送线程 */
    @Volatile
    private var isSending = false
    private var sendThread: Thread? = null
    private var recvThread: Thread? = null

    /** 心跳相关 */
    private var lastHeartbeatTime = 0L

    // ==================== 连接管理 ====================

    override suspend fun connect(host: String, port: Int): Unit = withContext(Dispatchers.IO) {
        try {
            targetHost = host
            targetPort = port
            targetAddress = InetAddress.getByName(host)

            _connectionStatus.value = ConnectionStatus.CONNECTING

            // 创建 Socket
            socket = DatagramSocket()
            socket?.soTimeout = 1000 // 1秒超时，用于接收线程检查心跳

            // 启动接收线程
            startRecvLoop()

            // 执行三步认证
            if (performAuth()) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _isConnected.value = true

                // 启动发送循环
                isSending = true
                startSendLoop()

                // 启动心跳
                startHeartbeat()

                Log.i(TAG, "Connected and authenticated: $host:$port")
            } else {
                _connectionStatus.value = ConnectionStatus.FAILED
                cleanup()
                throw Exception("Authentication failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed: ${e.message}", e)
            _connectionStatus.value = ConnectionStatus.FAILED
            cleanup()
            throw e
        }
    }

    override fun disconnect() {
        isSending = false
        sendThread?.interrupt()
        recvThread?.interrupt()
        cleanup()
        _isConnected.value = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _token.value = null
        tokenExpiresAt = 0L
        _isTokenValid.value = false
        Log.i(TAG, "Disconnected")
    }

    // ==================== 三步认证 ====================

    /**
     * 执行三步认证流程
     *
     * @return 认证是否成功
     */
    private suspend fun performAuth(): Boolean {
        val deviceId = getOrCreateDeviceId()

        repeat(config.auth.retryCount) { attempt ->
            authState = AuthState.IDLE
            authCompletable = CompletableDeferred()

            // ① 发送认证请求
            val request = AuthRequest(deviceId = deviceId)
            sendUdpImmediate(jsonParser.encodeToString(request))
            authState = AuthState.WAITING_CHALLENGE
            Log.d(TAG, "Auth attempt ${attempt + 1}: sent AuthRequest")

            // 等待认证完成（带超时）
            val result = withTimeoutOrNull(config.auth.timeoutMs) {
                authCompletable.await()
            }

            if (result == true) {
                Log.i(TAG, "Authentication successful, token received")
                return true
            }
            Log.w(TAG, "Auth attempt ${attempt + 1} failed or timeout")
        }
        return false
    }

    /**
     * 电池数据回调
     */
    var onBatteryDataReceived: ((BatteryData) -> Unit)? = null

    /**
     * 技能动作确认回调
     */
    var onSkillActionAckReceived: ((String) -> Unit)? = null
    var onSkillActionAckCompleted: ((String, String, Long) -> Unit)? = null

    /**
     * 处理服务器消息（接收线程调用）
     */
    private fun handleServerMessage(data: String) {
        try {
            val msg = jsonParser.decodeFromString<ServerMessage>(data)
            when (msg.type) {
                "challenge" -> handleChallenge(msg)
                "auth_result" -> handleAuthResult(msg)
                "heartbeat" -> handleHeartbeatResponse(msg)
                "battery" -> handleBatteryData(msg)
                "skill_action_ack_received" -> handleSkillActionAckReceived(msg)
                "skill_action_ack_completed" -> handleSkillActionAckCompleted(msg)
                else -> Log.w(TAG, "Unknown message type: ${msg.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse server message: ${e.message}")
        }
    }

    /**
     * 处理挑战码
     */
    private fun handleChallenge(msg: ServerMessage) {
        if (authState != AuthState.WAITING_CHALLENGE) {
            Log.w(TAG, "Unexpected challenge, state=$authState")
            return
        }

        val challengeCode = msg.challengeCode ?: return
        val deviceId = getOrCreateDeviceId()

        // 计算签名
        val signature = CryptoHelper.computeSignature(deviceId, challengeCode)

        // ③ 发送挑战响应
        val response = ChallengeResponse(deviceId = deviceId, signature = signature)
        sendUdpImmediate(jsonParser.encodeToString(response))
        authState = AuthState.WAITING_RESULT
        Log.d(TAG, "Sent ChallengeResponse")
    }

    /**
     * 处理认证结果
     */
    private fun handleAuthResult(msg: ServerMessage) {
        if (authState != AuthState.WAITING_RESULT) {
            Log.w(TAG, "Unexpected auth_result, state=$authState")
            return
        }

        if (msg.ok == true && msg.token != null) {
            _token.value = msg.token
            tokenExpiresAt = msg.expiresAt ?: 0L
            _isTokenValid.value = true
            authState = AuthState.AUTHENTICATED
            authCompletable.complete(true)
            Log.i(TAG, "Auth success, token: ${msg.token.take(10)}..., expires at: $tokenExpiresAt")
        } else {
            Log.w(TAG, "Auth failed: ${msg.reason}")
            authCompletable.complete(false)
        }
    }

    /**
     * 处理心跳响应
     */
    @Suppress("UNUSED_PARAMETER")
    private fun handleHeartbeatResponse(msg: ServerMessage) {
        lastHeartbeatTime = System.currentTimeMillis()
        Log.v(TAG, "Heartbeat received")
    }

    /**
     * 处理电池数据
     */
    private fun handleBatteryData(msg: ServerMessage) {
        val batteryData = BatteryData(
            voltage = msg.voltage ?: 0f,
            current = msg.current ?: 0f,
            soc = msg.soc ?: 0f,
            temp = msg.temp ?: 0f
        )
        Log.i(TAG, "Battery data: voltage=${batteryData.voltage}V, soc=${batteryData.soc}%, temp=${batteryData.temp}°C")
        onBatteryDataReceived?.invoke(batteryData)
    }

    /**
     * 处理技能动作接收确认（ACK1）
     */
    private fun handleSkillActionAckReceived(msg: ServerMessage) {
        val requestId = msg.requestId ?: return
        Log.i(TAG, "Skill action ACK1 received: $requestId")
        onSkillActionAckReceived?.invoke(requestId)
    }

    /**
     * 处理技能动作执行完成（ACK2）
     */
    private fun handleSkillActionAckCompleted(msg: ServerMessage) {
        val requestId = msg.requestId ?: return
        val status = msg.status ?: "unknown"
        val executionTime = msg.executionTimeMs ?: 0
        Log.i(TAG, "Skill action ACK2 received: $requestId, status=$status, time=${executionTime}ms")
        onSkillActionAckCompleted?.invoke(requestId, status, executionTime)
    }

    // ==================== 接收线程 ====================

    private fun startRecvLoop() {
        recvThread = Thread({
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val pkt = DatagramPacket(buffer, buffer.size)
                    socket?.receive(pkt) // 阻塞等待
                    val data = String(pkt.data, 0, pkt.length, Charsets.UTF_8)
                    handleServerMessage(data)
                } catch (_: SocketTimeoutException) {
                    // 超时检查心跳
                    if (isSending) {
                        checkHeartbeatTimeout()
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    if (isSending) {
                        Log.w(TAG, "Recv error: ${e.message}")
                    }
                }
            }
        }, "udp-control-receiver")
        recvThread?.isDaemon = true
        recvThread?.start()
    }

    // ==================== 心跳保活 ====================

    private fun startHeartbeat() {
        lastHeartbeatTime = System.currentTimeMillis()
        scope.launch {
            while (isSending) {
                try {
                    val hb = buildJsonObject {
                        put("type", "heartbeat")
                        put("token", _token.value ?: "")
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendUdpImmediate(hb.toString())
                    delay(config.auth.heartbeatIntervalMs)
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat send error: ${e.message}")
                }
            }
        }
    }

    private fun checkHeartbeatTimeout() {
        if (lastHeartbeatTime == 0L) return
        val elapsed = System.currentTimeMillis() - lastHeartbeatTime
        if (elapsed > config.auth.heartbeatTimeoutMs) {
            Log.w(TAG, "Heartbeat timeout ($elapsed ms), connection lost")
            handleConnectionLost()
        }
    }

    // ==================== 自动重连 ====================

    private fun handleConnectionLost() {
        if (_connectionStatus.value == ConnectionStatus.RECONNECTING) return

        _connectionStatus.value = ConnectionStatus.LOST
        _isConnected.value = false
        isSending = false
        sendThread?.interrupt()

        scope.launch {
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            Log.i(TAG, "Starting reconnect...")

            repeat(config.auth.reconnectMaxCount) { attempt ->
                Log.i(TAG, "Reconnect attempt ${attempt + 1}/${config.auth.reconnectMaxCount}")
                delay(config.auth.reconnectDelayMs)

                try {
                    // 重新创建 Socket
                    socket?.close()
                    socket = DatagramSocket()
                    socket?.soTimeout = 1000

                    // 重新认证
                    if (performAuth()) {
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        _isConnected.value = true
                        isSending = true
                        startSendLoop()
                        startHeartbeat()
                        Log.i(TAG, "Reconnected successfully")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Reconnect attempt ${attempt + 1} failed: ${e.message}")
                }
            }

            _connectionStatus.value = ConnectionStatus.FAILED
            Log.e(TAG, "Reconnect failed after ${config.auth.reconnectMaxCount} attempts")
        }
    }

    // ==================== 指令发送 ====================

    override fun sendControl(packet: ControlPacket) {
        if (!_isConnected.value) return
        if (!checkTokenValidity()) {
            Log.w(TAG, "Token invalid, skipping control packet")
            return
        }

        // 携带 Token
        val packetWithToken = packet.copy(token = _token.value ?: "")

        // 序列化为 JSON，存入 pendingFrame（覆盖旧帧）
        val data = jsonParser.encodeToString(packetWithToken)
        Log.d(TAG, "OUT [${packetWithToken.type}]: seq=${packetWithToken.sequence}, lx=${packetWithToken.leftStickX}, ly=${packetWithToken.leftStickY}")
        pendingFrame.set(data)
    }

    override fun sendEstop(packet: ControlPacket) {
        if (!_isConnected.value) return
        // 急停指令不检查 token 有效性，始终发送
        Log.w(TAG, "ESTOP: bypassing token check")

        // 急停帧优先：立即同步发送，不走队列
        val packetWithToken = packet.copy(token = _token.value ?: "")
        val data = jsonParser.encodeToString(packetWithToken)
        Log.w(TAG, "OUT [${packetWithToken.type}]: ESTOP packet sent")
        sendUdpImmediate(data)
    }

    override fun sendJson(json: String) {
        if (!_isConnected.value) return
        if (!checkTokenValidity()) {
            Log.w(TAG, "Token invalid, skipping json packet")
            return
        }
        // 从 JSON 中提取 type 用于日志
        val type = try {
            jsonParser.parseToJsonElement(json).jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
        Log.d(TAG, "OUT [$type]: $json")
        sendUdpImmediate(json)
    }

    // ==================== 发送循环 ====================

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

    // ==================== 设备 ID 管理 ====================

    /**
     * 获取或生成设备唯一标识
     *
     * 使用 SharedPreferences 持久化，确保应用重启后保持一致
     */
    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        val existing = prefs.getString("device_id", null)
        if (existing != null) return existing

        val newId = "android-${UUID.randomUUID().toString().take(8)}"
        prefs.edit().putString("device_id", newId).apply()
        Log.i(TAG, "Generated new device ID: $newId")
        return newId
    }

    // ==================== 清理 ====================

    private fun cleanup() {
        pendingFrame.set(null)
        socket?.close()
        socket = null
        targetAddress = null
        authState = AuthState.IDLE
        lastHeartbeatTime = 0L
    }

    override fun destroy() {
        disconnect()
    }
}
