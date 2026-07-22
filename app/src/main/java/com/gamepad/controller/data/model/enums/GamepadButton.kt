package com.gamepad.controller.data.model.enums

/**
 * 手柄按键归一枚举
 *
 * 对应设计文档 2.3 "按键码归一"：将各厂商按键码统一为内部枚举
 * 业务层只使用此枚举，不关心具体硬件按键码
 *
 * 位图约定：每个按键对应 buttons 位图中的一个位（bit 0 ~ bit 15）
 */
enum class GamepadButton(val bit: Int) {
    A(0),
    B(1),
    X(2),
    Y(3),
    LB(4),       // 左肩键
    RB(5),       // 右肩键
    LT(6),       // 左扳机（按下状态）
    RT(7),       // 右扳机（按下状态）
    START(8),
    SELECT(9),
    L3(10),      // 左摇杆按下
    R3(11),      // 右摇杆按下
    DPAD_UP(12),
    DPAD_DOWN(13),
    DPAD_LEFT(14),
    DPAD_RIGHT(15);

    /** 该按键对应的位掩码值 */
    val mask: Int get() = 1 shl bit
}

/**
 * 按键位图工具函数
 *
 * buttons 为 uint32 整型，每一位对应一个 GamepadButton
 */
object ButtonBitmap {

    /** 设置按键为按下状态 */
    fun setButton(bitmap: Int, button: GamepadButton): Int =
        bitmap or button.mask

    /** 清除按键（设为未按下） */
    fun clearButton(bitmap: Int, button: GamepadButton): Int =
        bitmap and button.mask.inv()

    /** 判断按键是否按下 */
    fun isButtonSet(bitmap: Int, button: GamepadButton): Boolean =
        (bitmap and button.mask) != 0

    /** 从位图中提取所有按下的按键 */
    fun getPressedButtons(bitmap: Int): Set<GamepadButton> =
        GamepadButton.entries.filter { isButtonSet(bitmap, it) }.toSet()

    /** 检查位图是否全为零（无按键按下） */
    fun isEmpty(bitmap: Int): Boolean = bitmap == 0
}
