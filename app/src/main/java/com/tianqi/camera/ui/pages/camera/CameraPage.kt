package com.tianqi.camera.ui.pages.camera

import androidx.compose.runtime.Composable
import com.tianqi.camera.ui.components.PlaceholderPage

/** 拍照页（空壳）：前后摄切换、闪光灯、1:1 / 3:4 / 9:16 画幅 —— 见 PRD 3.1 */
@Composable
fun CameraPage(onBack: () -> Unit) {
    PlaceholderPage(title = "拍一张", hint = "拍照功能开发中\n（前后摄切换 · 闪光灯 · 画幅切换）", onBack = onBack)
}
