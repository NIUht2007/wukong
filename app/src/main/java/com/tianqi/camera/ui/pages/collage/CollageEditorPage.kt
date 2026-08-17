package com.tianqi.camera.ui.pages.collage

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.tianqi.camera.model.CanvasRatio
import com.tianqi.camera.model.CollageBackground
import com.tianqi.camera.model.CollageEditState
import com.tianqi.camera.model.CollageTemplate
import com.tianqi.camera.model.SlotEditState
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.service.FilterEngine
import com.tianqi.camera.model.SweetFilters
import com.tianqi.camera.ui.components.FilterPanel
import com.tianqi.camera.ui.components.PlaceholderPage
import com.tianqi.camera.ui.theme.AppShapes
import com.tianqi.camera.ui.theme.BerryRed
import com.tianqi.camera.ui.theme.CardWhite
import com.tianqi.camera.ui.theme.MilkApricot
import com.tianqi.camera.ui.theme.MilkWhite
import com.tianqi.camera.ui.theme.Mint
import com.tianqi.camera.ui.theme.Peach
import com.tianqi.camera.ui.theme.SweetPink

private enum class EditorTab(val label: String) {
    LAYOUT("布局"), FRAME("边框"), BACKGROUND("背景"), FILTER("滤镜")
}

/**
 * 拼图编辑页（PRD 3.3）：画布比例切换、槽位取景手势、边框/圆角/间距、
 * 纯色/渐变/模糊背景、单图独立滤镜。导出见 CollageRenderer。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorPage(onBack: () -> Unit, onExport: () -> Unit) {
    val template = EditSession.collageTemplate
    val initial = EditSession.collageState
    if (template == null || initial == null) {
        PlaceholderPage(title = "拼图编辑", hint = "先回去选个模板吧～", onBack = onBack)
        return
    }

    var state by remember { mutableStateOf(initial) }
    var selectedSlot by remember { mutableIntStateOf(-1) }
    var tab by remember { mutableStateOf(EditorTab.LAYOUT) }
    // 滤镜写在 EditSession（按 uri 共享），用 version 触发画布重组
    var filterVersion by remember { mutableIntStateOf(0) }

    fun update(newState: CollageEditState) {
        state = newState
        EditSession.collageState = newState
    }

    // 换图 / 填空槽：系统单图选择器
    val pickSingle = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && selectedSlot >= 0) {
            update(state.updateSlot(selectedSlot, SlotEditState(uri = uri)))
        }
    }
    fun launchPicker() {
        pickSingle.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拼图编辑", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = onExport,
                        shape = AppShapes.Pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text("导出", color = Color.White) }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 画布区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                CollageCanvas(
                    template = template,
                    state = state,
                    selectedSlot = selectedSlot,
                    filterVersion = filterVersion,
                    onSlotTap = { index ->
                        selectedSlot = index
                        if (state.slots[index].uri == null) launchPicker()
                    },
                    onSlotTransform = { index, panX, panY, zoom ->
                        val slot = state.slots[index]
                        update(
                            state.updateSlot(
                                index,
                                slot.copy(
                                    scale = (slot.scale * zoom).coerceIn(1f, 5f),
                                    offsetX = slot.offsetX + panX,
                                    offsetY = slot.offsetY + panY
                                )
                            )
                        )
                    }
                )
            }

            // 选中槽位的快捷工具：换图 / 旋转 / 镜像
            if (selectedSlot >= 0 && state.slots.getOrNull(selectedSlot)?.uri != null) {
                SlotToolbar(
                    onReplace = { launchPicker() },
                    onRotate = {
                        val slot = state.slots[selectedSlot]
                        update(
                            state.updateSlot(
                                selectedSlot,
                                slot.copy(rotationQuarter = (slot.rotationQuarter + 1) % 4)
                            )
                        )
                    },
                    onMirror = {
                        val slot = state.slots[selectedSlot]
                        update(
                            state.updateSlot(selectedSlot, slot.copy(mirrored = !slot.mirrored))
                        )
                    }
                )
            }

            // 底部面板 + Tab
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                when (tab) {
                    EditorTab.LAYOUT -> LayoutPanel(state = state, onChange = ::update)
                    EditorTab.FRAME -> FramePanel(state = state, onChange = ::update)
                    EditorTab.BACKGROUND -> BackgroundPanel(state = state, onChange = ::update)
                    EditorTab.FILTER -> {
                        val uri = state.slots.getOrNull(selectedSlot)?.uri
                        if (uri == null) {
                            PanelHint("先点选一张图片哦～")
                        } else {
                            @Suppress("UNUSED_EXPRESSION") filterVersion
                            FilterPanel(
                                photo = uri,
                                state = EditSession.filterStateOf(uri),
                                onChange = {
                                    EditSession.updateFilterState(uri, it)
                                    filterVersion++
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EditorTab.entries.forEach { t ->
                        TabButton(
                            label = t.label,
                            selected = t == tab,
                            onClick = { tab = t }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** 拼图画布：背景 + 槽位布局，几何换算与 CollageRenderer 一致 */
@Composable
private fun CollageCanvas(
    template: CollageTemplate,
    state: CollageEditState,
    selectedSlot: Int,
    filterVersion: Int,
    onSlotTap: (Int) -> Unit,
    onSlotTransform: (index: Int, panX: Float, panY: Float, zoom: Float) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    BoxWithConstraints {
        val ratio = state.canvasRatio.aspect
        // 在可用区域内放最大的画布
        val canvasWidth: Dp
        val canvasHeight: Dp
        if (maxWidth / maxHeight > ratio) {
            canvasHeight = maxHeight
            canvasWidth = maxHeight * ratio
        } else {
            canvasWidth = maxWidth
            canvasHeight = maxWidth / ratio
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(canvasWidth, canvasHeight)
                .clip(RoundedCornerShape(state.cornerRadius.dp))
        ) {
            CanvasBackground(state = state)

            template.slots.forEachIndexed { index, spec ->
                @Suppress("UNUSED_EXPRESSION") filterVersion
                val slot = state.slots.getOrNull(index) ?: return@forEachIndexed
                val frameX = canvasWidth * spec.x + state.spacing.dp / 2
                val frameY = canvasHeight * spec.y + state.spacing.dp / 2
                val frameW = canvasWidth * spec.w - state.spacing.dp
                val frameH = canvasHeight * spec.h - state.spacing.dp

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = frameX, y = frameY)
                        .size(frameW, frameH)
                ) {
                    SlotView(
                        slot = slot,
                        borderWidth = state.borderWidth.dp,
                        cornerRadius = state.cornerRadius.dp,
                        selected = index == selectedSlot,
                        onTap = { onSlotTap(index) },
                        onTransform = { panX, panY, zoom ->
                            // 像素位移转成相对槽位尺寸的比例，与导出渲染保持一致
                            val frameWPx = with(density) { frameW.toPx() }
                            val frameHPx = with(density) { frameH.toPx() }
                            onSlotTransform(index, panX / frameWPx, panY / frameHPx, zoom)
                        }
                    )
                }
            }
        }
    }
}

/** 单个槽位：白色边框 + 圆角裁剪 + 图片取景变换 + 滤镜 */
@Composable
private fun SlotView(
    slot: SlotEditState,
    borderWidth: Dp,
    cornerRadius: Dp,
    selected: Boolean,
    onTap: () -> Unit,
    onTransform: (panX: Float, panY: Float, zoom: Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
            .padding(borderWidth)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MilkApricot)
            .pointerInput(Unit) { detectTapGestures { onTap() } }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(pan.x, pan.y, zoom)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val uri = slot.uri
        if (uri == null) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "添加图片",
                tint = SweetPink,
                modifier = Modifier.size(32.dp)
            )
        } else {
            // 用图片固有尺寸手动做 cover 缩放，保证预览与导出渲染几何一致
            SubcomposeAsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            ) content@{
                val imageScope = this
                val intrinsicSize = painter.intrinsicSize
                if (intrinsicSize == Size.Unspecified) {
                    SubcomposeAsyncImageContent()
                } else {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val boxW = constraints.maxWidth.toFloat()
                        val boxH = constraints.maxHeight.toFloat()
                        val rotated = slot.rotationQuarter % 2 != 0
                        val bmpW = if (rotated) intrinsicSize.height else intrinsicSize.width
                        val bmpH = if (rotated) intrinsicSize.width else intrinsicSize.height
                        val cover = maxOf(boxW / bmpW, boxH / bmpH)
                        val totalScale = cover * slot.scale
                        val filterState = EditSession.filterStateOf(uri)
                        Box(
                            modifier = Modifier
                                .size(
                                    width = with(androidx.compose.ui.platform.LocalDensity.current) { intrinsicSize.width.toDp() },
                                    height = with(androidx.compose.ui.platform.LocalDensity.current) { intrinsicSize.height.toDp() }
                                )
                                .align(Alignment.Center)
                                .graphicsLayer {
                                    scaleX = if (slot.mirrored) -totalScale else totalScale
                                    scaleY = totalScale
                                    translationX = slot.offsetX * boxW
                                    translationY = slot.offsetY * boxH
                                    rotationZ = slot.rotationQuarter * 90f
                                }
                        ) {
                            imageScope.SubcomposeAsyncImageContent(
                                contentScale = ContentScale.None,
                                colorFilter = FilterEngine.composeColorFilter(
                                    SweetFilters.byId(filterState.filterId),
                                    filterState.intensity
                                )
                            )
                        }
                    }
                }
            }
        }
        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, SweetPink, RoundedCornerShape(cornerRadius))
            )
        }
    }
}

/** 画布背景：纯色 / 渐变 / 模糊（模糊在 API 31+ 用实时模糊，以下用半透明遮罩近似） */
@Composable
private fun CanvasBackground(state: CollageEditState) {
    when (val bg = state.background) {
        is CollageBackground.Solid ->
            Box(Modifier.fillMaxSize().background(Color(bg.argb)))
        is CollageBackground.Gradient ->
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(bg.fromArgb), Color(bg.toArgb)))
                )
            )
        CollageBackground.Blur -> {
            val firstUri = state.slots.firstOrNull { it.uri != null }?.uri
            Box(Modifier.fillMaxSize().background(MilkWhite)) {
                if (firstUri != null) {
                    coil.compose.AsyncImage(
                        model = firstUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                    Modifier.blur(24.dp)
                                else Modifier
                            )
                    )
                    Box(Modifier.fillMaxSize().background(MilkWhite.copy(alpha = 0.4f)))
                }
            }
        }
    }
}

@Composable
private fun SlotToolbar(onReplace: () -> Unit, onRotate: () -> Unit, onMirror: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TextButtonWithIcon(Icons.Outlined.PhotoLibrary, "换图", onReplace)
        TextButtonWithIcon(Icons.AutoMirrored.Outlined.RotateRight, "旋转", onRotate)
        TextButtonWithIcon(Icons.Outlined.Flip, "镜像", onMirror)
    }
}

@Composable
private fun TextButtonWithIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .background(MilkApricot)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(imageVector, contentDescription = null, tint = SweetPink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = SweetPink)
    }
}

/** 布局面板：画布比例 + 槽位间距 */
@Composable
private fun LayoutPanel(state: CollageEditState, onChange: (CollageEditState) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CanvasRatio.entries.forEach { r ->
                PanelChip(
                    label = r.label,
                    selected = r == state.canvasRatio,
                    onClick = { onChange(state.copy(canvasRatio = r)) }
                )
            }
        }
        PanelSlider(
            label = "间距",
            value = state.spacing,
            onValueChange = { onChange(state.copy(spacing = it)) }
        )
    }
}

/** 边框面板：粗细 + 圆角 */
@Composable
private fun FramePanel(state: CollageEditState, onChange: (CollageEditState) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        PanelSlider(
            label = "边框粗细",
            value = state.borderWidth,
            onValueChange = { onChange(state.copy(borderWidth = it)) }
        )
        PanelSlider(
            label = "圆角",
            value = state.cornerRadius,
            onValueChange = { onChange(state.copy(cornerRadius = it)) }
        )
    }
}

/** 背景面板：甜系纯色 / 同族渐变 / 模糊 */
@Composable
private fun BackgroundPanel(state: CollageEditState, onChange: (CollageEditState) -> Unit) {
    val solids = listOf(MilkWhite, CardWhite, MilkApricot, Peach, SweetPink, BerryRed, Mint)
    val gradients = listOf(
        SweetPink to Peach,
        Peach to MilkApricot,
        SweetPink to MilkApricot
    )
    Column(Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(solids) { color ->
                val argb = color.value.toLong() and 0xFFFFFFFF
                BackgroundSwatch(
                    brush = Brush.linearGradient(listOf(color, color)),
                    selected = (state.background as? CollageBackground.Solid)?.argb == argb,
                    onClick = { onChange(state.copy(background = CollageBackground.Solid(argb))) }
                )
            }
            items(gradients) { (from, to) ->
                val fromArgb = from.value.toLong() and 0xFFFFFFFF
                val toArgb = to.value.toLong() and 0xFFFFFFFF
                val selected = (state.background as? CollageBackground.Gradient)?.let {
                    it.fromArgb == fromArgb && it.toArgb == toArgb
                } == true
                BackgroundSwatch(
                    brush = Brush.linearGradient(listOf(from, to)),
                    selected = selected,
                    onClick = {
                        onChange(state.copy(background = CollageBackground.Gradient(fromArgb, toArgb)))
                    }
                )
            }
            item {
                PanelChip(
                    label = "模糊",
                    selected = state.background is CollageBackground.Blur,
                    onClick = { onChange(state.copy(background = CollageBackground.Blur)) }
                )
            }
        }
    }
}

@Composable
private fun BackgroundSwatch(brush: Brush, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(brush)
            .then(
                if (selected)
                    Modifier.border(2.dp, SweetPink, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun PanelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else SweetPink,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .background(if (selected) SweetPink else MilkApricot)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun PanelSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..40f,
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

@Composable
private fun PanelHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
