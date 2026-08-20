package com.gamepad.controller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.data.model.ImuData

/**
 * IMU 数据列表
 *
 * 显示 IMU 关键数据，点击可查看详情
 */
@Composable
fun ImuDataList(
    imuData: ImuData?,
    onImuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (imuData == null) {
            // 无数据状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F8F8))
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "等待 IMU 数据...",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
        } else {
            // 姿态角（从四元数转换）
            val attitude = imuData.euler

            DataCard(
                title = "姿态角",
                onClick = onImuClick
            ) {
                DataRow("俯仰 (Pitch)", "${String.format("%.1f", attitude.pitch)}°")
                DataRow("横滚 (Roll)", "${String.format("%.1f", attitude.roll)}°")
                DataRow("偏航 (Yaw)", "${String.format("%.1f", attitude.yaw)}°")
            }

            // 线性加速度
            DataCard(
                title = "线性加速度 (m/s²)",
                onClick = onImuClick
            ) {
                DataRow(
                    "X",
                    String.format("%.2f", imuData.linAccX),
                    getAccelColor(imuData.linAccX)
                )
                DataRow(
                    "Y",
                    String.format("%.2f", imuData.linAccY),
                    getAccelColor(imuData.linAccY)
                )
                DataRow(
                    "Z",
                    String.format("%.2f", imuData.linAccZ),
                    getAccelColor(imuData.linAccZ - 9.8f)
                )
            }

            // 角速度
            DataCard(
                title = "角速度 (rad/s)",
                onClick = onImuClick
            ) {
                DataRow(
                    "X",
                    String.format("%.4f", imuData.angVelX),
                    getGyroColor(imuData.angVelX)
                )
                DataRow(
                    "Y",
                    String.format("%.4f", imuData.angVelY),
                    getGyroColor(imuData.angVelY)
                )
                DataRow(
                    "Z",
                    String.format("%.4f", imuData.angVelZ),
                    getGyroColor(imuData.angVelZ)
                )
            }

            // 温度
            DataCard(
                title = "传感器信息",
                onClick = onImuClick
            ) {
                DataRow("温度", String.format("%.1f °C", imuData.temp))
            }
        }
    }
}

/**
 * 数据卡片
 */
@Composable
private fun DataCard(
    title: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F8F8))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666)
            )

            content()
        }
    }
}

/**
 * 数据行
 */
@Composable
private fun DataRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1A1A2E)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * 获取加速度颜色（根据偏差）
 */
private fun getAccelColor(value: Float): Color {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue > 2f -> Color(0xFFEF4444)   // 红色
        absValue > 1f -> Color(0xFFF59E0B)   // 黄色
        else -> Color(0xFF1A1A2E)            // 正常
    }
}

/**
 * 获取角速度颜色
 */
private fun getGyroColor(value: Float): Color {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue > 10f -> Color(0xFFEF4444)  // 红色
        absValue > 5f -> Color(0xFFF59E0B)   // 黄色
        else -> Color(0xFF1A1A2E)            // 正常
    }
}
