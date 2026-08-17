package com.tianqi.camera.model

/** 美颜参数，各项 0-100（PRD 3.5：一期默认值保守、效果自然） */
data class BeautyState(
    /** 磨皮（保边模糊） */
    val smooth: Float = 0f,
    /** 美白（亮度曲线） */
    val whiten: Float = 0f,
    /** 红润（红色增益） */
    val rosy: Float = 0f,
    /** 瘦脸（脸颊液化，需检测到人脸） */
    val slimFace: Float = 0f,
    /** 大眼（眼部膨胀，需检测到人脸） */
    val bigEyes: Float = 0f
)

/**
 * 人脸关键点（坐标均为相对图片宽/高的比例值，适配任意分辨率）
 */
data class FaceData(
    /** 左眼中心 */
    val leftEyeX: Float,
    val leftEyeY: Float,
    /** 右眼中心 */
    val rightEyeX: Float,
    val rightEyeY: Float,
    /** 左脸颊（液化中心） */
    val cheekLeftX: Float,
    val cheekLeftY: Float,
    /** 右脸颊（液化中心） */
    val cheekRightX: Float,
    val cheekRightY: Float,
    /** 脸宽比例（用于换算液化半径） */
    val faceWidthFraction: Float
)
