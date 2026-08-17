package com.tianqi.camera.model

/**
 * 单款滤镜的调色参数。
 * 所有滤镜遵循甜系审美：偏低对比、偏暖、轻微褪色（黑位抬升）、整体低饱和。
 *
 * @param saturation 饱和度倍率，1 = 不变
 * @param contrast 对比度倍率，1 = 不变（< 1 画面更柔和）
 * @param lift 黑位抬升量（0-255 尺度），制造褪色/空气感
 * @param rGain/gGain/bGain 各通道增益，> 1 提亮该通道
 * @param rOffset/gOffset/bOffset 各通道偏移（0-255 尺度），微调色调
 */
data class FilterAdjust(
    val saturation: Float = 1f,
    val contrast: Float = 1f,
    val lift: Float = 0f,
    val rGain: Float = 1f,
    val gGain: Float = 1f,
    val bGain: Float = 1f,
    val rOffset: Float = 0f,
    val gOffset: Float = 0f,
    val bOffset: Float = 0f
)

/** 一款滤镜：id + 显示名 + 调色参数 */
data class FilterSpec(
    val id: String,
    val name: String,
    val adjust: FilterAdjust
)

/**
 * 内置甜系滤镜库（PRD 3.2：一期 10 款）。
 * 调色思路写在每款的注释里，方便后续微调。
 */
object SweetFilters {

    /** 无滤镜 */
    val NONE = FilterSpec("none", "原图", FilterAdjust())

    val ALL: List<FilterSpec> = listOf(
        NONE,
        // 奶油：柔和低对比 + 明显褪色 + 轻暖调，像奶油一样软糯
        FilterSpec(
            "cream", "奶油",
            FilterAdjust(saturation = 0.90f, contrast = 0.92f, lift = 18f, rGain = 1.04f, bGain = 0.94f)
        ),
        // 蜜桃：暖粉橙调，红通道增益高、蓝通道压低，甜度最高的一款
        FilterSpec(
            "peach", "蜜桃",
            FilterAdjust(saturation = 0.95f, contrast = 0.95f, lift = 12f, rGain = 1.08f, bGain = 0.92f, rOffset = 6f)
        ),
        // 樱粉：粉红倾向，红增绿减，像樱花的淡粉
        FilterSpec(
            "sakura", "樱粉",
            FilterAdjust(saturation = 0.92f, contrast = 0.93f, lift = 15f, rGain = 1.10f, gGain = 0.98f)
        ),
        // 薄荷：唯一偏冷的一款，绿通道提亮、红通道略压，清新但不破坏暖调基底
        FilterSpec(
            "mint", "薄荷",
            FilterAdjust(saturation = 0.88f, contrast = 0.94f, lift = 14f, rGain = 0.96f, gGain = 1.06f, bGain = 1.02f)
        ),
        // 甜橙：橘子汽水感，暖橙 + 蓝通道明显压低，饱和度比其他款略高
        FilterSpec(
            "orange", "甜橙",
            FilterAdjust(saturation = 1.00f, contrast = 0.95f, lift = 10f, rGain = 1.10f, gGain = 1.02f, bGain = 0.88f)
        ),
        // 胶片甜：强褪色 + 低饱和 + 低对比，胶片扫街感
        FilterSpec(
            "film", "胶片甜",
            FilterAdjust(saturation = 0.85f, contrast = 0.90f, lift = 25f, rGain = 1.03f, bGain = 0.95f)
        ),
        // 奶杏：杏子色的暖 beige，介于奶油和蜜桃之间，日常百搭
        FilterSpec(
            "apricot", "奶杏",
            FilterAdjust(saturation = 0.90f, contrast = 0.94f, lift = 16f, rGain = 1.05f, gGain = 1.01f, bGain = 0.93f)
        ),
        // 云朵：最亮最空气感的一款，黑位抬得最高，像阴天柔光
        FilterSpec(
            "cloud", "云朵",
            FilterAdjust(saturation = 0.82f, contrast = 0.88f, lift = 30f)
        ),
        // 蜜茶：暖棕茶调，对比略高一点点，适合食物和咖啡店场景
        FilterSpec(
            "tea", "蜜茶",
            FilterAdjust(saturation = 0.90f, contrast = 0.96f, lift = 8f, rGain = 1.06f, gGain = 1.02f, bGain = 0.90f)
        ),
        // 莓果：莓果红强调，红通道增益 + 轻压蓝，饱和略高，适合人像腮红感
        FilterSpec(
            "berry", "莓果",
            FilterAdjust(saturation = 1.02f, contrast = 0.97f, lift = 6f, rGain = 1.08f, bGain = 0.97f)
        )
    )

    fun byId(id: String): FilterSpec = ALL.firstOrNull { it.id == id } ?: NONE
}
