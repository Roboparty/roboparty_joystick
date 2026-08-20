package com.gamepad.controller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.data.model.SkillAction
import com.gamepad.controller.data.model.SkillActionRegistry
import com.gamepad.controller.domain.model.ActionRequestState
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * 技能库面板 — 屏幕下半部分，单卡片布局
 *
 * 风格与设置面板保持一致，顶部带表情/动作/才艺导航
 */
@Composable
fun SkillLibraryPanel(
    onBack: () -> Unit,
    onExecuteAction: (String) -> Unit,
    actionStates: Map<String, StateFlow<ActionRequestState>>,
    currentActionId: String?,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("表情", "动作", "才艺")
    val isAnyActionExecuting = currentActionId != null

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
                    text = "技能库",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )

                // 显示当前执行的动作
                if (isAnyActionExecuting && currentActionId != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    val actionName = SkillActionRegistry.getAction(currentActionId)?.name ?: currentActionId
                    Text(
                        text = "执行中: $actionName",
                        fontSize = 12.sp,
                        color = Color(0xFF2D7FF9),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 顶部标签导航
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedTabIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Brush.linearGradient(
                                    colors = listOf(Color(0xFF2D7FF9), Color(0xFF6C5CE7))
                                ) else Brush.linearGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent)
                                )
                            )
                            .clickable { selectedTabIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF1A1A2E)
                        )
                    }
                }
            }

            // 内容区域
            val category = when (selectedTabIndex) {
                0 -> "expression"
                1 -> "action"
                2 -> "talent"
                else -> "action"
            }

            val actions = SkillActionRegistry.getActionsByCategory(category)

            if (actions.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (category) {
                            "expression" -> Icons.Filled.Mood
                            "talent" -> Icons.Filled.MusicNote
                            else -> Icons.Filled.Handshake
                        },
                        contentDescription = null,
                        modifier = Modifier.width(48.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无${tabs[selectedTabIndex]}技能",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }
            } else {
                // 动作列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(actions) { action ->
                        val stateFlow = actionStates[action.id]
                        val state by (stateFlow?.collectAsState() ?: remember {
                            mutableStateOf(ActionRequestState.Idle)
                        })

                        val isThisActionExecuting = currentActionId == action.id
                        val isDisabled = isAnyActionExecuting && !isThisActionExecuting

                        SwipeToExecuteCard(
                            action = action,
                            state = state,
                            isDisabled = isDisabled,
                            onExecute = { onExecuteAction(action.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 滑动执行卡片
 */
@Composable
private fun SwipeToExecuteCard(
    action: SkillAction,
    state: ActionRequestState,
    isDisabled: Boolean,
    onExecute: () -> Unit
) {
    val density = LocalDensity.current
    val maxSwipeDp = 200.dp
    val maxSwipePx = with(density) { maxSwipeDp.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var executed by remember { mutableStateOf(false) }

    // 根据状态确定颜色和文字
    val backgroundColor = when {
        isDisabled -> Color(0xFFF5F5F5)  // 禁用状态灰色
        state is ActionRequestState.Idle -> Color(0xFFF0F4FF)
        state is ActionRequestState.Sending -> Color(0xFFFFF8E1)
        state is ActionRequestState.Executing -> Color(0xFFE3F2FD)
        state is ActionRequestState.Success -> Color(0xFFE8F5E9)
        state is ActionRequestState.Failed -> Color(0xFFFFEBEE)
        state is ActionRequestState.Timeout -> Color(0xFFFFF3E0)
        else -> Color(0xFFF0F4FF)
    }

    val statusText = when {
        isDisabled -> "其他动作执行中"
        state is ActionRequestState.Idle -> "→ 滑动执行"
        state is ActionRequestState.Sending -> "发送中..."
        state is ActionRequestState.Executing -> "执行中..."
        state is ActionRequestState.Success -> "✓ 执行成功"
        state is ActionRequestState.Failed -> "❌ ${state.reason}"
        state is ActionRequestState.Timeout -> "❓ 状态未知"
        else -> ""
    }

    val statusColor = when {
        isDisabled -> Color(0xFFCCCCCC)
        state is ActionRequestState.Idle -> Color(0xFF2D7FF9).copy(alpha = 0.5f)
        state is ActionRequestState.Sending -> Color(0xFFF59E0B)
        state is ActionRequestState.Executing -> Color(0xFF2D7FF9)
        state is ActionRequestState.Success -> Color(0xFF10B981)
        state is ActionRequestState.Failed -> Color(0xFFEF4444)
        state is ActionRequestState.Timeout -> Color(0xFFF59E0B)
        else -> Color(0xFFCCCCCC)
    }

    val statusIcon = when {
        isDisabled -> Icons.Filled.Timer
        state is ActionRequestState.Idle -> Icons.AutoMirrored.Filled.Send
        state is ActionRequestState.Sending -> Icons.Filled.Refresh
        state is ActionRequestState.Executing -> Icons.Filled.Timer
        state is ActionRequestState.Success -> Icons.Filled.CheckCircle
        state is ActionRequestState.Failed -> Icons.Filled.Error
        state is ActionRequestState.Timeout -> Icons.AutoMirrored.Filled.Help
        else -> Icons.AutoMirrored.Filled.Send
    }

    // 是否可交互
    val isInteractive = !isDisabled && (state is ActionRequestState.Idle || state is ActionRequestState.Failed || state is ActionRequestState.Timeout)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
    ) {
        // 背景提示
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor
            )
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // 可滑动卡片
        val cardColor = when {
            isDisabled -> Brush.linearGradient(
                colors = listOf(Color(0xFFCCCCCC), Color(0xFFBBBBBB))
            )
            state is ActionRequestState.Success -> Brush.linearGradient(
                colors = listOf(Color(0xFF10B981), Color(0xFF059669))
            )
            state is ActionRequestState.Failed -> Brush.linearGradient(
                colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
            )
            else -> Brush.linearGradient(
                colors = listOf(Color(0xFF2D7FF9), Color(0xFF6C5CE7))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .width(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardColor)
                .pointerInput(isInteractive) {
                    if (isInteractive) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > maxSwipePx * 0.7f) {
                                    offsetX = maxSwipePx
                                    if (!executed) {
                                        executed = true
                                        onExecute()
                                    }
                                } else {
                                    offsetX = 0f
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (offsetX + dragAmount).coerceIn(0f, maxSwipePx)
                                if (!executed) {
                                    offsetX = newOffset
                                }
                            }
                        )
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Handshake,
                contentDescription = null,
                tint = if (isDisabled) Color(0xFF999999) else Color.White,
                modifier = Modifier.width(28.dp)
            )
            Column {
                Text(
                    text = action.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDisabled) Color(0xFF999999) else Color.White
                )
                Text(
                    text = when {
                        isDisabled -> "等待中"
                        state is ActionRequestState.Idle -> "→ 滑动"
                        state is ActionRequestState.Sending -> "发送中..."
                        state is ActionRequestState.Executing -> "执行中..."
                        state is ActionRequestState.Success -> "完成 ✓"
                        state is ActionRequestState.Failed -> "失败"
                        state is ActionRequestState.Timeout -> "超时"
                        else -> ""
                    },
                    fontSize = 11.sp,
                    color = if (isDisabled) Color(0xFF999999) else Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // 成功/失败后重置
        if (state is ActionRequestState.Success || state is ActionRequestState.Failed || state is ActionRequestState.Timeout) {
            executed = false
            offsetX = 0f
        }
    }
}
