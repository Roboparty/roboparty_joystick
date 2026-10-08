// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.service

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.gamepad.controller.data.model.DeviceProfile
import com.gamepad.controller.data.model.enums.GamepadButton
import com.gamepad.controller.data.model.packet.AxesState
import com.gamepad.controller.domain.model.RawInput
import com.gamepad.controller.domain.service.GamepadInputService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 手柄输入管理器
 *
 * 职责：
 * - 发现已连接手柄设备（InputManager）
 * - 监听热插拔（InputDeviceListener）
 * - 处理 KeyEvent / MotionEvent 并分流
 * - 径向死区过滤 + 缩放补偿
 * - 输出 RawInput 到 ViewModel
 */
@Singleton
class GamepadInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) : GamepadInputService, InputManager.InputDeviceListener {

    companion object {
        private const val TAG = "GamepadInput"
        /** 归零阈值 — 当摇杆值非常接近中心时，强制归零 */
        private const val ZERO_THRESHOLD = 0.1f
    }

    private val inputManager: InputManager =
        context.getSystemService(Context.INPUT_SERVICE) as InputManager

    // ==================== 设备注册表 ====================

    private val deviceRegistry = mutableMapOf<Int, DeviceProfile>()

    private val _activeDevice = MutableStateFlow<DeviceProfile?>(null)
    override val activeDevice: StateFlow<DeviceProfile?> = _activeDevice.asStateFlow()

    // ==================== 输入流 ====================

    private val _rawInputFlow = MutableStateFlow(RawInput())
    private var inputCallback: ((RawInput) -> Unit)? = null

    private var deviceChangeListener: GamepadInputService.DeviceChangeListener? = null

    /** 上一帧 HAT 轴对应的方向键（用于检测状态变化） */
    private var lastHatButtons = emptySet<GamepadButton>()

    // ==================== GamepadInputService 实现 ====================

    override fun rawInputFlow(): Flow<RawInput> = _rawInputFlow.asStateFlow()

    fun setInputCallback(callback: ((RawInput) -> Unit)?) {
        inputCallback = callback
    }

    override fun connectedDevices(): List<DeviceProfile> = deviceRegistry.values.toList()

    override fun startListening() {
        inputManager.registerInputDeviceListener(this, null)
        scanDevices()
    }

    override fun stopListening() {
        inputManager.unregisterInputDeviceListener(this)
        deviceRegistry.clear()
        _activeDevice.value = null
    }

    override fun setDeviceChangeListener(listener: GamepadInputService.DeviceChangeListener?) {
        deviceChangeListener = listener
    }

    // ==================== 设备扫描 ====================

    private fun scanDevices() {
        inputManager.inputDeviceIds.forEach { deviceId ->
            val device = inputManager.getInputDevice(deviceId) ?: return@forEach
            if (isGamepad(device)) {
                addDevice(device)
            }
        }
    }

    private fun isGamepad(device: InputDevice): Boolean {
        val sources = device.sources
        return (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) ||
                (sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD)
    }

    private fun addDevice(device: InputDevice) {
        val profile = DeviceProfile(
            deviceId = device.id,
            name = device.name ?: "Unknown",
            vendorId = device.vendorId,
            productId = device.productId
        )
        deviceRegistry[device.id] = profile
        _activeDevice.value = profile
        deviceChangeListener?.onDeviceAdded(profile)
    }

    /**
     * 确保设备已注册，未注册则自动创建默认 profile
     *
     * 处理两种场景：
     * - 真实设备未被 isGamepad() 识别（如 SOURCE_KEYBOARD 的手柄）
     * - onPreviewKeyEvent 合成事件 deviceId=0
     */
    private fun ensureDevice(deviceId: Int, device: InputDevice?) {
        if (deviceRegistry.containsKey(deviceId)) return
        val profile = if (device != null) {
            DeviceProfile(
                deviceId = device.id,
                name = device.name ?: "Unknown",
                vendorId = device.vendorId,
                productId = device.productId
            )
        } else {
            // 合成事件（deviceId=0，device=null）
            DeviceProfile(deviceId = deviceId, name = "Synthetic")
        }
        deviceRegistry[deviceId] = profile
        _activeDevice.value = profile
        deviceChangeListener?.onDeviceAdded(profile)
    }

    // ==================== InputDeviceListener ====================

    override fun onInputDeviceAdded(deviceId: Int) {
        val device = inputManager.getInputDevice(deviceId) ?: return
        if (isGamepad(device)) {
            addDevice(device)
        }
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        val removed = deviceRegistry.remove(deviceId)
        if (removed != null) {
            deviceChangeListener?.onDeviceRemoved(deviceId)
            _activeDevice.value = deviceRegistry.values.lastOrNull()
        }
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        onInputDeviceRemoved(deviceId)
        onInputDeviceAdded(deviceId)
    }

    // ==================== 事件处理入口 ====================

    /**
     * 处理按键事件（在 Activity.dispatchKeyEvent 中调用）
     */
    fun processKeyEvent(event: KeyEvent): Boolean {
        val deviceId = event.deviceId

        // 来源校验：接受 GAMEPAD、DPAD、JOYSTICK 或 KEYBOARD 来源
        val source = event.source
        val isValidSource = (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                           (source and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD ||
                           (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                           (source and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD

        if (!isValidSource) return false

        // 未注册设备自动按默认 profile 注册（含 onPreviewKeyEvent 合成事件 deviceId=0）
        ensureDevice(deviceId, event.device)

        // 更新活跃设备
        deviceRegistry[deviceId]?.let { _activeDevice.value = it }

        val button = mapKeyCodeToButton(event.keyCode) ?: return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    emitInput(deviceId, button = button, isActionDown = true)
                }
            }
            KeyEvent.ACTION_UP -> {
                emitInput(deviceId, button = button, isActionDown = false)
            }
        }
        return true
    }

    /**
     * 处理摇杆事件（在 Activity.onGenericMotionEvent 中调用）
     */
    fun processMotionEvent(event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK) &&
            !event.isFromSource(InputDevice.SOURCE_GAMEPAD)
        ) return false

        val deviceId = event.deviceId
        ensureDevice(deviceId, event.device)
        val profile = deviceRegistry[deviceId] ?: return false

        _activeDevice.value = profile

        val rawLx = event.getAxisValue(MotionEvent.AXIS_X)
        val rawLy = event.getAxisValue(MotionEvent.AXIS_Y)
        val rawRx = event.getAxisValue(MotionEvent.AXIS_Z)
        val rawRy = event.getAxisValue(MotionEvent.AXIS_RZ)
        val rawLt = event.getAxisValue(MotionEvent.AXIS_BRAKE)
        val rawRt = event.getAxisValue(MotionEvent.AXIS_GAS)

        // 归零阈值：绝对值 <= 0.01 时归零
        val THRESHOLD = 0.01f
        val lx = if (kotlin.math.abs(rawLx) <= THRESHOLD) 0f else rawLx
        val ly = if (kotlin.math.abs(rawLy) <= THRESHOLD) 0f else rawLy
        val rx = if (kotlin.math.abs(rawRx) <= THRESHOLD) 0f else rawRx
        val ry = if (kotlin.math.abs(rawRy) <= THRESHOLD) 0f else rawRy

        // Y 轴反转
        val finalLy = if (profile.invertY) -ly else ly
        val finalRy = if (profile.invertY) -ry else ry

        val axes = AxesState(
            lx = lx,
            ly = finalLy,
            rx = rx,
            ry = finalRy,
            lt = rawLt.coerceIn(0f, 1f),
            rt = rawRt.coerceIn(0f, 1f)
        )

        emitInput(deviceId, axes = axes)

        // HAT 轴 → D-Pad 方向键（部分手柄如 Switch Pro Controller 走此通道）
        processHatAxis(deviceId, event)

        return true
    }

    /**
     * HAT 轴转方向键
     *
     * AXIS_HAT_X: -1.0=左, 0=中, 1.0=右
     * AXIS_HAT_Y: -1.0=上, 0=中, 1.0=下
     */
    private fun processHatAxis(deviceId: Int, event: MotionEvent) {
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        // 如果两个轴都是 0，说明这个手柄没有 HAT 轴，直接跳过
        if (hatX == 0f && hatY == 0f && lastHatButtons.isEmpty()) return

        val currentHatButtons = mutableSetOf<GamepadButton>()
        if (hatX < -0.5f) currentHatButtons.add(GamepadButton.DPAD_LEFT)
        if (hatX > 0.5f) currentHatButtons.add(GamepadButton.DPAD_RIGHT)
        if (hatY < -0.5f) currentHatButtons.add(GamepadButton.DPAD_UP)
        if (hatY > 0.5f) currentHatButtons.add(GamepadButton.DPAD_DOWN)

        // 计算变化：新按下 + 新释放
        val pressed = currentHatButtons - lastHatButtons
        val released = lastHatButtons - currentHatButtons

        pressed.forEach { emitInput(deviceId, button = it, isActionDown = true) }
        released.forEach { emitInput(deviceId, button = it, isActionDown = false) }

        lastHatButtons = currentHatButtons
    }

    // ==================== 死区过滤算法 ====================

    private fun applyRadialDeadzone(x: Float, y: Float, deadzone: Float): Pair<Float, Float> {
        val magnitude = sqrt(x * x + y * y)

        // 归零阈值：非常接近中心时强制归零
        if (magnitude <= ZERO_THRESHOLD) return 0f to 0f

        // 死区过滤
        if (magnitude <= deadzone) return 0f to 0f

        val scale = (magnitude - deadzone) / (1f - deadzone)
        val unitX = x / magnitude
        val unitY = y / magnitude
        return (unitX * scale) to (unitY * scale)
    }

    private fun applyGamma(value: Float, gamma: Float): Float {
        if (gamma == 1.0f) return value
        val sign = if (value >= 0) 1f else -1f
        return sign * kotlin.math.abs(value).toDouble().pow(gamma.toDouble()).toFloat()
    }

    // ==================== 辅助方法 ====================

    private fun mapKeyCodeToButton(keyCode: Int): GamepadButton? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> GamepadButton.A
        KeyEvent.KEYCODE_BUTTON_B -> GamepadButton.B
        KeyEvent.KEYCODE_BUTTON_X -> GamepadButton.X
        KeyEvent.KEYCODE_BUTTON_Y -> GamepadButton.Y
        KeyEvent.KEYCODE_BUTTON_L1 -> GamepadButton.LB
        KeyEvent.KEYCODE_BUTTON_R1 -> GamepadButton.RB
        KeyEvent.KEYCODE_BUTTON_L2 -> GamepadButton.LT
        KeyEvent.KEYCODE_BUTTON_R2 -> GamepadButton.RT
        KeyEvent.KEYCODE_BUTTON_START -> GamepadButton.START
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadButton.SELECT
        KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadButton.R3
        KeyEvent.KEYCODE_DPAD_UP -> GamepadButton.DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> GamepadButton.DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> GamepadButton.DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadButton.DPAD_RIGHT
        else -> null
    }

    private fun emitInput(
        deviceId: Int,
        axes: AxesState = AxesState.ZERO,
        button: GamepadButton? = null,
        isActionDown: Boolean = false
    ) {
        val input = RawInput(
            deviceId = deviceId,
            axes = axes,
            button = button,
            isActionDown = isActionDown,
            eventTime = System.currentTimeMillis()
        )
        // 同步回调
        inputCallback?.invoke(input)

        // 保留 Flow 供其他订阅者使用
        _rawInputFlow.value = input
    }
}
