package com.gamepad.controller.data.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.gamepad.controller.data.config.ConnectionConfig
import com.gamepad.controller.data.model.BatteryData
import com.gamepad.controller.data.model.BleDevice
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference

/**
 * BLE 控制通道实现
 *
 * 通过 BLE 传输控制指令和接收服务器数据
 */
class BleControlChannel(
    private val context: Context,
    private val config: ConnectionConfig
) : ControlChannel {

    companion object {
        private const val TAG = "BleControl"
        private const val SEND_INTERVAL_MS = 20L // 50Hz 发送间隔

        // 分包协议常量
        private const val PACKET_HEADER_1: Byte = 0xAA.toByte()
        private const val PACKET_HEADER_2: Byte = 0x55
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bleManager = BleConnectionManager(context)

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

    // ==================== 发送队列 ====================

    private val pendingFrame = AtomicReference<String?>(null)
    private var isSending = false
    private var sendJob: kotlinx.coroutines.Job? = null

    // ==================== 认证状态 ====================

    private var authCompletable = CompletableDeferred<Boolean>()

    // ==================== 数据缓冲 ====================

    private val receiveBuffer = StringBuilder()

    // ==================== 初始化 ====================

    init {
        // 设置 BLE 数据接收回调
        bleManager.onDataReceived = { data ->
            handleReceivedData(data)
        }

        // 设置连接状态回调
        bleManager.onConnectionStateChanged = { connected ->
            if (!connected && _isConnected.value) {
                handleConnectionLost()
            }
        }
    }

    // ==================== 扫描 ====================

    fun startScan() {
        bleManager.startScan(config.ble.scanTimeoutMs)
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun getDiscoveredDevices(): StateFlow<List<BleDevice>> {
        return bleManager.discoveredDevices
    }

    fun isScanning(): StateFlow<Boolean> {
        return bleManager.isScanning
    }

    // ==================== 连接 ====================

    @SuppressLint("MissingPermission")
    override suspend fun connect(host: String, port: Int): Unit = withContext(Dispatchers.IO) {
        // BLE 连接不使用 host/port，需要使用 connectBle 方法
        throw UnsupportedOperationException("BLE connection requires BluetoothDevice, use connectBle() instead")
    }

    @SuppressLint("MissingPermission")
    suspend fun connectBle(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        try {
            _connectionStatus.value = ConnectionStatus.CONNECTING

            // 连接 BLE 设备
            bleManager.connect(device)

            // 等待连接建立（最多 10 秒）
            val connected = withTimeoutOrNull(10000) {
                while (!bleManager.isConnected()) {
                    delay(100)
                }
                true
            }

            if (connected != true) {
                throw Exception("BLE connection timeout")
            }

            // 等待 MTU 协商完成
            delay(1000)

            // 执行三步认证
            if (performAuth()) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _isConnected.value = true

                // 启动发送循环
                isSending = true
                startSendLoop()

                Log.i(TAG, "BLE connected and authenticated")
            } else {
                _connectionStatus.value = ConnectionStatus.FAILED
                bleManager.disconnect()
                throw Exception("Authentication failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "BLE connect failed: ${e.message}", e)
            _connectionStatus.value = ConnectionStatus.FAILED
            _isConnected.value = false
            throw e
        }
    }

    override fun disconnect() {
        isSending = false
        sendJob?.cancel()
        bleManager.disconnect()
        _isConnected.value = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _token.value = null
        tokenExpiresAt = 0L
        _isTokenValid.value = false
        receiveBuffer.clear()
        Log.i(TAG, "BLE disconnected")
    }

    // ==================== 认证 ====================

    private suspend fun performAuth(): Boolean {
        val deviceId = getOrCreateDeviceId()

        repeat(config.auth.retryCount) { attempt ->
            authCompletable = CompletableDeferred()

            // 发送认证请求
            val request = AuthRequest(deviceId = deviceId)
            sendJsonData(json.encodeToString(request))
            Log.d(TAG, "Auth attempt ${attempt + 1}: sent AuthRequest")

            // 等待认证完成
            val result = withTimeoutOrNull(config.auth.timeoutMs) {
                authCompletable.await()
            }

            if (result == true) {
                Log.i(TAG, "Authentication successful")
                return true
            }
            Log.w(TAG, "Auth attempt ${attempt + 1} failed or timeout")
        }
        return false
    }

    // ==================== 数据接收 ====================

    private fun handleReceivedData(data: ByteArray) {
        try {
            // 将字节数据转换为字符串
            val text = String(data, Charsets.UTF_8)
            receiveBuffer.append(text)

            // 检查是否收到完整消息（假设以换行符结尾或尝试解析）
            val bufferContent = receiveBuffer.toString()
            if (bufferContent.contains("\n") || isValidJson(bufferContent)) {
                receiveBuffer.clear()
                handleServerMessage(bufferContent.trim())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling received data: ${e.message}")
        }
    }

    private fun isValidJson(text: String): Boolean {
        return try {
            json.decodeFromString<ServerMessage>(text)
            true
        } catch (_: Exception) {
            false
        }
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

    private fun handleServerMessage(data: String) {
        try {
            val msg = json.decodeFromString<ServerMessage>(data)
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

    private fun handleChallenge(msg: ServerMessage) {
        val challengeCode = msg.challengeCode ?: return
        val deviceId = getOrCreateDeviceId()

        // 计算签名
        val signature = CryptoHelper.computeSignature(deviceId, challengeCode)

        // 发送挑战响应
        val response = ChallengeResponse(deviceId = deviceId, signature = signature)
        scope.launch {
            sendJsonData(json.encodeToString(response))
        }
        Log.d(TAG, "Sent ChallengeResponse")
    }

    private fun handleAuthResult(msg: ServerMessage) {
        if (msg.ok == true && msg.token != null) {
            _token.value = msg.token
            tokenExpiresAt = msg.expiresAt ?: 0L
            _isTokenValid.value = true
            authCompletable.complete(true)
            Log.i(TAG, "Auth success, token received, expires at: $tokenExpiresAt")
        } else {
            Log.w(TAG, "Auth failed: ${msg.reason}")
            authCompletable.complete(false)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleHeartbeatResponse(msg: ServerMessage) {
        // 更新心跳时间
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

    // ==================== 数据发送 ====================

    override fun sendControl(packet: ControlPacket) {
        if (!_isConnected.value) return
        if (!checkTokenValidity()) {
            Log.w(TAG, "Token invalid, skipping control packet")
            return
        }

        val packetWithToken = packet.copy(token = _token.value ?: "")
        val data = json.encodeToString(packetWithToken)
        Log.d(TAG, "OUT [${packetWithToken.type}]: seq=${packetWithToken.sequence}, lx=${packetWithToken.leftStickX}, ly=${packetWithToken.leftStickY}")
        pendingFrame.set(data)
    }

    override fun sendEstop(packet: ControlPacket) {
        if (!_isConnected.value) return
        // 急停指令不检查 token 有效性，始终发送
        Log.w(TAG, "ESTOP: bypassing token check")

        val packetWithToken = packet.copy(token = _token.value ?: "")
        val data = json.encodeToString(packetWithToken)
        Log.w(TAG, "OUT [${packetWithToken.type}]: ESTOP packet sent")
        scope.launch {
            sendJsonData(data)
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun sendJson(jsonStr: String) {
        if (!_isConnected.value) return
        if (!checkTokenValidity()) {
            Log.w(TAG, "Token invalid, skipping json packet")
            return
        }
        // 从 JSON 中提取 type 用于日志
        val type = try {
            json.parseToJsonElement(jsonStr).jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
        Log.d(TAG, "OUT [$type]: $jsonStr")
        scope.launch {
            sendJsonData(jsonStr)
        }
    }

    private fun startSendLoop() {
        sendJob = scope.launch {
            while (isSending) {
                try {
                    val frame = pendingFrame.getAndSet(null)
                    if (frame != null) {
                        sendJsonData(frame)
                    }
                    delay(SEND_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Send error: ${e.message}")
                }
            }
        }
    }

    private fun sendJsonData(jsonData: String) {
        val data = jsonData.toByteArray(Charsets.UTF_8)
        val mtu = bleManager.mtu.value

        if (data.size <= mtu - 3) {
            // 单包发送
            bleManager.sendData(data)
        } else {
            // 分包发送
            val packets = splitPacket(data, mtu - 3)
            packets.forEach { packet ->
                bleManager.sendData(packet)
                // 短暂延迟避免缓冲区溢出
                Thread.sleep(5)
            }
        }
    }

    // ==================== 分包协议 ====================

    /**
     * 将数据分包
     *
     * 包格式：
     * | 包头 (2字节) | 包序号 (1字节) | 总包数 (1字节) | 数据 (变长) |
     * | 0xAA 0x55   | 0x01          | 0x03          | {...}       |
     */
    private fun splitPacket(data: ByteArray, maxDataSize: Int): List<ByteArray> {
        val headerSize = 4 // 包头大小
        val maxPayloadSize = maxDataSize - headerSize
        val totalPackets = (data.size + maxPayloadSize - 1) / maxPayloadSize
        val packets = mutableListOf<ByteArray>()

        for (i in 0 until totalPackets) {
            val offset = i * maxPayloadSize
            val length = minOf(maxPayloadSize, data.size - offset)

            val packet = ByteBuffer.allocate(headerSize + length).apply {
                order(ByteOrder.BIG_ENDIAN)
                put(PACKET_HEADER_1)
                put(PACKET_HEADER_2)
                put(i.toByte())
                put(totalPackets.toByte())
                put(data, offset, length)
            }.array()

            packets.add(packet)
        }

        return packets
    }

    // ==================== 连接丢失处理 ====================

    private fun handleConnectionLost() {
        Log.w(TAG, "BLE connection lost")
        _connectionStatus.value = ConnectionStatus.LOST
        _isConnected.value = false
        isSending = false
        sendJob?.cancel()

        // 可以在这里添加自动重连逻辑
    }

    // ==================== 设备 ID 管理 ====================

    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
        val existing = prefs.getString("device_id", null)
        if (existing != null) return existing

        val newId = "android-${java.util.UUID.randomUUID().toString().take(8)}"
        prefs.edit().putString("device_id", newId).apply()
        Log.i(TAG, "Generated new device ID: $newId")
        return newId
    }

    // ==================== 清理 ====================

    override fun destroy() {
        disconnect()
        bleManager.destroy()
    }
}
