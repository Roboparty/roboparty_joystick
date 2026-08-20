package com.gamepad.controller.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamepad.controller.data.model.enums.ConnectionStatus
import kotlinx.coroutines.delay

/**
 * 连接对话框
 *
 * 功能：
 * - 显示服务器 IP 和端口输入
 * - 显示连接状态和进度
 * - 确认后进行连接
 * - 连接成功后自动关闭
 */
@Composable
fun ConnectionDialog(
    serverHost: String,
    serverPort: Int,
    connectionStatus: ConnectionStatus,
    onDismiss: () -> Unit,
    onConnect: (host: String, port: Int) -> Unit,
    onDisconnect: () -> Unit,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier
) {
    var editHost by remember { mutableStateOf(serverHost) }
    var editPort by remember { mutableStateOf(serverPort.toString()) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionMessage by remember { mutableStateOf("") }

    // 连接状态监听
    LaunchedEffect(connectionStatus) {
        when (connectionStatus) {
            ConnectionStatus.CONNECTING -> {
                isConnecting = true
                connectionMessage = "正在连接服务器..."
            }
            ConnectionStatus.CONNECTED -> {
                isConnecting = false
                connectionMessage = "连接成功！"
                delay(1000)
                onDismiss()
            }
            ConnectionStatus.FAILED -> {
                isConnecting = false
                connectionMessage = "连接失败，请检查服务器地址"
            }
            ConnectionStatus.DISCONNECTED -> {
                isConnecting = false
                connectionMessage = ""
            }
            else -> {
                isConnecting = false
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isConnecting) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isConnecting,
            dismissOnClickOutside = !isConnecting
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "连接机器人",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                    if (!isConnecting) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF333333)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 服务器图标
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F4FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (connectionStatus == ConnectionStatus.CONNECTED)
                            Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> Color(0xFF3FB950)
                            ConnectionStatus.CONNECTING -> Color(0xFF2D7FF9)
                            ConnectionStatus.FAILED -> Color(0xFFEF4444)
                            else -> Color(0xFF333333)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 连接状态文本
                AnimatedVisibility(
                    visible = connectionMessage.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = connectionMessage,
                        fontSize = 14.sp,
                        color = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> Color(0xFF3FB950)
                            ConnectionStatus.CONNECTING -> Color(0xFF2D7FF9)
                            ConnectionStatus.FAILED -> Color(0xFFEF4444)
                            else -> Color(0xFF666666)
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // 服务器地址输入
                OutlinedTextField(
                    value = editHost,
                    onValueChange = { editHost = it },
                    label = { Text("服务器 IP 地址") },
                    placeholder = { Text("例如: 192.168.1.100") },
                    singleLine = true,
                    enabled = !isConnecting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2D7FF9),
                        focusedLabelColor = Color(0xFF2D7FF9),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 端口输入
                OutlinedTextField(
                    value = editPort,
                    onValueChange = { editPort = it.filter { c -> c.isDigit() } },
                    label = { Text("端口号") },
                    placeholder = { Text("例如: 9000") },
                    singleLine = true,
                    enabled = !isConnecting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2D7FF9),
                        focusedLabelColor = Color(0xFF2D7FF9),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier.width(150.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 断开按钮（已连接时显示）
                    if (connectionStatus == ConnectionStatus.CONNECTED) {
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("断开连接")
                        }
                    }

                    // 连接/取消按钮
                    Button(
                        onClick = {
                            if (isConnecting) {
                                onDisconnect()
                            } else {
                                val port = editPort.toIntOrNull() ?: 9000
                                onConnect(editHost, port)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnecting)
                                Color(0xFF666666) else Color(0xFF2D7FF9)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isConnecting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Text("取消")
                            }
                        } else {
                            Text(
                                text = if (connectionStatus == ConnectionStatus.CONNECTED)
                                    "重新连接" else "连接"
                            )
                        }
                    }
                }

                // 连接中显示进度条
                AnimatedVisibility(
                    visible = isConnecting,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF2D7FF9),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "正在连接 $editHost:$editPort",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }
            }
        }
    }
}
