package com.gamepad.controller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.data.model.MotorErrorCode
import com.gamepad.controller.data.model.MotorStatus
import com.gamepad.controller.data.model.MotorStatusType

/**
 * 电机详情弹窗
 *
 * 显示单个电机的详细信息
 */
@Composable
fun MotorDetailCard(
    motor: MotorStatus,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .clickable(enabled = true, onClick = {})
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "电机 ${motor.id} 详情",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E)
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() },
                    tint = Color(0xFF999999)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 状态概览
            StatusOverview(motor)

            Spacer(modifier = Modifier.height(16.dp))

            // 运行数据
            InfoSection(title = "运行数据") {
                InfoRow("位置", "${String.format("%.4f", motor.position)} rad")
                InfoRow("速度", "${String.format("%.4f", motor.speed)} rad/s")
                InfoRow("扭矩", "${String.format("%.4f", motor.torque)} Nm")
                InfoRow("温度", "${String.format("%.1f", motor.temperature)}°C", getTemperatureColor(motor.temperature))
                InfoRow("模式", motor.modeDescription)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 错误信息
            InfoSection(title = "错误信息") {
                InfoRow(
                    "错误码",
                    "${MotorErrorCode.toHexString(motor.errorCode)} (${
                        if (motor.errorCode == 0) "无错误" else motor.errorCode.toString()
                    })"
                )
                InfoRow("错误描述", MotorErrorCode.getDescription(motor.errorCode))

                val suggestion = MotorErrorCode.getSuggestion(motor.errorCode)
                if (suggestion.isNotEmpty()) {
                    InfoRow("处理建议", suggestion, Color(0xFFF59E0B))
                }
            }
        }
    }
}

/**
 * 状态概览
 */
@Composable
private fun StatusOverview(motor: MotorStatus) {
    val statusColor = when (motor.statusType) {
        MotorStatusType.NORMAL -> Color(0xFF3FB950)
        MotorStatusType.WARNING -> Color(0xFFF59E0B)
        MotorStatusType.ERROR -> Color(0xFFEF4444)
        MotorStatusType.DISABLED -> Color(0xFF9CA3AF)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(statusColor.copy(alpha = 0.1f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态图标（用文字模拟）
            Text(
                text = when (motor.statusType) {
                    MotorStatusType.NORMAL -> "✓"
                    MotorStatusType.WARNING -> "⚠"
                    MotorStatusType.ERROR -> "✕"
                    MotorStatusType.DISABLED -> "○"
                },
                fontSize = 32.sp,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = motor.statusType.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )

            if (motor.errorCode != 0) {
                Text(
                    text = MotorErrorCode.getDescription(motor.errorCode),
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * 信息区块
 */
@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F8F8))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1A1A2E)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * 获取温度颜色
 */
private fun getTemperatureColor(temp: Float): Color = when {
    temp > 70 -> Color(0xFFEF4444)  // 红色
    temp > 60 -> Color(0xFFF59E0B)  // 黄色
    else -> Color(0xFF1A1A2E)       // 正常
}
