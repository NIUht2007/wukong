package com.tianqi.camera.service

import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.tianqi.camera.model.CanvasRatio
import com.tianqi.camera.model.CollageBackground
import com.tianqi.camera.model.CollageEditState
import com.tianqi.camera.model.DoodleLayer
import com.tianqi.camera.model.DoodlePath
import com.tianqi.camera.model.EditorLayer
import com.tianqi.camera.model.SlotEditState
import com.tianqi.camera.model.StickerLayer
import com.tianqi.camera.model.TextLayer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 拼图草稿自动保存：编辑状态序列化为 JSON 存私有目录，
 * 下次进 App 时提示"继续上次编辑？"（PRD 阶段6）。
 */
object DraftStore {

    private fun draftFile(context: Context) = File(context.filesDir, "collage_draft.json")

    fun hasDraft(context: Context): Boolean = draftFile(context).exists()

    fun clear(context: Context) {
        draftFile(context).delete()
    }

    fun saveCollage(context: Context, state: CollageEditState, layers: List<EditorLayer>) {
        runCatching {
            val root = JSONObject().apply {
                put("templateId", state.templateId)
                put("canvasRatio", state.canvasRatio.label)
                put("spacing", state.spacing.toDouble())
                put("borderWidth", state.borderWidth.toDouble())
                put("cornerRadius", state.cornerRadius.toDouble())
                put("background", backgroundToJson(state.background))
                put("slots", JSONArray().apply {
                    state.slots.forEach { slot ->
                        put(JSONObject().apply {
                            put("uri", slot.uri?.toString() ?: JSONObject.NULL)
                            put("scale", slot.scale.toDouble())
                            put("offsetX", slot.offsetX.toDouble())
                            put("offsetY", slot.offsetY.toDouble())
                            put("rotationQuarter", slot.rotationQuarter)
                            put("mirrored", slot.mirrored)
                        })
                    }
                })
                put("layers", JSONArray().apply {
                    layers.forEach { put(layerToJson(it)) }
                })
                // 槽位图片的滤镜状态一并保存
                put("filters", JSONObject().apply {
                    state.slots.mapNotNull { it.uri }.forEach { uri ->
                        val fs = EditSession.filterStateOf(uri)
                        put(uri.toString(), JSONObject().apply {
                            put("filterId", fs.filterId)
                            put("intensity", fs.intensity.toDouble())
                        })
                    }
                })
            }
            draftFile(context).writeText(root.toString())
        }
    }

    data class CollageDraft(
        val templateId: String,
        val state: CollageEditState,
        val layers: List<EditorLayer>
    )

    fun loadCollage(context: Context): CollageDraft? = runCatching {
        val root = JSONObject(draftFile(context).readText())
        val slotsJson = root.getJSONArray("slots")
        val slots = (0 until slotsJson.length()).map { i ->
            val s = slotsJson.getJSONObject(i)
            SlotEditState(
                uri = if (s.isNull("uri")) null else Uri.parse(s.getString("uri")),
                scale = s.getDouble("scale").toFloat(),
                offsetX = s.getDouble("offsetX").toFloat(),
                offsetY = s.getDouble("offsetY").toFloat(),
                rotationQuarter = s.getInt("rotationQuarter"),
                mirrored = s.getBoolean("mirrored")
            )
        }
        // 还原滤镜状态
        val filters = root.optJSONObject("filters")
        filters?.keys()?.forEach { key ->
            val f = filters.getJSONObject(key)
            EditSession.filterStates[key] = FilterState(
                filterId = f.getString("filterId"),
                intensity = f.getDouble("intensity").toFloat()
            )
        }
        val layersJson = root.getJSONArray("layers")
        CollageDraft(
            templateId = root.getString("templateId"),
            state = CollageEditState(
                templateId = root.getString("templateId"),
                canvasRatio = CanvasRatio.of(root.getString("canvasRatio")),
                slots = slots,
                spacing = root.getDouble("spacing").toFloat(),
                borderWidth = root.getDouble("borderWidth").toFloat(),
                cornerRadius = root.getDouble("cornerRadius").toFloat(),
                background = backgroundFromJson(root.getJSONObject("background"))
            ),
            layers = (0 until layersJson.length()).map { layerFromJson(layersJson.getJSONObject(it)) }
        )
    }.getOrNull()

    private fun backgroundToJson(bg: CollageBackground): JSONObject = when (bg) {
        is CollageBackground.Solid -> JSONObject().apply {
            put("type", "solid"); put("argb", bg.argb)
        }
        is CollageBackground.Gradient -> JSONObject().apply {
            put("type", "gradient"); put("from", bg.fromArgb); put("to", bg.toArgb)
        }
        CollageBackground.Blur -> JSONObject().apply { put("type", "blur") }
    }

    private fun backgroundFromJson(json: JSONObject): CollageBackground = when (json.getString("type")) {
        "gradient" -> CollageBackground.Gradient(json.getLong("from"), json.getLong("to"))
        "blur" -> CollageBackground.Blur
        else -> CollageBackground.Solid(json.getLong("argb"))
    }

    private fun layerToJson(layer: EditorLayer): JSONObject = when (layer) {
        is StickerLayer -> JSONObject().apply {
            put("type", "sticker"); put("id", layer.id); put("stickerId", layer.stickerId)
            put("cx", layer.centerX.toDouble()); put("cy", layer.centerY.toDouble())
            put("scale", layer.scale.toDouble()); put("rotation", layer.rotation.toDouble())
        }
        is TextLayer -> JSONObject().apply {
            put("type", "text"); put("id", layer.id); put("text", layer.text)
            put("color", layer.colorArgb); put("font", layer.fontIndex)
            put("stroke", layer.stroke); put("shadow", layer.shadow)
            put("alpha", layer.alpha.toDouble())
            put("cx", layer.centerX.toDouble()); put("cy", layer.centerY.toDouble())
            put("scale", layer.scale.toDouble()); put("rotation", layer.rotation.toDouble())
        }
        is DoodleLayer -> JSONObject().apply {
            put("type", "doodle"); put("id", layer.id)
            put("paths", JSONArray().apply {
                layer.paths.forEach { path ->
                    put(JSONObject().apply {
                        put("color", path.colorArgb)
                        put("width", path.widthFraction.toDouble())
                        put("eraser", path.eraser)
                        put("points", JSONArray().apply {
                            path.points.forEach { p ->
                                put(JSONArray().apply {
                                    put(p.x.toDouble()); put(p.y.toDouble())
                                })
                            }
                        })
                    })
                }
            })
        }
    }

    private fun layerFromJson(json: JSONObject): EditorLayer = when (json.getString("type")) {
        "sticker" -> StickerLayer(
            id = json.getString("id"),
            stickerId = json.getString("stickerId"),
            centerX = json.getDouble("cx").toFloat(),
            centerY = json.getDouble("cy").toFloat(),
            scale = json.getDouble("scale").toFloat(),
            rotation = json.getDouble("rotation").toFloat()
        )
        "text" -> TextLayer(
            id = json.getString("id"),
            text = json.getString("text"),
            colorArgb = json.getLong("color"),
            fontIndex = json.getInt("font"),
            stroke = json.getBoolean("stroke"),
            shadow = json.getBoolean("shadow"),
            alpha = json.getDouble("alpha").toFloat(),
            centerX = json.getDouble("cx").toFloat(),
            centerY = json.getDouble("cy").toFloat(),
            scale = json.getDouble("scale").toFloat(),
            rotation = json.getDouble("rotation").toFloat()
        )
        else -> {
            val pathsJson = json.getJSONArray("paths")
            DoodleLayer(
                id = json.getString("id"),
                paths = (0 until pathsJson.length()).map { i ->
                    val p = pathsJson.getJSONObject(i)
                    val pointsJson = p.getJSONArray("points")
                    DoodlePath(
                        points = (0 until pointsJson.length()).map { j ->
                            val pt = pointsJson.getJSONArray(j)
                            Offset(pt.getDouble(0).toFloat(), pt.getDouble(1).toFloat())
                        },
                        colorArgb = p.getLong("color"),
                        widthFraction = p.getDouble("width").toFloat(),
                        eraser = p.getBoolean("eraser")
                    )
                }
            )
        }
    }
}
