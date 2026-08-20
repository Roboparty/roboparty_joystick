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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.gamepad.controller.data.model.MotorStatus
import com.gamepad.controller.data.model.MotorStatusType

/**
 * 电机状态列表
 *
 * 显示左右电机的状态信息，点击可查看详情
 */
@Composable
fun MotorStatusList(
    motors: List<MotorStatus>,
    onMotorClick: (MotorStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        motors.forEach { motor ->
            MotorStatusItem(
                motor = motor,
                onClick = { onMotorClick(motor) }
            )
        }
    }
}

/**
 * 单个电机状态项
 */
@Composable
private fun MotorStatusItem(
    motor: MotorStatus,
    onClick: () -> Unit
) {
    val statusColor = when (motor.statusType) {
        MotorStatusType.NORMAL -> Color(0xFF3FB950)
        MotorStatusType.WARNING -> Color(0xFFF59E0B)
        MotorStatusType.ERROR -> Color(0xFFEF4444)
        MotorStatusType.DISABLED -> Color(0xFF9CA3AF)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F8F8))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            // 顶部：电机名称和状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态指示灯
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    // 电机名称
                    Text(
                        text = "电机 ${motor.id}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                }

                // 状态文本
                Text(
                    text = motor.statusType.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            // 中间：关键数据
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 位置
                DataChip(
                    label = "位置",
                    value = "${String.format("%.2f", motor.position)} rad",
                    color = Color(0xFF667EEA)
                )

                // 速度
                DataChip(
                    label = "速度",
                    value = "${String.format("%.3f", motor.speed)} rad/s",
                    color = Color(0xFF8B5CF6)
                )

                // 温度
                DataChip(
                    label = "温度",
                    value = "${motor.temperature.toInt()}°C",
                    color = getTemperatureColor(motor.temperature)
                )

                // 模式
                DataChip(
                    label = "模式",
                    value = motor.modeDescription,
                    color = Color(0xFF6B7280)
                )
            }

            // 错误信息（如果有错误）
            if (motor.errorCode != 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠ 错误 ${motor.errorCode}: ${com.gamepad.controller.data.model.MotorErrorCode.getDescription(motor.errorCode)}",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

/**
 * 数据芯片
 */
@Composable
private fun DataChip(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * 获取温度颜色
 */
private fun getTemperatureColor(temp: Float): Color = when {
    temp > 70 -> Color(0xFFEF4444)  // 红色 - 过热
    temp > 60 -> Color(0xFFF59E0B)  // 黄色 - 警告
    temp > 50 -> Color(0xFF3B82F6)  // 蓝色 - 偏高
    else -> Color(0xFF3FB950)       // 绿色 - 正常
}
