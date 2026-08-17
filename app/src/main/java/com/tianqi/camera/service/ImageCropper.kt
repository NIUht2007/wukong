package com.tianqi.camera.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/** 拍摄画幅（宽:高），与拍照页遮罩一致 */
enum class CaptureRatio(val label: String, val width: Int, val height: Int) {
    R1_1("1:1", 1, 1),
    R3_4("3:4", 3, 4),
    R9_16("9:16", 9, 16)
}

object ImageCropper {

    /**
     * 解码原图（按 EXIF 旋转归正）→ 居中裁切到目标画幅 → 另存为新 JPEG。
     * 返回裁切后的文件；解码失败时返回 null。
     */
    fun centerCropToRatio(srcFile: File, ratio: CaptureRatio, outDir: File): File? {
        val raw = BitmapFactory.decodeFile(srcFile.absolutePath) ?: return null
        val bitmap = applyExifRotation(raw, srcFile)

        val targetW: Int
        val targetH: Int
        val srcRatio = ratio.width.toFloat() / ratio.height
        val bmpRatio = bitmap.width.toFloat() / bitmap.height
        if (bmpRatio > srcRatio) {
            // 原图偏宽，裁两边
            targetH = bitmap.height
            targetW = (targetH * srcRatio).toInt()
        } else {
            // 原图偏高，裁上下
            targetW = bitmap.width
            targetH = (targetW / srcRatio).toInt()
        }
        val x = ((bitmap.width - targetW) / 2).coerceAtLeast(0)
        val y = ((bitmap.height - targetH) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(bitmap, x, y, targetW, targetH)

        val outFile = File(outDir, "photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        if (cropped != bitmap) cropped.recycle()
        if (bitmap != raw) bitmap.recycle()
        raw.recycle()
        return outFile
    }

    private fun applyExifRotation(bitmap: Bitmap, file: File): Bitmap {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
