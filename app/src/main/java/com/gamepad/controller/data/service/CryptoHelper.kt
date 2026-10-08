// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.service

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 签名工具
 *
 * 用于三步认证中的挑战码签名
 *
 * 签名算法：
 * - 输入：deviceId:challengeCode
 * - 密钥：预共享密钥（SECRET_KEY）
 * - 输出：十六进制签名字符串
 */
object CryptoHelper {

    // TODO: 从安全存储获取，不要硬编码在代码中
    // 建议方案：
    // 1. 开发阶段：BuildConfig 注入
    // 2. 生产环境：Android Keystore 或远程配置
    private const val SECRET_KEY = "your-pre-shared-secret-key"

    /**
     * 计算 HMAC-SHA256 签名
     *
     * @param deviceId 设备唯一标识
     * @param challengeCode 服务器下发的挑战码
     * @return 十六进制签名字符串（小写）
     */
    fun computeSignature(deviceId: String, challengeCode: String): String {
        val data = "$deviceId:$challengeCode"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * 验证签名（用于测试）
     *
     * @param deviceId 设备唯一标识
     * @param challengeCode 挑战码
     * @param signature 待验证的签名
     * @return 签名是否有效
     */
    fun verifySignature(deviceId: String, challengeCode: String, signature: String): Boolean {
        val expected = computeSignature(deviceId, challengeCode)
        return expected.equals(signature, ignoreCase = true)
    }
}
