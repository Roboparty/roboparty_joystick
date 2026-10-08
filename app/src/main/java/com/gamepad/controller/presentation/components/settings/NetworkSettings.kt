// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.presentation.components.settings

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * 网络设置内容
 *
 * 显示 WiFi 状态、服务器 IP、当前连接网络、可用网络列表
 */
@Composable
fun NetworkSettings(
    serverHost: String,
    serverPort: Int,
    onServerConfigChanged: (host: String, port: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var editHost by remember { mutableStateOf(serverHost) }
    var editPort by remember { mutableStateOf(serverPort.toString()) }
    val wifiManager = remember { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    var wifiEnabled by remember { mutableStateOf(wifiManager.isWifiEnabled) }
    var currentSsid by remember { mutableStateOf<String?>(null) }
    var availableNetworks by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasLocationPermission by remember {
        mutableStateOf(checkLocationPermission(context))
    }

    // 位置权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        if (hasLocationPermission) {
            refreshWifiInfo(wifiManager, context) { ssid, networks ->
                currentSsid = ssid
                availableNetworks = networks
            }
        }
    }

    // 刷新 WiFi 信息
    fun refreshWifiState() {
        wifiEnabled = wifiManager.isWifiEnabled
        if (wifiEnabled && hasLocationPermission) {
            refreshWifiInfo(wifiManager, context) { ssid, networks ->
                currentSsid = ssid
                availableNetworks = networks
            }
        } else {
            currentSsid = null
            availableNetworks = emptyList()
        }
    }

    // 监听 WiFi 状态变化
    DisposableEffect(Unit) {
        val wifiStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshWifiState()
            }
        }
        val scanResultReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (hasLocationPermission) {
                    refreshWifiInfo(wifiManager, context) { ssid, networks ->
                        currentSsid = ssid
                        availableNetworks = networks
                    }
                }
            }
        }

        val wifiFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        val scanFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

        context.registerReceiver(wifiStateReceiver, wifiFilter)
        context.registerReceiver(scanResultReceiver, scanFilter)

        // 初始化
        if (wifiEnabled && !hasLocationPermission) {
            // 请求位置权限
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            refreshWifiState()
        }

        onDispose {
            try {
                context.unregisterReceiver(wifiStateReceiver)
                context.unregisterReceiver(scanResultReceiver)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- 第一行：无线局域网开关 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F9FA))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (wifiEnabled) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (wifiEnabled) Color(0xFF2D7FF9) else Color(0xFF999999)
                )
                Text(
                    text = "无线局域网",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A2E)
                )
            }
            Switch(
                checked = wifiEnabled,
                onCheckedChange = {
                    // Android 10+ 无法直接开关 WiFi，跳转系统设置面板
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                        context.startActivity(panelIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        wifiManager.isWifiEnabled = !wifiEnabled
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2D7FF9)
                )
            )
        }

        // ---- 第二行：服务器 IP ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F9FA))
                .clickable { showEditDialog = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "机器人服务器",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A2E),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$serverHost:$serverPort",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "编辑",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp),
                tint = Color(0xFF999999)
            )
        }

        // ---- 第三行：当前连接的 WiFi ----
        if (wifiEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8F9FA))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前网络",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (!hasLocationPermission) "需要位置权限" else (currentSsid ?: "未连接"),
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        // ---- 其他网络 ----
        if (wifiEnabled) {
            Text(
                text = "其他网络",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )

            if (!hasLocationPermission) {
                Text(
                    text = "需要位置权限才能扫描可用网络",
                    fontSize = 13.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F9FA))
                        .clickable {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        .padding(horizontal = 16.dp)
                )
            } else {
                // 可用网络列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableNetworks) { network ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F9FA))
                                .clickable { /* 点击连接需要系统 API，暂不实现 */ }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF666666)
                            )
                            Text(
                                text = network,
                                fontSize = 14.sp,
                                color = Color(0xFF1A1A2E)
                            )
                        }
                    }
                }
            }
        }
    }

    // 编辑服务器配置对话框
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "服务器配置",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editHost,
                        onValueChange = { editHost = it },
                        label = { Text("服务器 IP 地址") },
                        placeholder = { Text("例如: 192.168.1.100") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPort,
                        onValueChange = { editPort = it.filter { c -> c.isDigit() } },
                        label = { Text("端口号") },
                        placeholder = { Text("例如: 9000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val port = editPort.toIntOrNull() ?: 9000
                        onServerConfigChanged(editHost, port)
                        showEditDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 检查位置权限
 */
private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * 刷新 WiFi 信息（当前 SSID 和可用网络）
 */
@Suppress("DEPRECATION")
private fun refreshWifiInfo(
    wifiManager: WifiManager,
    @Suppress("UNUSED_PARAMETER") context: Context,
    onUpdate: (ssid: String?, networks: List<String>) -> Unit
) {
    // 获取当前 SSID
    val currentSsid = try {
        val info = wifiManager.connectionInfo
        info?.ssid?.removePrefix("\"")?.removeSuffix("\"")?.takeIf { it != "<unknown ssid>" }
    } catch (_: Exception) {
        null
    }

    // 触发扫描并获取缓存结果
    try {
        wifiManager.startScan()
    } catch (_: Exception) {}

    val networks = try {
        wifiManager.scanResults
            ?.filter { it.SSID.isNotBlank() && it.SSID != currentSsid }
            ?.distinctBy { it.SSID }
            ?.sortedWith(compareByDescending<android.net.wifi.ScanResult> { it.level }.thenBy { it.SSID })
            ?.map { it.SSID }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    onUpdate(currentSsid, networks)
}
