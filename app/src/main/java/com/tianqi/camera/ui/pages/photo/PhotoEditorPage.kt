package com.tianqi.camera.ui.pages.photo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.ui.components.PlaceholderPage

/** 单图编辑页：滤镜 | 美颜 | 贴纸 | 文字 | 涂鸦（PRD 3.2/3.5）。当前展示拍照成片，编辑功能后续阶段实现 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorPage(onBack: () -> Unit) {
    val photo = EditSession.capturedPhoto
    if (photo == null) {
        PlaceholderPage(title = "照片编辑", hint = "还没有照片哦，先去拍一张吧～", onBack = onBack)
        return
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
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photo,
                contentDescription = "拍摄成片",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
