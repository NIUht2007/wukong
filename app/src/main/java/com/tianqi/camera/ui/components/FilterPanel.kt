package com.tianqi.camera.ui.components

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tianqi.camera.service.FilterEngine
import com.tianqi.camera.service.FilterState
import com.tianqi.camera.ui.theme.AppShapes

/**
 * 通用滤镜面板（单图编辑页 / 拼图编辑页共用）：
 * 强度滑杆（非原图时显示）+ 滤镜缩略图横排，实时预览。
 */
@Composable
fun FilterPanel(
    photo: Uri,
    state: FilterState,
    onChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (state.filterId != SweetFilters.NONE.id) {
            Slider(
                value = state.intensity,
                onValueChange = { onChange(state.copy(intensity = it)) },
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
                    selected = item.id == state.filterId,
                    onClick = { onChange(state.copy(filterId = item.id)) }
                )
            }
        }
    }
}

/** 单个滤镜缩略图：实时渲染该滤镜满强度效果，选中态主色描边 */
@Composable
private fun FilterThumbnail(
    photo: Uri,
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
