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
import com.gamepad.controller.data.model.ImuData

/**
 * IMU 详情弹窗
 *
 * 显示完整的 IMU 数据，包括姿态角、加速度、角速度、磁场
 */
@Composable
fun ImuDetailCard(
    imuData: ImuData?,
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
                    text = "IMU 数据详情",
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

            if (imuData == null) {
                // 无数据状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "等待 IMU 数据...",
                        fontSize = 16.sp,
                        color = Color(0xFF999999)
                    )
                }
            } else {
                // 姿态角
                InfoSection(title = "姿态角 (Attitude)") {
                    AngleRow("俯仰角 (Pitch)", imuData.pitch, -90f, 90f)
                    AngleRow("横滚角 (Roll)", imuData.roll, -180f, 180f)
                    AngleRow("偏航角 (Yaw)", imuData.yaw, 0f, 360f)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 加速度
                InfoSection(title = "加速度 (m/s²)") {
                    VectorRow("X", imuData.accelX)
                    VectorRow("Y", imuData.accelY)
                    VectorRow("Z", imuData.accelZ)

                    Spacer(modifier = Modifier.height(8.dp))

                    // 合加速度
                    DataRow("合加速度", String.format("%.2f m/s²", imuData.totalAcceleration))

                    // 重力分量偏差
                    DataRow(
                        "重力偏差",
                        String.format("%.2f m/s²", imuData.gravityDeviation),
                        if (kotlin.math.abs(imuData.gravityDeviation) > 0.5f) Color(0xFFF59E0B) else Color(0xFF1A1A2E)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 角速度
                InfoSection(title = "角速度 (°/s)") {
                    VectorRow("X", imuData.gyroX)
                    VectorRow("Y", imuData.gyroY)
                    VectorRow("Z", imuData.gyroZ)

                    Spacer(modifier = Modifier.height(8.dp))

                    // 合角速度
                    DataRow("合角速度", String.format("%.2f °/s", imuData.totalAngularVelocity))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 磁场
                InfoSection(title = "磁场 (uT)") {
                    VectorRow("X", imuData.magX)
                    VectorRow("Y", imuData.magY)
                    VectorRow("Z", imuData.magZ)

                    Spacer(modifier = Modifier.height(8.dp))

                    // 合磁场强度
                    DataRow("合磁场强度", String.format("%.1f uT", imuData.totalMagneticField))
                }

                // 错误信息
                if (imuData.imuError != 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "⚠ IMU 错误码: ${imuData.imuError}",
                            fontSize = 14.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 角度行（带进度条）
 */
@Composable
private fun AngleRow(
    label: String,
    value: Float,
    min: Float,
    max: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "${String.format("%.1f", value)}°",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A2E)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 进度条
        val progress = (value - min) / (max - min)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE8E8E8))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF667EEA))
            )
        }
    }
}

/**
 * 向量行
 */
@Composable
private fun VectorRow(
    label: String,
    value: Float
) {
    DataRow(
        label,
        String.format("%.2f", value),
        if (kotlin.math.abs(value) > 10f) Color(0xFFF59E0B) else Color(0xFF1A1A2E)
    )
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
