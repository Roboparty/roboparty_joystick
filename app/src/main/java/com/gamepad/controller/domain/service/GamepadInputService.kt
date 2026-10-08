// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.domain.service

import com.gamepad.controller.data.model.DeviceProfile
import com.gamepad.controller.domain.model.RawInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 手柄输入服务接口
 *
 * 对应设计文档 ③ 手柄输入适配层 GamepadInputManager
 * 封装 Android InputDevice API，提供：
 * - 设备发现与热插拔监听
 * - 原始输入事件流（KeyEvent + MotionEvent 统一封装）
 * - 多手柄管理与主设备切换
 */
interface GamepadInputService {

    /**
     * 原始输入事件流
     *
     * 所有手柄事件（轴/按键）统一封装为 RawInput 后推送到此 Flow
     * ViewModel 订阅后做死区过滤、归一化、节流
     */
    fun rawInputFlow(): Flow<RawInput>

    /**
     * 当前活跃手柄设备
     *
     * 多手柄场景下，以"最近产生有效输入的 deviceId"为活跃设备
     * UI 层通过 StateFlow 展示当前活跃设备信息
     */
    val activeDevice: StateFlow<DeviceProfile?>

    /**
     * 已连接的手柄设备列表
     */
    fun connectedDevices(): List<DeviceProfile>

    /**
     * 开始监听手柄输入
     *
     * 应在 Activity.onCreate() 或 ViewModel.init{} 中调用
     * 注册 InputManager.InputDeviceListener 监听热插拔
     */
    fun startListening()

    /**
     * 停止监听手柄输入
     *
     * 应在 Activity.onDestroy() 中调用
     */
    fun stopListening()

    /**
     * 设置手柄设备变更回调
     */
    fun setDeviceChangeListener(listener: DeviceChangeListener?)

    /**
     * 手柄设备变更监听器
     */
    interface DeviceChangeListener {
        /** 手柄接入 */
        fun onDeviceAdded(device: DeviceProfile)

        /** 手柄断开 */
        fun onDeviceRemoved(deviceId: Int)
    }
}
