package com.tianqi.camera.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/**
 * 内置贴纸工厂（占位策略，见 04-素材与设计规范.md）：
 * 先用程序自绘 8 枚甜系贴纸，素材包到位后替换为 assets PNG。
 * 预览与导出共用同一份位图，保证一致。
 */
object StickerFactory {

    val stickerIds = listOf("star", "heart", "bow", "flower", "sparkle", "cloud", "bubble", "circle")

    fun nameOf(id: String): String = when (id) {
        "star" -> "星星"
        "heart" -> "爱心"
        "bow" -> "蝴蝶结"
        "flower" -> "小花"
        "sparkle" -> "闪闪"
        "cloud" -> "云朵"
        "bubble" -> "气泡"
        else -> "圈圈"
    }

    private const val PINK = 0xFFFF9FB2.toInt()
    private const val PEACH = 0xFFFFC9A3.toInt()
    private const val BERRY = 0xFFF2708A.toInt()
    private const val APRICOT = 0xFFFFE9E4.toInt()
    private const val MINT = 0xFFBFE3D5.toInt()

    fun createBitmap(stickerId: String, sizePx: Int = 256): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = sizePx * 0.035f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val s = sizePx.toFloat()
        when (stickerId) {
            "star" -> drawStar(canvas, s, PINK, fill, outline)
            "heart" -> drawHeart(canvas, s, BERRY, fill, outline)
            "bow" -> drawBow(canvas, s, PINK, fill, outline)
            "flower" -> drawFlower(canvas, s, PEACH, fill, outline)
            "sparkle" -> drawSparkle(canvas, s, PEACH, fill, outline)
            "cloud" -> drawCloud(canvas, s, APRICOT, fill, outline)
            "bubble" -> drawBubble(canvas, s, MINT, fill, outline)
            else -> drawDoodleCircle(canvas, s, BERRY, fill, outline)
        }
        return bitmap
    }

    private fun drawStar(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        val path = Path()
        val cx = s / 2
        val cy = s / 2
        val outer = s * 0.42f
        val inner = outer * 0.45f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val angle = Math.toRadians((i * 36 - 90).toDouble())
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fill.color = color
        canvas.drawPath(path, fill)
        canvas.drawPath(path, outline)
    }

    private fun drawHeart(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        val path = Path()
        val w = s * 0.8f
        val left = (s - w) / 2
        val top = s * 0.22f
        path.moveTo(s / 2, top + w * 0.32f)
        path.cubicTo(left, top - w * 0.08f, left - w * 0.12f, top + w * 0.5f, s / 2, top + w * 0.85f)
        path.cubicTo(s - left + w * 0.12f, top + w * 0.5f, s - left, top - w * 0.08f, s / 2, top + w * 0.32f)
        path.close()
        fill.color = color
        canvas.drawPath(path, fill)
        canvas.drawPath(path, outline)
    }

    private fun drawBow(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        fill.color = color
        val leftWing = Path().apply {
            moveTo(s * 0.5f, s * 0.5f)
            cubicTo(s * 0.28f, s * 0.22f, s * 0.08f, s * 0.3f, s * 0.12f, s * 0.5f)
            cubicTo(s * 0.08f, s * 0.7f, s * 0.28f, s * 0.78f, s * 0.5f, s * 0.5f)
            close()
        }
        val rightWing = Path().apply {
            moveTo(s * 0.5f, s * 0.5f)
            cubicTo(s * 0.72f, s * 0.22f, s * 0.92f, s * 0.3f, s * 0.88f, s * 0.5f)
            cubicTo(s * 0.92f, s * 0.7f, s * 0.72f, s * 0.78f, s * 0.5f, s * 0.5f)
            close()
        }
        canvas.drawPath(leftWing, fill)
        canvas.drawPath(rightWing, fill)
        canvas.drawPath(leftWing, outline)
        canvas.drawPath(rightWing, outline)
        fill.color = BERRY
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.09f, fill)
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.09f, outline)
    }

    private fun drawFlower(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        fill.color = color
        val petalR = s * 0.16f
        val ringR = s * 0.22f
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72 - 90).toDouble())
            val px = s / 2 + (ringR * cos(angle)).toFloat()
            val py = s / 2 + (ringR * sin(angle)).toFloat()
            canvas.drawCircle(px, py, petalR, fill)
            canvas.drawCircle(px, py, petalR, outline)
        }
        fill.color = 0xFFFFF3D6.toInt()
        canvas.drawCircle(s / 2, s / 2, s * 0.13f, fill)
        canvas.drawCircle(s / 2, s / 2, s * 0.13f, outline)
    }

    private fun drawSparkle(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.08f)
            quadTo(s * 0.58f, s * 0.42f, s * 0.92f, s * 0.5f)
            quadTo(s * 0.58f, s * 0.58f, s * 0.5f, s * 0.92f)
            quadTo(s * 0.42f, s * 0.58f, s * 0.08f, s * 0.5f)
            quadTo(s * 0.42f, s * 0.42f, s * 0.5f, s * 0.08f)
            close()
        }
        fill.color = color
        canvas.drawPath(path, fill)
        canvas.drawPath(path, outline)
    }

    private fun drawCloud(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        fill.color = color
        val path = Path().apply {
            addCircle(s * 0.36f, s * 0.55f, s * 0.16f, Path.Direction.CW)
            addCircle(s * 0.52f, s * 0.44f, s * 0.19f, Path.Direction.CW)
            addCircle(s * 0.66f, s * 0.56f, s * 0.15f, Path.Direction.CW)
            addRect(RectF(s * 0.28f, s * 0.52f, s * 0.74f, s * 0.7f), Path.Direction.CW)
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, outline)
    }

    private fun drawBubble(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        fill.color = color
        val rect = RectF(s * 0.14f, s * 0.2f, s * 0.86f, s * 0.68f)
        canvas.drawRoundRect(rect, s * 0.14f, s * 0.14f, fill)
        canvas.drawRoundRect(rect, s * 0.14f, s * 0.14f, outline)
        val tail = Path().apply {
            moveTo(s * 0.32f, s * 0.66f)
            lineTo(s * 0.26f, s * 0.84f)
            lineTo(s * 0.46f, s * 0.66f)
            close()
        }
        canvas.drawPath(tail, fill)
        canvas.drawPath(tail, outline)
    }

    private fun drawDoodleCircle(canvas: Canvas, s: Float, color: Int, fill: Paint, outline: Paint) {
        // 手绘感圆圈：略不规整的双圈
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.color = color
            strokeWidth = s * 0.05f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.32f, paint)
        paint.strokeWidth = s * 0.03f
        canvas.drawCircle(s * 0.52f, s * 0.48f, s * 0.38f, paint)
    }
}
