package com.tianqi.camera.service

import android.graphics.Bitmap
import com.tianqi.camera.model.BeautyState
import com.tianqi.camera.model.FaceData
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 美颜引擎（PRD 3.5 基础版）：磨皮/美白/红润为全图像素级处理，
 * 瘦脸/大眼为基于人脸关键点的局部网格液化。
 * 预览用小图调参，导出时对原图全分辨率计算（同一函数）。
 */
object BeautyEngine {

    fun apply(src: Bitmap, state: BeautyState, faces: List<FaceData>): Bitmap {
        var pixels = src.toPixels()
        val w = src.width
        val h = src.height

        // 1. 磨皮：保边模糊（近似双边滤波）
        if (state.smooth > 0f) {
            pixels = surfaceBlur(pixels, w, h, state.smooth / 100f)
        }
        // 2. 美白 + 红润：通道 LUT
        if (state.whiten > 0f || state.rosy > 0f) {
            toneAdjust(pixels, state.whiten / 100f, state.rosy / 100f)
        }
        // 3. 瘦脸：脸颊处向液化中心收缩
        if (state.slimFace > 0f && faces.isNotEmpty()) {
            val strength = 0.35f * (state.slimFace / 100f)
            faces.forEach { face ->
                val radius = face.faceWidthFraction * w * 0.28f
                pixels = liquify(pixels, w, h, face.cheekLeftX * w, face.cheekLeftY * h, radius, -strength)
                pixels = liquify(pixels, w, h, face.cheekRightX * w, face.cheekRightY * h, radius, -strength)
            }
        }
        // 4. 大眼：眼部局部膨胀
        if (state.bigEyes > 0f && faces.isNotEmpty()) {
            val strength = 0.30f * (state.bigEyes / 100f)
            faces.forEach { face ->
                val radius = face.faceWidthFraction * w * 0.16f
                pixels = liquify(pixels, w, h, face.leftEyeX * w, face.leftEyeY * h, radius, strength)
                pixels = liquify(pixels, w, h, face.rightEyeX * w, face.rightEyeY * h, radius, strength)
            }
        }
        return pixels.toBitmap(w, h)
    }

    /** 保边模糊：盒式模糊 + 按色差回退原图，保留边缘 */
    private fun surfaceBlur(pixels: IntArray, w: Int, h: Int, t: Float): IntArray {
        val radius = (2 + t * 6).toInt()
        val blurred = boxBlur(pixels, w, h, radius)
        val out = IntArray(pixels.size)
        // 色差阈值：强度越高，越大范围的像素被磨平
        val threshold = 12f + t * 30f
        for (i in pixels.indices) {
            val o = pixels[i]
            val b = blurred[i]
            val dr = abs((o shr 16 and 0xFF) - (b shr 16 and 0xFF))
            val dg = abs((o shr 8 and 0xFF) - (b shr 8 and 0xFF))
            val db = abs((o and 0xFF) - (b and 0xFF))
            val diff = (dr + dg + db) / 3f
            // 软过渡：色差小→用模糊值，色差大（边缘）→保留原图
            val edge = (1f - diff / threshold).coerceIn(0f, 1f)
            val mix = t * edge
            out[i] = mixColor(o, b, mix)
        }
        return out
    }

    /** 可分离盒式模糊（水平 + 垂直两遍） */
    private fun boxBlur(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        val tmp = IntArray(src.size)
        val out = IntArray(src.size)
        val div = radius * 2 + 1
        // 水平
        for (y in 0 until h) {
            var sumR = 0; var sumG = 0; var sumB = 0
            val row = y * w
            for (x in -radius..radius) {
                val c = src[row + x.coerceIn(0, w - 1)]
                sumR += c shr 16 and 0xFF; sumG += c shr 8 and 0xFF; sumB += c and 0xFF
            }
            for (x in 0 until w) {
                tmp[row + x] = 0xFF shl 24 or (sumR / div shl 16) or (sumG / div shl 8) or (sumB / div)
                val addC = src[row + (x + radius + 1).coerceAtMost(w - 1)]
                val subC = src[row + (x - radius).coerceIn(0, w - 1)]
                sumR += (addC shr 16 and 0xFF) - (subC shr 16 and 0xFF)
                sumG += (addC shr 8 and 0xFF) - (subC shr 8 and 0xFF)
                sumB += (addC and 0xFF) - (subC and 0xFF)
            }
        }
        // 垂直
        for (x in 0 until w) {
            var sumR = 0; var sumG = 0; var sumB = 0
            for (y in -radius..radius) {
                val c = tmp[y.coerceIn(0, h - 1) * w + x]
                sumR += c shr 16 and 0xFF; sumG += c shr 8 and 0xFF; sumB += c and 0xFF
            }
            for (y in 0 until h) {
                out[y * w + x] = 0xFF shl 24 or (sumR / div shl 16) or (sumG / div shl 8) or (sumB / div)
                val addC = tmp[(y + radius + 1).coerceAtMost(h - 1) * w + x]
                val subC = tmp[(y - radius).coerceIn(0, h - 1) * w + x]
                sumR += (addC shr 16 and 0xFF) - (subC shr 16 and 0xFF)
                sumG += (addC shr 8 and 0xFF) - (subC shr 8 and 0xFF)
                sumB += (addC and 0xFF) - (subC and 0xFF)
            }
        }
        return out
    }

    /** 美白（伽马提亮）+ 红润（红通道增益），用 LUT 一次完成 */
    private fun toneAdjust(pixels: IntArray, whiten: Float, rosy: Float) {
        val lutR = IntArray(256)
        val lutG = IntArray(256)
        val lutB = IntArray(256)
        val gamma = 1f - 0.35f * whiten
        for (v in 0..255) {
            val brightened = if (whiten > 0f)
                (255f * (v / 255f).pow(gamma)).toInt()
            else v
            lutR[v] = (brightened + 14f * rosy).toInt().coerceIn(0, 255)
            lutG[v] = (brightened + 2f * rosy).toInt().coerceIn(0, 255)
            lutB[v] = (brightened + 6f * rosy).toInt().coerceIn(0, 255)
        }
        for (i in pixels.indices) {
            val c = pixels[i]
            pixels[i] = (0xFF shl 24) or
                    (lutR[c shr 16 and 0xFF] shl 16) or
                    (lutG[c shr 8 and 0xFF] shl 8) or
                    lutB[c and 0xFF]
        }
    }

    /**
     * 局部液化（逆向映射 + 双线性采样）。
     * strength > 0 膨胀（大眼），< 0 收缩（瘦脸）；影响随距离二次衰减，收敛不扭曲背景。
     */
    private fun liquify(
        src: IntArray, w: Int, h: Int,
        cx: Float, cy: Float, radius: Float, strength: Float
    ): IntArray {
        val out = src.copyOf()
        val x0 = max(0, (cx - radius).toInt())
        val x1 = min(w - 1, (cx + radius).toInt())
        val y0 = max(0, (cy - radius).toInt())
        val y1 = min(h - 1, (cy + radius).toInt())
        val r2 = radius * radius
        for (y in y0..y1) {
            for (x in x0..x1) {
                val dx = x - cx
                val dy = y - cy
                val d2 = dx * dx + dy * dy
                if (d2 >= r2) continue
                val d = sqrt(d2) / radius
                val k = (1f - d) * (1f - d)
                val factor = 1f - strength * k
                val sx = cx + dx * factor
                val sy = cy + dy * factor
                out[y * w + x] = sampleBilinear(src, w, h, sx, sy)
            }
        }
        return out
    }

    private fun sampleBilinear(src: IntArray, w: Int, h: Int, fx: Float, fy: Float): Int {
        val x = fx.coerceIn(0f, w - 1.001f)
        val y = fy.coerceIn(0f, h - 1.001f)
        val x0 = x.toInt()
        val y0 = y.toInt()
        val tx = x - x0
        val ty = y - y0
        val c00 = src[y0 * w + x0]
        val c10 = src[y0 * w + min(x0 + 1, w - 1)]
        val c01 = src[min(y0 + 1, h - 1) * w + x0]
        val c11 = src[min(y0 + 1, h - 1) * w + min(x0 + 1, w - 1)]
        fun chan(shift: Int): Int {
            val v00 = c00 shr shift and 0xFF
            val v10 = c10 shr shift and 0xFF
            val v01 = c01 shr shift and 0xFF
            val v11 = c11 shr shift and 0xFF
            val top = v00 + (v10 - v00) * tx
            val bottom = v01 + (v11 - v01) * tx
            return (top + (bottom - top) * ty).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (chan(16) shl 16) or (chan(8) shl 8) or chan(0)
    }

    private fun mixColor(a: Int, b: Int, t: Float): Int {
        if (t <= 0f) return a
        if (t >= 1f) return b
        fun mix(shift: Int): Int {
            val va = a shr shift and 0xFF
            val vb = b shr shift and 0xFF
            return (va + (vb - va) * t).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    private fun Bitmap.toPixels(): IntArray {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels
    }

    private fun IntArray.toBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(this, 0, w, 0, 0, w, h)
        return bitmap
    }
}
