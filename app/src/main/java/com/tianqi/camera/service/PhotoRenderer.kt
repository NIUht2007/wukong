package com.tianqi.camera.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.tianqi.camera.model.BeautyState
import com.tianqi.camera.model.EditorLayer
import com.tianqi.camera.model.FaceData
import com.tianqi.camera.model.SweetFilters

/** 单图导出渲染：美颜 → 滤镜 → 装饰图层，全分辨率（最长边 2560） */
object PhotoRenderer {

    fun render(
        context: Context,
        uri: android.net.Uri,
        filterState: FilterState,
        beautyState: BeautyState,
        faces: List<FaceData>,
        layers: List<EditorLayer>,
        longEdge: Int = 2560
    ): Bitmap? {
        var bitmap = BitmapLoader.decode(context, uri, maxDimension = longEdge) ?: return null
        // inSampleSize 只能按 2 的幂采样，再精确缩放到最长边 longEdge，控制美颜计算量
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest > longEdge) {
            val scale = longEdge.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            bitmap.recycle()
            bitmap = scaled
        }

        if (beautyState != BeautyState()) {
            val processed = BeautyEngine.apply(bitmap, beautyState, faces)
            if (processed != bitmap) bitmap.recycle()
            bitmap = processed
        }

        val filter = SweetFilters.byId(filterState.filterId)
        val filtered = FilterEngine.applyToBitmap(bitmap, filter, filterState.intensity)
        if (filtered != bitmap) bitmap.recycle()
        bitmap = filtered

        if (layers.isNotEmpty()) {
            val canvas = Canvas(bitmap)
            LayerRenderer.drawLayers(canvas, layers, bitmap.width, bitmap.height)
        }
        return bitmap
    }
}
