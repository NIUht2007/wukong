package com.tianqi.camera.service

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import com.tianqi.camera.model.DoodleLayer
import com.tianqi.camera.model.DoodlePath
import com.tianqi.camera.model.EditorLayer
import com.tianqi.camera.model.StickerLayer
import com.tianqi.camera.model.TextLayer

/**
 * 图层导出渲染：把装饰图层合成到导出画布上。
 * 坐标/缩放换算与 LayerOverlay 预览完全一致。
 */
object LayerRenderer {

    /** 贴纸基准尺寸：画布短边的 25% */
    const val STICKER_BASE_FRACTION = 0.25f
    /** 文字基准字号：画布短边的 8% */
    const val TEXT_BASE_FRACTION = 0.08f

    fun drawLayers(canvas: Canvas, layers: List<EditorLayer>, width: Int, height: Int) {
        layers.forEach { layer ->
            when (layer) {
                is StickerLayer -> drawSticker(canvas, layer, width, height)
                is TextLayer -> drawText(canvas, layer, width, height)
                is DoodleLayer -> {
                    val doodle = renderDoodle(layer.paths, width, height)
                    canvas.drawBitmap(doodle, 0f, 0f, null)
                    doodle.recycle()
                }
            }
        }
    }

    private fun drawSticker(canvas: Canvas, layer: StickerLayer, width: Int, height: Int) {
        val sticker = StickerFactory.createBitmap(layer.stickerId)
        val base = minOf(width, height) * STICKER_BASE_FRACTION
        val size = base * layer.scale
        val matrix = Matrix().apply {
            postScale(size / sticker.width, size / sticker.height)
            postTranslate(-size / 2, -size / 2)
            postRotate(layer.rotation)
            postTranslate(layer.centerX * width, layer.centerY * height)
        }
        canvas.drawBitmap(sticker, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        sticker.recycle()
    }

    private fun drawText(canvas: Canvas, layer: TextLayer, width: Int, height: Int) {
        val textSize = minOf(width, height) * TEXT_BASE_FRACTION * layer.scale
        val typeface = when (layer.fontIndex) {
            1 -> Typeface.SERIF
            2 -> Typeface.MONOSPACE
            else -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.typeface = typeface
            color = layer.colorArgb.toInt()
            alpha = (layer.alpha * 255).toInt().coerceIn(0, 255)
            textAlign = Paint.Align.CENTER
        }
        val x = layer.centerX * width
        val y = layer.centerY * height
        canvas.save()
        canvas.rotate(layer.rotation, x, y)
        // 基线修正：让文字视觉中心落在 (x, y)
        val baseline = y - (base.descent() + base.ascent()) / 2
        if (layer.shadow) {
            val shadowPaint = Paint(base).apply {
                color = Color.BLACK
                alpha = (base.alpha * 0.25f).toInt()
                maskFilter = BlurMaskFilter(textSize * 0.08f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawText(layer.text, x + textSize * 0.04f, baseline + textSize * 0.04f, shadowPaint)
        }
        if (layer.stroke) {
            val strokePaint = Paint(base).apply {
                style = Paint.Style.STROKE
                strokeWidth = textSize * 0.08f
                color = Color.WHITE
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawText(layer.text, x, baseline, strokePaint)
        }
        canvas.drawText(layer.text, x, baseline, base)
        canvas.restore()
    }

    /** 涂鸦渲染为整幅画布位图；橡皮擦用 CLEAR 模式只擦除涂鸦自身 */
    fun renderDoodle(paths: List<DoodlePath>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        paths.forEach { doodlePath ->
            if (doodlePath.points.size < 2) return@forEach
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = doodlePath.widthFraction * width
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (doodlePath.eraser) {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    color = doodlePath.colorArgb.toInt()
                }
            }
            val path = Path().apply {
                moveTo(doodlePath.points.first().x * width, doodlePath.points.first().y * height)
                doodlePath.points.drop(1).forEach { lineTo(it.x * width, it.y * height) }
            }
            canvas.drawPath(path, paint)
        }
        return bitmap
    }
}
