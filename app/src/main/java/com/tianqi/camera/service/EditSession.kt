package com.tianqi.camera.service

import android.net.Uri

/**
 * 当前编辑会话的临时状态：拍照/选图结果在页面间传递用。
 * 一期纯内存持有，进程被杀即丢弃（草稿恢复在后续阶段做）。
 */
object EditSession {
    /** 拍照产出的单张照片（进单图编辑页） */
    var capturedPhoto: Uri? = null

    /** 相册多选的照片（进模板选择页），1-9 张 */
    var pickedPhotos: List<Uri> = emptyList()
}
