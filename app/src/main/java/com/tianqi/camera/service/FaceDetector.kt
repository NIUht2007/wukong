package com.tianqi.camera.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.tianqi.camera.model.FaceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** ML Kit 人脸检测：返回归一化关键点；检测不到返回空列表 */
object FaceDetector {

    private const val TAG = "FaceDetector"

    /** 最近一次检测的异常信息（调试用，null = 无异常） */
    var lastError: String? = null
        private set

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()
        )
    }

    suspend fun detect(context: Context, uri: Uri): List<FaceData> =
        withContext(Dispatchers.Default) {
            lastError = null
            val bitmap = BitmapLoader.decode(context, uri, maxDimension = 1024)
            if (bitmap == null) {
                lastError = "图片解码失败"
                Log.e(TAG, "decode failed for $uri")
                return@withContext emptyList()
            }
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val faces = detector.process(image).await()
                Log.d(TAG, "detected ${faces.size} face(s)")
                val w = bitmap.width.toFloat()
                val h = bitmap.height.toFloat()
                faces.map { face ->
                    val box = face.boundingBox
                    // 眼睛中心：优先轮廓点均值，缺失时用包围盒估计
                    val leftEye = averagePoint(face.getContour(FaceContour.LEFT_EYE)?.points)
                        ?: android.graphics.PointF(
                            box.left + box.width() * 0.30f,
                            box.top + box.height() * 0.40f
                        )
                    val rightEye = averagePoint(face.getContour(FaceContour.RIGHT_EYE)?.points)
                        ?: android.graphics.PointF(
                            box.left + box.width() * 0.70f,
                            box.top + box.height() * 0.40f
                        )
                    FaceData(
                        leftEyeX = leftEye.x / w,
                        leftEyeY = leftEye.y / h,
                        rightEyeX = rightEye.x / w,
                        rightEyeY = rightEye.y / h,
                        cheekLeftX = (box.left + box.width() * 0.18f) / w,
                        cheekLeftY = (box.top + box.height() * 0.68f) / h,
                        cheekRightX = (box.right - box.width() * 0.18f) / w,
                        cheekRightY = (box.top + box.height() * 0.68f) / h,
                        faceWidthFraction = box.width() / w
                    )
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
                Log.e(TAG, "face detection failed", e)
                emptyList()
            } finally {
                bitmap.recycle()
            }
        }

    private fun averagePoint(points: List<android.graphics.PointF>?): android.graphics.PointF? {
        if (points.isNullOrEmpty()) return null
        return android.graphics.PointF(
            points.map { it.x }.average().toFloat(),
            points.map { it.y }.average().toFloat()
        )
    }
}
