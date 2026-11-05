// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.service

import com.gamepad.controller.data.auth.AppKeyStore
import com.gamepad.controller.data.auth.RobotBindingStore
import com.gamepad.controller.data.auth.SpkiPinTrustManager
import com.gamepad.controller.data.auth.IdentityClient

import android.content.Context
import android.os.Build
import android.util.Log
import com.gamepad.controller.data.config.ConnectionConfig
import com.gamepad.controller.data.model.BatteryData
import com.gamepad.controller.data.model.DeviceIdentity
import com.gamepad.controller.data.model.ImuData
import com.gamepad.controller.data.model.MotorStatusResponse
import com.gamepad.controller.data.model.enums.ConnectionStatus
import com.gamepad.controller.data.model.packet.ControlPacket
import com.gamepad.controller.domain.service.ControlChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

/**
 * JSON over WebSocket 控制通道（/ws-json），支持 WSS + SPKI pin。
 *
 * 认证：App 私钥（Keystore, EC P-256）对 `<appKeyId>:<challenge>` 做
 * SHA256withECDSA；服务端验签后下发 JWT。传输安全完全交给 TLS，不手搓加密。
 */
class WssJsonControlChannel(
    private val context: Context,
    private val config: ConnectionConfig
) : ControlChannel {

    companion object {
        private const val TAG = "WssJsonControl"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; isLenient = true }
    private val appKeyStore = AppKeyStore(context)
    private val bindingStore = RobotBindingStore(context)
    private val identityClient = IdentityClient(config.auth.timeoutMs.toInt())

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    private val _token = MutableStateFlow<String?>(null)
    override val token: StateFlow<String?> = _token.asStateFlow()
    private val _isTokenValid = MutableStateFlow(false)
    override val isTokenValid: StateFlow<Boolean> = _isTokenValid.asStateFlow()
    private val _pendingPairing = MutableStateFlow<DeviceIdentity?>(null)
    override val pendingPairing: StateFlow<DeviceIdentity?> = _pendingPairing.asStateFlow()

    private var tokenExpiresAt = 0L
    private var targetHost = ""
    private var targetPort = 0
    private var pendingPairingHost = ""
    private var targetTlsPin = ""
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var authCompletable = CompletableDeferred<Boolean>()
    private var leaseCompletable = CompletableDeferred<Boolean>()
    @Volatile private var hasControlLease = false
    @Volatile private var leaseRequested = false
    private var stopCompletable = CompletableDeferred<Boolean>()
    private var clearCompletable = CompletableDeferred<Boolean>()
    private var heartbeatJob: Job? = null
    private var lastHeartbeat = 0L

    var onBatteryDataReceived: ((BatteryData) -> Unit)? = null
    var onImuDataReceived: ((ImuData) -> Unit)? = null
    var onMotorDataReceived: ((MotorStatusResponse) -> Unit)? = null
    var onSkillActionAckReceived: ((String) -> Unit)? = null
    var onSkillActionAckCompleted: ((String, String, Long) -> Unit)? = null

    override fun checkTokenValidity(): Boolean {
        if (_token.value == null) {
            _isTokenValid.value = false
            return false
        }
        if (tokenExpiresAt > 0 && System.currentTimeMillis() >= tokenExpiresAt) {
            _isTokenValid.value = false
            return false
        }
        _isTokenValid.value = true
        return true
    }

    override suspend fun connect(host: String, port: Int): Unit = withContext(Dispatchers.IO) {
        cleanup()
        targetHost = host
        targetPort = port
        _connectionStatus.value = ConnectionStatus.CONNECTING
        try {
            verifyRobotIdentity(host, port)
            val appKeyId = appKeyStore.appKeyId()
            if (appKeyId.isBlank()) {
                throw IllegalStateException("尚未配对：请在设置→授权与绑定中粘贴配对 URI")
            }
            client = buildClient()
            val url = "wss://$host:$port/ws-json"
            authCompletable = CompletableDeferred()
            leaseCompletable = CompletableDeferred()
            webSocket = client?.newWebSocket(Request.Builder().url(url).build(), listener)
            val authenticated = withTimeoutOrNull(config.auth.timeoutMs.toLong() * 3) {
                authCompletable.await()
            } ?: false
            if (!authenticated) {
                throw IllegalStateException("WSS 认证失败（可能已被新设备替换或凭据已撤销）")
            }
            _connectionStatus.value = ConnectionStatus.CONNECTED
            _isConnected.value = true
            Log.i(TAG, "Connected (control lease deferred): $url")
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed: ${e.message}", e)
            _connectionStatus.value = ConnectionStatus.FAILED
            cleanup()
            throw e
        }
    }

    override fun disconnect() {
        cleanup()
        _isConnected.value = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _token.value = null
        tokenExpiresAt = 0L
        _isTokenValid.value = false
        _pendingPairing.value = null
        Log.i(TAG, "Disconnected")
    }

    private fun cleanup() {
        hasControlLease = false
        leaseRequested = false
        _isConnected.value = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "client disconnect")
        webSocket = null
        client = null
    }

    override fun sendControl(packet: ControlPacket) {
        val socket = webSocket ?: return
        if (!_isConnected.value) return
        if (!hasControlLease) {
            requestLease()
            return
        }
        socket.send(json.encodeToString(packet.copy(token = _token.value ?: "")))
    }

    private fun requestLease() {
        if (leaseRequested || hasControlLease) return
        leaseRequested = true
        scope.launch {
            try {
                ensureLease()
            } finally {
                leaseRequested = false
            }
        }
    }

    private suspend fun ensureLease(): Boolean {
        val socket = webSocket ?: return false
        if (hasControlLease) return true
        if (!_isConnected.value) return false
        leaseCompletable = CompletableDeferred()
        socket.send(buildJsonObject {
            put("type", "lease.acquire")
            put("token", _token.value ?: "")
        }.toString())
        val acquired = withTimeoutOrNull(config.auth.timeoutMs) { leaseCompletable.await() } ?: false
        if (acquired && hasControlLease) {
            startHeartbeat()
            return true
        }
        return false
    }

    override suspend fun sendEstop(): Boolean {
        val socket = webSocket ?: return false
        stopCompletable = CompletableDeferred()
        val requestId = "joy-${System.currentTimeMillis()}"
        socket.send(buildJsonObject {
            put("type", "stop")
            put("status", "request")
            put("request_id", requestId)
            put("token", _token.value ?: "")
            put("timestamp", System.currentTimeMillis())
        }.toString())
        val stopped = withTimeoutOrNull(config.auth.timeoutMs.toLong() * 3) {
            stopCompletable.await()
        } ?: false
        if (stopped) hasControlLease = false
        return stopped
    }

    override suspend fun clearEstop(): Boolean {
        val socket = webSocket ?: return false
        clearCompletable = CompletableDeferred()
        socket.send(buildJsonObject {
            put("type", "stop")
            put("status", "clear")
            put("token", _token.value ?: "")
            put("timestamp", System.currentTimeMillis())
        }.toString())
        val cleared = withTimeoutOrNull(config.auth.timeoutMs) { clearCompletable.await() } ?: false
        if (!cleared) return false

        leaseCompletable = CompletableDeferred()
        socket.send(buildJsonObject {
            put("type", "lease.acquire")
            put("token", _token.value ?: "")
        }.toString())
        val acquired = withTimeoutOrNull(config.auth.timeoutMs) { leaseCompletable.await() } ?: false
        if (acquired && hasControlLease) {
            lastHeartbeat = System.currentTimeMillis()
            startHeartbeat()
        }
        return acquired && hasControlLease
    }

    override fun sendJson(json: String) {
        if (!_isConnected.value) return
        if (!hasControlLease) {
            requestLease()
            return
        }
        webSocket?.send(json)
    }

    override fun confirmPairing(): Boolean {
        // v2 bindings are created only from a pairing URI (settings screen).
        _pendingPairing.value = null
        return false
    }

    override fun rejectPairing() {
        _pendingPairing.value = null
    }

    override fun destroy() {
        disconnect()
    }

    // ==================== 身份与绑定 ====================

    private suspend fun verifyRobotIdentity(host: String, port: Int): DeviceIdentity {
        val binding = bindingStore.load()
            ?: throw IllegalStateException("未完成安全配对：请在设置中粘贴配对 URI")
        if (binding.tlsSpkiSha256.isBlank()) {
            throw IllegalStateException("绑定缺少 TLS SPKI pin：请重新配对")
        }
        targetTlsPin = binding.tlsSpkiSha256
        val identity = try {
            identityClient.fetch(host, port, targetTlsPin)
        } catch (e: Exception) {
            throw IllegalStateException("无法在 pin 下读取机器人身份: ${e.message}")
        }
        if (identity.protocolVersion < 2 || "pair-pin-v2" !in identity.features) {
            throw IllegalStateException("机器人协议版本过旧，需要 v2 配对协议")
        }
        if (identity.tlsEnabled != true) {
            throw IllegalStateException("机器人未启用 TLS，拒绝非认证连接")
        }
        if (identity.tlsSpkiSha256.orEmpty() != binding.tlsSpkiSha256) {
            throw IllegalStateException("机器人 TLS 证书已变化；请重新配对")
        }
        val serial = identity.serial.trim()
        if (binding.serial.isNotEmpty() && serial.isNotEmpty() && binding.serial != serial) {
            throw IllegalStateException(
                "机器人 SN 已变化：${binding.serial} → $serial；请在设置中清除绑定后重新配对"
            )
        }
        val spki = identity.spkiSha256.orEmpty()
        if (binding.spkiSha256.isNotEmpty() && spki.isNotEmpty() && binding.spkiSha256 != spki) {
            throw IllegalStateException("机器人设备密钥已变化；请重新配对")
        }
        return identity
    }

    // ==================== TLS ====================

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.auth.timeoutMs, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
        if (targetTlsPin.isBlank()) {
            throw IllegalStateException("缺少已钉住的 TLS SPKI pin，拒绝连接")
        }
        val trust = SpkiPinTrustManager(targetTlsPin)
        val ssl = SSLContext.getInstance("TLS")
        ssl.init(null, arrayOf<TrustManager>(trust), SecureRandom())
        builder.sslSocketFactory(ssl.socketFactory, trust)
        return builder.build()
    }

    // ==================== WebSocket ====================

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val appKeyId = appKeyStore.appKeyId()
            webSocket.send(buildJsonObject {
                put("type", "auth_request")
                put("device_id", appKeyId)
                put("device_name", Build.MODEL)
                put("timestamp", System.currentTimeMillis())
            }.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== this@WssJsonControlChannel.webSocket) return
            try {
                handleMessage(text)
            } catch (e: Exception) {
                Log.w(TAG, "Bad server message: ${e.message}")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== this@WssJsonControlChannel.webSocket) return
            Log.w(TAG, "WebSocket failure: ${t.message}")
            if (!authCompletable.isCompleted) authCompletable.complete(false)
            if (!leaseCompletable.isCompleted) leaseCompletable.complete(false)
            cleanup()
            _isConnected.value = false
            _connectionStatus.value = ConnectionStatus.FAILED
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket !== this@WssJsonControlChannel.webSocket) return
            if (!leaseCompletable.isCompleted) leaseCompletable.complete(false)
            cleanup()
            _isConnected.value = false
            if (_connectionStatus.value != ConnectionStatus.FAILED) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }
    }

    private fun handleMessage(text: String) {
        val obj = json.parseToJsonElement(text).jsonObject
        when (obj["type"]?.jsonPrimitive?.content) {
            "challenge" -> {
                val challenge = obj["challenge_code"]?.jsonPrimitive?.content ?: return
                val appKeyId = appKeyStore.appKeyId()
                val signature = appKeyStore.signBase64("$appKeyId:$challenge")
                webSocket?.send(buildJsonObject {
                    put("type", "challenge_response")
                    put("device_id", appKeyId)
                    put("signature", signature)
                }.toString())
            }
            "auth_result" -> {
                val ok = obj["ok"]?.jsonPrimitive?.content == "true"
                if (ok) {
                    _token.value = obj["token"]?.jsonPrimitive?.content
                    tokenExpiresAt = obj["expires_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    _isTokenValid.value = true
                    lastHeartbeat = System.currentTimeMillis()
                }
                if (!authCompletable.isCompleted) authCompletable.complete(ok)
            }
            "battery" -> onBatteryDataReceived?.invoke(json.decodeFromString<BatteryData>(text))
            "imu" -> onImuDataReceived?.invoke(json.decodeFromString<ImuData>(text))
            "motors" -> onMotorDataReceived?.invoke(json.decodeFromString<MotorStatusResponse>(text))
            "heartbeat" -> lastHeartbeat = System.currentTimeMillis()
            "lease" -> {
                val status = obj["status"]?.jsonPrimitive?.content
                if (status == "acquired") {
                    hasControlLease = true
                    if (!leaseCompletable.isCompleted) leaseCompletable.complete(true)
                } else if (status != "renewed" && status != "active") {
                    _connectionStatus.value = ConnectionStatus.FAILED
                    cleanup()
                }
            }
            "stop" -> when (obj["status"]?.jsonPrimitive?.content) {
                "stopped" -> if (!stopCompletable.isCompleted) stopCompletable.complete(true)
                "failed" -> if (!stopCompletable.isCompleted) stopCompletable.complete(false)
                "cleared" -> if (!clearCompletable.isCompleted) clearCompletable.complete(true)
            }
            "skill_action_ack_received" -> {
                obj["request_id"]?.jsonPrimitive?.content?.let { onSkillActionAckReceived?.invoke(it) }
            }
            "skill_action_ack_completed" -> {
                val id = obj["request_id"]?.jsonPrimitive?.content ?: return
                val status = obj["status"]?.jsonPrimitive?.content ?: "completed"
                val elapsed = obj["execution_time_ms"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                onSkillActionAckCompleted?.invoke(id, status, elapsed)
            }
            "error" -> {
                Log.w(TAG, "server error: ${obj["message"]?.jsonPrimitive?.content}")
                if (!leaseCompletable.isCompleted) leaseCompletable.complete(false)
                _connectionStatus.value = ConnectionStatus.FAILED
                cleanup()
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(minOf(config.auth.heartbeatIntervalMs, 500L))
                if (!hasControlLease) break
                webSocket?.send(buildJsonObject {
                    put("type", "heartbeat")
                    put("token", _token.value ?: "")
                    put("timestamp", System.currentTimeMillis())
                }.toString())
                if (lastHeartbeat > 0
                    && System.currentTimeMillis() - lastHeartbeat > config.auth.heartbeatTimeoutMs) {
                    Log.w(TAG, "Heartbeat timeout")
                    _isConnected.value = false
                    _connectionStatus.value = ConnectionStatus.FAILED
                    cleanup()
                    break
                }
            }
        }
    }
}
