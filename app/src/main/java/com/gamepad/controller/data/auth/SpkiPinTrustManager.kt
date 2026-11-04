// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/** Trust exactly one TLS certificate SPKI fingerprint (hex SHA-256). */
class SpkiPinTrustManager(pinHex: String) : X509TrustManager {
    private val pin: String = hexToBase64(pinHex)

    init {
        require(pinHex.isNotBlank()) { "TLS SPKI pin is required" }
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        val certificate = chain.firstOrNull()
            ?: throw CertificateException("no server certificate")
        val digest = MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded)
        val actual = Base64.encodeToString(digest, Base64.NO_WRAP)
        if (actual != pin) throw CertificateException("SPKI pin mismatch")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        fun hexToBase64(hex: String): String {
            val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }
}
