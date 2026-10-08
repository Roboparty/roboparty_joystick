// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * 手柄控制器 Application
 *
 * Hilt 入口点（@HiltAndroidApp 自动生成 DI 组件）
 *
 * 初始化职责（后续扩展）：
 * - SDK 初始化
 * - 全局配置加载
 * - 日志初始化
 */
@HiltAndroidApp
class GamepadControlApplication : Application() {

    companion object {
        private const val TAG = "GamepadControl"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GamepadControlApplication initialized")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.i(TAG, "GamepadControlApplication terminated")
    }
}
