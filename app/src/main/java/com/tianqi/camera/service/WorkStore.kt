package com.tianqi.camera.service

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/** 我的作品：导出图在 App 私有目录留一份副本，供首页展示和再次分享 */
object WorkStore {

    private fun worksDir(context: Context): File =
        File(context.filesDir, "works").apply { mkdirs() }

    fun add(context: Context, bitmap: Bitmap): File {
        val file = File(worksDir(context), "work_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        EditSession.worksVersion.intValue++
        return file
    }

    /** 已存在的图片文件（如查看历史作品时）复制进作品目录 */
    fun add(context: Context, source: File): File {
        val file = File(worksDir(context), "work_${System.currentTimeMillis()}.jpg")
        source.copyTo(file, overwrite = true)
        EditSession.worksVersion.intValue++
        return file
    }

    fun list(context: Context): List<File> =
        worksDir(context).listFiles { f -> f.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun delete(file: File) {
        file.delete()
        EditSession.worksVersion.intValue++
    }
}
