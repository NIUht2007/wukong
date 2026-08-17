package com.tianqi.camera.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream

/** 从 Uri（content:// 或 file://）解码 Bitmap，带采样压缩与 EXIF 旋转归正 */
object BitmapLoader {

    private const val TAG = "BitmapLoader"

    /** file:// 直接读文件；content:// 走 ContentResolver（带异常保护） */
    private fun openStream(context: Context, uri: Uri): InputStream? =
        if (uri.scheme == "file") {
            uri.path?.let { path -> runCatching { File(path).inputStream() }.getOrNull() }
        } else {
            runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
        }

    /**
     * @param maxDimension 最长边上限，超出按比例采样缩小；<= 0 表示不压缩
     */
    fun decode(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        // 先读尺寸
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Log.e(TAG, "decode bounds failed for $uri (scheme=${uri.scheme})")
            return null
        }

        // 计算采样率
        var sample = 1
        if (maxDimension > 0) {
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / (sample * 2) >= maxDimension) sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (raw == null) {
            Log.e(TAG, "decode pixels failed for $uri")
            return null
        }

        // EXIF 旋转/镜像归正
        val exif = openStream(context, uri)?.use { ExifInterface(it) } ?: return raw
        val matrix = Matrix()
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return raw
        }
        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (rotated != raw) raw.recycle()
        return rotated
    }
}
