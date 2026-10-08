// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.domain.model

/**
 * OTA 更新状态
 *
 * 记录版本检查、下载、安装各阶段的状态
 */
data class OtaState(
    /** 当前 App 版本号 */
    val currentVersion: String = "1.0.0",
    /** 是否正在检查更新 */
    val isChecking: Boolean = false,
    /** 检查失败的错误信息 */
    val checkError: String? = null,
    /** 是否有新版本 */
    val hasUpdate: Boolean = false,
    /** 最新版本号 */
    val latestVersion: String? = null,
    /** APK 下载地址 */
    val downloadUrl: String? = null,
    /** 文件大小（字节） */
    val fileSize: Long? = null,
    /** 文件 MD5 校验值 */
    val md5: String? = null,
    /** 更新日志 */
    val changelog: String? = null,
    /** 是否正在下载 */
    val isDownloading: Boolean = false,
    /** 下载进度 0.0 ~ 1.0 */
    val downloadProgress: Float = 0f,
    /** 已下载字节数 */
    val downloadedBytes: Long = 0L,
    /** 下载失败的错误信息 */
    val downloadError: String? = null,
    /** 下载是否完成 */
    val isDownloadComplete: Boolean = false,
    /** 下载的 APK 文件路径 */
    val apkFilePath: String? = null
) {
    /** 格式化的文件大小 */
    val fileSizeFormatted: String
        get() {
            val size = fileSize ?: return "未知"
            return when {
                size < 1024 -> "${size} B"
                size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
                else -> "%.1f MB".format(size / (1024.0 * 1024.0))
            }
        }

    /** 格式化的已下载大小 */
    val downloadedFormatted: String
        get() {
            return when {
                downloadedBytes < 1024 -> "${downloadedBytes} B"
                downloadedBytes < 1024 * 1024 -> "%.1f KB".format(downloadedBytes / 1024.0)
                else -> "%.1f MB".format(downloadedBytes / (1024.0 * 1024.0))
            }
        }
}
