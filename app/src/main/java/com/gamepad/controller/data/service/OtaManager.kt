// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.gamepad.controller.data.config.ConnectionConfig
import com.gamepad.controller.data.model.packet.OtaVersionCheckRequest
import com.gamepad.controller.data.model.packet.OtaVersionCheckResponse
import com.gamepad.controller.domain.model.OtaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.security.MessageDigest

/**
 * OTA 管理器 — 版本检查、APK 下载、安装
 *
 * 使用独立的 UDP socket 进行版本检查，不复用控制通道。
 * APK 下载使用 HTTP 协议。
 */
class OtaManager(
    private val context: Context,
    private val config: ConnectionConfig
) {
    companion object {
        private const val TAG = "OtaManager"
        private const val BUFFER_SIZE = 8192
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val otaHost = config.ota.host
    private val otaPort = config.ota.port
    private val otaTimeoutMs = config.ota.timeoutMs

    // ==================== 状态 ====================

    private val _otaState = MutableStateFlow(OtaState())
    val otaState: StateFlow<OtaState> = _otaState.asStateFlow()

    init {
        // 读取当前版本号
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _otaState.value = _otaState.value.copy(
                currentVersion = packageInfo.versionName ?: "1.0.0"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version name", e)
        }
    }

    // ==================== 版本检查 ====================

    /**
     * 检查是否有新版本
     *
     * 通过 UDP 发送版本检查请求到 OTA 服务器
     *
     * @return OtaVersionCheckResponse? 响应数据，超时返回 null
     */
    suspend fun checkForUpdate(): OtaVersionCheckResponse? {
        _otaState.value = _otaState.value.copy(
            isChecking = true,
            checkError = null,
            hasUpdate = false,
            latestVersion = null,
            downloadUrl = null,
            fileSize = null,
            md5 = null,
            changelog = null,
            isDownloadComplete = false,
            downloadError = null,
            apkFilePath = null
        )

        return withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = otaTimeoutMs.toInt()

                val request = OtaVersionCheckRequest(
                    currentVersion = _otaState.value.currentVersion,
                    deviceId = getDeviceId()
                )
                val jsonStr = json.encodeToString(request)
                val data = jsonStr.toByteArray()
                val address = InetAddress.getByName(otaHost)

                Log.d(TAG, "OUT [ota_version_check]: $jsonStr → $otaHost:$otaPort")

                val packet = DatagramPacket(data, data.size, address, otaPort)
                socket.send(packet)

                val buffer = ByteArray(4096)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(responsePacket)

                val responseJson = String(responsePacket.data, 0, responsePacket.length)
                Log.d(TAG, "IN [ota_version_check_response]: $responseJson")

                val response = json.decodeFromString<OtaVersionCheckResponse>(responseJson)

                _otaState.value = _otaState.value.copy(
                    isChecking = false,
                    hasUpdate = response.hasUpdate,
                    latestVersion = response.latestVersion,
                    downloadUrl = response.downloadUrl,
                    fileSize = response.fileSize,
                    md5 = response.md5,
                    changelog = response.changelog
                )

                response
            } catch (e: Exception) {
                Log.e(TAG, "Version check failed", e)
                _otaState.value = _otaState.value.copy(
                    isChecking = false,
                    checkError = e.message ?: "检查更新失败"
                )
                null
            } finally {
                socket?.close()
            }
        }
    }

    // ==================== APK 下载 ====================

    /**
     * 下载 APK 文件
     *
     * @param url 下载地址（为 null 时使用服务器响应的 download_url）
     */
    suspend fun downloadApk(url: String? = null) {
        val downloadUrl = url ?: _otaState.value.downloadUrl
        if (downloadUrl.isNullOrEmpty()) {
            _otaState.value = _otaState.value.copy(downloadError = "无下载地址")
            return
        }

        _otaState.value = _otaState.value.copy(
            isDownloading = true,
            downloadProgress = 0f,
            downloadedBytes = 0L,
            downloadError = null,
            isDownloadComplete = false
        )

        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // 准备输出目录
                val outputDir = File(context.getExternalFilesDir(null), "updates")
                if (!outputDir.exists()) outputDir.mkdirs()
                val outputFile = File(outputDir, "app-update.apk")
                if (outputFile.exists()) outputFile.delete()

                Log.d(TAG, "Downloading APK from: $downloadUrl")

                // 打开 HTTP 连接
                val urlObj = java.net.URL(downloadUrl)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.connect()

                val totalSize = connection.contentLength.toLong()
                Log.d(TAG, "APK file size: $totalSize bytes")

                // 分块下载
                val inputStream = connection.inputStream
                val outputStream = outputFile.outputStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var downloadedBytes = 0L
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = if (totalSize > 0) {
                        downloadedBytes.toFloat() / totalSize
                    } else {
                        0f
                    }

                    _otaState.value = _otaState.value.copy(
                        downloadProgress = progress,
                        downloadedBytes = downloadedBytes
                    )
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d(TAG, "APK download complete: $downloadedBytes bytes")

                // MD5 校验
                val expectedMd5 = _otaState.value.md5
                if (!expectedMd5.isNullOrEmpty()) {
                    val actualMd5 = calculateMd5(outputFile)
                    if (actualMd5 != expectedMd5) {
                        Log.e(TAG, "MD5 mismatch: expected=$expectedMd5, actual=$actualMd5")
                        _otaState.value = _otaState.value.copy(
                            isDownloading = false,
                            downloadError = "文件校验失败，请重新下载"
                        )
                        outputFile.delete()
                        return@withContext
                    }
                    Log.d(TAG, "MD5 verified: $actualMd5")
                }

                _otaState.value = _otaState.value.copy(
                    isDownloading = false,
                    isDownloadComplete = true,
                    downloadProgress = 1f,
                    apkFilePath = outputFile.absolutePath
                )

            } catch (e: Exception) {
                Log.e(TAG, "APK download failed", e)
                _otaState.value = _otaState.value.copy(
                    isDownloading = false,
                    downloadError = e.message ?: "下载失败"
                )
            } finally {
                connection?.disconnect()
            }
        }
    }

    // ==================== APK 安装 ====================

    /**
     * 安装已下载的 APK 文件
     *
     * 通过 FileProvider 生成 content:// URI，启动系统安装器
     */
    fun installApk() {
        val apkPath = _otaState.value.apkFilePath
        if (apkPath.isNullOrEmpty()) {
            Log.e(TAG, "No APK file to install")
            return
        }

        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file not found: $apkPath")
            _otaState.value = _otaState.value.copy(downloadError = "APK 文件不存在")
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Log.d(TAG, "Install intent started for: $apkPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start install", e)
            _otaState.value = _otaState.value.copy(downloadError = "无法启动安装: ${e.message}")
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取设备唯一标识
     */
    private fun getDeviceId(): String {
        return try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * 计算文件的 MD5 值
     */
    private fun calculateMd5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        inputStream.close()

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _otaState.value = OtaState(currentVersion = _otaState.value.currentVersion)
    }
}
