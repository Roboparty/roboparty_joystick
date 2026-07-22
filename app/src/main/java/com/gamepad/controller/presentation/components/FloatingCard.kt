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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class CardTab(val label: String) {
    FUNCTIONS("功能清单"),
    WARNINGS("告警列表")
}

/**
 * 悬浮卡片 — 全屏遮罩 + 居中弹出
 */
@Composable
fun FloatingCard(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf(CardTab.FUNCTIONS) }
    var estopProgress by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(660.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .clickable(enabled = true, onClick = {})
                .padding(32.dp)
        ) {
            // ---- 标题行 ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "控制面板",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E)
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onDismiss() },
                    tint = Color(0xFF999999)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---- 紧急停机（同一行） ----
            EStopSlider(
                progress = estopProgress,
                onProgressChange = { estopProgress = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ---- 双列 Radio ----
            TabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- 列表内容 ----
            when (selectedTab) {
                CardTab.FUNCTIONS -> FunctionList()
                CardTab.WARNINGS -> WarningList()
            }
        }
    }
}

/**
 * 紧急停机 — 文字 + 滑动条同一行
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EStopSlider(
    progress: Float,
    onProgressChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFF5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⚠ 紧急停机",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEF4444)
        )

        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier.weight(1f),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = sliderState.value)
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444))
                    )
                }
            },
            colors = SliderDefaults.colors()
        )

        Text(
            text = if (progress > 0.9f) "已触发" else "",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEF4444)
        )
    }
}

/**
 * 双列 Tab — 浅灰色选中
 */
@Composable
private fun TabSelector(
    selectedTab: CardTab,
    onTabSelected: (CardTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CardTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFFE8E8E8) else Color.Transparent)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF1A1A2E) else Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * 功能清单列表 — 无图标，紧凑行间距
 */
@Composable
private fun FunctionList() {
    val items = listOf(
        "手柄控制", "姿态切换", "状态监控",
        "急停复位", "参数校准", "信号检测"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8F8F8))
                    .clickable { /* TODO */ }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A2E)
                )
            }
        }
    }
}

/**
 * 告警列表 — 无图标，紧凑行间距
 */
@Composable
private fun WarningList() {
    val warnings = listOf(
        "电机过热警告", "电池电量低", "信号强度不足",
        "连接已恢复", "急停触发记录"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        warnings.forEach { desc ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8F8F8))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = desc,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A2E)
                )
            }
        }
    }
}
