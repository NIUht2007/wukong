# 甜气相机 release 构建规则
# ML Kit 人脸检测模型与 native 库由 SDK 自带规则处理，这里保留反射相关

# ML Kit / Google Play Services 相关
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# data class 在 JSON 序列化中用到（org.json 按字段名手动取值，无需额外 keep）
