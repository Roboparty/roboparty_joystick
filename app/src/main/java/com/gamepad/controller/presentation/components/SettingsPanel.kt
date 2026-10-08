// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.domain.model.OtaState
import com.gamepad.controller.presentation.components.settings.AboutSettings
import com.gamepad.controller.presentation.components.settings.AuthPasswordSettings
import com.gamepad.controller.presentation.components.settings.MotionSettings
import com.gamepad.controller.presentation.components.settings.NetworkSettings
import com.gamepad.controller.presentation.components.settings.OtaSettings
import com.gamepad.controller.presentation.components.settings.VoiceSettings
import com.gamepad.controller.presentation.components.settings.VolumeSettings

/**
 * 设置导航项
 */
private data class SettingsNavItem(
    val icon: ImageVector,
    val label: String
)

/**
 * 设置面板 — 屏幕下半部分，左右双卡片布局
 *
 * 左侧窄卡片：标题 + 功能导航列表
 * 右侧宽卡片：对应功能的具体设置内容
 */
@Composable
fun SettingsPanel(
    onBack: () -> Unit,
    isEstopping: Boolean = false,
    estopStatus: com.gamepad.controller.domain.model.EstopStatus = com.gamepad.controller.domain.model.EstopStatus.IDLE,
    onEstopChanged: (Boolean) -> Unit,
    serverHost: String = "10.43.21.32",
    serverPort: Int = 9000,
    onServerConfigChanged: (host: String, port: Int) -> Unit = { _, _ -> },
    otaState: OtaState = OtaState(),
    onCheckOtaUpdate: () -> Unit = {},
    onDownloadOta: () -> Unit = {},
    onInstallOta: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNavIndex by remember { mutableStateOf(0) }

    val navItems = listOf(
        SettingsNavItem(Icons.Filled.NetworkWifi, "网络"),
        SettingsNavItem(Icons.Filled.Key, "授权密码"),
        SettingsNavItem(Icons.Filled.Speed, "运动"),
        SettingsNavItem(Icons.Filled.SettingsVoice, "语音"),
        SettingsNavItem(Icons.AutoMirrored.Filled.VolumeUp, "音量"),
        SettingsNavItem(Icons.Filled.Info, "关于"),
        SettingsNavItem(Icons.Filled.SystemUpdate, "OTA更新"),
        SettingsNavItem(Icons.Filled.Refresh, "还原")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- 左侧窄卡片：导航 ----
        Column(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp)
        ) {
            // 标题行 + 返回按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(4.dp),
                    tint = Color(0xFF666666)
                )
                Text(
                    text = "设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
            }

            // 导航列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = index == selectedNavIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFFF0F4FF) else Color.Transparent
                            )
                            .clickable { selectedNavIndex = index }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.width(18.dp),
                            tint = if (isSelected) Color(0xFF2D7FF9) else Color(0xFF666666)
                        )
                        Text(
                            text = item.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF2D7FF9) else Color(0xFF1A1A2E)
                        )
                    }
                }
            }
        }

        // ---- 右侧宽卡片：内容 ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedNavIndex) {
                    0 -> NetworkSettings(
                        serverHost = serverHost,
                        serverPort = serverPort,
                        onServerConfigChanged = onServerConfigChanged
                    )
                    1 -> AuthPasswordSettings()
                    2 -> MotionSettings(
                        isEstopping = isEstopping,
                        estopStatus = estopStatus,
                        onEstopChanged = onEstopChanged
                    )
                    3 -> VoiceSettings()
                    4 -> VolumeSettings()
                    5 -> AboutSettings()
                    6 -> OtaSettings(
                        otaState = otaState,
                        onCheckUpdate = onCheckOtaUpdate,
                        onDownload = onDownloadOta,
                        onInstall = onInstallOta
                    )
                    7 -> {
                        // 还原 — 空内容
                        Text(
                            text = "还原设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF999999)
                        )
                    }
                }
            }
        }
    }
}
