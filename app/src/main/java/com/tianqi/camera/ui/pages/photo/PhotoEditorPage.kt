package com.tianqi.camera.ui.pages.photo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tianqi.camera.model.SweetFilters
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.service.FilterEngine
import com.tianqi.camera.ui.components.FilterPanel
import com.tianqi.camera.ui.components.PlaceholderPage

/**
 * 单图编辑页。底部 Tab 规划：滤镜 | 美颜 | 贴纸 | 文字 | 涂鸦（PRD 4）。
 * 当前实现滤镜（PRD 3.2）：实时预览 + 强度 0-100 滑杆，每张图独立保存滤镜状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorPage(onBack: () -> Unit) {
    val photo = EditSession.capturedPhoto
    if (photo == null) {
        PlaceholderPage(title = "照片编辑", hint = "还没有照片哦，先去拍一张吧～", onBack = onBack)
        return
    }

    var filterState by remember { mutableStateOf(EditSession.filterStateOf(photo)) }
    val filter = SweetFilters.byId(filterState.filterId)

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 画布区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photo,
                    contentDescription = "编辑中的照片",
                    contentScale = ContentScale.Fit,
                    colorFilter = FilterEngine.composeColorFilter(filter, filterState.intensity),
                    modifier = Modifier.fillMaxSize()
                )
            }

            FilterPanel(
                photo = photo,
                state = filterState,
                onChange = {
                    filterState = it
                    EditSession.updateFilterState(photo, it)
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        }
    }
}
