// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.presentation.components.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamepad.controller.domain.model.OtaState

/**
 * OTA 更新设置页面
 *
 * 功能：
 * - 显示当前版本号
 * - 检查更新
 * - 显示更新日志
 * - 下载并安装
 */
@Composable
fun OtaSettings(
    otaState: OtaState,
    onCheckUpdate: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "固件更新",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        // 当前版本
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "当前版本",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "v${otaState.currentVersion}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
        }

        // 检查更新按钮
        Button(
            onClick = onCheckUpdate,
            enabled = !otaState.isChecking && !otaState.isDownloading,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2D7FF9)
            )
        ) {
            if (otaState.isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "检查中...", fontSize = 14.sp)
            } else {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "检查更新", fontSize = 14.sp)
            }
        }

        // 检查错误
        if (otaState.checkError != null) {
            ErrorCard(message = otaState.checkError)
        }

        // 已是最新版本
        if (!otaState.isChecking && !otaState.hasUpdate && otaState.checkError == null && otaState.latestVersion == null) {
            Text(
                text = "已是最新版本",
                fontSize = 13.sp,
                color = Color(0xFF999999),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // 发现新版本
        if (otaState.hasUpdate && otaState.latestVersion != null) {
            UpdateInfoCard(
                otaState = otaState,
                onDownload = onDownload,
                onInstall = onInstall
            )
        }
    }
}

/**
 * 更新信息卡片
 */
@Composable
private fun UpdateInfoCard(
    otaState: OtaState,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 新版本标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF2D7FF9)
            )
            Text(
                text = "发现新版本 v${otaState.latestVersion}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A2E)
            )
        }

        // 更新日志
        if (!otaState.changelog.isNullOrEmpty()) {
            Column {
                Text(
                    text = "更新内容：",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = otaState.changelog,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    lineHeight = 18.sp
                )
            }
        }

        // 文件大小
        Text(
            text = "文件大小：${otaState.fileSizeFormatted}",
            fontSize = 12.sp,
            color = Color(0xFF999999)
        )

        // 下载进度
        if (otaState.isDownloading) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { otaState.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF2D7FF9),
                    trackColor = Color(0xFFE0E0E0)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(otaState.downloadProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "${otaState.downloadedFormatted} / ${otaState.fileSizeFormatted}",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }

        // 下载错误
        if (otaState.downloadError != null) {
            ErrorCard(message = otaState.downloadError)
        }

        // 下载按钮
        if (!otaState.isDownloadComplete && !otaState.isDownloading) {
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2D7FF9)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "下载并安装", fontSize = 14.sp)
            }
        }

        // 下载完成 - 安装按钮
        if (otaState.isDownloadComplete) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF4CAF50)
                )
                Text(
                    text = "下载完成",
                    fontSize = 13.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            Button(
                onClick = onInstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "安装更新", fontSize = 14.sp)
            }
        }
    }
}

/**
 * 错误提示卡片
 */
@Composable
private fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF3F0))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFFE53935)
        )
        Text(
            text = message,
            fontSize = 12.sp,
            color = Color(0xFFE53935)
        )
    }
}
