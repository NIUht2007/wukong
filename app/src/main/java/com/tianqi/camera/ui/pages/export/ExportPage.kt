package com.tianqi.camera.ui.pages.export

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tianqi.camera.service.CollageRenderer
import com.tianqi.camera.service.DraftStore
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.service.GallerySaver
import com.tianqi.camera.service.PhotoRenderer
import com.tianqi.camera.service.WorkStore
import com.tianqi.camera.ui.components.MiniChip
import com.tianqi.camera.ui.components.PlaceholderPage
import com.tianqi.camera.ui.theme.AppShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 导出页（PRD 阶段6）：三种模式——
 * 1. 拼图导出（CollageRenderer 2560px）
 * 2. 单图导出（PhotoRenderer：美颜→滤镜→图层）
 * 3. 查看历史作品（从首页作品列表进入，可分享/删除）
 * 保存到相册 + 系统分享，JPEG 质量 90，可选 PNG。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workPath = EditSession.viewingWorkPath
    val template = EditSession.collageTemplate
    val collageState = EditSession.collageState
    val photo = EditSession.capturedPhoto

    var format by remember { mutableStateOf(GallerySaver.Format.JPEG) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    // 渲染导出图（三种模式之一）
    val rendered by produceState<Bitmap?>(
        initialValue = null,
        workPath, template?.id, collageState, photo
    ) {
        value = withContext(Dispatchers.Default) {
            when {
                workPath != null -> BitmapFactory.decodeFile(workPath)
                template != null && collageState != null -> CollageRenderer.render(
                    context, template, collageState,
                    layers = EditSession.collageLayers,
                    longEdge = 2560
                )
                photo != null -> PhotoRenderer.render(
                    context, photo,
                    filterState = EditSession.filterStateOf(photo),
                    beautyState = EditSession.beautyState,
                    faces = EditSession.beautyFaces ?: emptyList(),
                    layers = EditSession.photoLayers,
                    longEdge = 2560
                )
                else -> null
            }
        }
    }

    // API 28 及以下保存相册前先申请存储权限
    var pendingSave by remember { mutableStateOf(false) }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) pendingSave = true }

    fun doSave(bitmap: Bitmap) {
        if (working) return
        if (GallerySaver.needsLegacyPermission() &&
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        working = true
        scope.launch {
            val uri = GallerySaver.save(context, bitmap, format)
            if (uri != null) {
                if (workPath == null) WorkStore.add(context, bitmap)
                DraftStore.clear(context)
                statusText = "存好啦，去相册看看吧～"
            } else {
                statusText = "保存失败了，再试一次吧"
            }
            working = false
        }
    }

    // 权限补发成功后自动继续保存
    LaunchedEffect(pendingSave) {
        if (pendingSave && rendered != null) {
            pendingSave = false
            doSave(rendered!!)
        }
    }

    fun doShare(bitmap: Bitmap) {
        scope.launch {
            val shareFile = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "share").apply { mkdirs() }
                val file = File(dir, "share_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                file
            }
            val uri = FileProvider.getUriForFile(
                context, "com.tianqi.camera.fileprovider", shareFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享甜甜的图～"))
        }
    }

    fun doDeleteWork(path: String) {
        WorkStore.delete(File(path))
        EditSession.viewingWorkPath = null
        onBack()
    }

    val hasContent = workPath != null || (template != null && collageState != null) || photo != null
    if (!hasContent) {
        PlaceholderPage(title = "导出", hint = "还没有作品哦，先去创作一个吧～", onBack = onBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (workPath != null) "我的作品" else "导出",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
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
                .padding(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = rendered
                if (bitmap == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "拼好啦，正在生成高清大图～",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "导出预览（2560px）",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AppShapes.Card)
                    )
                }
            }

            statusText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                if (workPath == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        MiniChip("JPEG", format == GallerySaver.Format.JPEG) {
                            format = GallerySaver.Format.JPEG
                        }
                        MiniChip("PNG", format == GallerySaver.Format.PNG) {
                            format = GallerySaver.Format.PNG
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (workPath != null) {
                        OutlinedButton(
                            onClick = { doDeleteWork(workPath) },
                            shape = AppShapes.Pill
                        ) { Text("删除") }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { rendered?.let(::doShare) },
                            enabled = rendered != null,
                            shape = AppShapes.Pill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("分享", color = Color.White) }
                    } else {
                        OutlinedButton(
                            onClick = { rendered?.let(::doShare) },
                            enabled = rendered != null,
                            shape = AppShapes.Pill
                        ) { Text("分享") }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { rendered?.let(::doSave) },
                            enabled = rendered != null && !working,
                            shape = AppShapes.Pill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("保存到相册", color = Color.White) }
                    }
                }
            }
        }
    }
}
