// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 摇杆可视化圆盘 — 左下角 200px
 *
 * 双向交互：
 * - 实时反映手柄摇杆位置（stickX, stickY）
 * - 触屏拖拽圆盘可发送摇杆控制（onMove / onRelease）
 *
 * @param stickX 左摇杆 X 轴（-1=左, 1=右）
 * @param stickY 左摇杆 Y 轴（-1=上, 1=下）
 * @param onMove 触屏拖拽回调 (x, y) 范围 -1~1
 * @param onRelease 触屏释放回调
 */
@Composable
fun JoystickDisc(
    stickX: Float,
    stickY: Float,
    onMove: (Float, Float) -> Unit = { _, _ -> },
    onRelease: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF2D7FF9)
    val trackColor = Color(0xFFE2E8F0)
    val knobColor = Color.White
    val tickColor = Color(0xFFB0B8C4)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                val radius = min(size.width, size.height) / 2f * 0.85f
                val knobRadius = radius * 0.35f
                val maxDist = radius - knobRadius
                val cx = size.width / 2f
                val cy = size.height / 2f

                detectDragGestures(
                    onDragStart = { offset ->
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist <= radius) {
                            val clampedDist = min(dist, maxDist)
                            val angle = atan2(dy, dx)
                            val nx = (cos(angle) * clampedDist / maxDist)
                            val ny = (sin(angle) * clampedDist / maxDist)
                            onMove(nx, ny)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        val clampedDist = min(dist, maxDist)
                        val angle = atan2(dy, dx)
                        val nx = (cos(angle) * clampedDist / maxDist)
                        val ny = (sin(angle) * clampedDist / maxDist)
                        onMove(nx, ny)
                    },
                    onDragEnd = { onRelease() },
                    onDragCancel = { onRelease() }
                )
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.85f
        val knobRadius = radius * 0.35f
        val strokeW = 2.5.dp.toPx()

        // ---- 外圈轨道 ----
        drawCircle(
            color = trackColor,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = strokeW)
        )

        // ---- 刻度点（12 个） ----
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30.0))
            val tickX = cx + cos(angle).toFloat() * radius
            val tickY = cy + sin(angle).toFloat() * radius
            drawCircle(
                color = tickColor,
                radius = 2.dp.toPx(),
                center = Offset(tickX, tickY)
            )
        }

        // ---- 十字准线 ----
        val crossLen = radius * 0.15f
        drawLine(trackColor, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), 1.dp.toPx())
        drawLine(trackColor, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), 1.dp.toPx())

        // ---- 毛玻璃效果层 ----
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            radius = radius - strokeW / 2f,
            center = Offset(cx, cy)
        )

        // ---- 滑块 ----
        val maxDist = radius - knobRadius
        val clampedX = stickX.coerceIn(-1f, 1f)
        val clampedY = stickY.coerceIn(-1f, 1f)
        val knobCx = cx + clampedX * maxDist
        val knobCy = cy + clampedY * maxDist

        // 滑块阴影
        drawCircle(
            color = Color.Black.copy(alpha = 0.1f),
            radius = knobRadius + 3.dp.toPx(),
            center = Offset(knobCx, knobCy)
        )

        // 滑块本体
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(knobColor, Color(0xFFF0F0F0)),
                center = Offset(knobCx, knobCy),
                radius = knobRadius
            ),
            radius = knobRadius,
            center = Offset(knobCx, knobCy)
        )

        // 滑块边框
        drawCircle(
            color = activeColor.copy(alpha = 0.4f),
            radius = knobRadius,
            center = Offset(knobCx, knobCy),
            style = Stroke(width = 2.dp.toPx())
        )

        // 滑块中心蓝点
        drawCircle(
            color = activeColor,
            radius = 4.dp.toPx(),
            center = Offset(knobCx, knobCy)
        )

        // ---- 连接线 ----
        if (clampedX != 0f || clampedY != 0f) {
            drawLine(
                color = activeColor.copy(alpha = 0.3f),
                start = Offset(cx, cy),
                end = Offset(knobCx, knobCy),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
