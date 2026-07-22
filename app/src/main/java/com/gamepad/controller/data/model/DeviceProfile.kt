package com.gamepad.controller.data.model

/**
 * 手柄设备配置信息
 *
 * 对应设计文档 2.3 DeviceRegistry + DeviceProfile
 * 不同品牌手柄（Xbox/PS/北通/8BitDo）的轴映射和按键码存在差异，
 * 通过 DeviceProfile 做归一化处理
 *
 * @param deviceId Android InputDevice ID
 * @param name 设备名称
 * @param vendorId 厂商 ID
 * @param productId 产品 ID
 * @param axisMap 逻辑轴 → 物理轴映射（key=逻辑轴名, value=物理轴编号）
 * @param deadzone 死区半径（默认 0.12）
 * @param invertY 是否反转 Y 轴（部分手柄 Y 轴上为负）
 * @param gamma 灵敏度曲线参数（幂函数指数，1.0=线性，>1=微操区放大）
 */
data class DeviceProfile(
    val deviceId: Int,
    val name: String = "",
    val vendorId: Int = 0,
    val productId: Int = 0,
    val axisMap: Map<String, Int> = DEFAULT_AXIS_MAP,
    val deadzone: Float = DEFAULT_DEADZONE,
    val invertY: Boolean = false,
    val gamma: Float = 1.0f,
    val buttonMap: Map<Int, Int> = emptyMap() // 原始按键码 → GamepadButton.ordinal
) {
    companion object {
        const val DEFAULT_DEADZONE = 0.12f

        /** 通用轴映射：左摇杆=X/Y，右摇杆=Z/RZ，扳机=BRAKE/GAS */
        val DEFAULT_AXIS_MAP = mapOf(
            "LX" to android.view.MotionEvent.AXIS_X,
            "LY" to android.view.MotionEvent.AXIS_Y,
            "RX" to android.view.MotionEvent.AXIS_Z,
            "RY" to android.view.MotionEvent.AXIS_RZ,
            "LT" to android.view.MotionEvent.AXIS_BRAKE,
            "RT" to android.view.MotionEvent.AXIS_GAS
        )
    }

    /** 是否为通用 Profile（未匹配到特定设备） */
    val isGeneric: Boolean
        get() = vendorId == 0 && productId == 0
}
