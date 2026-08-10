package com.gamepad.controller.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamepad.controller.data.config.ConnectionConfig
import com.gamepad.controller.data.model.BatteryData
import com.gamepad.controller.data.model.enums.ConnectionStatus
import com.gamepad.controller.data.model.enums.GamepadButton
import com.gamepad.controller.data.model.packet.AxesState
import com.gamepad.controller.data.model.packet.ControlPacket
import com.gamepad.controller.data.service.BleControlChannel
import com.gamepad.controller.data.service.ReliableActionSender
import com.gamepad.controller.data.service.UdpControlChannel
import com.gamepad.controller.domain.model.ActionRequestState
import com.gamepad.controller.domain.model.PostureMode
import com.gamepad.controller.domain.model.RawInput
import com.gamepad.controller.domain.model.UiIntent
import com.gamepad.controller.domain.model.UiState
import com.gamepad.controller.domain.service.ControlChannel
import com.gamepad.controller.domain.service.GamepadInputService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 机器人控制 ViewModel
 *
 * MVI 架构核心：
 * - UiState 唯一可信源（StateFlow）
 * - 手柄输入 → 按键状态跟踪 → 节流 → UDP 发送（JSON 格式）
 * - Fail-safe：后台 → 急停
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class RobotControlViewModel @Inject constructor(
    private val controlChannel: ControlChannel,
    private val gamepadInputService: GamepadInputService,
    private val config: ConnectionConfig
) : ViewModel() {

    companion object {
        private const val TAG = "RobotControlVM"
    }

    // ==================== UI 状态 ====================

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ==================== 内部状态 ====================

    /** 按键状态表：GamepadButton → 是否按下 */
    private val buttonStates = mutableMapOf<GamepadButton, Boolean>()

    private var seqCounter = 0L
    private var controlSendJob: Job? = null
    private val latestAxes = MutableStateFlow(AxesState.ZERO)

    // ==================== 技能动作 ====================

    private val actionSender = ReliableActionSender(controlChannel)

    /**
     * 当前正在执行的动作ID
     */
    val currentActionId: StateFlow<String?> = actionSender.currentAction

    /**
     * 获取指定动作的状态流
     */
    fun getActionState(actionId: String): StateFlow<ActionRequestState> {
        return actionSender.getActionState(actionId)
    }

    /**
     * 是否有动作正在执行
     */
    fun isActionExecuting(): Boolean {
        return actionSender.isActionExecuting()
    }

    /**
     * 执行技能动作
     */
    fun executeSkillAction(actionId: String) {
        viewModelScope.launch {
            try {
                val success = actionSender.sendAction(actionId)
                Log.i(TAG, "Skill action $actionId result: $success")
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Action rejected: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Skill action failed: ${e.message}")
            }
        }
    }

    /**
     * 处理技能动作确认（ACK1）
     */
    fun handleSkillActionAckReceived(requestId: String) {
        actionSender.handleAckReceived(requestId)
    }

    /**
     * 处理技能动作执行完成（ACK2）
     */
    fun handleSkillActionAckCompleted(requestId: String, status: String, executionTime: Long) {
        actionSender.handleAckCompleted(requestId, status, executionTime)
    }

    /**
     * 处理电池数据
     */
    fun handleBatteryData(batteryData: BatteryData) {
        _uiState.value = _uiState.value.copy(batteryData = batteryData)
        Log.d(TAG, "Battery data: voltage=${batteryData.voltage}V, soc=${batteryData.soc}%, temp=${batteryData.temp}°C")
    }

    // ==================== 初始化 ====================

    init {
        setupInputListener()
        setupControlLoop()
        observeConnectionStatus()
        observeActiveDevice()
        observeTokenValidity()
        setupChannelCallbacks()
    }

    /**
     * 设置通道回调
     */
    private fun setupChannelCallbacks() {
        // 电池数据回调
        val batteryCallback: (BatteryData) -> Unit = { batteryData ->
            handleBatteryData(batteryData)
        }

        // 技能动作确认回调
        val ackReceivedCallback: (String) -> Unit = { requestId ->
            handleSkillActionAckReceived(requestId)
        }

        val ackCompletedCallback: (String, String, Long) -> Unit = { requestId, status, executionTime ->
            handleSkillActionAckCompleted(requestId, status, executionTime)
        }

        // 设置 UDP 通道回调
        (controlChannel as? UdpControlChannel)?.let { udp ->
            udp.onBatteryDataReceived = batteryCallback
            udp.onSkillActionAckReceived = ackReceivedCallback
            udp.onSkillActionAckCompleted = ackCompletedCallback
        }

        // 设置 BLE 通道回调
        (controlChannel as? BleControlChannel)?.let { ble ->
            ble.onBatteryDataReceived = batteryCallback
            ble.onSkillActionAckReceived = ackReceivedCallback
            ble.onSkillActionAckCompleted = ackCompletedCallback
        }
    }

    private fun setupInputListener() {
        viewModelScope.launch {
            gamepadInputService.rawInputFlow()
                .catch { e -> Log.e(TAG, "Input flow error: ${e.message}") }
                .collect { input -> handleRawInput(input) }
        }
    }

    private fun setupControlLoop() {
        val intervalMs = 1000L / config.controlParams.sendFrequencyHz
        controlSendJob = viewModelScope.launch {
            while (true) {
                sendControlPacket()
                kotlinx.coroutines.delay(intervalMs)
            }
        }
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            controlChannel.connectionStatus.collect { status ->
                _uiState.value = _uiState.value.copy(connectionStatus = status)
            }
        }
    }

    private fun observeActiveDevice() {
        viewModelScope.launch {
            gamepadInputService.activeDevice.collect { device ->
                _uiState.value = _uiState.value.copy(
                    activeDeviceId = device?.deviceId ?: -1,
                    deviceName = device?.name ?: ""
                )
            }
        }
    }

    private fun observeTokenValidity() {
        viewModelScope.launch {
            controlChannel.isTokenValid.collect { isValid ->
                _uiState.value = _uiState.value.copy(isTokenValid = isValid)
                if (!isValid && _uiState.value.isConnected) {
                    Log.w(TAG, "Token expired or invalid, attempting reconnection")
                    // Token 过期，尝试重新连接
                    reconnectRobot()
                }
            }
        }
    }

    /**
     * 重新连接机器人
     */
    private fun reconnectRobot() {
        viewModelScope.launch {
            Log.i(TAG, "Reconnecting due to token expiration")
            controlChannel.disconnect()
            kotlinx.coroutines.delay(1000)
            connectRobot()
        }
    }

    // ==================== Intent 分发 ====================

    fun sendIntent(intent: UiIntent) {
        when (intent) {
            is UiIntent.ButtonPressed -> handleButtonPressed(intent)
            is UiIntent.ButtonReleased -> handleButtonReleased(intent)
            is UiIntent.ConnectRobot -> connectRobot()
            is UiIntent.DisconnectRobot -> disconnectRobot()
            is UiIntent.DeviceChanged -> handleDeviceChanged(intent)
            is UiIntent.Estop -> triggerEstop()
            is UiIntent.EstopRelease -> releaseEstop()
            is UiIntent.PostureChanged -> handlePostureChanged(intent)
        }
    }

    // ==================== Intent 处理 ====================

    private fun handleRawInput(input: RawInput) {
        if (input.isAxisEvent) {
            latestAxes.value = input.axes
            _uiState.value = _uiState.value.copy(
                stickX = input.axes.lx,
                stickY = input.axes.ly,
                rightStickX = input.axes.rx,
                rightStickY = input.axes.ry
            )
        } else if (input.isButtonEvent) {
            val button = input.button!!
            if (input.isActionDown) {
                sendIntent(UiIntent.ButtonPressed(input.deviceId, button))
            } else {
                sendIntent(UiIntent.ButtonReleased(input.deviceId, button))
            }
        }
    }

    private fun handleButtonPressed(intent: UiIntent.ButtonPressed) {
        buttonStates[intent.button] = true
        Log.d(TAG, "Button pressed: ${intent.button}")

        when (intent.button) {
            GamepadButton.START -> triggerEstop()
            else -> Unit
        }
    }

    private fun handleButtonReleased(intent: UiIntent.ButtonReleased) {
        buttonStates[intent.button] = false
    }

    private fun handleDeviceChanged(intent: UiIntent.DeviceChanged) {
        if (!intent.isConnected && intent.deviceId == _uiState.value.activeDeviceId) {
            Log.w(TAG, "Active gamepad disconnected!")
            triggerEstop()
        }
    }

    private fun handlePostureChanged(intent: UiIntent.PostureChanged) {
        _uiState.value = _uiState.value.copy(posture = intent.posture)
        Log.i(TAG, "Posture changed: ${intent.posture.label}")
    }

    /**
     * 触屏摇杆拖拽 — 更新 UI + 控制通道
     */
    fun onJoystickTouchMove(x: Float, y: Float) {
        val nx = x.coerceIn(-1f, 1f)
        val ny = y.coerceIn(-1f, 1f)
        _uiState.value = _uiState.value.copy(stickX = nx, stickY = ny)
        latestAxes.value = latestAxes.value.copy(lx = nx, ly = ny)
    }

    fun onJoystickTouchRelease() {
        _uiState.value = _uiState.value.copy(stickX = 0f, stickY = 0f)
        latestAxes.value = latestAxes.value.copy(lx = 0f, ly = 0f)
    }

    fun onRightJoystickTouchMove(x: Float, y: Float) {
        val nx = x.coerceIn(-1f, 1f)
        val ny = y.coerceIn(-1f, 1f)
        _uiState.value = _uiState.value.copy(rightStickX = nx, rightStickY = ny)
        latestAxes.value = latestAxes.value.copy(rx = nx, ry = ny)
    }

    fun onRightJoystickTouchRelease() {
        _uiState.value = _uiState.value.copy(rightStickX = 0f, rightStickY = 0f)
        latestAxes.value = latestAxes.value.copy(rx = 0f, ry = 0f)
    }

    // ==================== 连接管理 ====================

    fun connectRobot() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.CONNECTING)
            try {
                controlChannel.connect(config.control.host, config.control.port)
                Log.i(TAG, "Connected to robot at ${config.control.host}:${config.control.port}")
            } catch (e: Exception) {
                Log.e(TAG, "Connect failed: ${e.message}")
                _uiState.value = _uiState.value.copy(connectionStatus = ConnectionStatus.FAILED)
            }
        }
    }

    fun disconnectRobot() {
        viewModelScope.launch {
            controlChannel.disconnect()
        }
    }

    // ==================== 指令发送 ====================

    /**
     * 构建目标 JSON 格式的 ControlPacket 并发送
     *
     * 格式：
     * {
     *   "timestamp": ..., "sequence": ...,
     *   "left_stick_x": ..., "left_stick_y": ...,
     *   "right_stick_x": ..., "right_stick_y": ...,
     *   "btn_a": true/false, "btn_b": ..., ...
     *   "dpad_up": ..., "dpad_down": ..., ...
     * }
     */
    private fun sendControlPacket() {
        if (_uiState.value.connectionStatus != ConnectionStatus.CONNECTED) return
        if (_uiState.value.isEstopping) return

        val axes = latestAxes.value
        val packet = ControlPacket(
            timestamp = System.currentTimeMillis(),
            sequence = ++seqCounter,
            leftStickX = axes.lx,
            leftStickY = axes.ly,
            rightStickX = axes.rx,
            rightStickY = axes.ry,
            btnA = buttonStates[GamepadButton.A] == true,
            btnB = buttonStates[GamepadButton.B] == true,
            btnX = buttonStates[GamepadButton.X] == true,
            btnY = buttonStates[GamepadButton.Y] == true,
            dpadUp = buttonStates[GamepadButton.DPAD_UP] == true,
            dpadDown = buttonStates[GamepadButton.DPAD_DOWN] == true,
            dpadLeft = buttonStates[GamepadButton.DPAD_LEFT] == true,
            dpadRight = buttonStates[GamepadButton.DPAD_RIGHT] == true
        )

        Log.d(TAG, "Sending ${packet.type} packet, seq=${packet.sequence}")
        controlChannel.sendControl(packet)
    }

    // ==================== 急停 ====================

    private fun triggerEstop() {
        _uiState.value = _uiState.value.copy(isEstopping = true)
        // 发送全零包作为急停信号
        val packet = ControlPacket(
            timestamp = System.currentTimeMillis(),
            sequence = ++seqCounter
        )
        controlChannel.sendEstop(packet)
        Log.w(TAG, "!!! ESTOP TRIGGERED !!!")
    }

    private fun releaseEstop() {
        _uiState.value = _uiState.value.copy(isEstopping = false)
    }

    // ==================== 生命周期 ====================

    fun onPause() {
        triggerEstop()
    }

    fun onResume() {
        gamepadInputService.startListening()
    }

    override fun onCleared() {
        super.onCleared()
        controlSendJob?.cancel()
        viewModelScope.launch {
            controlChannel.disconnect()
        }
        gamepadInputService.stopListening()
    }
}
