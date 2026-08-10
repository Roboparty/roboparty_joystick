package com.gamepad.controller.data.model.packet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 技能动作请求包
 *
 * 通用动作请求，支持任意动作类型和参数
 */
@Serializable
data class SkillActionPacket(
    @SerialName("type") val type: String = "skill_action",
    @SerialName("action_id") val actionId: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("sequence") val sequence: Long,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerialName("token") val token: String = ""
)

/**
 * 动作接收确认包（ACK1）
 *
 * 服务器确认已收到请求
 */
@Serializable
data class SkillActionAckReceived(
    @SerialName("type") val type: String = "skill_action_ack_received",
    @SerialName("request_id") val requestId: String,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * 动作执行完成包（ACK2）
 *
 * 服务器确认动作执行结果
 */
@Serializable
data class SkillActionAckCompleted(
    @SerialName("type") val type: String = "skill_action_ack_completed",
    @SerialName("request_id") val requestId: String,
    @SerialName("status") val status: String,  // success / failed / duplicate
    @SerialName("execution_time_ms") val executionTimeMs: Long = 0,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)
