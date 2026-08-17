package com.tianqi.camera.ui.pages.camera

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tianqi.camera.service.CaptureRatio
import com.tianqi.camera.service.ImageCropper
import com.tianqi.camera.ui.theme.AppShapes
import java.io.File
import java.util.concurrent.Executors

/** 拍照页：前后摄切换、闪光灯、1:1 / 3:4 / 9:16 画幅遮罩，成片按遮罩居中裁切（PRD 3.1） */
@Composable
fun CameraPage(onBack: () -> Unit, onCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showGoSettings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        // 拒绝且不再询问 → 引导去系统设置
        showGoSettings = !granted
    }

    when {
        hasPermission -> CameraView(onBack = onBack, onCaptured = onCaptured)
        else -> PermissionGuide(
            onBack = onBack,
            onRequest = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }
        )
    }

    if (showGoSettings) {
        GoSettingsDialog(
            onDismiss = { showGoSettings = false },
            onGoSettings = {
                showGoSettings = false
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
        )
    }
}

@Composable
private fun CameraView(onBack: () -> Unit, onCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashOn by remember { mutableStateOf(false) }
    var ratio by remember { mutableStateOf(CaptureRatio.R3_4) }
    var capturing by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // PreviewView 的 surfaceProvider 需要在 bind 时拿到，先持有引用
    var previewSurfaceProvider by remember { mutableStateOf<Preview.SurfaceProvider?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewSurfaceProvider = previewView.surfaceProvider
                ProcessCameraProvider.getInstance(ctx).also { future ->
                    future.addListener({
                        cameraProvider.value = future.get()
                    }, ContextCompat.getMainExecutor(ctx))
                }
                previewView
            }
        )

        // 相机就绪 / 镜头切换后重新绑定
        LaunchedEffect(cameraProvider.value, lensFacing, flashOn, previewSurfaceProvider) {
            val provider = cameraProvider.value ?: return@LaunchedEffect
            val surfaceProvider = previewSurfaceProvider ?: return@LaunchedEffect
            provider.unbindAll()
            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            runCatching {
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            }
            imageCapture.flashMode =
                if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        }

        // 画幅遮罩（取景框外压暗）
        RatioMask(ratio = ratio)

        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { flashOn = !flashOn }) {
                Icon(
                    if (flashOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    contentDescription = "闪光灯",
                    tint = if (flashOn) MaterialTheme.colorScheme.secondary else Color.White
                )
            }
        }

        // 底部控制区：画幅切换 + 快门 + 翻转镜头
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CaptureRatio.entries.forEach { r ->
                    RatioChip(
                        label = r.label,
                        selected = r == ratio,
                        onClick = { ratio = r }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(Modifier.size(48.dp))
                ShutterButton(enabled = !capturing) {
                    capturing = true
                    takePhoto(context, imageCapture, ratio, executor) { uri ->
                        capturing = false
                        if (uri != null) onCaptured(uri)
                    }
                }
                IconButton(
                    onClick = {
                        lensFacing =
                            if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT
                            else CameraSelector.LENS_FACING_BACK
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Outlined.Cameraswitch,
                        contentDescription = "切换镜头",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/** 取景遮罩：目标画幅区域镂空，其余压暗 + 白框 */
@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.RatioMask(ratio: CaptureRatio) {
    val frameAspect = ratio.width.toFloat() / ratio.height
    val viewAspect = maxWidth / maxHeight
    val frameWidth: Float
    val frameHeight: Float
    if (frameAspect > viewAspect) {
        frameWidth = maxWidth.value
        frameHeight = maxWidth.value / frameAspect
    } else {
        frameHeight = maxHeight.value
        frameWidth = maxHeight.value * frameAspect
    }
    val horizontalPad = ((maxWidth.value - frameWidth) / 2f).coerceAtLeast(0f)
    val verticalPad = ((maxHeight.value - frameHeight) / 2f).coerceAtLeast(0f)

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val scrim = Color.Black.copy(alpha = 0.5f)
        val w = size.width
        val h = size.height
        val left = horizontalPad.dp.toPx()
        val top = verticalPad.dp.toPx()
        val frameW = w - left * 2
        val frameH = h - top * 2
        // 上下左右四块遮罩
        drawRect(scrim, size = Size(w, top))
        drawRect(scrim, topLeft = Offset(0f, h - top), size = Size(w, top))
        drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, frameH))
        drawRect(scrim, topLeft = Offset(w - left, top), size = Size(left, frameH))
        // 取景白框
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(frameW, frameH),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun RatioChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    ratio: CaptureRatio,
    executor: java.util.concurrent.Executor,
    onResult: (Uri?) -> Unit
) {
    val rawFile = File(context.cacheDir, "capture_raw_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()
    val mainHandler = android.os.Handler(context.mainLooper)
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                executor.execute {
                    val cropped = ImageCropper.centerCropToRatio(rawFile, ratio, context.cacheDir)
                    rawFile.delete()
                    mainHandler.post { onResult(cropped?.let { Uri.fromFile(it) }) }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                rawFile.delete()
                mainHandler.post { onResult(null) }
            }
        }
    )
}

/** 权限说明页（首次进入未授权时） */
@Composable
private fun PermissionGuide(onBack: () -> Unit, onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "想拍张甜甜的照片～",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "需要相机权限才能拍照哦\n照片只保存在你的手机里，不会上传",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                shape = AppShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("授权相机", color = Color.White)
            }
        }
    }
}

/** 拒绝权限后的"去设置"引导弹窗 */
@Composable
private fun GoSettingsDialog(onDismiss: () -> Unit, onGoSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("相机权限被拒绝啦") },
        text = { Text("没有相机权限就没办法拍照了哦，去设置里打开一下吧～") },
        confirmButton = {
            Button(
                onClick = onGoSettings,
                shape = AppShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("去设置", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("下次吧") }
        },
        shape = AppShapes.LargeCard,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
