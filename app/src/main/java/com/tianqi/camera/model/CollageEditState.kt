package com.tianqi.camera.model

import android.net.Uri

/** 单个槽位内图片的取景状态 */
data class SlotEditState(
    val uri: Uri? = null,
    /** 缩放倍率，1 = 铺满槽位 */
    val scale: Float = 1f,
    /** 取景偏移，相对于槽位宽/高的比例（-1 ~ 1） */
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** 旋转角度（90 度的倍数，0-3） */
    val rotationQuarter: Int = 0,
    /** 水平镜像 */
    val mirrored: Boolean = false
)

/** 拼图背景 */
sealed interface CollageBackground {
    /** 甜系纯色 */
    data class Solid(val argb: Long) : CollageBackground

    /** 同族色系渐变（135°） */
    data class Gradient(val fromArgb: Long, val toArgb: Long) : CollageBackground

    /** 高斯模糊背景（取第一张图模糊铺满） */
    data object Blur : CollageBackground
}

/** 一次拼图编辑的完整状态 */
data class CollageEditState(
    val templateId: String,
    val canvasRatio: CanvasRatio,
    val slots: List<SlotEditState>,
    /** 槽位间距 0-40px */
    val spacing: Float = 8f,
    /** 边框粗细 0-40px（白色描边） */
    val borderWidth: Float = 0f,
    /** 图片槽位圆角 0-40px */
    val cornerRadius: Float = 12f,
    val background: CollageBackground = CollageBackground.Solid(0xFFFFF9F7)
) {
    fun updateSlot(index: Int, slot: SlotEditState): CollageEditState =
        copy(slots = slots.mapIndexed { i, s -> if (i == index) slot else s })
}
