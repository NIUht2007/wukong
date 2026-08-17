package com.tianqi.camera.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 圆角规范：大卡片 24px，小卡片/按钮 16px，图片槽位 12px；主按钮胶囊形见 AppShapes.Pill
val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

object AppShapes {
    /** 图片槽位默认圆角 */
    val Slot = RoundedCornerShape(12.dp)
    /** 小卡片 / 按钮 */
    val Card = RoundedCornerShape(16.dp)
    /** 大卡片 */
    val LargeCard = RoundedCornerShape(24.dp)
    /** 主按钮胶囊形 */
    val Pill = RoundedCornerShape(999.dp)
}
