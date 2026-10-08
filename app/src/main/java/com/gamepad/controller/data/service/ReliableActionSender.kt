// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.service

import android.util.Log
import com.gamepad.controller.data.model.SkillActionRegistry
import com.gamepad.controller.data.model.packet.SkillActionPacket
import com.gamepad.controller.domain.model.ActionRequestState
import com.gamepad.controller.domain.service.ControlChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 可靠动作发送器
 *
 * 实现两阶段确认机制：
 * 1. ACK1 - 服务器确认收到请求
 * 2. ACK2 - 服务器确认动作执行完成
 *
 * 同一时间只能执行一个动作
 */
class ReliableActionSender(
    private val controlChannel: ControlChannel
) {
    companion object {
        private const val TAG = "ReliableActionSender"
        const val MAX_RETRIES = 3
        const val ACK1_TIMEOUT_MS = 500L
        const val ACK2_TIMEOUT_MS = 5000L
        const val RETRY_BACKOFF = 1.5f
        const val STATE_RESET_DELAY_MS = 3000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true  // 确保默认值（如 type）被序列化
    }

    // 等待确认的请求
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()

    // 每个动作的状态流（内部可变）
    private val _actionStates = ConcurrentHashMap<String, MutableStateFlow<ActionRequestState>>()

    // 当前正在执行的动作ID（同一时间只能执行一个）
    private val _currentAction = MutableStateFlow<String?>(null)
    val currentAction: StateFlow<String?> = _currentAction.asStateFlow()

    // 序列号计数器
    private var sequenceCounter = 0L

    /**
     * 获取指定动作的状态流
     */
    fun getActionState(actionId: String): StateFlow<ActionRequestState> {
        return getMutableActionState(actionId)
    }

    /**
     * 获取指定动作的可变状态流（内部使用）
     */
    private fun getMutableActionState(actionId: String): MutableStateFlow<ActionRequestState> {
        return _actionStates.getOrPut(actionId) {
            MutableStateFlow(ActionRequestState.Idle)
        }
    }

    /**
     * 是否有动作正在执行
     */
    fun isActionExecuting(): Boolean {
        return _currentAction.value != null
    }

    /**
     * 发送动作请求
     *
     * @param actionId 动作标识
     * @return 是否执行成功
     * @throws IllegalStateException 如果已有动作在执行
     */
    suspend fun sendAction(actionId: String): Boolean {
        // 检查是否有动作正在执行
        if (_currentAction.value != null) {
            Log.w(TAG, "Action already executing: ${_currentAction.value}")
            throw IllegalStateException("有动作正在执行，请等待完成")
        }

        val action = SkillActionRegistry.getAction(actionId)
            ?: throw IllegalArgumentException("Unknown action: $actionId")

        val requestId = UUID.randomUUID().toString()
        val stateFlow = getMutableActionState(actionId)

        // 设置当前执行的动作
        _currentAction.value = actionId

        Log.i(TAG, "Sending action: $actionId, requestId: $requestId")
        stateFlow.value = ActionRequestState.Sending

        val packet = SkillActionPacket(
            actionId = actionId,
            requestId = requestId,
            sequence = ++sequenceCounter,
            timestamp = System.currentTimeMillis(),
            token = controlChannel.token.value ?: ""
        )

        return try {
            sendWithReliability(packet, action.estimatedDuration)
        } finally {
            // 无论成功失败，清除当前动作状态
            _currentAction.value = null
        }
    }

    private suspend fun sendWithReliability(
        packet: SkillActionPacket,
        estimatedDuration: Long
    ): Boolean {
        val requestId = packet.requestId
        val completable = CompletableDeferred<Boolean>()

        pendingRequests[requestId] = PendingRequest(
            actionId = packet.actionId,
            completable = completable,
            estimatedDuration = estimatedDuration
        )

        // 发送请求
        sendPacket(packet)
        startRetryTimer(requestId, packet)

        // 等待结果（带超时）
        val timeout = ACK2_TIMEOUT_MS + estimatedDuration
        return withTimeoutOrNull(timeout) {
            completable.await()
        } ?: run {
            Log.w(TAG, "Action timeout: ${packet.actionId}")
            pendingRequests.remove(requestId)
            getMutableActionState(packet.actionId).value = ActionRequestState.Timeout(packet.actionId)
            false
        }
    }

    private fun sendPacket(packet: SkillActionPacket) {
        val jsonStr = json.encodeToString(packet)
        controlChannel.sendJson(jsonStr)
        Log.d(TAG, "Sent packet: $jsonStr")
    }

    private fun startRetryTimer(requestId: String, packet: SkillActionPacket) {
        scope.launch {
            var retryCount = 0
            var timeout = ACK1_TIMEOUT_MS

            // 保存retryJob的引用
            val job = coroutineContext[Job]
            pendingRequests[requestId]?.retryJob = job

            while (retryCount < MAX_RETRIES && pendingRequests.containsKey(requestId)) {
                delay(timeout.toLong())

                val currentPending = pendingRequests[requestId]
                if (currentPending != null && !currentPending.ack1Received) {
                    retryCount++
                    Log.w(TAG, "Retry $retryCount for ${packet.actionId}")
                    sendPacket(packet.copy(sequence = ++sequenceCounter))
                    timeout = (timeout * RETRY_BACKOFF).toLong()
                } else {
                    break
                }
            }

            // 超过重试次数
            val currentPending = pendingRequests[requestId]
            if (currentPending != null && !currentPending.ack1Received) {
                Log.w(TAG, "Max retries reached for ${packet.actionId}")
                pendingRequests.remove(requestId)
                getMutableActionState(packet.actionId).value = ActionRequestState.Failed("网络异常，请求未到达服务器")
                currentPending.completable.complete(false)
            }
        }
    }

    /**
     * 处理接收确认（ACK1）
     */
    fun handleAckReceived(requestId: String) {
        val pending = pendingRequests[requestId] ?: run {
            Log.w(TAG, "Received ACK1 for unknown request: $requestId")
            return
        }

        Log.i(TAG, "ACK1 received for ${pending.actionId}")
        pending.ack1Received = true
        pending.retryJob?.cancel()

        getMutableActionState(pending.actionId).value = ActionRequestState.Executing(pending.estimatedDuration)
    }

    /**
     * 处理执行完成（ACK2）
     */
    fun handleAckCompleted(requestId: String, status: String, executionTime: Long) {
        val pending = pendingRequests.remove(requestId) ?: run {
            Log.w(TAG, "Received ACK2 for unknown request: $requestId")
            return
        }

        Log.i(TAG, "ACK2 received for ${pending.actionId}, status: $status, time: ${executionTime}ms")

        val stateFlow = getMutableActionState(pending.actionId)
        stateFlow.value = when (status) {
            "success" -> ActionRequestState.Success(executionTime)
            "failed" -> ActionRequestState.Failed("执行失败")
            "duplicate" -> ActionRequestState.Success(0)
            else -> ActionRequestState.Failed("未知状态: $status")
        }

        pending.completable.complete(status == "success" || status == "duplicate")

        // 延迟后重置状态
        scope.launch {
            delay(STATE_RESET_DELAY_MS)
            stateFlow.value = ActionRequestState.Idle
        }
    }

    /**
     * 释放资源
     */
    fun destroy() {
        pendingRequests.values.forEach { it.retryJob?.cancel() }
        pendingRequests.clear()
        _actionStates.clear()
    }

    /**
     * 等待中的请求
     */
    private class PendingRequest(
        val actionId: String,
        val completable: CompletableDeferred<Boolean>,
        val estimatedDuration: Long,
        var ack1Received: Boolean = false,
        var retryJob: Job? = null
    )
}
