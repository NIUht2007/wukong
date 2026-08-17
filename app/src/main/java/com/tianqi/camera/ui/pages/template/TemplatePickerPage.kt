package com.tianqi.camera.ui.pages.template

import androidx.compose.runtime.Composable
import com.tianqi.camera.ui.components.PlaceholderPage

/** 模板选择页（空壳）：按图片数量分组展示拼图模板 —— 见 PRD 3.3 */
@Composable
fun TemplatePickerPage(onBack: () -> Unit) {
    PlaceholderPage(title = "拼个图", hint = "模板选择开发中\n（2图 / 3图 / 4图 / 6图 / 9图模板）", onBack = onBack)
}
