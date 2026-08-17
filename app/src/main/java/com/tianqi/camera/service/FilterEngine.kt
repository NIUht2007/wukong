package com.tianqi.camera.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.tianqi.camera.model.FilterAdjust
import com.tianqi.camera.model.FilterSpec
import com.tianqi.camera.model.SweetFilters

/**
 * 滤镜引擎：FilterAdjust → ColorMatrix。
 * 同一份矩阵既用于 Compose 实时预览（缩略图/大图 ColorFilter），
 * 也用于导出全分辨率大图（Canvas 离屏绘制），保证预览与导出无色差。
 */
object FilterEngine {

    private val IDENTITY = FloatArray(20).apply {
        this[0] = 1f; this[6] = 1f; this[12] = 1f; this[18] = 1f
    }

    /**
     * 生成滤镜矩阵，intensity 0-100 与原图（单位矩阵）插值。
     * 通道公式：out = in * (contrast * gain) + ((1 - contrast) * 128 + lift + offset)
     */
    fun matrixValues(filter: FilterSpec, intensity: Float): FloatArray {
        val t = (intensity / 100f).coerceIn(0f, 1f)
        if (t <= 0f || filter.id == SweetFilters.NONE.id) return IDENTITY.copyOf()

        val full = buildFullMatrix(filter.adjust)
        // 与单位矩阵线性插值，实现强度 0-100 平滑过渡
        return FloatArray(20) { i -> IDENTITY[i] + (full[i] - IDENTITY[i]) * t }
    }

    /** Compose 预览用 ColorFilter（大图与缩略图共用） */
    fun composeColorFilter(filter: FilterSpec, intensity: Float): androidx.compose.ui.graphics.ColorFilter? {
        if (filter.id == SweetFilters.NONE.id || intensity <= 0f) return null
        val composeMatrix = androidx.compose.ui.graphics.ColorMatrix(matrixValues(filter, intensity))
        return androidx.compose.ui.graphics.ColorFilter.colorMatrix(composeMatrix)
    }

    /**
     * 导出用：把滤镜应用到全分辨率 Bitmap，返回新 Bitmap（原图不修改）。
     */
    fun applyToBitmap(src: Bitmap, filter: FilterSpec, intensity: Float): Bitmap {
        val config = src.config ?: Bitmap.Config.ARGB_8888
        if (filter.id == SweetFilters.NONE.id || intensity <= 0f) return src.copy(config, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(matrixValues(filter, intensity)))
        }
        val out = Bitmap.createBitmap(src.width, src.height, config)
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** 完整强度（t=1）下的滤镜矩阵 */
    private fun buildFullMatrix(adjust: FilterAdjust): FloatArray {
        // 先饱和度，再逐通道 增益*对比 + 偏移
        val saturationMatrix = ColorMatrix().apply { setSaturation(adjust.saturation) }
        val channelMatrix = ColorMatrix(
            floatArrayOf(
                adjust.rGain * adjust.contrast, 0f, 0f, 0f, channelOffset(adjust, adjust.rOffset),
                0f, adjust.gGain * adjust.contrast, 0f, 0f, channelOffset(adjust, adjust.gOffset),
                0f, 0f, adjust.bGain * adjust.contrast, 0f, channelOffset(adjust, adjust.bOffset),
                0f, 0f, 0f, 1f, 0f
            )
        )
        // 顺序：先做通道调整，再套饱和度（后乘左矩阵）
        saturationMatrix.postConcat(channelMatrix)
        return saturationMatrix.array
    }

    private fun channelOffset(adjust: FilterAdjust, channelOffset: Float): Float =
        (1f - adjust.contrast) * 128f + adjust.lift + channelOffset
}
