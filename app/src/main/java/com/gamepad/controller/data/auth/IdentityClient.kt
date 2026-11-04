// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.auth

import com.gamepad.controller.data.model.DeviceIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

/**
 * 读取 rp_server 的公开身份（GET /identity）。
 *
 * v2 起必须使用首次配对钉住的 TLS SPKI 指纹读取，禁止 trust-all；主机名
 * 校验保持开启，证书必须包含匹配的 SAN。
 */
class IdentityClient(private val timeoutMs: Int = 5000) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetch(host: String, port: Int, pinSha256: String): DeviceIdentity =
        withContext(Dispatchers.IO) {
            require(pinSha256.isNotBlank()) { "TLS SPKI pin is required" }
            val connection = (URL("https://$host:$port/identity").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("Accept", "application/json")
            }
            if (connection is HttpsURLConnection) {
                val trust = SpkiPinTrustManager(pinSha256)
                val context = SSLContext.getInstance("TLS")
                context.init(null, arrayOf<TrustManager>(trust), SecureRandom())
                connection.sslSocketFactory = context.socketFactory
            }
            try {
                if (connection.responseCode !in 200..299) {
                    throw IOException("identity HTTP ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<DeviceIdentity>(body)
            } finally {
                connection.disconnect()
            }
        }
}
