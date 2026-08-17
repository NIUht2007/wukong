package com.tianqi.camera.model

import android.content.Context
import org.json.JSONObject

/** 画布比例（宽:高） */
enum class CanvasRatio(val label: String, val width: Int, val height: Int) {
    R1_1("1:1", 1, 1),
    R3_4("3:4", 3, 4),
    R4_3("4:3", 4, 3),
    R9_16("9:16", 9, 16);

    val aspect: Float get() = width.toFloat() / height

    companion object {
        fun of(label: String): CanvasRatio = entries.firstOrNull { it.label == label } ?: R1_1
    }
}

/** 模板槽位：坐标和尺寸都是画布的比例值（0-1） */
data class SlotSpec(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

/**
 * 拼图模板。模板定义在 assets/templates.json，与代码解耦，
 * 新增模板只需加 JSON 条目（PRD 3.3）。
 */
data class CollageTemplate(
    val id: String,
    val name: String,
    val photoCount: Int,
    val defaultRatio: CanvasRatio,
    val slots: List<SlotSpec>
)

object TemplateRepository {

    @Volatile
    private var cache: List<CollageTemplate>? = null

    fun load(context: Context): List<CollageTemplate> {
        cache?.let { return it }
        val json = context.assets.open("templates.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val array = root.getJSONArray("templates")
        val result = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val slotsJson = obj.getJSONArray("slots")
            CollageTemplate(
                id = obj.getString("id"),
                name = obj.getString("name"),
                photoCount = obj.getInt("count"),
                defaultRatio = CanvasRatio.of(obj.optString("ratio", "1:1")),
                slots = (0 until slotsJson.length()).map { j ->
                    val s = slotsJson.getJSONObject(j)
                    SlotSpec(
                        x = s.getDouble("x").toFloat(),
                        y = s.getDouble("y").toFloat(),
                        w = s.getDouble("w").toFloat(),
                        h = s.getDouble("h").toFloat()
                    )
                }
            )
        }
        cache = result
        return result
    }

    fun byId(context: Context, id: String): CollageTemplate? = load(context).firstOrNull { it.id == id }
}
