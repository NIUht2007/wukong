package com.tianqi.camera.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.tianqi.camera.model.DoodleLayer
import com.tianqi.camera.model.DoodlePath
import com.tianqi.camera.model.EditorLayer
import com.tianqi.camera.service.UndoStack

/**
 * 图层编辑控制器：管理图层列表、选中态、涂鸦设置与撤销/重做栈。
 * 手势期间的连续变化走 preview（不入栈），手势结束走 commit。
 */
class LayerEditController(
    initial: List<EditorLayer>,
    private val onPersist: (List<EditorLayer>) -> Unit
) {
    private val stack = UndoStack(initial)

    var layers by mutableStateOf(initial)
        private set
    var selectedId by mutableStateOf<String?>(null)
    var doodleSettings by mutableStateOf(DoodleSettings())
    var version by mutableIntStateOf(0)
        private set

    val canUndo: Boolean get() = stack.canUndo
    val canRedo: Boolean get() = stack.canRedo

    fun commit(next: List<EditorLayer>) {
        if (next != stack.current) stack.push(next)
        apply(next)
    }

    fun preview(next: List<EditorLayer>) {
        layers = next
    }

    fun undo() = apply(stack.undo())
    fun redo() = apply(stack.redo())

    fun replace(updated: EditorLayer): List<EditorLayer> =
        layers.map { if (it.id == updated.id) updated else it }

    fun addLayer(layer: EditorLayer) {
        commit(layers + layer)
        selectedId = layer.id
    }

    fun deleteLayer(id: String) {
        commit(layers.filterNot { it.id == id })
        if (selectedId == id) selectedId = null
    }

    fun addDoodlePoint(point: Offset, isStart: Boolean) {
        val doodle = layers.filterIsInstance<DoodleLayer>().firstOrNull()
            ?: DoodleLayer(id = "doodle")
        val others = layers.filterNot { it is DoodleLayer }
        val paths = if (isStart) {
            doodle.paths + DoodlePath(
                points = listOf(point),
                colorArgb = doodleSettings.colorArgb,
                widthFraction = doodleSettings.widthFraction,
                eraser = doodleSettings.eraser
            )
        } else {
            val last = doodle.paths.lastOrNull() ?: return
            doodle.paths.dropLast(1) + last.copy(points = last.points + point)
        }
        preview(others + doodle.copy(paths = paths))
    }

    private fun apply(next: List<EditorLayer>) {
        layers = next
        onPersist(next)
        version++
    }
}
