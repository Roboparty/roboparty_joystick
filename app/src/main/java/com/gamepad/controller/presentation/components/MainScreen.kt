package com.gamepad.controller.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.R
import com.gamepad.controller.data.model.enums.ConnectionStatus
import com.gamepad.controller.domain.model.UiIntent
import com.gamepad.controller.presentation.RobotControlViewModel

/**
 * 主屏幕 — 1920×1080 布局
 *
 * ┌──────────────────────────────────────────────┐
 * │              StatusBar (40px)                │
 * │           TopNavBar (紧贴状态栏)              │
 * │                                              │
 * │ ┌──────────┐                ┌──────────────┐ │
 * │ │ 姿态切换  │   Robot Lottie │ SideControl  │ │
 * │ │ 站/坐/待 │                │  全部技能     │ │
 * │ └──────────┘                └──────────────┘ │
 * │                                              │
 * │ ┌────────────┐                               │
 * │ │ JoystickDisc│                              │
 * │ │  (400px)   │                               │
 * │ └────────────┘                               │
 * └──────────────────────────────────────────────┘
 */
@Composable
fun MainScreen(viewModel: RobotControlViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showCard by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showNotification by remember { mutableStateOf(false) }
    var showSkillLibrary by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        // 背景图片 — 铺满全屏含状态栏区域
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ---- 主内容（卡片弹出时添加模糊效果） ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (showCard) 20.dp else 0.dp)
        ) {
            // 主内容列
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部信号栏
                StatusBar()

                // 顶部导航栏 — 紧贴状态栏
                TopNavBar(
                    selectedIndex = when {
                        showSkillLibrary -> 0
                        showSettings -> 3
                        showNotification -> 2
                        else -> 1
                    },
                    onNavClick = { index ->
                        // 互斥：打开一个面板时关闭另一个
                        showSkillLibrary = index == 0
                        showSettings = index == 3
                        showNotification = index == 2
                    }
                )

                // 连接状态指示器 / 连接按钮
                ConnectionStatusButton(
                    status = uiState.connectionStatus,
                    serverHost = uiState.serverHost,
                    serverPort = uiState.serverPort,
                    onClick = {
                        if (uiState.isConnected) {
                            viewModel.disconnectRobot()
                        } else {
                            showConnectionDialog = true
                        }
                    }
                )

                // 机器人 Lottie — 填满剩余空间
                RobotViewer(
                    modifier = Modifier.weight(1f)
                )
            }

            // ---- 左侧上部：姿态切换 ----
            PostureSwitcher(
                currentPosture = uiState.posture,
                onPostureChanged = { posture ->
                    viewModel.sendIntent(UiIntent.PostureChanged(posture))
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 120.dp)
            )

            // ---- 左下角：左摇杆圆盘（触屏 + 手柄双向） ----
            JoystickDisc(
                stickX = uiState.stickX,
                stickY = uiState.stickY,
                onMove = { x, y -> viewModel.onJoystickTouchMove(x, y) },
                onRelease = { viewModel.onJoystickTouchRelease() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp)
                    .size(200.dp)
            )

            // ---- 右侧上部：全部技能 ----
            SideControlBar(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 120.dp)
            )

            // ---- 右下角：右摇杆圆盘 ----
            JoystickDisc(
                stickX = uiState.rightStickX,
                stickY = uiState.rightStickY,
                onMove = { x, y -> viewModel.onRightJoystickTouchMove(x, y) },
                onRelease = { viewModel.onRightJoystickTouchRelease() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp)
                    .size(120.dp)
            )

            // ---- 右下角：悬浮球 ----
            FloatingBall(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 156.dp, bottom = 60.dp),
                onClick = { showCard = !showCard }
            )

            // ---- 设置面板（屏幕下半部分） ----
            if (showSettings) {
                val otaState by viewModel.otaState.collectAsState()
                SettingsPanel(
                    onBack = { showSettings = false },
                    isEstopping = uiState.isEstopping,
                    estopStatus = uiState.estopStatus,
                    onEstopChanged = { enabled ->
                        if (enabled) viewModel.sendIntent(UiIntent.Estop)
                        else viewModel.sendIntent(UiIntent.EstopRelease)
                    },
                    serverHost = uiState.serverHost,
                    serverPort = uiState.serverPort,
                    onServerConfigChanged = { host, port ->
                        viewModel.sendIntent(UiIntent.ServerConfigChanged(host, port))
                    },
                    otaState = otaState,
                    onCheckOtaUpdate = { viewModel.checkOtaUpdate() },
                    onDownloadOta = { viewModel.downloadOtaApk() },
                    onInstallOta = { viewModel.installOtaApk() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 19.dp)
                )
            }

            // ---- 通知面板（屏幕下半部分） ----
            if (showNotification) {
                NotificationPanel(
                    onBack = { showNotification = false },
                    onRefresh = { /* TODO: 请求最新的电池数据 */ },
                    batteryData = uiState.batteryData,
                    imuData = uiState.imuData,
                    motors = uiState.motors,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 19.dp)
                )
            }

            // ---- 技能库面板（屏幕下半部分） ----
            if (showSkillLibrary) {
                val currentActionId by viewModel.currentActionId.collectAsState()

                SkillLibraryPanel(
                    onBack = { showSkillLibrary = false },
                    onExecuteAction = { actionId ->
                        viewModel.executeSkillAction(actionId)
                    },
                    actionStates = mapOf(
                        "handshake" to viewModel.getActionState("handshake"),
                        "bow" to viewModel.getActionState("bow"),
                        "wave" to viewModel.getActionState("wave"),
                        "nod" to viewModel.getActionState("nod"),
                        "dance" to viewModel.getActionState("dance")
                    ),
                    currentActionId = currentActionId,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 19.dp)
                )
            }
        }

        // ---- 弹出卡片（全屏遮罩 + 模糊背景） ----
        if (showCard) {
            FloatingCard(
                onDismiss = { showCard = false },
                isEstopping = uiState.isEstopping,
                estopStatus = uiState.estopStatus,
                onEstopChanged = { enabled ->
                    if (enabled) viewModel.sendIntent(UiIntent.Estop)
                    else viewModel.sendIntent(UiIntent.EstopRelease)
                },
                motors = uiState.motors,
                imuData = uiState.imuData
            )
        }

        // ---- 连接对话框 ----
        if (showConnectionDialog) {
            ConnectionDialog(
                serverHost = uiState.serverHost,
                serverPort = uiState.serverPort,
                connectionStatus = uiState.connectionStatus,
                onDismiss = { showConnectionDialog = false },
                onConnect = { host, port ->
                    viewModel.sendIntent(UiIntent.ServerConfigChanged(host, port))
                    viewModel.connectRobot()
                },
                onDisconnect = {
                    viewModel.disconnectRobot()
                }
            )
        }
    }
}

/**
 * 连接状态按钮
 *
 * - 未连接/失败：显示"连接"按钮
 * - 连接中：显示加载状态
 * - 已连接：显示绿色状态点（点击断开）
 */
@Composable
private fun ConnectionStatusButton(
    status: ConnectionStatus,
    @Suppress("UNUSED_PARAMETER") serverHost: String,
    @Suppress("UNUSED_PARAMETER") serverPort: Int,
    onClick: () -> Unit
) {
    when (status) {
        ConnectionStatus.CONNECTED -> {
            // 已连接：绿色小点，点击断开
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8F5E9))
                    .clickable { onClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3FB950))
                )
                Text(
                    text = "已连接",
                    fontSize = 12.sp,
                    color = Color(0xFF3FB950)
                )
            }
        }

        ConnectionStatus.CONNECTING -> {
            // 连接中：显示加载状态
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFF8E1))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Color(0xFFF59E0B),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "连接中...",
                    fontSize = 12.sp,
                    color = Color(0xFFF59E0B)
                )
            }
        }

        else -> {
            // 未连接/失败：显示连接按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (status == ConnectionStatus.FAILED)
                            Color(0xFFFFEBEE) else Color(0xFFF0F4FF)
                    )
                    .clickable { onClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (status == ConnectionStatus.FAILED)
                        Color(0xFFEF4444) else Color(0xFF2D7FF9)
                )
                Text(
                    text = if (status == ConnectionStatus.FAILED) "重试连接" else "连接",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (status == ConnectionStatus.FAILED)
                        Color(0xFFEF4444) else Color(0xFF2D7FF9)
                )
            }
        }
    }
}
