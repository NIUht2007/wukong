package com.tianqi.camera.ui.pages.photo

import androidx.compose.runtime.Composable
import com.tianqi.camera.ui.components.PlaceholderPage

/** 单图编辑页（空壳）：滤镜 | 美颜 | 贴纸 | 文字 | 涂鸦 —— 见 PRD 3.2/3.5 */
@Composable
fun PhotoEditorPage(onBack: () -> Unit) {
    PlaceholderPage(title = "照片编辑", hint = "照片编辑开发中\n（滤镜 · 美颜 · 贴纸 · 文字 · 涂鸦）", onBack = onBack)
}
