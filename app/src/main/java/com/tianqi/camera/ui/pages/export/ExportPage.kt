package com.tianqi.camera.ui.pages.export

import androidx.compose.runtime.Composable
import com.tianqi.camera.ui.components.PlaceholderPage

/** 导出页（空壳）：保存到相册 / 系统分享 —— 见 PRD 第 4 节 */
@Composable
fun ExportPage(onBack: () -> Unit) {
    PlaceholderPage(title = "导出", hint = "导出功能开发中\n（保存到相册 · 系统分享）", onBack = onBack)
}
