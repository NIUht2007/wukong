package com.tianqi.camera.ui.pages.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tianqi.camera.service.DraftStore
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.service.WorkStore
import com.tianqi.camera.ui.theme.AppShapes
import com.tianqi.camera.ui.theme.GrayPinkBrown
import com.tianqi.camera.ui.theme.MilkApricot
import com.tianqi.camera.ui.theme.Peach
import com.tianqi.camera.ui.theme.SweetPink
import com.tianqi.camera.ui.theme.TianqiCameraTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 首页：标题区 + 拍照/拼图入口卡片 + 我的作品（见 04-素材与设计规范.md 第四节） */
@Composable
fun HomePage(
    onCameraClick: () -> Unit,
    onPhotosPicked: (List<android.net.Uri>) -> Unit,
    onWorkClick: (java.io.File) -> Unit,
    onRestoreDraft: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // 首次启动：隐私政策弹窗，同意后不再弹出
    val prefs = remember { context.getSharedPreferences("tianqi", android.content.Context.MODE_PRIVATE) }
    var showPrivacy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showPrivacy = !prefs.getBoolean("privacy_agreed", false)
    }

    // 系统相册多选（Photo Picker 无需权限，HEIC 由系统解码）
    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // 取持久化读权限，草稿恢复后仍能读图
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            onPhotosPicked(uris)
        }
    }
    val launchPicker = {
        pickPhotos.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // 我的作品列表（worksVersion 驱动刷新）
    val worksVersion = EditSession.worksVersion.intValue
    var works by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    LaunchedEffect(worksVersion) {
        works = withContext(Dispatchers.IO) { WorkStore.list(context) }
    }

    // 草稿恢复提示
    var showDraftDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showDraftDialog = DraftStore.hasDraft(context)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "甜气相机",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "随手拍，甜甜拼",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            EntryCard(
                title = "拍一张",
                icon = Icons.Outlined.PhotoCamera,
                background = Brush.linearGradient(
                    colors = listOf(SweetPink, Peach),
                    // 同族色系渐变，角度约 135°
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset.Infinite
                ),
                contentColor = Color.White,
                onClick = onCameraClick
            )
            Spacer(Modifier.height(16.dp))
            EntryCard(
                title = "拼个图",
                icon = Icons.Outlined.GridOn,
                background = Brush.linearGradient(listOf(MilkApricot, MilkApricot)),
                contentColor = SweetPink,
                borderColor = SweetPink,
                onClick = launchPicker
            )

            Spacer(Modifier.height(32.dp))
            Text(
                text = "我的作品",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            WorksRow(works = works, onWorkClick = onWorkClick)
        }
    }

    if (showPrivacy) {
        com.tianqi.camera.ui.components.PrivacyPolicyDialog(
            onAgree = {
                prefs.edit().putBoolean("privacy_agreed", true).apply()
                showPrivacy = false
            },
            onDecline = {
                // 不同意则退出 App
                (context as? android.app.Activity)?.finish()
            }
        )
    }

    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("继续上次编辑？") },
            text = { Text("上次的拼图还没弄完呢，接着来吧～") },
            confirmButton = {
                Button(
                    onClick = {
                        showDraftDialog = false
                        onRestoreDraft()
                    },
                    shape = AppShapes.Pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("继续编辑", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDraftDialog = false
                    DraftStore.clear(context)
                }) { Text("不了，重新开始") }
            },
            shape = AppShapes.LargeCard,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun EntryCard(
    title: String,
    icon: ImageVector,
    background: Brush,
    contentColor: Color,
    onClick: () -> Unit,
    borderColor: Color? = null
) {
    val shape = AppShapes.LargeCard
    var modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .clip(shape)
        .background(background)
    if (borderColor != null) {
        modifier = modifier.border(1.5.dp, borderColor, shape)
    }
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
        )
    }
}

/** 我的作品：本地历史导出缩略图，空态显示虚线框 */
@Composable
private fun WorksRow(works: List<java.io.File>, onWorkClick: (java.io.File) -> Unit) {
    if (works.isEmpty()) {
        val shape = AppShapes.Card
        val dashColor = GrayPinkBrown
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 120.dp)
                .clip(shape)
                .drawBehind {
                    drawRoundRect(
                        color = dashColor,
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "还没有作品，去拍第一张吧～",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(works) { file ->
            coil.compose.AsyncImage(
                model = file,
                contentDescription = "历史作品",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 120.dp, height = 120.dp)
                    .clip(AppShapes.Card)
                    .clickable { onWorkClick(file) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePagePreview() {
    TianqiCameraTheme {
        HomePage(onCameraClick = {}, onPhotosPicked = {}, onWorkClick = {}, onRestoreDraft = {}, onSettingsClick = {})
    }
}
