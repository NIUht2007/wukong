package com.tianqi.camera.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/** 保存到系统相册（API 29+ 走 MediaStore 免权限，28 及以下需 WRITE_EXTERNAL_STORAGE） */
object GallerySaver {

    enum class Format(val mime: String, val ext: String) {
        JPEG("image/jpeg", "jpg"), PNG("image/png", "png")
    }

    /** 28 及以下需要先申请 WRITE_EXTERNAL_STORAGE */
    fun needsLegacyPermission(): Boolean = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    suspend fun save(context: Context, bitmap: Bitmap, format: Format): Uri? =
        withContext(Dispatchers.IO) {
            val name = "tianqi_${System.currentTimeMillis()}.${format.ext}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, bitmap, format, name)
            } else {
                saveLegacy(context, bitmap, format, name)
            }
        }

    private fun saveWithMediaStore(
        context: Context, bitmap: Bitmap, format: Format, name: String
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/甜气相机")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                compress(bitmap, format, out)
            } ?: return null
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private suspend fun saveLegacy(
        context: Context, bitmap: Bitmap, format: Format, name: String
    ): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "甜气相机"
        )
        dir.mkdirs()
        val file = File(dir, name)
        return try {
            file.outputStream().use { compress(bitmap, format, it) }
            // 通知系统相册扫描
            suspendCancellableCoroutine { cont ->
                MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), arrayOf(format.mime)
                ) { _, uri -> cont.resume(uri) }
            }
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    private fun compress(bitmap: Bitmap, format: Format, out: java.io.OutputStream) {
        when (format) {
            Format.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            Format.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
