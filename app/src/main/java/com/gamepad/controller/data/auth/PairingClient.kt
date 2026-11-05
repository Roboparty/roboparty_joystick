// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

@Serializable
private data class PairRequest(
    val code: String,
    val appPublicKey: String,
    val deviceName: String
)

@Serializable
data class PairResult(
    val ok: Boolean = false,
    @SerialName("appKeyId") val appKeyId: String = "",
    val scopes: List<String> = emptyList(),
    val replaced: List<String> = emptyList()
)

/**
 * 用配对 URI 中携带的一次性码把 App 公钥登记到机器人。
 *
 * 注册请求必须使用配对 URI 中钉住的 TLS SPKI 指纹，禁止 trust-all 和跳过
 * 主机名校验。
 */
class PairingClient(private val timeoutMs: Int = 5000) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun register(
        host: String,
        port: Int,
        code: String,
        appPublicKey: String,
        deviceName: String,
        tlsSpkiSha256: String
    ): PairResult = withContext(Dispatchers.IO) {
        require(tlsSpkiSha256.isNotBlank()) { "TLS SPKI pin is required" }
        val body = json.encodeToString(PairRequest(code, appPublicKey, deviceName)).toByteArray()
        val connection = (URL("https://$host:$port/pair/register").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        if (connection is HttpsURLConnection) {
            val trust = SpkiPinTrustManager(tlsSpkiSha256)
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf<TrustManager>(trust), SecureRandom())
            connection.sslSocketFactory = context.socketFactory
        }
        try {
            connection.outputStream.use { it.write(body) }
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                throw IOException("pairing HTTP ${connection.responseCode}: $text")
            }
            json.decodeFromString<PairResult>(text)
        } finally {
            connection.disconnect()
        }
    }
}
