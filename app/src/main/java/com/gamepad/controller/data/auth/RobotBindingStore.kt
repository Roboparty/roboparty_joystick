// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 [cenliey]

package com.gamepad.controller.data.auth

import android.content.Context
import com.gamepad.controller.data.model.DeviceIdentity

/**
 * App 与机器人的绑定记录。
 *
 * v2 起 SN 用于识别机器人，设备公钥 SPKI 用于识别身份变化，TLS SPKI 用于
 * 后续所有 HTTPS/WSS 连接的证书 pinning。绑定只能由配对 URI 写入。
 */
class RobotBindingStore(context: Context) {

    data class Binding(
        val serial: String,
        val machineId: String,
        val spkiSha256: String,
        val tlsSpkiSha256: String,
        val host: String,
        val boundAt: Long
    )

    private val prefs = context.getSharedPreferences("robot_binding", Context.MODE_PRIVATE)

    fun load(): Binding? {
        val serial = prefs.getString(KEY_SERIAL, null) ?: return null
        return Binding(
            serial = serial,
            machineId = prefs.getString(KEY_MACHINE, "").orEmpty(),
            spkiSha256 = prefs.getString(KEY_SPKI, "").orEmpty(),
            tlsSpkiSha256 = prefs.getString(KEY_TLS_SPKI, "").orEmpty(),
            host = prefs.getString(KEY_HOST, "").orEmpty(),
            boundAt = prefs.getLong(KEY_BOUND_AT, 0L)
        )
    }

    fun savePairing(
        serial: String,
        machineId: String,
        deviceSpkiSha256: String,
        tlsSpkiSha256: String,
        host: String
    ) {
        prefs.edit()
            .putString(KEY_SERIAL, serial)
            .putString(KEY_MACHINE, machineId)
            .putString(KEY_SPKI, deviceSpkiSha256)
            .putString(KEY_TLS_SPKI, tlsSpkiSha256)
            .putString(KEY_HOST, host)
            .putLong(KEY_BOUND_AT, System.currentTimeMillis())
            .apply()
    }

    fun save(identity: DeviceIdentity, host: String) {
        savePairing(
            serial = identity.serial,
            machineId = identity.machineId,
            deviceSpkiSha256 = identity.spkiSha256.orEmpty(),
            tlsSpkiSha256 = identity.tlsSpkiSha256.orEmpty(),
            host = host
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_SERIAL = "serial"
        const val KEY_MACHINE = "machine_id"
        const val KEY_SPKI = "spki_sha256"
        const val KEY_TLS_SPKI = "tls_spki_sha256"
        const val KEY_HOST = "host"
        const val KEY_BOUND_AT = "bound_at"
    }
}
