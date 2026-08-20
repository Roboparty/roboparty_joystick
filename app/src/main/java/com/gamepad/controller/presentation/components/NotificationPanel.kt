package com.gamepad.controller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.data.model.BatteryData
import com.gamepad.controller.data.model.ImuData
import com.gamepad.controller.data.model.MotorStatus

/**
 * 通知面板 — 屏幕下半部分，单卡片布局
 *
 * 风格与设置面板保持一致，显示 BMS 电池数据、IMU 数据和电机数据
 */
@Composable
fun NotificationPanel(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    batteryData: BatteryData?,
    imuData: ImuData? = null,
    motors: List<MotorStatus> = emptyList(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 24.dp)
    ) {
        // 卡片
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp)
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
                    text = "通知",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
            }

            // 数据内容（可滚动）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BMS 电池数据
                BatterySection(batteryData = batteryData, onRefresh = onRefresh)

                // IMU 惯性数据
                ImuSection(imuData = imuData)

                // 电机数据
                MotorSection(motors = motors)
            }
        }
    }
}

/**
 * BMS 电池数据区域
 */
@Composable
private fun BatterySection(
    batteryData: BatteryData?,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BMS 电池状态",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )

            // 刷新按钮
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "刷新",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onRefresh),
                tint = Color(0xFF2D7FF9)
            )
        }

        if (batteryData == null || batteryData.voltage < 0) {
            // 无数据状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无电池数据",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
        } else {
            // 电量指示
            BatteryLevelIndicator(soc = batteryData.soc)

            // 数据卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 电压
                BatteryDataCard(
                    icon = Icons.Filled.Power,
                    label = "电压",
                    value = "${batteryData.voltage} V",
                    color = Color(0xFF2D7FF9),
                    modifier = Modifier.weight(1f)
                )

                // 电流
                BatteryDataCard(
                    icon = Icons.Filled.ElectricBolt,
                    label = "电流",
                    value = "${batteryData.current} A",
                    color = if (batteryData.current < 0) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 电量
                BatteryDataCard(
                    icon = Icons.Filled.BatteryChargingFull,
                    label = "电量",
                    value = "${batteryData.soc} %",
                    color = when {
                        batteryData.soc > 60 -> Color(0xFF10B981)
                        batteryData.soc > 20 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    },
                    modifier = Modifier.weight(1f)
                )

                // 温度
                BatteryDataCard(
                    icon = Icons.Filled.Thermostat,
                    label = "温度",
                    value = "${batteryData.temp} °C",
                    color = when {
                        batteryData.temp > 45 -> Color(0xFFEF4444)
                        batteryData.temp > 35 -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 充放电状态
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F9FA))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (batteryData.current < 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (batteryData.current < 0) Color(0xFFEF4444) else Color(0xFF10B981)
                )
                Text(
                    text = if (batteryData.current < 0) "放电中" else if (batteryData.current > 0) "充电中" else "空闲",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * IMU 惯性数据区域
 */
@Composable
private fun ImuSection(imuData: ImuData?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题
        Text(
            text = "IMU 惯性数据",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E)
        )

        if (imuData == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无 IMU 数据",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
        } else {
            // 姿态角（从四元数转换）
            val attitude = imuData.euler

            // 姿态卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImuDataCard(
                    icon = Icons.Filled.Explore,
                    label = "俯仰 (Pitch)",
                    value = "${String.format("%.1f", attitude.pitch)}°",
                    color = Color(0xFF2D7FF9),
                    modifier = Modifier.weight(1f)
                )
                ImuDataCard(
                    icon = Icons.Filled.Explore,
                    label = "横滚 (Roll)",
                    value = "${String.format("%.1f", attitude.roll)}°",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                ImuDataCard(
                    icon = Icons.Filled.Explore,
                    label = "偏航 (Yaw)",
                    value = "${String.format("%.1f", attitude.yaw)}°",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            // 加速度卡片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImuDataCard(
                    icon = Icons.Filled.Speed,
                    label = "加速度 X",
                    value = "${String.format("%.2f", imuData.linAccX)} m/s²",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                ImuDataCard(
                    icon = Icons.Filled.Speed,
                    label = "加速度 Y",
                    value = "${String.format("%.2f", imuData.linAccY)} m/s²",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                ImuDataCard(
                    icon = Icons.Filled.Speed,
                    label = "加速度 Z",
                    value = "${String.format("%.2f", imuData.linAccZ)} m/s²",
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
            }

            // 角速度和温度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImuDataCard(
                    icon = Icons.Filled.Refresh,
                    label = "角速度",
                    value = "${String.format("%.3f", imuData.totalAngularVelocity)} rad/s",
                    color = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f)
                )
                ImuDataCard(
                    icon = Icons.Filled.Thermostat,
                    label = "温度",
                    value = "${String.format("%.1f", imuData.temp)} °C",
                    color = when {
                        imuData.temp > 45 -> Color(0xFFEF4444)
                        imuData.temp > 35 -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 电机数据区域
 */
@Composable
private fun MotorSection(motors: List<MotorStatus>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "电机状态",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
            Text(
                text = "${motors.size} 个电机",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }

        if (motors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无电机数据",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
        } else {
            // 展示所有电机（每行4个，紧凑布局）
            motors.chunked(4).forEach { rowMotors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowMotors.forEach { motor ->
                        MotorCardCompact(
                            motor = motor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 填充空白
                    repeat(4 - rowMotors.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 单个电机卡片（紧凑版）
 */
@Composable
private fun MotorCardCompact(
    motor: MotorStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = when (motor.statusType) {
        com.gamepad.controller.data.model.MotorStatusType.NORMAL -> Color(0xFF10B981)
        com.gamepad.controller.data.model.MotorStatusType.WARNING -> Color(0xFFF59E0B)
        com.gamepad.controller.data.model.MotorStatusType.ERROR -> Color(0xFFEF4444)
        com.gamepad.controller.data.model.MotorStatusType.DISABLED -> Color(0xFF999999)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 状态点
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        // 电机ID
        Text(
            text = "M${motor.id}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        // 温度
        Text(
            text = "${String.format("%.0f", motor.temperature)}°",
            fontSize = 9.sp,
            color = if (motor.temperature > 60) Color(0xFFEF4444) else Color(0xFF999999)
        )
    }
}

/**
 * 电量指示器
 */
@Composable
private fun BatteryLevelIndicator(soc: Float) {
    val color = when {
        soc > 60 -> Color(0xFF10B981)
        soc > 20 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "电量",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "${soc.toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (soc / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

/**
 * 电池数据卡片
 */
@Composable
private fun BatteryDataCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E)
        )
    }
}

/**
 * IMU 数据卡片
 */
@Composable
private fun ImuDataCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FA))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF666666)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A2E)
        )
    }
}
