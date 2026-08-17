package com.tianqi.camera.service

import android.net.Uri

/** 一张图片的滤镜状态：滤镜 id + 强度 0-100（PRD 3.2：每张图独立设置） */
data class FilterState(
    val filterId: String = "none",
    val intensity: Float = 100f
)

/**
 * 当前编辑会话的临时状态：拍照/选图结果在页面间传递用。
 * 一期纯内存持有，进程被杀即丢弃（草稿恢复在后续阶段做）。
 */
object EditSession {
    /** 拍照产出的单张照片（进单图编辑页） */
    var capturedPhoto: Uri? = null

    /** 相册多选的照片（进模板选择页），1-9 张 */
    var pickedPhotos: List<Uri> = emptyList()

    /** 每张图片的滤镜状态，key = uri.toString() */
    val filterStates = mutableMapOf<String, FilterState>()

    fun filterStateOf(uri: Uri): FilterState = filterStates[uri.toString()] ?: FilterState()

    fun updateFilterState(uri: Uri, state: FilterState) {
        filterStates[uri.toString()] = state
    }
}
