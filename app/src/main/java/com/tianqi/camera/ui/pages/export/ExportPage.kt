package com.tianqi.camera.ui.pages.export

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tianqi.camera.service.CollageRenderer
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.ui.components.PlaceholderPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 导出页。本阶段先渲染 2560px 拼图结果预览（验证导出与预览一致），
 * 保存到相册 / 系统分享在后续阶段实现。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val template = EditSession.collageTemplate
    val state = EditSession.collageState

    if (template == null || state == null) {
        PlaceholderPage(title = "导出", hint = "还没有拼图哦，先去拼一个吧～", onBack = onBack)
        return
    }

    val rendered by produceState<Bitmap?>(initialValue = null, template.id, state) {
        value = withContext(Dispatchers.Default) {
            CollageRenderer.render(context, template, state, longEdge = 2560)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出", style = MaterialTheme.typography.headlineSmall) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
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
                    contentDescription = "拼图导出预览（2560px）",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
