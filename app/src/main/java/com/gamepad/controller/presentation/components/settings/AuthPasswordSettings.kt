// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.presentation.components.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.data.config.ConnectionConfig
import com.gamepad.controller.data.auth.AppKeyStore
import com.gamepad.controller.data.auth.PairingClient
import com.gamepad.controller.data.auth.PairingPayloadParser
import com.gamepad.controller.data.auth.RobotBindingStore
import kotlinx.coroutines.launch

/**
 * 授权密钥与机器人绑定设置。
 *
 * 密钥由 Android Keystore 加密保存；未设置时认证失败（fail-closed）。
 */
@Composable
fun AuthPasswordSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bindingStore = remember { RobotBindingStore(context) }
    val config = remember { ConnectionConfig.load(context) }
    val appKeyStore = remember { AppKeyStore(context) }
    val pairingClient = remember { PairingClient(config.auth.timeoutMs.toInt()) }
    val scope = rememberCoroutineScope()

    var binding by remember { mutableStateOf(bindingStore.load()) }
    var pairUri by remember { mutableStateOf("") }
    var pairStatus by remember { mutableStateOf("") }
    var appKeyId by remember { mutableStateOf(appKeyStore.appKeyId()) }
    var showClearBindingDialog by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanned = result.data?.getStringExtra("SCAN_RESULT").orEmpty()
            if (scanned.startsWith("roboparty://pair?data=")) {
                pairUri = scanned
                pairStatus = "已读取二维码，请点击配对"
            } else if (scanned.isNotBlank()) {
                pairStatus = "二维码不是 RoboParty 配对 URI"
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "授权与绑定",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Text(
            text = "App 使用本机 Keystore 中的 EC 密钥完成挑战-响应认证；机器人绑定用于识别 SN 变化。",
            fontSize = 13.sp,
            color = Color(0xFF999999)
        )

        Text(
            text = "App 配对",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Text(
            text = "在机器人上运行 `rp-server-pair code`，扫描二维码或手动粘贴输出的 roboparty://pair?data=... URI。",
            fontSize = 13.sp,
            color = Color(0xFF999999)
        )
        OutlinedTextField(
            value = pairUri,
            onValueChange = { pairUri = it },
            label = { Text("配对 URI") },
            singleLine = false,
            maxLines = 3,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Uri
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = {
                scope.launch {
                    pairStatus = "配对中..."
                    try {
                        val payload = PairingPayloadParser.parse(pairUri)
                        val result = pairingClient.register(
                            config.control.host,
                            config.control.port,
                            payload.code,
                            appKeyStore.ensurePublicKeyBase64(),
                            Build.MODEL,
                            payload.tlsSpkiSha256
                        )
                        if (!result.ok || result.appKeyId.isBlank()) {
                            throw IllegalStateException("机器人拒绝配对")
                        }
                        appKeyStore.setAppKeyId(result.appKeyId)
                        appKeyId = result.appKeyId
                        val replaced = result.replaced.size
                        bindingStore.savePairing(
                            serial = payload.serial,
                            machineId = payload.machineId,
                            deviceSpkiSha256 = payload.deviceSpkiSha256,
                            tlsSpkiSha256 = payload.tlsSpkiSha256,
                            host = config.control.host
                        )
                        binding = bindingStore.load()
                        pairStatus = if (replaced > 0) {
                            "配对成功；已解除 $replaced 台旧设备绑定"
                        } else {
                            "配对成功"
                        }
                        pairUri = ""
                    } catch (e: Exception) {
                        pairStatus = "配对失败: ${e.message}"
                    }
                }
            },
            enabled = pairUri.startsWith("roboparty://pair?data="),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("使用配对 URI 配对")
        }
        OutlinedButton(
            onClick = {
                val intent = Intent("com.google.zxing.client.android.SCAN").apply {
                    putExtra("SCAN_MODE", "QR_CODE_MODE")
                }
                try {
                    scanLauncher.launch(intent)
                } catch (e: Exception) {
                    pairStatus = "未找到二维码扫描器，请手动粘贴配对 URI"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("扫描配对二维码")
        }
        if (pairStatus.isNotEmpty()) {
            Text(pairStatus, fontSize = 12.sp, color = Color(0xFF666666))
        }
        if (appKeyId.isNotEmpty()) {
            Text("appKeyId: $appKeyId", fontSize = 12.sp, color = Color(0xFF666666))
            OutlinedButton(
                onClick = {
                    appKeyStore.clear()
                    appKeyId = ""
                    pairStatus = "已清除本机 App 密钥（服务端记录需在板端 revoke）"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清除 App 配对密钥")
            }
        }

        Text(
            text = "机器人绑定",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        val bindingText = binding?.let {
            "SN: ${it.serial}\n主机: ${it.host}\nTLS pin: ${if (it.tlsSpkiSha256.isBlank()) "缺失" else "已设置"}"
        } ?: "未绑定（请先粘贴配对 URI）"
        Text(
            text = bindingText,
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
        if (binding != null) {
            OutlinedButton(
                onClick = { showClearBindingDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清除机器人绑定（换板/重新配对）")
            }
        }

        Text(
            text = "换控制板或 SN 变化后，需要清除绑定并重新连接。",
            fontSize = 12.sp,
            color = Color(0xFFBBBBBB),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showClearBindingDialog) {
        AlertDialog(
            onDismissRequest = { showClearBindingDialog = false },
            title = { Text("清除机器人绑定？") },
            text = { Text("下次连接需要在机器人侧重新生成配对 URI。") },
            confirmButton = {
                TextButton(onClick = {
                    bindingStore.clear()
                    binding = null
                    showClearBindingDialog = false
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearBindingDialog = false }) { Text("取消") }
            }
        )
    }
}
