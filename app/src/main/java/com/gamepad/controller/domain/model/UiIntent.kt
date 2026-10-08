// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.domain.model

import com.gamepad.controller.data.model.enums.GamepadButton

/**
 * UI 意图 — 密封类
 *
 * 所有用户操作（手柄/触屏）统一封装为 UiIntent，
 * 由 ViewModel 消费并更新 UiState
 */
sealed class UiIntent {

    data class ButtonPressed(val deviceId: Int, val button: GamepadButton) : UiIntent()

    data class ButtonReleased(val deviceId: Int, val button: GamepadButton) : UiIntent()

    data object ConnectRobot : UiIntent()

    data object DisconnectRobot : UiIntent()

    data class DeviceChanged(val deviceId: Int, val isConnected: Boolean) : UiIntent()

    data object Estop : UiIntent()

    data object EstopRelease : UiIntent()

    data class PostureChanged(val posture: PostureMode) : UiIntent()

    data class ServerConfigChanged(val host: String, val port: Int) : UiIntent()
}
