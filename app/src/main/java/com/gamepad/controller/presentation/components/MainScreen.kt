package com.gamepad.controller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
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
                    selectedIndex = 1,
                    onNavClick = { /* TODO */ }
                )

                // 连接/断开按钮
                ConnectionButton(
                    status = uiState.connectionStatus,
                    onClick = {
                        if (uiState.isConnected) viewModel.disconnectRobot()
                        else viewModel.connectRobot()
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
        }

        // ---- 弹出卡片（全屏遮罩 + 模糊背景） ----
        if (showCard) {
            FloatingCard(
                onDismiss = { showCard = false }
            )
        }
    }
}

/**
 * 连接/断开按钮 — 圆形状态指示器
 *
 * 点击切换连接/断开
 * 绿色=已连接，灰色=未连接，黄色=连接中
 */
@Composable
private fun ConnectionButton(
    status: ConnectionStatus,
    onClick: () -> Unit
) {
    val color = when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF3FB950)
        ConnectionStatus.CONNECTING -> Color(0xFFF59E0B)
        ConnectionStatus.FAILED -> Color(0xFFEF4444)
        else -> Color(0xFFB0B8C4)
    }

    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
    )
}
