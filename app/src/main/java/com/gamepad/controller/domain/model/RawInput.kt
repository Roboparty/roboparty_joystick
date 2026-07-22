package com.gamepad.controller.domain.model

import com.gamepad.controller.data.model.enums.GamepadButton
import com.gamepad.controller.data.model.packet.AxesState

/**
 * 原始手柄输入事件
 *
 * 从 GamepadInputManager 传入 ViewModel 的原始输入封装
 *
 * @param deviceId 产生输入的手柄设备 ID
 * @param axes 摇杆轴值（死区处理后）
 * @param button 映射后的按键（仅按键事件有效，null 表示轴事件）
 * @param isActionDown 是否为按下事件（true=按下, false=抬起）
 * @param eventTime 事件时间戳（毫秒）
 */
data class RawInput(
    val deviceId: Int,
    val axes: AxesState = AxesState.ZERO,
    val button: GamepadButton? = null,
    val isActionDown: Boolean = false,
    val eventTime: Long = System.currentTimeMillis()
) {
    /** 是否为摇杆/轴事件 */
    val isAxisEvent: Boolean get() = button == null && axes != AxesState.ZERO

    /** 是否为按键事件 */
    val isButtonEvent: Boolean get() = button != null
}
