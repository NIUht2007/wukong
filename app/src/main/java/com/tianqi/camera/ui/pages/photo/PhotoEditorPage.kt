package com.tianqi.camera.ui.pages.photo

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.tianqi.camera.model.BeautyState
import com.tianqi.camera.model.FaceData
import com.tianqi.camera.model.StickerLayer
import com.tianqi.camera.model.SweetFilters
import com.tianqi.camera.model.TextLayer
import com.tianqi.camera.service.BeautyEngine
import com.tianqi.camera.service.BitmapLoader
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.service.FaceDetector
import com.tianqi.camera.service.FilterEngine
import com.tianqi.camera.ui.components.DoodlePanel
import com.tianqi.camera.ui.components.FilterPanel
import com.tianqi.camera.ui.components.LayerEditController
import com.tianqi.camera.ui.components.LayerOverlay
import com.tianqi.camera.ui.components.MiniChip
import com.tianqi.camera.ui.components.PlaceholderPage
import com.tianqi.camera.ui.components.StickerPanel
import com.tianqi.camera.ui.components.TextInputDialog
import com.tianqi.camera.ui.components.TextStylePanel
import com.tianqi.camera.ui.theme.AppShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class PhotoTab(val label: String) {
    FILTER("滤镜"), BEAUTY("美颜"), STICKER("贴纸"), TEXT("文字"), DOODLE("涂鸦")
}

/**
 * 单图编辑页（PRD 3.2/3.4/3.5）：滤镜 | 美颜 | 贴纸 | 文字 | 涂鸦。
 * 美颜：磨皮/美白/红润全图级，瘦脸/大眼基于 ML Kit 人脸关键点液化；
 * 预览用 512px 小图调参，导出对原图全分辨率重算（BeautyEngine 同一管线）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorPage(onBack: () -> Unit, onExport: () -> Unit) {
    val photo = EditSession.capturedPhoto
    if (photo == null) {
        PlaceholderPage(title = "照片编辑", hint = "还没有照片哦，先去拍一张吧～", onBack = onBack)
        return
    }
    val context = LocalContext.current

    var tab by remember { mutableStateOf(PhotoTab.FILTER) }
    var filterState by remember { mutableStateOf(EditSession.filterStateOf(photo)) }
    val filter = SweetFilters.byId(filterState.filterId)
    val controller = remember {
        LayerEditController(EditSession.photoLayers) { EditSession.photoLayers = it }
    }
    var showTextDialog by remember { mutableStateOf(false) }

    // 美颜：参数 + 人脸检测结果（null=未检测）
    var beautyState by remember { mutableStateOf(EditSession.beautyState) }
    var faces by remember { mutableStateOf(EditSession.beautyFaces) }
    var detecting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (EditSession.beautyFaces == null && !detecting) {
            detecting = true
            faces = FaceDetector.detect(context, photo)
            EditSession.beautyFaces = faces
            detecting = false
        }
    }

    // 美颜预览：512px 小图实时计算（参数未开时不处理）
    val beautyBitmap by produceState<Bitmap?>(null, beautyState, faces) {
        value = if (beautyState == BeautyState()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                BitmapLoader.decode(context, photo, maxDimension = 512)?.let {
                    BeautyEngine.apply(it, beautyState, faces ?: emptyList())
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("照片编辑", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    @Suppress("UNUSED_EXPRESSION") controller.version
                    IconButton(onClick = { controller.undo() }, enabled = controller.canUndo) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "撤销",
                            tint = if (controller.canUndo) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { controller.redo() }, enabled = controller.canRedo) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = "重做",
                            tint = if (controller.canRedo) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
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
            // 画布区：照片 + 图层覆盖（图层坐标相对照片显示区域）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = photo,
                    contentDescription = "编辑中的照片",
                    modifier = Modifier.fillMaxSize()
                ) content@{
                    val imageScope = this
                    val intrinsic = painter.intrinsicSize
                    if (intrinsic == Size.Unspecified) {
                        SubcomposeAsyncImageContent(
                            contentScale = ContentScale.Fit,
                            colorFilter = FilterEngine.composeColorFilter(filter, filterState.intensity)
                        )
                    } else {
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val photoAspect = intrinsic.width / intrinsic.height
                            val boxAspect = maxWidth / maxHeight
                            val w = if (boxAspect > photoAspect) maxHeight * photoAspect else maxWidth
                            val h = if (boxAspect > photoAspect) maxHeight else maxWidth / photoAspect
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(w, h)
                            ) {
                                val beauty = beautyBitmap
                                if (tab == PhotoTab.BEAUTY && beauty != null) {
                                    Image(
                                        bitmap = beauty.asImageBitmap(),
                                        contentDescription = "美颜预览",
                                        contentScale = ContentScale.Fit,
                                        colorFilter = FilterEngine.composeColorFilter(
                                            filter, filterState.intensity
                                        ),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    imageScope.SubcomposeAsyncImageContent(
                                        contentScale = ContentScale.Fit,
                                        colorFilter = FilterEngine.composeColorFilter(
                                            filter, filterState.intensity
                                        ),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                @Suppress("UNUSED_EXPRESSION") controller.version
                                LayerOverlay(
                                    layers = controller.layers,
                                    selectedId = controller.selectedId,
                                    doodleMode = tab == PhotoTab.DOODLE,
                                    onSelect = { controller.selectedId = it },
                                    onLayerPreview = { controller.preview(controller.replace(it)) },
                                    onGestureCommit = { controller.commit(controller.layers) },
                                    onDelete = { controller.deleteLayer(it) },
                                    onDoodlePoint = { point, isStart ->
                                        controller.addDoodlePoint(point, isStart)
                                    },
                                    onDoodleEnd = { controller.commit(controller.layers) }
                                )
                            }
                        }
                    }
                }
            }

            // 底部面板 + Tab
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                when (tab) {
                    PhotoTab.FILTER -> FilterPanel(
                        photo = photo,
                        state = filterState,
                        onChange = {
                            filterState = it
                            EditSession.updateFilterState(photo, it)
                        }
                    )
                    PhotoTab.BEAUTY -> BeautyPanel(
                        state = beautyState,
                        detecting = detecting,
                        hasFace = !faces.isNullOrEmpty(),
                        detectionDone = faces != null,
                        onChange = {
                            beautyState = it
                            EditSession.beautyState = it
                        }
                    )
                    PhotoTab.STICKER -> StickerPanel(
                        onPick = { stickerId ->
                            controller.addLayer(
                                StickerLayer(
                                    id = "sticker_${System.currentTimeMillis()}",
                                    stickerId = stickerId
                                )
                            )
                        }
                    )
                    PhotoTab.TEXT -> {
                        val selectedText = controller.layers
                            .filterIsInstance<TextLayer>()
                            .firstOrNull { it.id == controller.selectedId }
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MiniChip(label = "＋ 添加文字", selected = true) {
                                    showTextDialog = true
                                }
                            }
                            if (selectedText != null) {
                                TextStylePanel(
                                    layer = selectedText,
                                    onChange = { controller.commit(controller.replace(it)) }
                                )
                            }
                        }
                    }
                    PhotoTab.DOODLE -> DoodlePanel(
                        settings = controller.doodleSettings,
                        onChange = { controller.doodleSettings = it }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PhotoTab.entries.forEach { t ->
                        Text(
                            text = t.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (t == tab) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { tab = t }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showTextDialog) {
        TextInputDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text ->
                showTextDialog = false
                controller.addLayer(
                    TextLayer(
                        id = "text_${System.currentTimeMillis()}",
                        text = text,
                        colorArgb = 0xFF4A3B38
                    )
                )
            }
        )
    }
}

/** 美颜面板：磨皮/美白/红润全图级 + 瘦脸/大眼（需人脸） */
@Composable
private fun BeautyPanel(
    state: BeautyState,
    detecting: Boolean,
    hasFace: Boolean,
    detectionDone: Boolean,
    onChange: (BeautyState) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        BeautySlider("磨皮", state.smooth) { onChange(state.copy(smooth = it)) }
        BeautySlider("美白", state.whiten) { onChange(state.copy(whiten = it)) }
        BeautySlider("红润", state.rosy) { onChange(state.copy(rosy = it)) }
        BeautySlider("瘦脸", state.slimFace, enabled = hasFace) {
            onChange(state.copy(slimFace = it))
        }
        BeautySlider("大眼", state.bigEyes, enabled = hasFace) {
            onChange(state.copy(bigEyes = it))
        }
        if (detecting || !detectionDone) {
            Text(
                "正在寻找小脸蛋…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (!hasFace) {
            Text(
                "未检测到小脸蛋哦，瘦脸大眼先休息啦～",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BeautySlider(
    label: String,
    value: Float,
    enabled: Boolean = true,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..100f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledThumbColor = Color.White.copy(alpha = 0.5f),
                disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
    }
}
