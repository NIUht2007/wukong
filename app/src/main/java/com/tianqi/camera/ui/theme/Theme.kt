package com.tianqi.camera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 一期只做浅色模式，禁用动态取色，保证品牌色不被系统主题覆盖（见设计规范）
private val TianqiColorScheme = lightColorScheme(
    primary = SweetPink,
    onPrimary = CardWhite,
    secondary = Peach,
    onSecondary = CocoaBrown,
    tertiary = BerryRed,
    onTertiary = CardWhite,
    background = MilkWhite,
    onBackground = CocoaBrown,
    surface = CardWhite,
    onSurface = CocoaBrown,
    surfaceVariant = MilkApricot,
    onSurfaceVariant = GrayPinkBrown,
    outline = MilkApricot
)

@Composable
fun TianqiCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TianqiColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
