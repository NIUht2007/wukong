package com.tianqi.camera.ui.pages.photo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tianqi.camera.model.FilterSpec
import com.tianqi.camera.model.SweetFilters
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.service.FilterEngine
import com.tianqi.camera.service.FilterState
import com.tianqi.camera.ui.components.PlaceholderPage
import com.tianqi.camera.ui.theme.AppShapes

/**
 * 单图编辑页。底部 Tab 规划：滤镜 | 美颜 | 贴纸 | 文字 | 涂鸦（PRD 4）。
 * 本阶段实现滤镜（PRD 3.2）：实时预览 + 强度 0-100 滑杆，每张图独立保存滤镜状态。
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
    fun update(state: FilterState) {
        filterState = state
        EditSession.updateFilterState(photo, state)
    }
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

            // 滤镜面板：强度滑杆 + 滤镜缩略图
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                if (filter.id != SweetFilters.NONE.id) {
                    Slider(
                        value = filterState.intensity,
                        onValueChange = { update(filterState.copy(intensity = it)) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(SweetFilters.ALL) { item ->
                        FilterThumbnail(
                            photo = photo,
                            filter = item,
                            selected = item.id == filterState.filterId,
                            onClick = { update(filterState.copy(filterId = item.id)) }
                        )
                    }
                }
            }
        }
    }
}

/** 单个滤镜缩略图：实时渲染该滤镜满强度效果，选中态主色描边 */
@Composable
private fun FilterThumbnail(
    photo: android.net.Uri,
    filter: FilterSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = photo,
            contentDescription = filter.name,
            contentScale = ContentScale.Crop,
            colorFilter = FilterEngine.composeColorFilter(filter, 100f),
            modifier = Modifier
                .size(64.dp)
                .clip(AppShapes.Slot)
                .then(
                    if (selected)
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, AppShapes.Slot)
                    else Modifier
                )
                .clickable(onClick = onClick)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = filter.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
