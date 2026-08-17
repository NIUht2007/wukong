package com.tianqi.camera.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianqi.camera.model.DoodleLayer
import com.tianqi.camera.model.EditorLayer
import com.tianqi.camera.model.StickerLayer
import com.tianqi.camera.model.TextLayer
import com.tianqi.camera.service.LayerRenderer
import com.tianqi.camera.service.StickerFactory
import com.tianqi.camera.ui.theme.AppShapes
import com.tianqi.camera.ui.theme.BerryRed
import com.tianqi.camera.ui.theme.CocoaBrown
import com.tianqi.camera.ui.theme.MilkApricot
import com.tianqi.camera.ui.theme.Mint
import com.tianqi.camera.ui.theme.Peach
import com.tianqi.camera.ui.theme.SweetPink
import kotlin.math.roundToInt

/** 图层涂鸦设置 */
data class DoodleSettings(
    val colorArgb: Long = 0xFFF2708A,
    val widthFraction: Float = 0.012f,
    val eraser: Boolean = false
)

/** 甜系色板（涂鸦/文字共用） */
val LayerPalette: List<Color> = listOf(
    SweetPink, Peach, BerryRed, MilkApricot, Mint, CocoaBrown, Color.White
)

/** 把内容中心放到画布坐标 (x, y)（px） */
private fun Modifier.centerAt(x: Float, y: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(minWidth = 0, minHeight = 0)
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(
            (x - placeable.width / 2f).roundToInt(),
            (y - placeable.height / 2f).roundToInt()
        )
    }
}

/**
 * 图层覆盖层：渲染贴纸/文字/涂鸦并处理手势。
 * 手势进行中通过 onLayerPreview 持续刷新（不入撤销栈），
 * 手势结束通过 onGestureCommit 通知外部入栈。
 */
@Composable
fun LayerOverlay(
    layers: List<EditorLayer>,
    selectedId: String?,
    doodleMode: Boolean,
    onSelect: (String?) -> Unit,
    onLayerPreview: (EditorLayer) -> Unit,
    onGestureCommit: () -> Unit,
    onDelete: (String) -> Unit,
    onDoodlePoint: (point: Offset, isStart: Boolean) -> Unit,
    onDoodleEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val canvasW = constraints.maxWidth.toFloat()
        val canvasH = constraints.maxHeight.toFloat()
        val base = minOf(canvasW, canvasH)

        layers.forEach { layer ->
            when (layer) {
                is DoodleLayer -> {
                    val bitmap = remember(layer.paths, canvasW, canvasH) {
                        LayerRenderer.renderDoodle(
                            layer.paths,
                            canvasW.roundToInt().coerceAtLeast(1),
                            canvasH.roundToInt().coerceAtLeast(1)
                        )
                    }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is StickerLayer -> {
                    val bitmap = remember(layer.stickerId) {
                        StickerFactory.createBitmap(layer.stickerId)
                    }
                    val sizePx = base * LayerRenderer.STICKER_BASE_FRACTION * layer.scale
                    DraggableLayer(
                        layer = layer,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        selected = layer.id == selectedId,
                        enabled = !doodleMode,
                        onSelect = onSelect,
                        onPreview = onLayerPreview,
                        onCommit = onGestureCommit,
                        onDelete = onDelete,
                        sizePx = sizePx
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = StickerFactory.nameOf(layer.stickerId),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                is TextLayer -> {
                    DraggableLayer(
                        layer = layer,
                        canvasW = canvasW,
                        canvasH = canvasH,
                        selected = layer.id == selectedId,
                        enabled = !doodleMode,
                        onSelect = onSelect,
                        onPreview = onLayerPreview,
                        onCommit = onGestureCommit,
                        onDelete = onDelete,
                        sizePx = null
                    ) {
                        TextLayerContent(layer = layer, base = base)
                    }
                }
            }
        }

        // 涂鸦模式：捕获绘制手势（置于最上层）
        if (doodleMode) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { start ->
                                onDoodlePoint(
                                    Offset(start.x / canvasW, start.y / canvasH),
                                    true
                                )
                            },
                            onDragEnd = { onDoodleEnd() },
                            onDragCancel = { onDoodleEnd() }
                        ) { change, _ ->
                            onDoodlePoint(
                                Offset(change.position.x / canvasW, change.position.y / canvasH),
                                false
                            )
                        }
                    }
            )
        } else {
            // 点击空白处取消选中
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { detectTapGestures { onSelect(null) } }
            )
        }
    }
}

/** 单个可拖拽图层的外壳：定位 + 旋转 + 手势 + 选中框 + 删除钮 */
@Composable
private fun DraggableLayer(
    layer: EditorLayer,
    canvasW: Float,
    canvasH: Float,
    selected: Boolean,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
    onPreview: (EditorLayer) -> Unit,
    onCommit: () -> Unit,
    onDelete: (String) -> Unit,
    sizePx: Float?,
    content: @Composable () -> Unit
) {
    val cx = layer.centerX * canvasW
    val cy = layer.centerY * canvasH

    Box(
        modifier = Modifier
            .centerAt(cx, cy)
            .then(if (sizePx != null) Modifier.size(with(androidx.compose.ui.platform.LocalDensity.current) { sizePx.toDp() }) else Modifier)
            .graphicsLayer { rotationZ = layer.rotation }
            .then(
                if (enabled)
                    Modifier
                        .pointerInput(layer.id) {
                            detectTapGestures { onSelect(layer.id) }
                        }
                        .pointerInput(layer.id) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val updated = layer.movedBy(
                                    dx = pan.x / canvasW,
                                    dy = pan.y / canvasH,
                                    zoom = zoom,
                                    rotationDelta = rotation
                                )
                                onPreview(updated)
                            }
                        }
                        .pointerInput(layer.id) {
                            // 所有手指抬起 = 一次手势结束，通知外部入撤销栈
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                                onCommit()
                            }
                        }
                else Modifier
            )
    ) {
        content()
        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .border(1.5.dp, SweetPink, AppShapes.Slot)
            )
        }
    }

    if (selected && enabled) {
        // 文字层没有固定尺寸，删除钮放在中心右上方一段距离
        val chipX = if (sizePx != null) cx + sizePx / 2 else cx + 120f
        val chipY = if (sizePx != null) cy - sizePx / 2 else cy - 120f
        Box(
            modifier = Modifier
                .centerAt(chipX, chipY)
                .size(24.dp)
                .clip(CircleShape)
                .background(BerryRed)
                .clickable { onDelete(layer.id) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "删除图层",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** 更新图层变换（位移/缩放/旋转），返回新实例 */
private fun EditorLayer.movedBy(
    dx: Float,
    dy: Float,
    zoom: Float,
    rotationDelta: Float
): EditorLayer = when (this) {
    is StickerLayer -> copy(
        centerX = (centerX + dx).coerceIn(0f, 1f),
        centerY = (centerY + dy).coerceIn(0f, 1f),
        scale = (scale * zoom).coerceIn(0.2f, 5f),
        rotation = rotation + rotationDelta
    )
    is TextLayer -> copy(
        centerX = (centerX + dx).coerceIn(0f, 1f),
        centerY = (centerY + dy).coerceIn(0f, 1f),
        scale = (scale * zoom).coerceIn(0.2f, 5f),
        rotation = rotation + rotationDelta
    )
    is DoodleLayer -> this
}

/** 文字图层内容：描边/阴影/透明度/字体 */
@Composable
private fun TextLayerContent(layer: TextLayer, base: Float) {
    val fontSizeSp = (base * LayerRenderer.TEXT_BASE_FRACTION * layer.scale /
            androidx.compose.ui.platform.LocalDensity.current.density)
    val fontFamily = when (layer.fontIndex) {
        1 -> FontFamily.Serif
        2 -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    val style = TextStyle(
        fontSize = fontSizeSp.sp,
        fontWeight = if (layer.fontIndex == 0) FontWeight.Bold else FontWeight.Normal,
        fontFamily = fontFamily,
        textAlign = TextAlign.Center,
        shadow = if (layer.shadow)
            Shadow(Color.Black.copy(alpha = 0.25f), Offset(2f, 2f), blurRadius = 6f)
        else null
    )
    Box {
        if (layer.stroke) {
            Text(
                text = layer.text,
                style = style.copy(
                    color = Color.White,
                    drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = fontSizeSp * 0.12f
                    )
                )
            )
        }
        Text(
            text = layer.text,
            style = style.copy(
                color = Color(layer.colorArgb).copy(alpha = layer.alpha)
            )
        )
    }
}

/** 贴纸选择面板 */
@Composable
fun StickerPanel(onPick: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(StickerFactory.stickerIds) { id ->
            val bitmap = remember(id) { StickerFactory.createBitmap(id) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = StickerFactory.nameOf(id),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(AppShapes.Slot)
                        .background(MilkApricot)
                        .clickable { onPick(id) }
                        .padding(6.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    StickerFactory.nameOf(id),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 涂鸦面板：颜色 + 笔宽 + 橡皮擦 */
@Composable
fun DoodlePanel(settings: DoodleSettings, onChange: (DoodleSettings) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LayerPalette.forEach { color ->
                val argb = color.value.toLong() and 0xFFFFFFFF
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (!settings.eraser && settings.colorArgb == argb)
                                Modifier.border(2.dp, SweetPink, CircleShape)
                            else Modifier
                        )
                        .clickable { onChange(settings.copy(colorArgb = argb, eraser = false)) }
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "橡皮擦",
                style = MaterialTheme.typography.labelLarge,
                color = if (settings.eraser) Color.White else SweetPink,
                modifier = Modifier
                    .clip(AppShapes.Pill)
                    .background(if (settings.eraser) SweetPink else MilkApricot)
                    .clickable { onChange(settings.copy(eraser = true)) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "笔宽",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = settings.widthFraction,
                onValueChange = { onChange(settings.copy(widthFraction = it)) },
                valueRange = 0.004f..0.05f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
    }
}

/** 文字样式面板：颜色 + 字体 + 描边/阴影 + 透明度 */
@Composable
fun TextStylePanel(layer: TextLayer, onChange: (TextLayer) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LayerPalette.forEach { color ->
                val argb = color.value.toLong() and 0xFFFFFFFF
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (layer.colorArgb == argb)
                                Modifier.border(2.dp, SweetPink, CircleShape)
                            else Modifier
                        )
                        .clickable { onChange(layer.copy(colorArgb = argb)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("圆润", "衬线", "等宽").forEachIndexed { index, name ->
                MiniChip(
                    label = name,
                    selected = layer.fontIndex == index,
                    onClick = { onChange(layer.copy(fontIndex = index)) }
                )
            }
            MiniChip(
                label = "描边",
                selected = layer.stroke,
                onClick = { onChange(layer.copy(stroke = !layer.stroke)) }
            )
            MiniChip(
                label = "阴影",
                selected = layer.shadow,
                onClick = { onChange(layer.copy(shadow = !layer.shadow)) }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "透明度",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = layer.alpha,
                onValueChange = { onChange(layer.copy(alpha = it)) },
                valueRange = 0.1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun MiniChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else SweetPink,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .background(if (selected) SweetPink else MilkApricot)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
