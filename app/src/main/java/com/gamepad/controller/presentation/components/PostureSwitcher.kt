package com.gamepad.controller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.domain.model.PostureMode

/**
 * 姿态切换面板 — 左侧上部，卡片样式
 *
 * 标题 + 图标 + 分段控制器（站起 / 坐下 / 待机）
 */
@Composable
fun PostureSwitcher(
    currentPosture: PostureMode,
    onPostureChanged: (PostureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 标题行：图标 + 文字
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccessibilityNew,
                contentDescription = null,
                modifier = Modifier.height(18.dp),
                tint = Color(0xFF2D7FF9)
            )
            Text(
                text = "姿态切换",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 分段控制器
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF5F5F5))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PostureMode.entries.forEach { mode ->
                val isSelected = mode == currentPosture
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) Brush.linearGradient(
                                colors = listOf(Color(0xFF2D7FF9), Color(0xFF6C5CE7))
                            ) else Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.Transparent)
                            )
                        )
                        .clickable { onPostureChanged(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF1A1A2E)
                    )
                }
            }
        }
    }
}
