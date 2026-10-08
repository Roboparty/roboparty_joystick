// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.presentation.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 运动设置
 */
@Composable
fun MotionSettings(
    isEstopping: Boolean = false,
    estopStatus: com.gamepad.controller.domain.model.EstopStatus = com.gamepad.controller.domain.model.EstopStatus.IDLE,
    onEstopChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var maxSpeed by remember { mutableFloatStateOf(0.5f) }
    var estopResponseTime by remember { mutableFloatStateOf(200f) }
    var softwareEstop by remember { mutableStateOf(isEstopping) }

    // 同步外部状态变化
    androidx.compose.runtime.LaunchedEffect(isEstopping) {
        softwareEstop = isEstopping
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "运动设置",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        // 最大遥控速度
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8F9FA))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最大遥控速度",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    text = "${(maxSpeed * 100).toInt()}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D7FF9)
                )
            }
            Slider(
                value = maxSpeed,
                onValueChange = { maxSpeed = it },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2D7FF9),
                    activeTrackColor = Color(0xFF2D7FF9)
                )
            )
            Text(
                text = "控制摇杆的最大输出速度比例",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }

        // 组合键急停响应时间
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8F9FA))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "组合键急停响应时间",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    text = "${estopResponseTime.toInt()}ms",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D7FF9)
                )
            }
            Slider(
                value = estopResponseTime,
                onValueChange = { estopResponseTime = it },
                valueRange = 50f..500f,
                steps = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2D7FF9),
                    activeTrackColor = Color(0xFF2D7FF9)
                )
            )
            Text(
                text = "按下急停组合键后触发急停的延迟时间",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }

        // 软件急停开关
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8F9FA))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "软件急停",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (softwareEstop) Color(0xFFEF4444) else Color(0xFF1A1A2E)
                )
                Switch(
                    checked = softwareEstop,
                    onCheckedChange = {
                        softwareEstop = it
                        onEstopChanged(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFEF4444)
                    )
                )
            }
            // 急停状态显示
            if (softwareEstop) {
                Text(
                    text = estopStatus.description,
                    fontSize = 12.sp,
                    color = when (estopStatus) {
                        com.gamepad.controller.domain.model.EstopStatus.CONFIRMED -> Color(0xFF3FB950)
                        com.gamepad.controller.domain.model.EstopStatus.FAILED -> Color(0xFFEF4444)
                        else -> Color(0xFF999999)
                    }
                )
            }
        }
    }
}
