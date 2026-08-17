package com.tianqi.camera.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.tianqi.camera.model.CollageBackground
import com.tianqi.camera.model.CollageEditState
import com.tianqi.camera.model.CollageTemplate
import kotlin.math.max

/**
 * 拼图导出渲染器：把编辑状态按指定长边离屏重绘为 Bitmap。
 * 与编辑页预览使用同一套几何换算（间距/边框/圆角/取景偏移），保证导出与预览一致。
 */
object CollageRenderer {

    /** 预览基准宽度：编辑态滑杆的 px 以此宽度的屏幕为参照，导出时按实际宽度等比放大 */
    private const val REFERENCE_WIDTH = 1080f

    fun render(
        context: Context,
        template: CollageTemplate,
        state: CollageEditState,
        longEdge: Int = 2560
    ): Bitmap {
        val aspect = state.canvasRatio.aspect
        val width: Int
        val height: Int
        if (aspect >= 1f) {
            width = longEdge
            height = (longEdge / aspect).toInt()
        } else {
            height = longEdge
            width = (longEdge * aspect).toInt()
        }
        val scale = width / REFERENCE_WIDTH

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        drawBackground(context, canvas, state, width.toFloat(), height.toFloat())

        val cornerPx = state.cornerRadius * scale
        val spacingPx = state.spacing * scale
        val borderPx = state.borderWidth * scale

        template.slots.forEachIndexed { index, spec ->
            val slotState = state.slots.getOrNull(index) ?: return@forEachIndexed
            val uri = slotState.uri ?: return@forEachIndexed

            // 槽位外框（含间距）
            val frame = RectF(
                spec.x * width + spacingPx / 2,
                spec.y * height + spacingPx / 2,
                (spec.x + spec.w) * width - spacingPx / 2,
                (spec.y + spec.h) * height - spacingPx / 2
            )
            // 边框（白色）
            if (borderPx > 0f) {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                canvas.drawRoundRect(frame, cornerPx, cornerPx, borderPaint)
            }
            // 图片区域 = 外框内缩边框
            val imageRect = RectF(frame).apply { inset(borderPx, borderPx) }

            val bitmap = BitmapLoader.decode(context, uri, maxDimension = longEdge) ?: return@forEachIndexed
            val filtered = FilterEngine.applyToBitmap(
                bitmap,
                com.tianqi.camera.model.SweetFilters.byId(EditSession.filterStateOf(uri).filterId),
                EditSession.filterStateOf(uri).intensity
            )

            val path = Path().apply {
                addRoundRect(imageRect, cornerPx, cornerPx, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(filtered, buildImageMatrix(filtered, slotState, imageRect), null)
            canvas.restore()
            if (filtered != bitmap) filtered.recycle()
            bitmap.recycle()
        }
        return out
    }

    /**
     * 图片绘制矩阵：cover 铺满 → 用户缩放 → 绕图中心旋转 → 平移到取景位置。
     * 与编辑页预览的 graphicsLayer（scale→rotate→translate，pivot 为中心）一一对应。
     */
    private fun buildImageMatrix(
        bitmap: Bitmap,
        slot: com.tianqi.camera.model.SlotEditState,
        rect: RectF
    ): Matrix {
        val rotated = slot.rotationQuarter % 2 != 0
        val bmpW = if (rotated) bitmap.height.toFloat() else bitmap.width.toFloat()
        val bmpH = if (rotated) bitmap.width.toFloat() else bitmap.height.toFloat()
        val cover = max(rect.width() / bmpW, rect.height() / bmpH)
        val s = cover * slot.scale
        val cx = rect.centerX() + slot.offsetX * rect.width()
        val cy = rect.centerY() + slot.offsetY * rect.height()
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f

        return Matrix().apply {
            postScale(if (slot.mirrored) -s else s, s, centerX, centerY)
            postRotate(slot.rotationQuarter * 90f, centerX, centerY)
            postTranslate(cx - centerX, cy - centerY)
        }
    }

    private fun drawBackground(
        context: Context,
        canvas: Canvas,
        state: CollageEditState,
        width: Float,
        height: Float
    ) {
        when (val bg = state.background) {
            is CollageBackground.Solid -> canvas.drawColor(bg.argb.toInt())
            is CollageBackground.Gradient -> {
                val paint = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width, height,
                        bg.fromArgb.toInt(), bg.toArgb.toInt(), Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, paint)
            }
            CollageBackground.Blur -> {
                // 取第一张图缩小后放大铺满，近似高斯模糊，再盖一层奶白柔化
                canvas.drawColor(0xFFFFF9F7.toInt())
                val firstUri = state.slots.firstOrNull { it.uri != null }?.uri
                if (firstUri != null) {
                    BitmapLoader.decode(context, firstUri, maxDimension = 32)?.let { tiny ->
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        val scale = max(width / tiny.width, height / tiny.height)
                        val matrix = Matrix().apply {
                            postScale(scale, scale)
                            postTranslate((width - tiny.width * scale) / 2, (height - tiny.height * scale) / 2)
                        }
                        canvas.drawBitmap(tiny, matrix, paint)
                        tiny.recycle()
                    }
                    canvas.drawColor(0x66FFF9F7)
                }
            }
        }
    }
}
