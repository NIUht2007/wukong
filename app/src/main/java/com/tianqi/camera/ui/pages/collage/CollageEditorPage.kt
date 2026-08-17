package com.tianqi.camera.ui.pages.collage

import androidx.compose.runtime.Composable
import com.tianqi.camera.ui.components.PlaceholderPage

/** 拼图编辑页（空壳）：布局 | 边框 | 背景 | 滤镜 | 贴纸 | 文字 | 涂鸦 —— 见 PRD 3.3/3.4 */
@Composable
fun CollageEditorPage(onBack: () -> Unit) {
    PlaceholderPage(title = "拼图编辑", hint = "拼图编辑器开发中\n（布局 · 边框 · 背景 · 滤镜 · 贴纸 · 文字 · 涂鸦）", onBack = onBack)
}
