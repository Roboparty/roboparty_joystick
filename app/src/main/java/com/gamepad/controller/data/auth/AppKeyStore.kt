// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature

/**
 * App 配对密钥（EC P-256）与 appKeyId。
 *
 * 私钥留在 Android Keystore；只把公钥 SPKI 发给机器人登记。
 */
class AppKeyStore(context: Context) {

    private val prefs = context.getSharedPreferences("app_pairing", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun ensurePublicKeyBase64(): String {
        if (!keyStore.containsAlias(ALIAS)) {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
            )
            generator.initialize(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
            )
            generator.generateKeyPair()
        }
        val publicKey = keyStore.getCertificate(ALIAS).publicKey
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun signBase64(message: String): String {
        ensurePublicKeyBase64()
        val entry = keyStore.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(entry.privateKey)
        signer.update(message.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
    }

    fun appKeyId(): String = prefs.getString(KEY_APP_ID, "").orEmpty()

    fun setAppKeyId(value: String) {
        prefs.edit().putString(KEY_APP_ID, value).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_APP_ID).apply()
        if (keyStore.containsAlias(ALIAS)) {
            keyStore.deleteEntry(ALIAS)
        }
    }

    private companion object {
        const val ALIAS = "roboparty-app-key"
        const val KEY_APP_ID = "app_key_id"
    }
}
