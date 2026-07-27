package com.gamepad.controller.presentation.components

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 顶部信号栏 — 40px 高
 *
 * 左侧：系统当前时间 + WiFi 信号强度
 */
@Composable
fun StatusBar(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(formatTime(System.currentTimeMillis())) }
    val wifiLevel by rememberWifiLevel()

    // 每秒更新时间
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatTime(System.currentTimeMillis())
            delay(1000L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.Transparent)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间
        Text(
            text = currentTime,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A2E)
        )

        // WiFi 信号强度
        WifiSignalIndicator(level = wifiLevel)
    }
}

/**
 * WiFi 图标 — 传统同心弧线 + 底部圆点
 *
 * level 0~4 控制显示几条弧线
 */
@Composable
private fun WifiSignalIndicator(level: Int) {
    val activeColor = Color(0xFF1A1A2E)
    val inactiveColor = Color(0xFFD0D0D0)

    Canvas(modifier = Modifier.size(16.dp)) {
        val cx = size.width / 2f
        val cy = size.height * 0.85f
        val dotRadius = 1.8.dp.toPx()
        val strokeW = 2.4.dp.toPx()

        // 底部圆点（始终显示）
        drawCircle(
            color = if (level > 0) activeColor else inactiveColor,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(cx, cy)
        )

        // 4 条弧线，从内到外，顺时针旋转90°
        val arcRadii = listOf(4.dp.toPx(), 7.dp.toPx(), 10.dp.toPx(), 13.dp.toPx())
        arcRadii.forEachIndexed { index, radius ->
            val arcLevel = index + 1
            val color = if (arcLevel <= level) activeColor else inactiveColor
            drawArc(
                color = color,
                startAngle = 315f,
                sweepAngle = -90f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
            )
        }
    }
}

/**
 * 获取当前 WiFi 信号等级 0~4
 */
@Composable
private fun rememberWifiLevel(): androidx.compose.runtime.State<Int> {
    val context = LocalContext.current
    val level = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            level.value = getWifiLevel(context)
            delay(3000L)
        }
    }

    return level
}

private fun getWifiLevel(context: Context): Int {
    return try {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        WifiManager.calculateSignalLevel(info.rssi, 5)
    } catch (_: Exception) {
        0
    }
}

private fun formatTime(millis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
}
