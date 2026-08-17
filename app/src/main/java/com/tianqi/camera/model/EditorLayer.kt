package com.tianqi.camera.model

import androidx.compose.ui.geometry.Offset

/**
 * 可拖拽装饰图层（PRD 3.4）：贴纸、文字、涂鸦统一抽象。
 * 坐标均为相对画布的比例值（0-1），缩放 1 = 基准尺寸，旋转单位为度。
 */
sealed interface EditorLayer {
    val id: String
    val centerX: Float
    val centerY: Float
    val scale: Float
    val rotation: Float
}

/** 贴纸图层 */
data class StickerLayer(
    override val id: String,
    val stickerId: String,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f
) : EditorLayer

/** 文字图层 */
data class TextLayer(
    override val id: String,
    val text: String,
    val colorArgb: Long,
    /** 字体序号：0 默认圆体感(粗体) / 1 衬线 / 2 等宽 */
    val fontIndex: Int = 0,
    val stroke: Boolean = false,
    val shadow: Boolean = false,
    val alpha: Float = 1f,
    override val centerX: Float = 0.5f,
    override val centerY: Float = 0.5f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f
) : EditorLayer

/** 一条涂鸦路径，点为画布比例坐标 */
data class DoodlePath(
    val points: List<Offset>,
    val colorArgb: Long,
    /** 笔宽，相对画布宽度的比例 */
    val widthFraction: Float,
    val eraser: Boolean = false
)

/** 涂鸦图层：整张涂鸦画布为一个图层，内部路径矢量化存储（缩放画布不失真） */
data class DoodleLayer(
    override val id: String,
    val paths: List<DoodlePath> = emptyList()
) : EditorLayer {
    // 涂鸦不可拖拽，占位字段固定
    override val centerX: Float get() = 0.5f
    override val centerY: Float get() = 0.5f
    override val scale: Float get() = 1f
    override val rotation: Float get() = 0f
}
