package com.tianqi.camera.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tianqi.camera.ui.theme.AppShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 隐私政策弹窗：内容来自 assets/privacy_policy.md。
 * 首次启动必须同意才能使用；设置页内查看时只读（dismissText 为 null 时隐藏拒绝按钮）。
 */
@Composable
fun PrivacyPolicyDialog(
    onAgree: () -> Unit,
    onDecline: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val policy by produceState<String>(initialValue = "加载中…") {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("privacy_policy.md").bufferedReader().use { it.readText() }
            }.getOrDefault("隐私政策加载失败")
        }
    }
    AlertDialog(
        onDismissRequest = { if (onDecline != null) onDecline() },
        title = { Text("隐私政策") },
        text = {
            Text(
                text = policy,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                shape = AppShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { Text(if (onDecline != null) "同意并继续" else "知道啦", color = Color.White) }
        },
        dismissButton = onDecline?.let { decline ->
            { TextButton(onClick = decline) { Text("暂不使用") } }
        },
        shape = AppShapes.LargeCard,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
