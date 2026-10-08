// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.model

/**
 * 参数类型枚举
 */
enum class ParameterType {
    STRING, INT, FLOAT, BOOLEAN
}

/**
 * 动作参数定义
 */
data class ActionParameter(
    val name: String,
    val type: ParameterType,
    val defaultValue: Any?,
    val required: Boolean = false
)

/**
 * 技能动作定义
 */
data class SkillAction(
    val id: String,                 // 唯一标识
    val name: String,               // 显示名称
    val description: String,        // 描述
    val category: String,           // 分类：expression/action/talent
    val parameters: List<ActionParameter> = emptyList(),  // 可配置参数
    val estimatedDuration: Long = 2000  // 预计执行时长（毫秒）
)

/**
 * 动作注册表
 *
 * 管理所有可用的技能动作
 */
object SkillActionRegistry {
    private val actions = mutableMapOf<String, SkillAction>()

    init {
        // 注册默认动作
        register(SkillAction(
            id = "handshake",
            name = "握手",
            description = "向右滑动执行握手动作",
            category = "action",
            estimatedDuration = 2000
        ))

        register(SkillAction(
            id = "bow",
            name = "鞠躬",
            description = "向右滑动执行鞠躬动作",
            category = "action",
            estimatedDuration = 1500
        ))

        register(SkillAction(
            id = "wave",
            name = "挥手",
            description = "向右滑动执行挥手动作",
            category = "action",
            estimatedDuration = 1000
        ))

        register(SkillAction(
            id = "nod",
            name = "点头",
            description = "向右滑动执行点头动作",
            category = "expression",
            estimatedDuration = 800
        ))

        register(SkillAction(
            id = "dance",
            name = "跳舞",
            description = "向右滑动执行跳舞动作",
            category = "talent",
            estimatedDuration = 5000
        ))
    }

    fun register(action: SkillAction) {
        actions[action.id] = action
    }

    fun getAction(id: String): SkillAction? = actions[id]

    fun getActionsByCategory(category: String): List<SkillAction> {
        return actions.values.filter { it.category == category }
    }

    fun getAllActions(): List<SkillAction> = actions.values.toList()
}
