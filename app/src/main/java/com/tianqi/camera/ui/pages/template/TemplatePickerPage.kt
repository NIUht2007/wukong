package com.tianqi.camera.ui.pages.template

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tianqi.camera.model.CollageTemplate
import com.tianqi.camera.model.TemplateRepository
import com.tianqi.camera.service.EditSession
import com.tianqi.camera.ui.theme.AppShapes
import com.tianqi.camera.ui.theme.MilkApricot
import com.tianqi.camera.ui.theme.Mint
import com.tianqi.camera.ui.theme.Peach
import com.tianqi.camera.ui.theme.SweetPink

/**
 * 模板选择页：按图片数量分组展示模板缩略图（PRD 3.3）。
 * 选图张数与模板槽位数一致时优先推荐；选了 N 张也可以用槽位更多的模板（空槽后补）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerPage(
    onBack: () -> Unit,
    onTemplateSelected: (CollageTemplate) -> Unit
) {
    val context = LocalContext.current
    val templates = remember { TemplateRepository.load(context) }
    val photoCount = EditSession.pickedPhotos.size
    // 与已选张数匹配的分组排最前
    val groups = remember(templates, photoCount) {
        templates.groupBy { it.photoCount }.toSortedMap().entries
            .sortedByDescending { it.key == photoCount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选个模板吧", style = MaterialTheme.typography.headlineSmall) },
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
        if (templates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("模板加载失败，请检查 assets/templates.json")
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groups.forEach { (count, groupTemplates) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = if (count == photoCount) "$count 图 · 推荐" else "$count 图",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(groupTemplates, key = { it.id }) { template ->
                    TemplateCard(template = template, onClick = { onTemplateSelected(template) })
                }
            }
        }
    }
}

/** 模板卡片：用占位色块实时渲染槽位布局 */
@Composable
private fun TemplateCard(template: CollageTemplate, onClick: () -> Unit) {
    val placeholderColors = listOf(SweetPink, Peach, MilkApricot, Mint)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(AppShapes.Card)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(8.dp)
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth
                val h = maxHeight
                template.slots.forEachIndexed { index, slot ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = w * slot.x, y = h * slot.y)
                            .padding(1.5.dp)
                            .size(width = w * slot.w - 3.dp, height = h * slot.h - 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(placeholderColors[index % placeholderColors.size])
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = template.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
